package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun F2WPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themeManager = ThemeManager.getInstance(context)
    val currentTheme by themeManager.currentTheme.collectAsState()

    val colors = ThemePaletteFactory.getColors(currentTheme)

    val colorScheme = darkColorScheme(
        primary = colors.primary,
        onPrimary = Color.White,
        primaryContainer = colors.surfaceHighlight,
        onPrimaryContainer = colors.primary,
        secondary = colors.primaryDark,
        onSecondary = Color.White,
        background = colors.background,
        onBackground = Color.White,
        surface = colors.surface,
        onSurface = Color.White,
        surfaceVariant = colors.surfaceElevated,
        onSurfaceVariant = Color(0xFFE2E8F0),
        outline = colors.cardBorder
    )

    CompositionLocalProvider(LocalF2WColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Kept for backward compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    F2WPlayerTheme(darkTheme = darkTheme, content = content)
}
