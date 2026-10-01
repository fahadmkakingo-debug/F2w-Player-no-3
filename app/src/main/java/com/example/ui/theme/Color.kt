package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Dynamic Theme Color Accessors linked directly to the active theme in real-time
val F2WBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalF2WColors.current.background

val F2WSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalF2WColors.current.surface

val F2WSurfaceElevated: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalF2WColors.current.surfaceElevated

val F2WSurfaceHighlight: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalF2WColors.current.surfaceHighlight

val F2WCardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalF2WColors.current.cardBorder

val F2WCyanPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalF2WColors.current.primary

val F2WCyanDark: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalF2WColors.current.primaryDark

val F2WTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = Color.White

val F2WTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = Color(0xFFE2E8F0)

val F2WTextTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = Color(0xFFA0AEC0)

val F2WNavBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalF2WColors.current.navBackground

val F2WNavPillActive: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalF2WColors.current.navPillActive

val F2WNavPillBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalF2WColors.current.navPillBorder

val F2WBorderGlow: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalF2WColors.current.cardBorder.copy(alpha = 0.5f)

// Static Accents & Gradients
val F2WVioletAccent = Color(0xFF8A4DFF)
val F2WEmerald = Color(0xFF10B981)
val F2WAmber = Color(0xFFF59E0B)

val F2WPrimaryGradient: Brush
    @Composable
    get() = Brush.horizontalGradient(
        listOf(LocalF2WColors.current.primary, LocalF2WColors.current.primaryDark)
    )

val F2WVaultGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF8A4DFF),
        Color(0xFF00C7DE)
    )
)
