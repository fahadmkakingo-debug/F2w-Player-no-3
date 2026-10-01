package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.settings.VideoSettingsPreferences
import com.example.ui.theme.F2WBackground
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WTextTertiary

@Composable
fun VideoSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val prefs = remember { VideoSettingsPreferences(context) }

    // Persistent State for Display
    var showStatusBar by remember { mutableStateOf(prefs.showStatusBarDuringPlayback) }
    var screenOrientation by remember { mutableStateOf(prefs.screenOrientation) }

    // Persistent State for Playback
    var resumePlayback by remember { mutableStateOf(prefs.resumePlayback) }
    var autoMiniplayer by remember { mutableStateOf(prefs.autoMiniplayer) }
    var autoPlayNext by remember { mutableStateOf(prefs.autoPlayNext) }

    // Persistent State for Control
    var gestureControl by remember { mutableStateOf(prefs.gestureControl) }
    var seekInterval by remember { mutableIntStateOf(prefs.seekIntervalSeconds) }
    var doubleTapToSeek by remember { mutableStateOf(prefs.doubleTapToSeek) }
    var longPressSpeedUp by remember { mutableStateOf(prefs.longPressSpeedUp) }
    var longPressSpeed by remember { mutableFloatStateOf(prefs.longPressSpeedMultiplier) }
    var longPressVibration by remember { mutableStateOf(prefs.longPressVibration) }
    var tapRatiosToSwitchDirectly by remember { mutableStateOf(prefs.tapRatiosToSwitchDirectly) }

    // Persistent State for Memory
    var rememberBackgroundPlay by remember { mutableStateOf(prefs.rememberBackgroundPlay) }
    var rememberRatio by remember { mutableStateOf(prefs.rememberRatio) }
    var rememberSpeed by remember { mutableStateOf(prefs.rememberSpeed) }
    var rememberBrightness by remember { mutableStateOf(prefs.rememberBrightness) }

    // Dialog state holders
    var showOrientationDialog by remember { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }
    var showSeekIntervalDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(F2WBackground)
            .statusBarsPadding()
            .testTag("video_settings_screen")
    ) {
        // Top Navigation Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0B252E))
                        .border(1.dp, F2WCardBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onBack)
                        .testTag("video_settings_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Video Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = F2WTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                    Text(
                        text = "Display, playback, gestures & memory",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = F2WTextTertiary,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        // Scrollable Options List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ---------------------------------------------
            // SECTION 1: DISPLAY
            // ---------------------------------------------
            SettingsSection(title = "Display") {
                // Show status bar during playback
                SettingsSwitchRow(
                    title = "Show status bar during playback",
                    subtitle = "Keep clock and system battery indicator visible while watching",
                    checked = showStatusBar,
                    onCheckedChange = {
                        showStatusBar = it
                        prefs.showStatusBarDuringPlayback = it
                    },
                    testTag = "switch_show_status_bar"
                )

                HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Screen orientation
                SettingsSelectionRow(
                    title = "Screen orientation",
                    subtitle = "Auto-rotate or lock video orientation",
                    currentValue = screenOrientation,
                    onClick = { showOrientationDialog = true },
                    testTag = "selection_screen_orientation"
                )
            }

            // ---------------------------------------------
            // SECTION 2: PLAYBACK
            // ---------------------------------------------
            SettingsSection(title = "Playback") {
                // Resume
                SettingsSelectionRow(
                    title = "Resume",
                    subtitle = "Continue from previous position",
                    currentValue = resumePlayback,
                    onClick = { showResumeDialog = true },
                    testTag = "selection_resume_playback"
                )

                HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Auto miniplayer
                SettingsSwitchRow(
                    title = "Auto miniplayer",
                    subtitle = "Seamlessly switch to floating PiP when exiting",
                    checked = autoMiniplayer,
                    onCheckedChange = {
                        autoMiniplayer = it
                        prefs.autoMiniplayer = it
                    },
                    testTag = "switch_auto_miniplayer"
                )

                HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Auto play next
                SettingsSwitchRow(
                    title = "Auto play next",
                    subtitle = "Play the next video in folder automatically",
                    checked = autoPlayNext,
                    onCheckedChange = {
                        autoPlayNext = it
                        prefs.autoPlayNext = it
                    },
                    testTag = "switch_auto_play_next"
                )
            }

            // ---------------------------------------------
            // SECTION 3: CONTROL
            // ---------------------------------------------
            SettingsSection(title = "Control") {
                // Gesture control
                SettingsSwitchRow(
                    title = "Gesture control",
                    subtitle = "Swipe vertically for brightness and volume",
                    checked = gestureControl,
                    onCheckedChange = {
                        gestureControl = it
                        prefs.gestureControl = it
                    },
                    testTag = "switch_gesture_control"
                )

                HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Fast forward/rewind interval
                SettingsSelectionRow(
                    title = "Fast forward/rewind interval",
                    subtitle = "Skip duration when seeking",
                    currentValue = "${seekInterval}s",
                    onClick = { showSeekIntervalDialog = true },
                    testTag = "selection_seek_interval"
                )

                HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Double tap to fast forward/rewind
                SettingsSwitchRow(
                    title = "Double tap to fast forward/rewind",
                    subtitle = "Double tap screen edges to skip forward or backward",
                    checked = doubleTapToSeek,
                    onCheckedChange = {
                        doubleTapToSeek = it
                        prefs.doubleTapToSeek = it
                    },
                    testTag = "switch_double_tap_seek"
                )

                HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Long press to speed up
                SettingsSwitchRow(
                    title = "Long press to speed up",
                    subtitle = "Hold screen to temporarily accelerate playback",
                    checked = longPressSpeedUp,
                    onCheckedChange = {
                        longPressSpeedUp = it
                        prefs.longPressSpeedUp = it
                    },
                    testTag = "switch_long_press_speed_up"
                )

                HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Long-press speed
                SettingsSelectionRow(
                    title = "Long-press speed",
                    subtitle = "Speed factor while holding down",
                    currentValue = "${longPressSpeed}x",
                    onClick = { showSpeedDialog = true },
                    testTag = "selection_long_press_speed"
                )

                HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Long press vibration
                SettingsSwitchRow(
                    title = "Long press vibration",
                    subtitle = "Haptic feedback when speed acceleration begins",
                    checked = longPressVibration,
                    onCheckedChange = {
                        longPressVibration = it
                        prefs.longPressVibration = it
                    },
                    testTag = "switch_long_press_vibration"
                )

                HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Tap ratios to switch directly
                SettingsSwitchRow(
                    title = "Tap ratios to switch directly",
                    subtitle = "Cycle aspect ratios (16:9, 4:3, Fill, Fit) on quick tap",
                    checked = tapRatiosToSwitchDirectly,
                    onCheckedChange = {
                        tapRatiosToSwitchDirectly = it
                        prefs.tapRatiosToSwitchDirectly = it
                    },
                    testTag = "switch_tap_ratios_directly"
                )
            }

            // ---------------------------------------------
            // SECTION 4: MEMORY
            // ---------------------------------------------
            SettingsSection(title = "Memory") {
                // Remember background play
                SettingsSwitchRow(
                    title = "Remember background play",
                    subtitle = "Retain audio-only background playback preference",
                    checked = rememberBackgroundPlay,
                    onCheckedChange = {
                        rememberBackgroundPlay = it
                        prefs.rememberBackgroundPlay = it
                    },
                    testTag = "switch_remember_bg_play"
                )

                HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Remember ratio
                SettingsSwitchRow(
                    title = "Remember ratio",
                    subtitle = "Keep aspect ratio across new video sessions",
                    checked = rememberRatio,
                    onCheckedChange = {
                        rememberRatio = it
                        prefs.rememberRatio = it
                    },
                    testTag = "switch_remember_ratio"
                )

                HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Remember speed
                SettingsSwitchRow(
                    title = "Remember speed",
                    subtitle = "Keep custom playback rate for all subsequent videos",
                    checked = rememberSpeed,
                    onCheckedChange = {
                        rememberSpeed = it
                        prefs.rememberSpeed = it
                    },
                    testTag = "switch_remember_speed"
                )

                HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Remember brightness
                SettingsSwitchRow(
                    title = "Remember brightness",
                    subtitle = "Maintain custom player brightness level",
                    checked = rememberBrightness,
                    onCheckedChange = {
                        rememberBrightness = it
                        prefs.rememberBrightness = it
                    },
                    testTag = "switch_remember_brightness"
                )
            }
        }
    }

    // ---------------------------------------------
    // SELECTION DIALOGS
    // ---------------------------------------------

    // Screen Orientation Dialog
    if (showOrientationDialog) {
        val options = listOf("Sensor / Auto", "Landscape", "Portrait", "Sensor Landscape")
        SettingsSingleChoiceDialog(
            title = "Screen orientation",
            options = options,
            selectedOption = screenOrientation,
            onSelect = {
                screenOrientation = it
                prefs.screenOrientation = it
                showOrientationDialog = false
            },
            onDismiss = { showOrientationDialog = false }
        )
    }

    // Resume Playback Dialog
    if (showResumeDialog) {
        val options = listOf("Always", "Ask each time", "Never / Off")
        SettingsSingleChoiceDialog(
            title = "Resume",
            options = options,
            selectedOption = resumePlayback,
            onSelect = {
                resumePlayback = it
                prefs.resumePlayback = it
                showResumeDialog = false
            },
            onDismiss = { showResumeDialog = false }
        )
    }

    // Seek Interval Dialog
    if (showSeekIntervalDialog) {
        val options = listOf("5s", "10s", "15s", "30s", "60s")
        SettingsSingleChoiceDialog(
            title = "Fast forward/rewind interval",
            options = options,
            selectedOption = "${seekInterval}s",
            onSelect = {
                val value = it.removeSuffix("s").toIntOrNull() ?: 10
                seekInterval = value
                prefs.seekIntervalSeconds = value
                showSeekIntervalDialog = false
            },
            onDismiss = { showSeekIntervalDialog = false }
        )
    }

    // Long-Press Speed Dialog
    if (showSpeedDialog) {
        val options = listOf("1.5x", "2.0x", "2.5x", "3.0x")
        SettingsSingleChoiceDialog(
            title = "Long-press speed",
            options = options,
            selectedOption = "${longPressSpeed}x",
            onSelect = {
                val value = it.removeSuffix("x").toFloatOrNull() ?: 2.0f
                longPressSpeed = value
                prefs.longPressSpeedMultiplier = value
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                color = F2WCyanPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 12.sp
            ),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color(0x3300E5FF)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(F2WSurfaceElevated)
                .border(1.dp, F2WCardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = F2WTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = F2WTextSecondary,
                    fontSize = 12.sp
                )
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = F2WCyanPrimary,
                uncheckedThumbColor = F2WTextTertiary,
                uncheckedTrackColor = Color(0xFF0C2B38),
                uncheckedBorderColor = F2WCardBorder
            )
        )
    }
}

@Composable
private fun SettingsSelectionRow(
    title: String,
    subtitle: String,
    currentValue: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = F2WTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = F2WTextSecondary,
                    fontSize = 12.sp
                )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0C2B38))
                    .border(1.dp, F2WCyanPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = currentValue,
                    color = F2WCyanPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = F2WTextTertiary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun SettingsSingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF07212A),
        titleContentColor = F2WTextPrimary,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, F2WCardBorder, RoundedCornerShape(20.dp)),
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(option) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = { onSelect(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = F2WCyanPrimary,
                                unselectedColor = F2WTextTertiary
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = option,
                            color = if (option == selectedOption) F2WTextPrimary else F2WTextSecondary,
                            fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = F2WCyanPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}
