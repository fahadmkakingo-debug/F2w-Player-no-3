package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurface
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary

@Composable
fun F2WTopBar(
    subtitle: String = "Local Media Player",
    onSearchClick: () -> Unit,
    isListView: Boolean = false,
    onToggleViewMode: () -> Unit = {},
    onThemeClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onEqualiserClick: () -> Unit = {},
    onSortClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val surfaceColor = F2WSurface
    val primaryColor = F2WCyanPrimary
    val elevatedColor = F2WSurfaceElevated
    val borderColor = F2WCardBorder
    val textPrimary = F2WTextPrimary
    val textSecondary = F2WTextSecondary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(surfaceColor)
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .testTag("f2w_top_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Glowing F2W Badge + Title & Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // F2W Square Badge with Rounded Corners and Theme Glow
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(9.dp),
                            spotColor = primaryColor,
                            ambientColor = primaryColor
                        )
                        .clip(RoundedCornerShape(9.dp))
                        .background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "F2W",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 0.4.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "F2W Player",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = subtitle.ifBlank { "Local Media Player" },
                        color = textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Right: Rounded Grid/List, Search & More Options Buttons with Popup Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // View Mode Toggle Button (Grid vs List) right beside search
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isListView) primaryColor.copy(alpha = 0.18f) else elevatedColor)
                        .border(
                            1.dp,
                            if (isListView) primaryColor else borderColor,
                            RoundedCornerShape(9.dp)
                        )
                        .clickable(onClick = onToggleViewMode)
                        .testTag("view_mode_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListView) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = if (isListView) "Badili Gridi" else "Badili Orodha (List)",
                        tint = if (isListView) primaryColor else textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Search Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(elevatedColor)
                        .border(1.dp, borderColor, RoundedCornerShape(9.dp))
                        .clickable(onClick = onSearchClick)
                        .testTag("search_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // More Options Button (Three Vertical Dots) with Dropdown Anchor
                Box(
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(elevatedColor)
                            .border(1.dp, borderColor, RoundedCornerShape(9.dp))
                            .clickable { menuExpanded = true }
                            .testTag("three_dot_menu_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Options",
                            tint = textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Premium Theme Dropdown Popup Menu
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .background(elevatedColor)
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = primaryColor.copy(alpha = 0.4f)
                            )
                            .testTag("popup_menu"),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = elevatedColor
                    ) {
                        // 1. Theme
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Theme",
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Palette,
                                    contentDescription = "Theme",
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onThemeClick()
                            },
                            modifier = Modifier.testTag("menu_item_theme")
                        )

                        // 2. Refresh
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Refresh",
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Refresh",
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onRefreshClick()
                            },
                            modifier = Modifier.testTag("menu_item_refresh")
                        )

                        // 3. Equaliser
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Equaliser",
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = "Equaliser",
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onEqualiserClick()
                            },
                            modifier = Modifier.testTag("menu_item_equaliser")
                        )

                        // 4. Sort By
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Sort By",
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort By",
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onSortClick()
                            },
                            modifier = Modifier.testTag("menu_item_sort")
                        )

                        // 4. Settings
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Settings",
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Settings",
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onSettingsClick()
                            },
                            modifier = Modifier.testTag("menu_item_settings")
                        )
                    }
                }
            }
        }
    }
}
