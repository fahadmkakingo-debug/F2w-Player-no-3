package com.example.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme(
    val id: String,
    val title: String,
    val isAuto: Boolean = false
) {
    SYSTEM_DEFAULT("system_default", "System Default", isAuto = true),
    F2W_CYAN("f2w_cyan", "Cyan Night"),
    LIGHT("light", "Silver Slate"),
    AMOLED_BLACK("amoled_black", "AMOLED Black"),
    DEEP_SLATE("deep_slate", "Dark Slate"),
    PURPLE_NEON("purple_neon", "Neon Purple"),
    EMERALD_MINT("emerald_mint", "Emerald Mint"),
    CRIMSON_ROSE("crimson_rose", "Crimson Rose"),
    ROYAL_INDIGO("royal_indigo", "Royal Indigo"),
    OCEAN_BLUE("ocean_blue", "Ocean Blue")
}

data class F2WThemeColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHighlight: Color,
    val cardBorder: Color,
    val primary: Color,
    val primaryDark: Color,
    val textPrimary: Color = Color.White, // Always pure white
    val textSecondary: Color = Color(0xFFE2E8F0), // Always clean light off-white
    val textTertiary: Color = Color(0xFFA0AEC0),
    val navBackground: Color,
    val navPillActive: Color,
    val navPillBorder: Color,
    val swatchGradient: Brush,
    val isSystemAuto: Boolean = false
)

object ThemePaletteFactory {
    // 1. Signature F2W Cyan Night
    val CyanNight = F2WThemeColors(
        background = Color(0xFF03141C),
        surface = Color(0xFF04141C),
        surfaceElevated = Color(0xFF07212A),
        surfaceHighlight = Color(0xFF0C2B36),
        cardBorder = Color(0xFF133B47),
        primary = Color(0xFF00C7DE),
        primaryDark = Color(0xFF009AB0),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE051A24),
        navPillActive = Color(0xFF0C2B38),
        navPillBorder = Color(0xFF00C7DE),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFF00C7DE), Color(0xFF04141C)))
    )

    // 2. Silver Slate (Clean Slate supporting crisp white text)
    val SilverSlate = F2WThemeColors(
        background = Color(0xFF1E293B),
        surface = Color(0xFF273548),
        surfaceElevated = Color(0xFF334155),
        surfaceHighlight = Color(0xFF3E4F66),
        cardBorder = Color(0xFF475569),
        primary = Color(0xFF38BDF8),
        primaryDark = Color(0xFF0284C7),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xF01E293B),
        navPillActive = Color(0xFF334155),
        navPillBorder = Color(0xFF38BDF8),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFF64748B), Color(0xFF1E293B)))
    )

    // 3. Pitch Black (AMOLED)
    val AmoledBlack = F2WThemeColors(
        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        surfaceElevated = Color(0xFF0D0D0D),
        surfaceHighlight = Color(0xFF171717),
        cardBorder = Color(0xFF262626),
        primary = Color(0xFF00E5FF),
        primaryDark = Color(0xFF00B0FF),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xF0000000),
        navPillActive = Color(0xFF171717),
        navPillBorder = Color(0xFF00E5FF),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFF1F1F1F), Color(0xFF000000)))
    )

    // 4. Dark Slate
    val DarkSlate = F2WThemeColors(
        background = Color(0xFF12161A),
        surface = Color(0xFF151B20),
        surfaceElevated = Color(0xFF1B2228),
        surfaceHighlight = Color(0xFF232C34),
        cardBorder = Color(0xFF2D3843),
        primary = Color(0xFF38BDF8),
        primaryDark = Color(0xFF0284C7),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE151B20),
        navPillActive = Color(0xFF232C34),
        navPillBorder = Color(0xFF38BDF8),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFF2D3843), Color(0xFF12161A)))
    )

    // 5. Neon Purple
    val NeonPurple = F2WThemeColors(
        background = Color(0xFF0F071D),
        surface = Color(0xFF140A26),
        surfaceElevated = Color(0xFF1B0E33),
        surfaceHighlight = Color(0xFF261447),
        cardBorder = Color(0xFF3B1F6E),
        primary = Color(0xFFA855F7),
        primaryDark = Color(0xFF7E22CE),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE140A26),
        navPillActive = Color(0xFF261447),
        navPillBorder = Color(0xFFA855F7),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFFA855F7), Color(0xFF6B21A8)))
    )

    // 6. Emerald Mint
    val EmeraldMint = F2WThemeColors(
        background = Color(0xFF041510),
        surface = Color(0xFF071C15),
        surfaceElevated = Color(0xFF0C2B21),
        surfaceHighlight = Color(0xFF123D2F),
        cardBorder = Color(0xFF185340),
        primary = Color(0xFF10B981),
        primaryDark = Color(0xFF059669),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE071C15),
        navPillActive = Color(0xFF123D2F),
        navPillBorder = Color(0xFF10B981),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF047857)))
    )

    // 7. Crimson Rose
    val CrimsonRose = F2WThemeColors(
        background = Color(0xFF18060D),
        surface = Color(0xFF200812),
        surfaceElevated = Color(0xFF2D0C1A),
        surfaceHighlight = Color(0xFF3E1124),
        cardBorder = Color(0xFF581833),
        primary = Color(0xFFF43F5E),
        primaryDark = Color(0xFFE11D48),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE200812),
        navPillActive = Color(0xFF3E1124),
        navPillBorder = Color(0xFFF43F5E),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFFF43F5E), Color(0xFF9F1239)))
    )

    // 8. Royal Indigo
    val RoyalIndigo = F2WThemeColors(
        background = Color(0xFF080B1C),
        surface = Color(0xFF0C1027),
        surfaceElevated = Color(0xFF13193D),
        surfaceHighlight = Color(0xFF1C2456),
        cardBorder = Color(0xFF2A367F),
        primary = Color(0xFF6366F1),
        primaryDark = Color(0xFF4F46E5),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE0C1027),
        navPillActive = Color(0xFF1C2456),
        navPillBorder = Color(0xFF6366F1),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFF6366F1), Color(0xFF3730A3)))
    )

    // 9. Ocean Blue
    val OceanBlue = F2WThemeColors(
        background = Color(0xFF031024),
        surface = Color(0xFF051630),
        surfaceElevated = Color(0xFF092248),
        surfaceHighlight = Color(0xFF0E3065),
        cardBorder = Color(0xFF154694),
        primary = Color(0xFF0284C7),
        primaryDark = Color(0xFF0369A1),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE051630),
        navPillActive = Color(0xFF0E3065),
        navPillBorder = Color(0xFF0284C7),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFF0284C7), Color(0xFF075985)))
    )

    // 10. System Default
    val SystemDefault = CyanNight.copy(
        isSystemAuto = true,
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFF475569), Color(0xFF0B1B24)))
    )

    fun getColors(theme: AppTheme): F2WThemeColors {
        return when (theme) {
            AppTheme.SYSTEM_DEFAULT -> SystemDefault
            AppTheme.F2W_CYAN -> CyanNight
            AppTheme.LIGHT -> SilverSlate
            AppTheme.AMOLED_BLACK -> AmoledBlack
            AppTheme.DEEP_SLATE -> DarkSlate
            AppTheme.PURPLE_NEON -> NeonPurple
            AppTheme.EMERALD_MINT -> EmeraldMint
            AppTheme.CRIMSON_ROSE -> CrimsonRose
            AppTheme.ROYAL_INDIGO -> RoyalIndigo
            AppTheme.OCEAN_BLUE -> OceanBlue
        }
    }
}

class ThemeManager private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("f2w_app_theme", Context.MODE_PRIVATE)

    private val _currentTheme = MutableStateFlow(loadTheme())
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private fun loadTheme(): AppTheme {
        val savedId = prefs.getString("selected_theme_id", AppTheme.F2W_CYAN.id)
        return AppTheme.entries.find { it.id == savedId } ?: AppTheme.F2W_CYAN
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("selected_theme_id", theme.id).apply()
        _currentTheme.value = theme
    }

    companion object {
        @Volatile
        private var instance: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

val LocalF2WColors = compositionLocalOf { ThemePaletteFactory.CyanNight }

object F2WTheme {
    val colors: F2WThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalF2WColors.current
}
