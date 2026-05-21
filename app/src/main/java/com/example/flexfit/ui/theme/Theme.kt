package com.example.flexfit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FluxDarkColorScheme = darkColorScheme(
    primary = FluxAccent,
    onPrimary = FluxBackground,
    primaryContainer = FluxHotPink.copy(alpha = 0.28f),
    onPrimaryContainer = FluxTextPrimary,
    secondary = FluxCyan,
    onSecondary = FluxBackground,
    tertiary = FluxAurora,
    onTertiary = FluxBackground,
    background = FluxBackground,
    onBackground = FluxTextPrimary,
    surface = FluxSurface,
    onSurface = FluxTextPrimary,
    surfaceVariant = FluxSurfaceElevated,
    onSurfaceVariant = FluxTextSecondary,
    outline = FluxStroke,
    outlineVariant = FluxSurfaceSoft,
    error = FluxDanger,
    onError = FluxTextPrimary
)

@Composable
fun FluxFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = FluxDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
