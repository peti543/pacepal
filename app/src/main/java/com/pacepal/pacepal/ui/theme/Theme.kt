package com.pacepal.pacepal.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PacePalColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = OnAmber,
    primaryContainer = AmberDark,
    onPrimaryContainer = Color.White,
    secondary = AmberLight,
    onSecondary = OnAmber,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF3A3A5C),
    outlineVariant = Color(0xFF2A2A44),
)

@Composable
fun PacePalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PacePalColorScheme,
        typography = Typography(),
        content = content
    )
}
