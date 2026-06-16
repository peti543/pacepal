package com.cosmos.orbit

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SpaceBlack = Color(0xFF05030F)
val SpaceNavy = Color(0xFF0B0726)
val SpacePanel = Color(0xFF141033)
val StarBlue = Color(0xFF5C7CFA)
val TextPrimary = Color(0xFFF2F1FA)
val TextSecondary = Color(0xFFA9A6C9)

private val OrbitColors = darkColorScheme(
    primary = StarBlue,
    onPrimary = Color.White,
    background = SpaceBlack,
    onBackground = TextPrimary,
    surface = SpacePanel,
    onSurface = TextPrimary,
    surfaceVariant = SpaceNavy,
    onSurfaceVariant = TextSecondary
)

@Composable
fun OrbitTheme(content: @Composable () -> Unit) {
    // Always dark — it's outer space.
    MaterialTheme(
        colorScheme = OrbitColors,
        content = content
    )
}
