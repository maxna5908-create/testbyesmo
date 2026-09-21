package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ColorPowerOn,
    secondary = ColorPowerOff,
    tertiary = ColorTagline,
    background = ColorSystemBackground,
    surface = ColorSystemBackground,
    onBackground = Color(0xFFF7F7F7),
    onSurface = Color(0xFFF7F7F7)
)

private val LightColorScheme = lightColorScheme(
    primary = ColorPowerOn,
    secondary = ColorPowerOff,
    tertiary = ColorTagline,
    background = ColorSystemBackground,
    surface = ColorSystemBackground,
    onBackground = Color(0xFFF7F7F7),
    onSurface = Color(0xFFF7F7F7)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
