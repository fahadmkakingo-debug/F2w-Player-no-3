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
    val isAuto: Boolean = false,
    val isLiveAnimated: Boolean = false
) {
    SYSTEM_DEFAULT("system_default", "System Default", isAuto = true),
    F2W_CYAN("f2w_cyan", "Cyan Night"),
    LIVE_GLASS_RAIN("live_glass_rain", "White Glass Rain", isLiveAnimated = true),
    LIVE_ADVENTURE("live_adventure", "Lush Adventure Forest", isLiveAnimated = true),
    LIVE_METROPOLIS("live_metropolis", "City Skyline", isLiveAnimated = true),
    LIVE_HIGHWAY_DRIVE("live_highway_drive", "Sunset Highway Drive", isLiveAnimated = true),
    LIVE_GOLDEN_FIELDS("live_golden_fields", "Golden Meadow Fields", isLiveAnimated = true),
    LIVE_WILDLIFE_SAFARI("live_wildlife_safari", "Wildlife Safari", isLiveAnimated = true),
    LIVE_AURORA_WAVE("live_aurora_wave", "Neon Aurora Live", isLiveAnimated = true),
    MIDNIGHT_GALAXY("midnight_galaxy", "Midnight Galaxy"),
    OCEAN_GLASS("ocean_glass", "Ocean Glass"),
    NATURE_GREEN("nature_green", "Nature Green"),
    SUNSET_GLOW("sunset_glow", "Sunset Glow"),
    BLACK_CARBON("black_carbon", "Black Carbon"),
    NEON_PURPLE("neon_purple", "Neon Purple"),
    CRYSTAL_ICE("crystal_ice", "Crystal Ice"),
    SOFT_PASTEL("soft_pastel", "Soft Pastel"),
    CYBER_RED("cyber_red", "Cyber Red"),
    CLOUDY_SKY("cloudy_sky", "Cloudy Sky"),
    LIGHT("light", "Silver Slate"),
    AMOLED_BLACK("amoled_black", "AMOLED Black"),
    DEEP_SLATE("deep_slate", "Dark Slate"),
    PURPLE_NEON("purple_neon", "Purple Neon"),
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
    val textPrimary: Color = Color.White,
    val textSecondary: Color = Color(0xFFE2E8F0),
    val textTertiary: Color = Color(0xFFA0AEC0),
    val navBackground: Color,
    val navPillActive: Color,
    val navPillBorder: Color,
    val swatchGradient: Brush,
    val isSystemAuto: Boolean = false,
    val isLightTheme: Boolean = false,
    val textShadow: androidx.compose.ui.graphics.Shadow? = null
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

    // 2. Midnight Galaxy (Dark navy + purple + glowing star accents)
    val MidnightGalaxy = F2WThemeColors(
        background = Color(0xFF060B1C),
        surface = Color(0xFF0B1229),
        surfaceElevated = Color(0xFF131D3F),
        surfaceHighlight = Color(0xFF1D2A54),
        cardBorder = Color(0xFF31437E),
        primary = Color(0xFF7928CA),
        primaryDark = Color(0xFF5A189A),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE0B1229),
        navPillActive = Color(0xFF1D2A54),
        navPillBorder = Color(0xFF79FFE1),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFF7928CA), Color(0xFF50E3C2), Color(0xFF060B1C)))
    )

    // 3. Ocean Glass (Blue/cyan gradient + transparent glass effect)
    val OceanGlass = F2WThemeColors(
        background = Color(0xFF021B2B),
        surface = Color(0xFF052A40),
        surfaceElevated = Color(0xFF093B57),
        surfaceHighlight = Color(0xFF0E4E73),
        cardBorder = Color(0xFF18709E),
        primary = Color(0xFF00E5FF),
        primaryDark = Color(0xFF009AB0),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE052A40),
        navPillActive = Color(0xFF0E4E73),
        navPillBorder = Color(0xFF00E5FF),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0xFF0077B6), Color(0xFF021B2B)))
    )

    // 4. Nature Green (Green + emerald + soft natural accents)
    val NatureGreen = F2WThemeColors(
        background = Color(0xFF03160D),
        surface = Color(0xFF062215),
        surfaceElevated = Color(0xFF0B3322),
        surfaceHighlight = Color(0xFF114A32),
        cardBorder = Color(0xFF196B49),
        primary = Color(0xFF10B981),
        primaryDark = Color(0xFF059669),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE062215),
        navPillActive = Color(0xFF114A32),
        navPillBorder = Color(0xFF10B981),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF03160D)))
    )

    // 5. Sunset Glow (Orange + pink + purple sunset gradient)
    val SunsetGlow = F2WThemeColors(
        background = Color(0xFF1A0A1C),
        surface = Color(0xFF260E28),
        surfaceElevated = Color(0xFF381438),
        surfaceHighlight = Color(0xFF4D1A4A),
        cardBorder = Color(0xFF702368),
        primary = Color(0xFFFF5E00),
        primaryDark = Color(0xFFE63946),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE260E28),
        navPillActive = Color(0xFF4D1A4A),
        navPillBorder = Color(0xFFFF007A),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFFFF5E00), Color(0xFFFF007A), Color(0xFF7B2CBF)))
    )

    // 6. Black Carbon (Black/dark gray, premium minimalist)
    val BlackCarbon = F2WThemeColors(
        background = Color(0xFF080808),
        surface = Color(0xFF101010),
        surfaceElevated = Color(0xFF181818),
        surfaceHighlight = Color(0xFF242424),
        cardBorder = Color(0xFF383838),
        primary = Color(0xFFE2E8F0),
        primaryDark = Color(0xFF94A3B8),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE101010),
        navPillActive = Color(0xFF242424),
        navPillBorder = Color(0xFFE2E8F0),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFF4B5563), Color(0xFF1F2937), Color(0xFF080808)))
    )

    // 7. Neon Purple (Purple + violet + blue neon glow)
    val NeonPurpleVibrant = F2WThemeColors(
        background = Color(0xFF0D031A),
        surface = Color(0xFF16062A),
        surfaceElevated = Color(0xFF220A3E),
        surfaceHighlight = Color(0xFF310E57),
        cardBorder = Color(0xFF4C1882),
        primary = Color(0xFFB026FF),
        primaryDark = Color(0xFF7928CA),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE16062A),
        navPillActive = Color(0xFF310E57),
        navPillBorder = Color(0xFF00E5FF),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFFB026FF), Color(0xFF00E5FF), Color(0xFF0D031A)))
    )

    // 8. Crystal Ice (White + light blue, glossy/frosted crystal glass)
    val CrystalIce = F2WThemeColors(
        background = Color(0xFF0A192B),
        surface = Color(0xFF0E233C),
        surfaceElevated = Color(0xFF153355),
        surfaceHighlight = Color(0xFF1D4570),
        cardBorder = Color(0xFF2C649E),
        primary = Color(0xFF38BDF8),
        primaryDark = Color(0xFF0284C7),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE0E233C),
        navPillActive = Color(0xFF1D4570),
        navPillBorder = Color(0xFFE0F2FE),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFFE0F2FE), Color(0xFF38BDF8), Color(0xFF0A192B)))
    )

    // 9. Soft Pastel (Pink + lavender + cream, clean & soft)
    val SoftPastel = F2WThemeColors(
        background = Color(0xFF1D1420),
        surface = Color(0xFF281C2B),
        surfaceElevated = Color(0xFF38273D),
        surfaceHighlight = Color(0xFF4B3450),
        cardBorder = Color(0xFF67486E),
        primary = Color(0xFFFF70A6),
        primaryDark = Color(0xFFE05780),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE281C2B),
        navPillActive = Color(0xFF4B3450),
        navPillBorder = Color(0xFFE0AAFF),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFFFF70A6), Color(0xFFE0AAFF), Color(0xFFFFD6E0)))
    )

    // 10. Cyber Red (Black + red/orange neon, futuristic)
    val CyberRed = F2WThemeColors(
        background = Color(0xFF140306),
        surface = Color(0xFF1E0509),
        surfaceElevated = Color(0xFF2D090F),
        surfaceHighlight = Color(0xFF400E17),
        cardBorder = Color(0xFF631522),
        primary = Color(0xFFFF1744),
        primaryDark = Color(0xFFD50000),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE1E0509),
        navPillActive = Color(0xFF400E17),
        navPillBorder = Color(0xFFFF6D00),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFFFF1744), Color(0xFFFF6D00), Color(0xFF140306)))
    )

    // 11. Cloudy Sky (Light blue + white, clouds & subtle blur)
    val CloudySky = F2WThemeColors(
        background = Color(0xFF0B1929),
        surface = Color(0xFF10253B),
        surfaceElevated = Color(0xFF183452),
        surfaceHighlight = Color(0xFF22476D),
        cardBorder = Color(0xFF316294),
        primary = Color(0xFF60A5FA),
        primaryDark = Color(0xFF3B82F6),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xEE10253B),
        navPillActive = Color(0xFF22476D),
        navPillBorder = Color(0xFFF0F9FF),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFF93C5FD), Color(0xFF60A5FA), Color(0xFF0B1929)))
    )

    // 12. Silver Slate (Clean Slate supporting crisp white text)
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

    // 13. Pitch Black (AMOLED)
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

    // 14. Dark Slate
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

    // 15. Neon Purple Classic
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

    // 16. Emerald Mint
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

    // 17. Crimson Rose
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

    // 18. Royal Indigo
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

    // 19. Ocean Blue
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

    // 20. System Default
    val SystemDefault = CyanNight.copy(
        isSystemAuto = true,
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFF475569), Color(0xFF0B1B24)))
    )

    // 21. Live Glass Rain (White Frosted Crystal Glass with Droplets)
    val LiveGlassRain = F2WThemeColors(
        background = Color(0xFFF1F5F9),
        surface = Color(0x33FFFFFF),
        surfaceElevated = Color(0x55FFFFFF),
        surfaceHighlight = Color(0x80FFFFFF),
        cardBorder = Color(0x660284C7),
        primary = Color(0xFF0284C7),
        primaryDark = Color(0xFF0369A1),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF334155),
        textTertiary = Color(0xFF64748B),
        navBackground = Color(0xCCF8FAFC),
        navPillActive = Color(0x99E2E8F0),
        navPillBorder = Color(0xFF0284C7),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE0F2FE), Color(0xFF0284C7))),
        isLightTheme = true,
        textShadow = androidx.compose.ui.graphics.Shadow(
            color = Color(0x40000000),
            offset = androidx.compose.ui.geometry.Offset(1f, 1.5f),
            blurRadius = 3f
        )
    )

    // 22. Live Adventure Quest (Lush Nature Forest, Trees, Flowers, Leaves)
    val LiveAdventure = F2WThemeColors(
        background = Color(0xFF041A11),
        surface = Color(0x4408291B),
        surfaceElevated = Color(0x660D3825),
        surfaceHighlight = Color(0x88144A32),
        cardBorder = Color(0x9910B981),
        primary = Color(0xFFFFB703),
        primaryDark = Color(0xFF10B981),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xCC041A11),
        navPillActive = Color(0x88144A32),
        navPillBorder = Color(0xFFFFB703),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFFFFB703), Color(0xFF10B981), Color(0xFF041A11)))
    )

    // 23. Live Metropolis (Illuminated City Skyline with Changing Building Lights)
    val LiveMetropolis = F2WThemeColors(
        background = Color(0xFF0A0F1D),
        surface = Color(0x44111827),
        surfaceElevated = Color(0x661F2937),
        surfaceHighlight = Color(0x88374151),
        cardBorder = Color(0x99FFC107),
        primary = Color(0xFFFFC107),
        primaryDark = Color(0xFF00E5FF),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xCC0A0F1D),
        navPillActive = Color(0x88374151),
        navPillBorder = Color(0xFFFFC107),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFFFFC107), Color(0xFF00E5FF), Color(0xFF0A0F1D)))
    )

    // 24. Live Highway Drive (Cruising Car on Scenic Sunset Highway)
    val LiveHighwayDrive = F2WThemeColors(
        background = Color(0xFF1A0A24),
        surface = Color(0x44270E36),
        surfaceElevated = Color(0x663B1452),
        surfaceHighlight = Color(0x88521C72),
        cardBorder = Color(0x99FF5E00),
        primary = Color(0xFFFF5E00),
        primaryDark = Color(0xFFFF007A),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xCC1A0A24),
        navPillActive = Color(0x88521C72),
        navPillBorder = Color(0xFFFF5E00),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFFFF5E00), Color(0xFFFF007A), Color(0xFF7B2CBF)))
    )

    // 25. Live Golden Fields (Rolling Meadow Hills, Waving Crops & Sunbeams)
    val LiveGoldenFields = F2WThemeColors(
        background = Color(0xFF101A0B),
        surface = Color(0x44192911),
        surfaceElevated = Color(0x66263D1A),
        surfaceHighlight = Color(0x88365525),
        cardBorder = Color(0x99E9C46A),
        primary = Color(0xFFE9C46A),
        primaryDark = Color(0xFF52B788),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xCC101A0B),
        navPillActive = Color(0x88365525),
        navPillBorder = Color(0xFFE9C46A),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFFE9C46A), Color(0xFF52B788), Color(0xFF101A0B)))
    )

    // 26. Live Wildlife Safari (Walking Deer, Birds in Flight, Savanna Twilight)
    val LiveWildlifeSafari = F2WThemeColors(
        background = Color(0xFF1C0D05),
        surface = Color(0x442B1408),
        surfaceElevated = Color(0x663D1D0C),
        surfaceHighlight = Color(0x88572A12),
        cardBorder = Color(0x99F4A261),
        primary = Color(0xFFF4A261),
        primaryDark = Color(0xFFE76F51),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xCC1C0D05),
        navPillActive = Color(0xFF572A12),
        navPillBorder = Color(0xFFF4A261),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFFF4A261), Color(0xFFE76F51), Color(0xFF1C0D05)))
    )

    // 27. Live Neon Aurora (Dynamic undulating Northern Lights & cosmic dust)
    val LiveAuroraWave = F2WThemeColors(
        background = Color(0xFF020912),
        surface = Color(0x44041824),
        surfaceElevated = Color(0x6608273A),
        surfaceHighlight = Color(0x880E3C56),
        cardBorder = Color(0x9900F5D4),
        primary = Color(0xFF00F5D4),
        primaryDark = Color(0xFF7B2CBF),
        textPrimary = Color.White,
        textSecondary = Color(0xFFE2E8F0),
        textTertiary = Color(0xFFA0AEC0),
        navBackground = Color(0xCC020912),
        navPillActive = Color(0xFF0E3C56),
        navPillBorder = Color(0xFF00F5D4),
        swatchGradient = Brush.verticalGradient(listOf(Color(0xFF00F5D4), Color(0xFF39FF14), Color(0xFF7B2CBF), Color(0xFF020912)))
    )

    fun getColors(theme: AppTheme): F2WThemeColors {
        return when (theme) {
            AppTheme.SYSTEM_DEFAULT -> SystemDefault
            AppTheme.F2W_CYAN -> CyanNight
            AppTheme.LIVE_GLASS_RAIN -> LiveGlassRain
            AppTheme.LIVE_ADVENTURE -> LiveAdventure
            AppTheme.LIVE_METROPOLIS -> LiveMetropolis
            AppTheme.LIVE_HIGHWAY_DRIVE -> LiveHighwayDrive
            AppTheme.LIVE_GOLDEN_FIELDS -> LiveGoldenFields
            AppTheme.LIVE_WILDLIFE_SAFARI -> LiveWildlifeSafari
            AppTheme.LIVE_AURORA_WAVE -> LiveAuroraWave
            AppTheme.MIDNIGHT_GALAXY -> MidnightGalaxy
            AppTheme.OCEAN_GLASS -> OceanGlass
            AppTheme.NATURE_GREEN -> NatureGreen
            AppTheme.SUNSET_GLOW -> SunsetGlow
            AppTheme.BLACK_CARBON -> BlackCarbon
            AppTheme.NEON_PURPLE -> NeonPurpleVibrant
            AppTheme.CRYSTAL_ICE -> CrystalIce
            AppTheme.SOFT_PASTEL -> SoftPastel
            AppTheme.CYBER_RED -> CyberRed
            AppTheme.CLOUDY_SKY -> CloudySky
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
