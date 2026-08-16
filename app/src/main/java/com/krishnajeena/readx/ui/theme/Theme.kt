package com.krishnajeena.readx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ReadXLightColorScheme = lightColorScheme(
    primary = ReadXPrimary,
    onPrimary = Color.White,
    primaryContainer = ReadXSurfaceVariant,
    onPrimaryContainer = ReadXPrimary,
    secondary = ReadXSecondary,
    onSecondary = Color.White,
    tertiary = ReadXAccent,
    background = ReadXBackground,
    onBackground = ReadXTextPrimary,
    surface = ReadXSurface,
    onSurface = ReadXTextPrimary,
    surfaceVariant = ReadXSurfaceVariant,
    onSurfaceVariant = ReadXTextSecondary
)

private val ReadXDarkColorScheme = darkColorScheme(
    primary = ReadXSecondary,
    onPrimary = Color.White,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = Color.White,
    secondary = ReadXPrimary,
    onSecondary = Color.White,
    tertiary = ReadXAccent,
    background = DarkBackground,
    onBackground = Color(0xFFF7FAFC),
    surface = DarkSurface,
    onSurface = Color(0xFFEDF2F7),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = ReadXTextMuted
)

@Composable
fun ReadXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ReadXDarkColorScheme else ReadXLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}