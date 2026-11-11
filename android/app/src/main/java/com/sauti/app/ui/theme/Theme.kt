package com.sauti.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SovereignDarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryBlueMuted,
    onPrimaryContainer = TextPrimary,
    secondary = AccentTeal,
    onSecondary = TextPrimary,
    background = BackgroundMatte,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderActive,
    error = StatusCritical,
    onError = TextPrimary
)

@Composable
fun SautiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SovereignDarkColorScheme,
        typography = SautiTypography,
        content = content
    )
}

@Composable
fun SautiThemeLegacy(content: @Composable () -> Unit) = SautiTheme(content)
