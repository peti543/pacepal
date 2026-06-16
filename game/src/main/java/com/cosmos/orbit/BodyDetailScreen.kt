package com.cosmos.orbit

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private data class Blob(val lat: Float, val size: Float, val phase: Float, val alpha: Float)

private val featureBlobs = listOf(
    Blob(lat = -0.30f, size = 0.34f, phase = 0.0f, alpha = 0.45f),
    Blob(lat = 0.12f, size = 0.42f, phase = 2.1f, alpha = 0.40f),
    Blob(lat = 0.40f, size = 0.26f, phase = 4.0f, alpha = 0.38f),
    Blob(lat = -0.05f, size = 0.22f, phase = 5.3f, alpha = 0.35f)
)

@Composable
fun BodyDetailScreen(body: CelestialBody, onBack: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "spin")
    val spinDuration = (9000f / body.spinSpeed).toInt().coerceIn(2500, 30000)
    val rot by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = spinDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Back to system",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }

        // Spinning planet.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(260.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension * 0.34f
                drawSpinningBody(c, r, body, rot)
            }
        }

        Text(
            text = body.name,
            color = TextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))

        // Type chip.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(SpacePanel, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(body.color, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = body.type.label,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Facts.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(SpacePanel, RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            body.facts.forEachIndexed { index, fact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = fact.label, color = TextSecondary, fontSize = 14.sp)
                    Text(
                        text = fact.value,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End
                    )
                }
                if (index != body.facts.lastIndex) {
                    Divider(color = SpaceNavy)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = body.description,
            color = TextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(40.dp))
    }
}

private fun DrawScope.drawSpinningBody(c: Offset, r: Float, body: CelestialBody, rot: Float) {
    val isStar = body.type == BodyType.STAR

    // Outer glow / corona.
    val glowColor = if (isStar) Color(0x88FFD24D) else body.color.copy(alpha = 0.22f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glowColor, Color.Transparent),
            center = c,
            radius = r * (if (isStar) 2.0f else 1.5f)
        ),
        radius = r * (if (isStar) 2.0f else 1.5f),
        center = c
    )

    // Back half of rings.
    if (body.hasRings) drawDetailRing(c, r, body.ringColor, frontOnly = false)

    val circle = Path().apply {
        addOval(Rect(c.x - r, c.y - r, c.x + r, c.y + r))
    }
    clipPath(circle) {
        // Base sphere.
        drawCircle(color = body.color, radius = r, center = c)

        // Latitudinal bands for the giants.
        if (body.type == BodyType.GAS_GIANT || body.type == BodyType.ICE_GIANT) {
            for (i in -2..2) {
                val y = c.y + i * r * 0.32f
                drawRect(
                    color = body.bandColor.copy(alpha = 0.28f),
                    topLeft = Offset(c.x - r, y - r * 0.07f),
                    size = Size(r * 2f, r * 0.14f)
                )
            }
        }

        // Rotating surface features.
        for (b in featureBlobs) {
            val ang = rot + b.phase
            val depth = cos(ang) // >0 => front-facing
            if (depth <= 0.04f) continue
            val latCos = cos(b.lat)
            val x = c.x + sin(ang) * r * 0.86f * latCos
            val y = c.y + b.lat * r * 0.86f
            val br = b.size * r * (0.45f + 0.55f * depth)
            val featureColor = if (isStar) {
                Color(0xFFFFB300).copy(alpha = b.alpha * depth)
            } else {
                body.bandColor.copy(alpha = b.alpha * depth)
            }
            drawCircle(color = featureColor, radius = br, center = Offset(x, y))
        }

        // Sphere shading: lit top-left, shadowed bottom-right.
        if (!isStar) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(c.x - r * 0.4f, c.y - r * 0.4f),
                    radius = r * 1.25f
                ),
                radius = r,
                center = c
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                    center = Offset(c.x + r * 0.5f, c.y + r * 0.5f),
                    radius = r * 1.45f
                ),
                radius = r,
                center = c
            )
        } else {
            // Bright stellar core.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF7D6), Color.Transparent),
                    center = c,
                    radius = r
                ),
                radius = r,
                center = c
            )
        }
    }

    // Front half of rings, drawn over the planet.
    if (body.hasRings) drawDetailRing(c, r, body.ringColor, frontOnly = true)
}

private fun DrawScope.drawDetailRing(c: Offset, r: Float, color: Color, frontOnly: Boolean) {
    val flatten = 0.34f
    val radii = listOf(r * 2.15f, r * 1.75f, r * 1.45f)
    for (rr in radii) {
        val topLeft = Offset(c.x - rr, c.y - rr * flatten)
        val size = Size(rr * 2f, rr * 2f * flatten)
        val stroke = Stroke(width = (r * 0.13f).coerceAtLeast(2f))
        if (frontOnly) {
            // Bottom half only (in front of the planet).
            drawArc(
                color = color.copy(alpha = 0.85f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = stroke
            )
        } else {
            drawArc(
                color = color.copy(alpha = 0.55f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = stroke
            )
        }
    }
}
