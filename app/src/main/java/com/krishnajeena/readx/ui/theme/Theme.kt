package com.krishnajeena.readx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CalmLightColorScheme = lightColorScheme(
    primary = SereneSlatePrimary,
    onPrimary = Color.White,
    primaryContainer = WarmPaperSurfaceVariant,
    onPrimaryContainer = SereneSlatePrimary,
    secondary = SereneTealSecondary,
    onSecondary = Color.White,
    tertiary = WarmGoldAccent,
    background = WarmPaperBackground,
    onBackground = Color(0xFF1A202C),
    surface = WarmPaperSurface,
    onSurface = Color(0xFF2D3748),
    surfaceVariant = WarmPaperSurfaceVariant,
    onSurfaceVariant = Color(0xFF4A5568)
)

private val CalmDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE2E8F0),
    onPrimary = DarkBackground,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF4FD1C5),
    onSecondary = DarkBackground,
    tertiary = WarmGoldAccent,
    background = DarkBackground,
    onBackground = Color(0xFFF7FAFC),
    surface = DarkSurface,
    onSurface = Color(0xFFEDF2F7),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E0)
)

@Composable
fun ReadXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep serene curated palette
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CalmDarkColorScheme else CalmLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}