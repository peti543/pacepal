package com.cosmos.orbit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Star(val x: Float, val y: Float, val r: Float, val phase: Float)

// Green continents that rotate across Earth's surface.
private data class Continent(val lat: Float, val size: Float, val phase: Float, val alpha: Float)

private val continents = listOf(
    Continent(lat = -0.25f, size = 0.40f, phase = 0.0f, alpha = 0.85f),
    Continent(lat = 0.15f, size = 0.34f, phase = 1.7f, alpha = 0.80f),
    Continent(lat = 0.45f, size = 0.24f, phase = 3.0f, alpha = 0.75f),
    Continent(lat = -0.05f, size = 0.30f, phase = 4.4f, alpha = 0.80f),
    Continent(lat = 0.30f, size = 0.20f, phase = 5.6f, alpha = 0.70f)
)

private val OceanColor = Color(0xFF2C6FD6)
private val LandColor = Color(0xFF3FA86B)
private val ShipColor = Color(0xFFE7ECF7)
private val ShipAccent = Color(0xFF5C7CFA)
private val FlameColor = Color(0xFFFFB454)

@Composable
fun EarthFlyScreen(onOpenSystem: () -> Unit) {
    val density = LocalDensity.current
    val speedPx = with(density) { 320.dp.toPx() }      // ship speed at full tilt (px/sec)
    val maxRadiusPx = with(density) { 92.dp.toPx() }    // joystick travel
    val deadzonePx = with(density) { 8.dp.toPx() }
    val shipSizePx = with(density) { 17.dp.toPx() }
    val edgeMarginPx = with(density) { 14.dp.toPx() }

    val stars = remember {
        val rnd = Random(11)
        List(150) {
            Star(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat() * 1.6f + 0.4f, rnd.nextFloat() * 6.283f)
        }
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var time by remember { mutableStateOf(0f) }

    var shipPos by remember { mutableStateOf(Offset.Zero) }
    var shipAngle by remember { mutableStateOf(-1.5708f) } // facing up initially
    var shipInit by remember { mutableStateOf(false) }

    // Archero-style floating joystick.
    var joyActive by remember { mutableStateOf(false) }
    var joyCenter by remember { mutableStateOf(Offset.Zero) }
    var joyThumb by remember { mutableStateOf(Offset.Zero) }

    // Animation + movement loop.
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else (now - last) / 1_000_000_000f
                last = now
                time += dt

                val sz = canvasSize
                if (sz.width > 0 && sz.height > 0) {
                    if (!shipInit) {
                        shipPos = Offset(sz.width / 2f, sz.height * 0.72f)
                        shipInit = true
                    }
                    if (joyActive) {
                        val raw = joyThumb - joyCenter
                        val dist = raw.getDistance()
                        if (dist > deadzonePx) {
                            val mag = (dist / maxRadiusPx).coerceAtMost(1f)
                            val dir = raw / dist
                            shipPos = Offset(
                                (shipPos.x + dir.x * speedPx * mag * dt)
                                    .coerceIn(edgeMarginPx, sz.width - edgeMarginPx),
                                (shipPos.y + dir.y * speedPx * mag * dt)
                                    .coerceIn(edgeMarginPx, sz.height - edgeMarginPx)
                            )
                            shipAngle = atan2(dir.y, dir.x)
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        joyActive = true
                        joyCenter = down.position
                        joyThumb = down.position
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) break
                            joyThumb = change.position
                            change.consume()
                        }
                        joyActive = false
                    }
                }
        ) {
            if (canvasSize.width != size.width.toInt() || canvasSize.height != size.height.toInt()) {
                canvasSize = IntSize(size.width.toInt(), size.height.toInt())
            }
            val t = time

            // Starfield.
            for (s in stars) {
                val a = (0.45f + 0.45f * (0.5f + 0.5f * sin(t * 0.6f + s.phase))).coerceIn(0f, 1f)
                drawCircle(Color.White.copy(alpha = a), s.r, Offset(s.x * size.width, s.y * size.height))
            }

            // Earth, centred and slowly spinning.
            val earthCenter = Offset(size.width / 2f, size.height * 0.42f)
            val earthR = size.minDimension * 0.30f
            drawEarth(earthCenter, earthR, t * 0.35f)

            // Spaceship.
            if (shipInit) drawShip(shipPos, shipAngle, shipSizePx, joyActive)

            // Joystick.
            if (joyActive) {
                val raw = joyThumb - joyCenter
                val dist = raw.getDistance()
                val thumb = if (dist > maxRadiusPx) joyCenter + raw / dist * maxRadiusPx else joyThumb
                drawCircle(Color.White.copy(alpha = 0.06f), maxRadiusPx, joyCenter)
                drawCircle(Color.White.copy(alpha = 0.18f), maxRadiusPx, joyCenter, style = Stroke(width = 2f))
                drawCircle(StarBlue.copy(alpha = 0.55f), maxRadiusPx * 0.42f, thumb)
            }
        }

        // Header.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Earth", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("Pilot your ship around the planet", color = TextSecondary, fontSize = 13.sp)
        }

        Text(
            text = "Touch and drag anywhere to fly",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        )

        // Open the full solar-system explorer.
        FilledTonalIconButton(
            onClick = onOpenSystem,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .systemBarsPadding()
                .padding(16.dp)
        ) {
            Icon(imageVector = Icons.Filled.Public, contentDescription = "Open solar system")
        }
    }
}

private fun DrawScope.drawEarth(c: Offset, r: Float, rot: Float) {
    // Soft glow.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(OceanColor.copy(alpha = 0.35f), Color.Transparent),
            center = c,
            radius = r * 1.6f
        ),
        radius = r * 1.6f,
        center = c
    )

    val circle = Path().apply { addOval(Rect(c.x - r, c.y - r, c.x + r, c.y + r)) }
    clipPath(circle) {
        drawCircle(OceanColor, r, c)
        for (land in continents) {
            val ang = rot + land.phase
            val depth = cos(ang)
            if (depth <= 0.04f) continue
            val latCos = cos(land.lat)
            val x = c.x + sin(ang) * r * 0.84f * latCos
            val y = c.y + land.lat * r * 0.84f
            val rr = land.size * r * (0.45f + 0.55f * depth)
            drawCircle(LandColor.copy(alpha = land.alpha * depth), rr, Offset(x, y))
        }
        // Day/night shading.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(c.x - r * 0.4f, c.y - r * 0.4f),
                radius = r * 1.25f
            ),
            radius = r, center = c
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                center = Offset(c.x + r * 0.5f, c.y + r * 0.5f),
                radius = r * 1.45f
            ),
            radius = r, center = c
        )
    }
}

private fun DrawScope.drawShip(pos: Offset, angleRad: Float, s: Float, thrusting: Boolean) {
    val degrees = angleRad * 180f / 3.1415927f
    rotate(degrees = degrees, pivot = pos) {
        // Engine flame when moving (points opposite the nose, i.e. -x).
        if (thrusting) {
            val flame = Path().apply {
                moveTo(pos.x - s * 0.7f, pos.y - s * 0.32f)
                lineTo(pos.x - s * 1.5f, pos.y)
                lineTo(pos.x - s * 0.7f, pos.y + s * 0.32f)
                close()
            }
            drawPath(flame, FlameColor.copy(alpha = 0.9f))
        }
        // Hull: a triangle pointing along +x.
        val hull = Path().apply {
            moveTo(pos.x + s, pos.y)
            lineTo(pos.x - s * 0.7f, pos.y - s * 0.62f)
            lineTo(pos.x - s * 0.4f, pos.y)
            lineTo(pos.x - s * 0.7f, pos.y + s * 0.62f)
            close()
        }
        drawPath(hull, ShipColor)
        // Cockpit.
        drawCircle(ShipAccent, s * 0.26f, Offset(pos.x + s * 0.12f, pos.y))
    }
}
