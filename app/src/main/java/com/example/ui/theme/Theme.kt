package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Cyan,
    onPrimary = DarkBg,
    primaryContainer = Color(0xFF003344),
    onPrimaryContainer = Cyan,
    secondary = PurpleSoft,
    onSecondary = DarkBg,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkSubText,
    outline = DarkBorder,
    error = ErrorRed,
    onError = DarkBg
)

private val LightColorScheme = lightColorScheme(
    primary = Blue,
    onPrimary = LightSurface,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = BlueDim,
    secondary = Purple,
    onSecondary = LightSurface,
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightSubText,
    outline = LightBorder,
    error = ErrorRed,
    onError = LightSurface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
