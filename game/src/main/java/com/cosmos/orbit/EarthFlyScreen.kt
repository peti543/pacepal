package com.cosmos.orbit

import android.graphics.Paint
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

private data class SkyStar(val x: Float, val y: Float, val r: Float, val phase: Float)

private data class Continent(val lat: Float, val size: Float, val phase: Float, val alpha: Float)

private val continents = listOf(
    Continent(lat = -0.25f, size = 0.40f, phase = 0.0f, alpha = 0.85f),
    Continent(lat = 0.15f, size = 0.34f, phase = 1.7f, alpha = 0.80f),
    Continent(lat = 0.45f, size = 0.24f, phase = 3.0f, alpha = 0.75f),
    Continent(lat = -0.05f, size = 0.30f, phase = 4.4f, alpha = 0.80f),
    Continent(lat = 0.30f, size = 0.20f, phase = 5.6f, alpha = 0.70f)
)

// Speed value for each gauge slot (slot 0 at the bottom of the stack).
private val SPEED_VALUES = floatArrayOf(0f, 1f, 2f, 3f, 4f)
private const val TRANSITION_SECONDS = 2f
private const val SPEED_UNIT_PX_PER_DP = 70f   // px/sec per speed unit (scaled by density)
private const val TURN_RATE = 2.8f             // radians/sec the ship rotates toward the stick

private val OceanColor = Color(0xFF2C6FD6)
private val LandColor = Color(0xFF3FA86B)
private val ShipColor = Color(0xFFE7ECF7)
private val ShipAccent = Color(0xFF5C7CFA)
private val FlameColor = Color(0xFFFFB454)
private val GaugeActive = Color(0xFF49E06B)
private val GaugeFilled = Color(0xFF2C7D46)
private val GaugeEmpty = Color(0xFF1B2236)
private val GaugeBorder = Color(0xFF44507A)

@Composable
fun EarthFlyScreen(onOpenSystem: () -> Unit) {
    val density = LocalDensity.current
    val speedUnitPx = with(density) { SPEED_UNIT_PX_PER_DP.dp.toPx() }
    val maxRadiusPx = with(density) { 92.dp.toPx() }
    val deadzonePx = with(density) { 8.dp.toPx() }
    val shipSizePx = with(density) { 18.dp.toPx() }
    val brickWPx = with(density) { 50.dp.toPx() }
    val brickHPx = with(density) { 22.dp.toPx() }
    val brickGapPx = with(density) { 7.dp.toPx() }
    val gaugeMarginRightPx = with(density) { 18.dp.toPx() }
    val gaugeMarginBottomPx = with(density) { 40.dp.toPx() }
    val brickLabelPx = with(density) { 12.sp.toPx() }

    val stars = remember {
        val rnd = Random(11)
        List(170) {
            SkyStar(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat() * 1.6f + 0.4f, rnd.nextFloat() * 6.283f)
        }
    }
    val brickPaint = remember { Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER } }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var time by remember { mutableStateOf(0f) }

    // Ship: fixed on screen, the world scrolls around it.
    var shipWorld by remember { mutableStateOf(Offset(0f, 220f)) } // start south of Earth so Earth is in view
    var heading by remember { mutableStateOf(-1.5708f) }           // facing up

    // Joystick.
    var joyActive by remember { mutableStateOf(false) }
    var joyCenter by remember { mutableStateOf(Offset.Zero) }
    var joyThumb by remember { mutableStateOf(Offset.Zero) }

    // Speed gauge.
    var currentSlot by remember { mutableStateOf(0) }
    var targetSlot by remember { mutableStateOf(0) }
    var transitioning by remember { mutableStateOf(false) }
    var transitionStart by remember { mutableStateOf(0f) }
    var transitionFromSpeed by remember { mutableStateOf(0f) }
    var actualSpeed by remember { mutableStateOf(0f) }

    fun brickRects(w: Float, h: Float): List<Rect> {
        val right = w - gaugeMarginRightPx
        val left = right - brickWPx
        return (0 until 5).map { i ->
            val bottom = h - gaugeMarginBottomPx - i * (brickHPx + brickGapPx)
            Rect(left, bottom - brickHPx, right, bottom)
        }
    }

    fun gaugeBounds(w: Float, h: Float): Rect {
        val rects = brickRects(w, h)
        val pad = brickGapPx
        return Rect(
            rects.first().left - pad,
            rects.last().top - pad,
            rects.first().right + pad,
            rects.first().bottom + pad
        )
    }

    fun trySelect(slot: Int) {
        if (transitioning) return
        if (slot < 0 || slot > 4) return
        if (kotlin.math.abs(slot - currentSlot) != 1) return
        targetSlot = slot
        transitionFromSpeed = actualSpeed
        transitionStart = time
        transitioning = true
    }

    // Animation, movement and speed-ramp loop.
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else (now - last) / 1_000_000_000f
                last = now
                time += dt

                // Speed ramp.
                if (transitioning) {
                    val p = ((time - transitionStart) / TRANSITION_SECONDS).coerceIn(0f, 1f)
                    actualSpeed = transitionFromSpeed + (SPEED_VALUES[targetSlot] - transitionFromSpeed) * p
                    if (p >= 1f) {
                        transitioning = false
                        currentSlot = targetSlot
                        actualSpeed = SPEED_VALUES[currentSlot]
                    }
                }

                // Steering: turn the ship toward the stick.
                if (joyActive) {
                    val raw = joyThumb - joyCenter
                    val dist = raw.getDistance()
                    if (dist > deadzonePx) {
                        val desired = atan2(raw.y, raw.x)
                        var delta = desired - heading
                        while (delta > 3.1415927f) delta -= 6.2831855f
                        while (delta < -3.1415927f) delta += 6.2831855f
                        val step = TURN_RATE * dt
                        heading = if (kotlin.math.abs(delta) <= step) desired else heading + kotlin.math.sign(delta) * step
                    }
                }

                // Forward motion (world scrolls; ship stays centred).
                val sp = actualSpeed * speedUnitPx
                if (sp > 0f) {
                    shipWorld = Offset(shipWorld.x + cos(heading) * sp * dt, shipWorld.y + sin(heading) * sp * dt)
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
                        val sz = canvasSize
                        val onGauge = sz.width > 0 &&
                            gaugeBounds(sz.width.toFloat(), sz.height.toFloat()).contains(down.position)

                        if (onGauge) {
                            // Treat as a speed-brick tap.
                            down.consume()
                            val rects = brickRects(sz.width.toFloat(), sz.height.toFloat())
                            val slot = rects.indexOfFirst { it.contains(down.position) }
                            while (true) {
                                val e = awaitPointerEvent()
                                val c = e.changes.firstOrNull { it.id == down.id }
                                if (c == null || !c.pressed) break
                                c.consume()
                            }
                            if (slot >= 0) trySelect(slot)
                        } else {
                            // Joystick.
                            joyActive = true
                            joyCenter = down.position
                            joyThumb = down.position
                            down.consume()
                            while (true) {
                                val e = awaitPointerEvent()
                                val c = e.changes.firstOrNull { it.id == down.id }
                                if (c == null || !c.pressed) break
                                joyThumb = c.position
                                c.consume()
                            }
                            joyActive = false
                        }
                    }
                }
        ) {
            if (canvasSize.width != size.width.toInt() || canvasSize.height != size.height.toInt()) {
                canvasSize = IntSize(size.width.toInt(), size.height.toInt())
            }
            val t = time
            val screenCenter = Offset(size.width / 2f, size.height / 2f)

            // Parallax starfield that wraps as the ship flies.
            val offX = shipWorld.x * 0.6f / size.width
            val offY = shipWorld.y * 0.6f / size.height
            for (s in stars) {
                val a = (0.45f + 0.45f * (0.5f + 0.5f * sin(t * 0.6f + s.phase))).coerceIn(0f, 1f)
                val nx = frac(s.x - offX)
                val ny = frac(s.y - offY)
                drawCircle(Color.White.copy(alpha = a), s.r, Offset(nx * size.width, ny * size.height))
            }

            // Earth, scrolling relative to the ship's camera.
            val earthCenter = screenCenter - shipWorld
            val earthR = size.minDimension * 0.24f
            drawEarth(earthCenter, earthR, t * 0.35f)

            // Ship: always centred, pointing along its heading.
            drawShip(screenCenter, heading, shipSizePx, actualSpeed > 0.01f)

            // Joystick.
            if (joyActive) {
                val raw = joyThumb - joyCenter
                val dist = raw.getDistance()
                val thumb = if (dist > maxRadiusPx) joyCenter + raw / dist * maxRadiusPx else joyThumb
                drawCircle(Color.White.copy(alpha = 0.06f), maxRadiusPx, joyCenter)
                drawCircle(Color.White.copy(alpha = 0.18f), maxRadiusPx, joyCenter, style = Stroke(width = 2f))
                drawCircle(StarBlue.copy(alpha = 0.55f), maxRadiusPx * 0.42f, thumb)
            }

            // Speed gauge (bottom-right).
            val rects = brickRects(size.width, size.height)
            val activeSlot = if (transitioning) targetSlot else currentSlot
            val flash = 0.5f + 0.5f * sin(t * 9f)
            rects.forEachIndexed { i, r ->
                val filled = i <= activeSlot
                val isActive = i == activeSlot
                val fill = when {
                    isActive && transitioning -> GaugeActive.copy(alpha = 0.35f + 0.65f * flash)
                    isActive -> GaugeActive
                    filled -> GaugeFilled
                    else -> GaugeEmpty
                }
                drawRoundedBrick(r, fill, GaugeBorder)
                brickPaint.textSize = brickLabelPx
                brickPaint.color = (if (filled) Color.White else TextSecondary).toArgb()
                drawContext.canvas.nativeCanvas.drawText(
                    SPEED_VALUES[i].toInt().toString(),
                    r.center.x,
                    r.center.y + brickLabelPx * 0.36f,
                    brickPaint
                )
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
            Text("Steer with the stick, set speed on the right", color = TextSecondary, fontSize = 13.sp)
        }

        Text(
            text = "Drag to turn  ·  Tap a neighbouring brick to change speed",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        )

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

private fun frac(v: Float): Float = v - floor(v)

private fun DrawScope.drawRoundedBrick(r: Rect, fill: Color, border: Color) {
    val corner = androidx.compose.ui.geometry.CornerRadius(r.height * 0.28f)
    drawRoundRect(color = fill, topLeft = r.topLeft, size = r.size, cornerRadius = corner)
    drawRoundRect(
        color = border,
        topLeft = r.topLeft,
        size = r.size,
        cornerRadius = corner,
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawEarth(c: Offset, r: Float, rot: Float) {
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
        if (thrusting) {
            val flame = Path().apply {
                moveTo(pos.x - s * 0.7f, pos.y - s * 0.32f)
                lineTo(pos.x - s * 1.5f, pos.y)
                lineTo(pos.x - s * 0.7f, pos.y + s * 0.32f)
                close()
            }
            drawPath(flame, FlameColor.copy(alpha = 0.9f))
        }
        val hull = Path().apply {
            moveTo(pos.x + s, pos.y)
            lineTo(pos.x - s * 0.7f, pos.y - s * 0.62f)
            lineTo(pos.x - s * 0.4f, pos.y)
            lineTo(pos.x - s * 0.7f, pos.y + s * 0.62f)
            close()
        }
        drawPath(hull, ShipColor)
        drawCircle(ShipAccent, s * 0.26f, Offset(pos.x + s * 0.12f, pos.y))
    }
}
