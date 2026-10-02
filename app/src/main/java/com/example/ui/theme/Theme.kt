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

    val colorScheme = if (colors.isLightTheme) {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = Color.White,
            primaryContainer = colors.surfaceHighlight,
            onPrimaryContainer = colors.primaryDark,
            secondary = colors.primaryDark,
            onSecondary = Color.White,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceElevated,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.cardBorder
        )
    } else {
        darkColorScheme(
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
    }

    val dynamicTypography = if (colors.textShadow != null) {
        androidx.compose.material3.Typography(
            headlineLarge = Typography.headlineLarge.copy(shadow = colors.textShadow),
            headlineMedium = Typography.headlineMedium.copy(shadow = colors.textShadow),
            headlineSmall = Typography.headlineSmall.copy(shadow = colors.textShadow),
            titleLarge = Typography.titleLarge.copy(shadow = colors.textShadow),
            titleMedium = Typography.titleMedium.copy(shadow = colors.textShadow),
            titleSmall = Typography.titleSmall.copy(shadow = colors.textShadow),
            bodyLarge = Typography.bodyLarge.copy(shadow = colors.textShadow),
            bodyMedium = Typography.bodyMedium.copy(shadow = colors.textShadow),
            bodySmall = Typography.bodySmall.copy(shadow = colors.textShadow),
            labelLarge = Typography.labelLarge.copy(shadow = colors.textShadow),
            labelMedium = Typography.labelMedium.copy(shadow = colors.textShadow),
            labelSmall = Typography.labelSmall.copy(shadow = colors.textShadow)
        )
    } else {
        Typography
    }

    CompositionLocalProvider(LocalF2WColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = dynamicTypography,
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
