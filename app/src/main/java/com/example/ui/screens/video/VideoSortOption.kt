package com.example.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.ThemePaletteFactory

enum class VideoSortOption(
    val title: String,
    val description: String,
    val shortLabel: String
) {
    NAME_ASC("Title (A to Z)", "Jina A - Z", "A → Z"),
    NAME_DESC("Title (Z to A)", "Jina Z - A", "Z → A"),
    DATE_NEWEST("Date (Newest)", "Mpya kwanza", "Mpya"),
    DATE_OLDEST("Date (Oldest)", "Kongwe kwanza", "Kongwe"),
    SIZE_LARGEST("Size (Largest)", "Kubwa kwanza", "Kubwa"),
    SIZE_SMALLEST("Size (Smallest)", "Ndogo kwanza", "Ndogo"),
    DURATION_LONGEST("Duration (Longest)", "Ndefu kwanza", "Ndefu"),
    DURATION_SHORTEST("Duration (Shortest)", "Fupi kwanza", "Fupi")
}

@Composable
fun VideoSortDialog(
    selectedOption: VideoSortOption,
    onOptionSelected: (VideoSortOption) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager.getInstance(context) }
    val currentThemeAppTheme by themeManager.currentTheme.collectAsState()
    val currentThemeColors = ThemePaletteFactory.getColors(currentThemeAppTheme)

    val surfaceColor = currentThemeColors.surfaceElevated
    val borderColor = currentThemeColors.cardBorder
    val primaryColor = currentThemeColors.primary
    val textPrimary = currentThemeColors.textPrimary
    val textSecondary = currentThemeColors.textSecondary

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(surfaceColor)
                .border(1.dp, borderColor, RoundedCornerShape(22.dp))
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(22.dp), spotColor = primaryColor.copy(alpha = 0.3f))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Smart Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(primaryColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sort By (Panga)",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Chagua mpangilio wa video",
                                color = textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(borderColor.copy(alpha = 0.5f))
                            .clickable(onClick = onDismissRequest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Funga",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Compact 2-Column Grid for Options (Sleek Smart Pop Menu)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val options = VideoSortOption.values()
                    for (i in options.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val opt1 = options[i]
                            val opt2 = options.getOrNull(i + 1)

                            SortOptionCard(
                                option = opt1,
                                isSelected = opt1 == selectedOption,
                                primaryColor = primaryColor,
                                borderColor = borderColor,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                onClick = {
                                    onOptionSelected(opt1)
                                    onDismissRequest()
                                },
                                modifier = Modifier.weight(1f)
                            )

                            if (opt2 != null) {
                                SortOptionCard(
                                    option = opt2,
                                    isSelected = opt2 == selectedOption,
                                    primaryColor = primaryColor,
                                    borderColor = borderColor,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    onClick = {
                                        onOptionSelected(opt2)
                                        onDismissRequest()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortOptionCard(
    option: VideoSortOption,
    isSelected: Boolean,
    primaryColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) primaryColor.copy(alpha = 0.18f) else borderColor.copy(alpha = 0.25f))
            .border(
                width = 1.dp,
                color = if (isSelected) primaryColor else borderColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.title,
                color = if (isSelected) primaryColor else textPrimary,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = option.description,
                color = if (isSelected) primaryColor.copy(alpha = 0.85f) else textSecondary,
                fontSize = 10.sp,
                maxLines = 1
            )
        }

        if (isSelected) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
