package com.campus.todo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF141A24),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEFEADF),
    onPrimaryContainer = Color(0xFF232321),
    secondary = Color(0xFFF1C94A),
    onSecondary = Color.White,
    tertiary = Color(0xFF8BA6A0),
    background = Color(0xFFFBF9F3),
    surface = Color(0xFFFFFEFC),
    surfaceVariant = Color(0xFFF0ECE1),
    onSurface = Color(0xFF1C1C1A),
    onSurfaceVariant = Color(0xFF67665F),
    outline = Color(0xFFD6D0C3)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FC4D2),
    onPrimary = Color(0xFF103038),
    primaryContainer = Color(0xFF2E5360),
    onPrimaryContainer = Color(0xFFD6EEF4),
    secondary = Color(0xFFB0CBD6),
    onSecondary = Color(0xFF1B2F37),
    tertiary = Color(0xFFB2CCBF),
    background = Color(0xFF12181B),
    surface = Color(0xFF12181B),
    surfaceVariant = Color(0xFF2A3439),
    onSurface = Color(0xFFE1E9EC),
    onSurfaceVariant = Color(0xFFB4C3CA),
    outline = Color(0xFF6F8088)
)

@Composable
fun CampusTodoTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
