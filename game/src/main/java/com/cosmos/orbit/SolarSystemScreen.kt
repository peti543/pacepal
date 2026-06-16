package com.cosmos.orbit

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val ORBIT_TIME_SCALE = 0.10f

private data class Star(val x: Float, val y: Float, val r: Float, val phase: Float)

@Composable
fun SolarSystemScreen(onBodySelected: (String) -> Unit) {
    val density = LocalDensity.current
    val touchSlopPx = with(density) { 26.dp.toPx() }
    val labelTextPx = with(density) { 12.sp.toPx() }

    // Static starfield (normalised 0..1 coordinates, multiplied by canvas size).
    val stars = remember {
        val rnd = java.util.Random(7L)
        List(140) {
            Star(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                r = rnd.nextFloat() * 1.6f + 0.4f,
                phase = rnd.nextFloat() * 6.283f
            )
        }
    }
    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }

    // Transform state.
    val scale = remember { mutableStateOf(1f) }
    val offset = remember { mutableStateOf(Offset.Zero) }
    val fitScale = remember { mutableStateOf(1f) }
    val center = remember { mutableStateOf(Offset.Zero) }
    val initialized = remember { mutableStateOf(false) }

    // Monotonic clock (seconds) driving orbital motion — never resets, so no snapping.
    val time = remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) time.value += (now - last) / 1_000_000_000f
                last = now
            }
        }
    }

    fun worldToScreen(world: Offset): Offset =
        Offset(offset.value.x + world.x * scale.value, offset.value.y + world.y * scale.value)

    fun bodyWorld(body: CelestialBody): Offset {
        if (body.orbitRadius == 0f) return Offset.Zero
        val angle = body.startAngle + time.value * body.orbitSpeed * ORBIT_TIME_SCALE
        return Offset(cos(angle) * body.orbitRadius, sin(angle) * body.orbitRadius)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { sz ->
                    if (!initialized.value && sz.width > 0 && sz.height > 0) {
                        val minDim = min(sz.width, sz.height).toFloat()
                        val fit = (minDim * 0.92f) / (2f * OrbitData.maxOrbitRadius)
                        val c = Offset(sz.width / 2f, sz.height / 2f)
                        fitScale.value = fit
                        scale.value = fit
                        center.value = c
                        offset.value = c
                        initialized.value = true
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newScale = (scale.value * zoom)
                            .coerceIn(fitScale.value * 0.45f, fitScale.value * 9f)
                        val zoomFactor = if (scale.value == 0f) 1f else newScale / scale.value
                        offset.value = centroid + (offset.value - centroid) * zoomFactor + pan
                        scale.value = newScale
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        var bestId: String? = null
                        var bestDist = Float.MAX_VALUE
                        for (body in OrbitData.bodies) {
                            val pos = worldToScreen(bodyWorld(body))
                            val dist = (tap - pos).getDistance()
                            val hitR = maxOf(body.displayRadius * scale.value, touchSlopPx)
                            if (dist <= hitR && dist < bestDist) {
                                bestDist = dist
                                bestId = body.id
                            }
                        }
                        bestId?.let(onBodySelected)
                    }
                }
        ) {
            val t = time.value
            // Starfield with gentle twinkle.
            for (s in stars) {
                val a = (0.45f + 0.45f * (0.5f + 0.5f * sin(t * 0.6f + s.phase))).coerceIn(0f, 1f)
                drawCircle(
                    color = Color.White.copy(alpha = a),
                    radius = s.r,
                    center = Offset(s.x * size.width, s.y * size.height)
                )
            }

            val sun = OrbitData.bodies.first()
            val sunScreen = worldToScreen(Offset.Zero)

            // Orbit paths.
            for (body in OrbitData.bodies) {
                if (body.orbitRadius == 0f) continue
                drawCircle(
                    color = StarBlue.copy(alpha = 0.10f),
                    radius = body.orbitRadius * scale.value,
                    center = sunScreen,
                    style = Stroke(width = 1f)
                )
            }

            // Sun glow + core.
            val sunR = sun.displayRadius * scale.value
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x66FFD24D), Color(0x00FFD24D)),
                    center = sunScreen,
                    radius = sunR * 3.0f
                ),
                radius = sunR * 3.0f,
                center = sunScreen
            )
            drawCircle(color = sun.color, radius = sunR, center = sunScreen)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF4C2), Color(0x00FFD24D)),
                    center = sunScreen,
                    radius = sunR
                ),
                radius = sunR,
                center = sunScreen
            )

            // Planets.
            for (body in OrbitData.bodies) {
                if (body.orbitRadius == 0f) continue
                val pos = worldToScreen(bodyWorld(body))
                val r = (body.displayRadius * scale.value).coerceAtLeast(2.5f)

                if (body.hasRings) drawRings(pos, r, body.ringColor)

                drawCircle(color = body.color, radius = r, center = pos)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(pos.x - r * 0.35f, pos.y - r * 0.35f),
                        radius = r * 1.2f
                    ),
                    radius = r,
                    center = pos
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f)),
                        center = Offset(pos.x + r * 0.45f, pos.y + r * 0.45f),
                        radius = r * 1.4f
                    ),
                    radius = r,
                    center = pos
                )

                // Label, fading in as the planet grows on screen.
                if (r > 4f) {
                    val alpha = ((r - 4f) / 10f).coerceIn(0.25f, 0.9f)
                    labelPaint.textSize = labelTextPx
                    labelPaint.color = Color.White.copy(alpha = alpha).toArgb()
                    drawContext.canvas.nativeCanvas.drawText(
                        body.name,
                        pos.x,
                        pos.y + r + labelTextPx * 1.4f,
                        labelPaint
                    )
                }
            }
        }

        // Header overlay.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Solar System",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "An interactive tour of our cosmic neighbourhood",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // Hint overlay.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Drag to pan  ·  Pinch to zoom  ·  Tap a planet",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        FilledTonalIconButton(
            onClick = {
                if (initialized.value) {
                    scale.value = fitScale.value
                    offset.value = center.value
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .systemBarsPadding()
                .padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.FilterCenterFocus,
                contentDescription = "Recenter view"
            )
        }
    }
}

/** Draws Saturn/Uranus-style rings as a flattened ellipse around the planet. */
private fun DrawScope.drawRings(center: Offset, planetR: Float, color: Color) {
    val flatten = 0.38f
    val radii = listOf(planetR * 2.2f, planetR * 1.8f, planetR * 1.45f)
    for (rr in radii) {
        drawOval(
            color = color.copy(alpha = 0.5f),
            topLeft = Offset(center.x - rr, center.y - rr * flatten),
            size = androidx.compose.ui.geometry.Size(rr * 2f, rr * 2f * flatten),
            style = Stroke(width = (planetR * 0.18f).coerceAtLeast(1f))
        )
    }
}
