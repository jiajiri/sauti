package com.sauti.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LocalAiDarkColorScheme = darkColorScheme(
    primary = AccentEmerald,
    secondary = AccentEmeraldOrb,
    background = BackgroundMatte,
    surface = SurfaceCard,
    surfaceVariant = SurfaceInput,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle
)

@Composable
fun SautiThemeLegacy(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LocalAiDarkColorScheme,
        content = content
    )
}
