package com.example.ui.screens.theme

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme
import com.example.ui.theme.F2WThemeColors
import com.example.ui.theme.LiveThemeBackground
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.ThemePaletteFactory

@Composable
fun ThemePickerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val themeManager = remember { ThemeManager.getInstance(context) }
    val activeTheme by themeManager.currentTheme.collectAsState()

    // Inspected theme for the preview card
    var inspectedTheme by remember { mutableStateOf(activeTheme) }

    val inspectedColors = ThemePaletteFactory.getColors(inspectedTheme)
    val isInspectedActive = inspectedTheme == activeTheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF333333)) // Neutral studio dark backdrop as in screenshot
            .statusBarsPadding()
            .testTag("theme_picker_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x33FFFFFF))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("theme_picker_back_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Back",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Centered Title: Name of currently inspected theme with emoji
                val themeHeaderIcon = when (inspectedTheme) {
                    AppTheme.LIVE_GLASS_RAIN -> "💧 "
                    AppTheme.LIVE_ADVENTURE -> "🌿 "
                    AppTheme.LIVE_METROPOLIS -> "🏙️ "
                    AppTheme.LIVE_HIGHWAY_DRIVE -> "🚗 "
                    AppTheme.LIVE_GOLDEN_FIELDS -> "🌾 "
                    AppTheme.LIVE_WILDLIFE_SAFARI -> "🦌 "
                    AppTheme.LIVE_AURORA_WAVE -> "🌌 "
                    AppTheme.MIDNIGHT_GALAXY -> "🪐 "
                    AppTheme.OCEAN_GLASS -> "🌊 "
                    AppTheme.NATURE_GREEN -> "🍃 "
                    AppTheme.SUNSET_GLOW -> "🌅 "
                    AppTheme.BLACK_CARBON -> "🖤 "
                    AppTheme.NEON_PURPLE -> "💜 "
                    AppTheme.CRYSTAL_ICE -> "💎 "
                    AppTheme.SOFT_PASTEL -> "🌸 "
                    AppTheme.CYBER_RED -> "🔥 "
                    AppTheme.CLOUDY_SKY -> "☁️ "
                    else -> ""
                }
                Text(
                    text = "$themeHeaderIcon${inspectedTheme.title}",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                // Quick Apply / Close Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF10B981))
                        .clickable {
                            themeManager.setTheme(inspectedTheme)
                            onBack()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("theme_picker_done_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Done",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Center: Interactive Theme Mockup Preview Card (Tap to apply & return)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .clickable {
                        themeManager.setTheme(inspectedTheme)
                        onBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = inspectedTheme,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "theme_preview_transition"
                ) { targetTheme ->
                    val colors = ThemePaletteFactory.getColors(targetTheme)
                    ThemePreviewCard(
                        theme = targetTheme,
                        colors = colors
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Section: Swatch Carousel + Use / In Use Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF22262B)) // Bottom container matching screenshot
                    .padding(top = 14.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Horizontally Scrollable Theme Swatches (The Red Circled Row)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_swatches_row"),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(AppTheme.entries.toTypedArray()) { themeItem ->
                        val themeColors = ThemePaletteFactory.getColors(themeItem)
                        val isThisActive = themeItem == activeTheme
                        val isThisInspected = themeItem == inspectedTheme

                        ThemeSwatchItem(
                            theme = themeItem,
                            colors = themeColors,
                            isInspected = isThisInspected,
                            isActive = isThisActive,
                            onClick = { inspectedTheme = themeItem }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Button ("Use" / "In use")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    if (isInspectedActive) {
                        // "In use" Button - Clickable to return to app
                        Button(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("button_theme_in_use"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF374151)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = "Active Theme • Tap to return to Player",
                                color = Color(0xFFE5E7EB),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // "Use" Button (Vibrant Orange-Pink Gradient) - Applies & returns to app
                        Button(
                            onClick = {
                                themeManager.setTheme(inspectedTheme)
                                onBack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    spotColor = Color(0xFFFF5E00)
                                )
                                .testTag("button_theme_use"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFFFF007A),
                                                Color(0xFFFF5E00),
                                                Color(0xFFFFB300)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Apply & Use Theme",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatchItem(
    theme: AppTheme,
    colors: F2WThemeColors,
    isInspected: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isInspected) Color.White else Color.Transparent

    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(onClick = onClick)
            .testTag("swatch_${theme.id}")
    ) {
        // Thumbnail Card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation = if (isInspected) 6.dp else 2.dp, shape = RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .border(if (isInspected) 2.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
                .background(colors.swatchGradient),
            contentAlignment = Alignment.Center
        ) {
            val swatchEmoji = when (theme) {
                AppTheme.LIVE_GLASS_RAIN -> "💧"
                AppTheme.LIVE_ADVENTURE -> "🌿"
                AppTheme.LIVE_METROPOLIS -> "🏙️"
                AppTheme.LIVE_HIGHWAY_DRIVE -> "🚗"
                AppTheme.LIVE_GOLDEN_FIELDS -> "🌾"
                AppTheme.LIVE_WILDLIFE_SAFARI -> "🦌"
                AppTheme.LIVE_AURORA_WAVE -> "🌌"
                AppTheme.MIDNIGHT_GALAXY -> "🪐"
                AppTheme.OCEAN_GLASS -> "🌊"
                AppTheme.NATURE_GREEN -> "🍃"
                AppTheme.SUNSET_GLOW -> "🌅"
                AppTheme.BLACK_CARBON -> "🖤"
                AppTheme.NEON_PURPLE -> "💜"
                AppTheme.CRYSTAL_ICE -> "💎"
                AppTheme.SOFT_PASTEL -> "🌸"
                AppTheme.CYBER_RED -> "🔥"
                AppTheme.CLOUDY_SKY -> "☁️"
                AppTheme.F2W_CYAN -> "✨"
                AppTheme.EMERALD_MINT -> "🍃"
                AppTheme.CRIMSON_ROSE -> "🌹"
                AppTheme.ROYAL_INDIGO -> "👑"
                AppTheme.OCEAN_BLUE -> "🐬"
                AppTheme.AMOLED_BLACK -> "🌑"
                AppTheme.DEEP_SLATE -> "⚡"
                else -> null
            }

            if (theme == AppTheme.SYSTEM_DEFAULT) {
                // Diagonal split icon for system default
                Icon(
                    imageVector = Icons.Filled.Brightness6,
                    contentDescription = "System Auto",
                    tint = Color(0xFF00C7DE),
                    modifier = Modifier.size(24.dp)
                )
            } else if (theme == AppTheme.LIGHT) {
                // Light mode preview badge
                Box(
                    modifier = Modifier
                        .size(width = 30.dp, height = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF009688))
                )
            } else if (swatchEmoji != null) {
                Text(
                    text = swatchEmoji,
                    fontSize = 18.sp
                )
            }
        }

        // Live badge for live animated themes
        if (theme.isLiveAnimated) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xE600E5FF))
                    .padding(horizontal = 3.dp, vertical = 0.5.dp)
            ) {
                Text(
                    text = "LIVE",
                    color = Color(0xFF021018),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Green Checkmark Badge for currently active theme in use
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Active",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    theme: AppTheme,
    colors: F2WThemeColors,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = Color.Black)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.background)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(22.dp))
    ) {
        // Live Animated Background for animated themes
        if (theme.isLiveAnimated) {
            LiveThemeBackground(
                theme = theme,
                modifier = Modifier.matchParentSize()
            )
        }

        if (theme == AppTheme.SYSTEM_DEFAULT) {
            // Auto Mode Card (Matching Screenshot 1)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Brightness6,
                    contentDescription = "Auto mode",
                    tint = Color.White,
                    modifier = Modifier.size(76.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Auto mode",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // Full miniature F2W Player interface (Matching Screenshot 2)
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Mini Top Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "F2W Player",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Mini Video Thumbnails Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniVideoCard(
                        colors = colors,
                        duration = "20:23",
                        gradient = Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFFEC4899))),
                        modifier = Modifier.weight(1f)
                    )
                    MiniVideoCard(
                        colors = colors,
                        duration = "03:07",
                        gradient = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFF10B981))),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Mini Folder List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniFolderRow("Recent Added", "4 videos", colors)
                    MiniFolderRow("Download", "8 videos", colors)
                    MiniFolderRow("Music", "12 tracks", colors)
                    MiniFolderRow("Album Leg", "20 videos", colors)
                    MiniFolderRow("Header League", "32 videos", colors)
                }

                // Mini Bottom Navigation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.navBackground)
                        .border(0.5.dp, colors.cardBorder)
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiniNavTabItem("Video", Icons.Filled.VideoLibrary, colors.primary, colors.primary)
                        MiniNavTabItem("Music", Icons.Filled.Headphones, colors.textTertiary, colors.textTertiary)
                        MiniNavTabItem("Playlist", Icons.Filled.QueueMusic, colors.textTertiary, colors.textTertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniVideoCard(
    colors: F2WThemeColors,
    duration: String,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(gradient)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(8.dp)
                )
                Text(
                    text = duration,
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MiniFolderRow(
    title: String,
    subtitle: String,
    colors: F2WThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surfaceElevated)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = colors.textTertiary,
                    fontSize = 9.sp
                )
            }
        }

        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun MiniNavTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = textColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
