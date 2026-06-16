package com.cosmos.orbit

import android.graphics.Paint
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
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

// A body floating in the world. Radius is a fraction of the screen's smaller
// side (constant on-screen size, since there is no zoom).
private data class FlyBody(
    val name: String,
    val world: Offset,
    val radiusFactor: Float,
    val base: Color,
    val feature: Color,
    val lockSpeed: Float,    // dp/sec you must reach to escape this planet's pull
    val fieldFactor: Float   // gravity field radius = visual radius * fieldFactor
)

private val MarsBase = Color(0xFFC1502E)
private val MarsFeature = Color(0xFF7E2F1A)
private val EscapeAmber = Color(0xFFFFC061)

// Gravity / orbit-capture tuning.
private const val GRAVITY_DRIFT_DP = 18f      // orbital drift speed even at gauge speed 0
private const val ORBIT_SETTLE = 1.6f         // how quickly the orbit radius settles
private const val CAPTURE_ORBIT_FACTOR = 1.5f // parked orbit radius = visual radius * this
private const val MIN_ORBIT_FACTOR = 1.12f    // never orbit closer than this

// Speed (in dp/sec) for each gauge slot; slot 0 is the bottom of the stack.
private val SPEED_VALUES = floatArrayOf(0f, 30f, 60f, 120f, 240f)
private const val TRANSITION_SECONDS = 3f
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
    // Forward speed is expressed in dp/sec, so one unit is one dp.
    val speedUnitPx = with(density) { 1.dp.toPx() }
    val maxRadiusPx = with(density) { 92.dp.toPx() }
    val deadzonePx = with(density) { 8.dp.toPx() }
    val shipSizePx = with(density) { 18.dp.toPx() }
    val brickWPx = with(density) { 64.dp.toPx() }
    val brickHPx = with(density) { 30.dp.toPx() }
    val brickGapPx = with(density) { 8.dp.toPx() }
    val gaugeMarginRightPx = with(density) { 18.dp.toPx() }
    val gaugeMarginBottomPx = with(density) { 40.dp.toPx() }
    val brickLabelPx = with(density) { 14.sp.toPx() }
    val edgeMarginPx = with(density) { 34.dp.toPx() }

    val flyBodies = remember {
        listOf(
            FlyBody("Earth", Offset(0f, 0f), 0.24f, OceanColor, LandColor, lockSpeed = 90f, fieldFactor = 2.3f),
            FlyBody("Mars", Offset(720f, -380f), 0.12f, MarsBase, MarsFeature, lockSpeed = 45f, fieldFactor = 3.0f)
        )
    }

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

    // Gravity capture: index of the planet we are orbit-locked to, or -1.
    var capturedIndex by remember { mutableStateOf(-1) }
    var orbitAngle by remember { mutableStateOf(0f) }
    var orbitDir by remember { mutableStateOf(1f) }
    var orbitRadius by remember { mutableStateOf(0f) }

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

                val sz = canvasSize
                val minDim = minOf(sz.width, sz.height).toFloat()

                if (minDim <= 0f) {
                    // Wait for layout.
                } else if (capturedIndex >= 0) {
                    // Orbit-locked: circle the planet until fast enough to escape.
                    val b = flyBodies[capturedIndex]
                    if (actualSpeed >= b.lockSpeed) {
                        heading = orbitAngle + orbitDir * 1.5707964f // fly off along the tangent
                        capturedIndex = -1
                        val sp = actualSpeed * speedUnitPx
                        shipWorld = Offset(shipWorld.x + cos(heading) * sp * dt, shipWorld.y + sin(heading) * sp * dt)
                    } else {
                        val vR = minDim * b.radiusFactor
                        val capR = vR * CAPTURE_ORBIT_FACTOR
                        orbitRadius += (capR - orbitRadius) * minOf(1f, dt * ORBIT_SETTLE)
                        val tangentialPx = maxOf(actualSpeed, GRAVITY_DRIFT_DP) * speedUnitPx
                        orbitAngle += orbitDir * (tangentialPx / orbitRadius) * dt
                        shipWorld = b.world + Offset(cos(orbitAngle), sin(orbitAngle)) * orbitRadius
                        heading = orbitAngle + orbitDir * 1.5707964f
                    }
                } else {
                    // Free flight: steer with the stick, then move forward.
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
                    val sp = actualSpeed * speedUnitPx
                    if (sp > 0f) {
                        shipWorld = Offset(shipWorld.x + cos(heading) * sp * dt, shipWorld.y + sin(heading) * sp * dt)
                    }
                    // Capture if drifting too slowly through a planet's gravity field.
                    var capi = -1
                    var capBest = Float.MAX_VALUE
                    for (i in flyBodies.indices) {
                        val b = flyBodies[i]
                        val field = minDim * b.radiusFactor * b.fieldFactor
                        val d = (shipWorld - b.world).getDistance()
                        if (d < field && actualSpeed < b.lockSpeed && d < capBest) {
                            capBest = d
                            capi = i
                        }
                    }
                    if (capi >= 0) {
                        val b = flyBodies[capi]
                        val vR = minDim * b.radiusFactor
                        val field = vR * b.fieldFactor
                        val rel = shipWorld - b.world
                        orbitRadius = rel.getDistance().coerceIn(vR * MIN_ORBIT_FACTOR, field)
                        orbitAngle = atan2(rel.y, rel.x)
                        val vel = Offset(cos(heading), sin(heading))
                        orbitDir = if (rel.x * vel.y - rel.y * vel.x >= 0f) 1f else -1f
                        capturedIndex = capi
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
                    // Multi-touch: one finger drives the joystick while another
                    // can tap the speed gauge at the same time.
                    awaitPointerEventScope {
                        var joyId: PointerId? = null
                        var gaugeId: PointerId? = null
                        while (true) {
                            val event = awaitPointerEvent()
                            val w = canvasSize.width.toFloat()
                            val h = canvasSize.height.toFloat()
                            for (c in event.changes) {
                                when {
                                    c.changedToDown() -> {
                                        val onGauge = w > 0f && gaugeBounds(w, h).contains(c.position)
                                        if (onGauge && gaugeId == null) {
                                            gaugeId = c.id
                                            c.consume()
                                        } else if (!onGauge && joyId == null) {
                                            joyId = c.id
                                            joyActive = true
                                            joyCenter = c.position
                                            joyThumb = c.position
                                            c.consume()
                                        }
                                    }
                                    c.changedToUp() || !c.pressed -> {
                                        when (c.id) {
                                            joyId -> {
                                                joyActive = false
                                                joyId = null
                                                c.consume()
                                            }
                                            gaugeId -> {
                                                val slot = brickRects(w, h).indexOfFirst { it.contains(c.position) }
                                                if (slot >= 0) trySelect(slot)
                                                gaugeId = null
                                                c.consume()
                                            }
                                        }
                                    }
                                    else -> {
                                        if (c.id == joyId) {
                                            joyThumb = c.position
                                            c.consume()
                                        }
                                    }
                                }
                            }
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

            // Bodies (Earth, Mars), scrolling relative to the ship's camera.
            flyBodies.forEachIndexed { i, b ->
                val bc = screenCenter + (b.world - shipWorld)
                val br = size.minDimension * b.radiusFactor
                val field = br * b.fieldFactor
                val captured = i == capturedIndex
                val ringAlpha = if (captured) (0.30f + 0.20f * sin(t * 4f)).coerceIn(0f, 0.5f) else 0.12f
                drawCircle(
                    color = b.base.copy(alpha = ringAlpha),
                    radius = field,
                    center = bc,
                    style = Stroke(width = if (captured) 3f else 1.5f)
                )
                drawPlanet(bc, br, t * 0.35f, b.base, b.feature)
            }

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

            // Edge pointers for off-screen bodies.
            for (b in flyBodies) {
                val bc = screenCenter + (b.world - shipWorld)
                drawEdgePointer(screenCenter, bc, b.base, edgeMarginPx)
            }

            // Speed gauge (bottom-right).
            val rects = brickRects(size.width, size.height)
            val activeSlot = if (transitioning) targetSlot else currentSlot
            val flash = 0.5f + 0.5f * sin(t * 9f)
            val escapeLock = if (capturedIndex >= 0) flyBodies[capturedIndex].lockSpeed else -1f
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
                // While orbit-locked, mark the speeds that let you escape.
                if (escapeLock >= 0f && SPEED_VALUES[i] >= escapeLock) {
                    drawRoundRect(
                        color = EscapeAmber.copy(alpha = 0.55f + 0.45f * flash),
                        topLeft = r.topLeft,
                        size = r.size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r.height * 0.28f),
                        style = Stroke(width = 3f)
                    )
                }
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

private fun DrawScope.drawPlanet(c: Offset, r: Float, rot: Float, base: Color, feature: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(base.copy(alpha = 0.35f), Color.Transparent),
            center = c,
            radius = r * 1.6f
        ),
        radius = r * 1.6f,
        center = c
    )
    val circle = Path().apply { addOval(Rect(c.x - r, c.y - r, c.x + r, c.y + r)) }
    clipPath(circle) {
        drawCircle(base, r, c)
        for (land in continents) {
            val ang = rot + land.phase
            val depth = cos(ang)
            if (depth <= 0.04f) continue
            val latCos = cos(land.lat)
            val x = c.x + sin(ang) * r * 0.84f * latCos
            val y = c.y + land.lat * r * 0.84f
            val rr = land.size * r * (0.45f + 0.55f * depth)
            drawCircle(feature.copy(alpha = land.alpha * depth), rr, Offset(x, y))
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

/** When [target] is off-screen, draws a small colored marker + arrow at the
 *  screen edge pointing toward it. Does nothing while the target is visible. */
private fun DrawScope.drawEdgePointer(center: Offset, target: Offset, color: Color, margin: Float) {
    val halfW = size.width / 2f - margin
    val halfH = size.height / 2f - margin
    val d = target - center
    if (kotlin.math.abs(d.x) <= halfW && kotlin.math.abs(d.y) <= halfH) return // visible

    val absx = kotlin.math.abs(d.x)
    val absy = kotlin.math.abs(d.y)
    val sx = if (absx > 1e-3f) halfW / absx else Float.MAX_VALUE
    val sy = if (absy > 1e-3f) halfH / absy else Float.MAX_VALUE
    val edge = center + d * minOf(sx, sy)
    val ang = atan2(d.y, d.x)

    val dot = margin * 0.22f
    drawCircle(color, dot, edge)
    drawCircle(Color.White.copy(alpha = 0.85f), dot, edge, style = Stroke(width = 2f))

    rotate(degrees = ang * 180f / 3.1415927f, pivot = edge) {
        val a = margin * 0.34f
        val arrow = Path().apply {
            moveTo(edge.x + dot + a, edge.y)
            lineTo(edge.x + dot + a * 0.2f, edge.y - a * 0.55f)
            lineTo(edge.x + dot + a * 0.2f, edge.y + a * 0.55f)
            close()
        }
        drawPath(arrow, color)
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
