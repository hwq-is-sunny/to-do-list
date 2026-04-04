package com.campus.todo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D6B7A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC9E3EA),
    onPrimaryContainer = Color(0xFF1A3A44),
    secondary = Color(0xFF5B8FA8),
    onSecondary = Color.White,
    tertiary = Color(0xFF7A9E8E),
    background = Color(0xFFF7F9FA),
    surface = Color(0xFFF7F9FA),
    surfaceVariant = Color(0xFFE6EEF1),
    onSurface = Color(0xFF1C2529),
    onSurfaceVariant = Color(0xFF4A5C64),
    outline = Color(0xFF9EB4BC)
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
