package com.example.ui.screens.player

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayDisabled
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.video.VideoItem
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WVioletAccent
import kotlin.math.roundToInt

@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismissRequest: () -> Unit
) {
    var speedValue by remember { mutableFloatStateOf(currentSpeed) }
    val presetSpeeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f, 4.0f)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1F22),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kasi ya Video (Speed)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${String.format("%.2f", speedValue)}X",
                    color = F2WCyanPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Chagua kasi ya kucheza video (0.25X hadi 4.0X):",
                    color = F2WTextSecondary,
                    fontSize = 13.sp
                )

                // Continuous Fine-Tuning Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newSpeed = (speedValue - 0.1f).coerceAtLeast(0.25f)
                            speedValue = (newSpeed * 100).roundToInt() / 100f
                            onSpeedSelected(speedValue)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Punguza", tint = Color.White)
                    }

                    Slider(
                        value = speedValue,
                        onValueChange = {
                            speedValue = (it * 20).roundToInt() / 20f
                            onSpeedSelected(speedValue)
                        },
                        valueRange = 0.25f..4.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = F2WCyanPrimary,
                            activeTrackColor = F2WCyanPrimary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            val newSpeed = (speedValue + 0.1f).coerceAtMost(4.0f)
                            speedValue = (newSpeed * 100).roundToInt() / 100f
                            onSpeedSelected(speedValue)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Ongeza", tint = Color.White)
                    }
                }

                // Grid of Presets
                Text(
                    text = "Vipimo vya Haraka:",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val rows = presetSpeeds.chunked(4)
                    rows.forEach { rowSpeeds ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowSpeeds.forEach { sp ->
                                val isSelected = (Math.abs(speedValue - sp) < 0.05f)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) F2WCyanPrimary else Color(0xFF2B2D31))
                                        .border(
                                            1.dp,
                                            if (isSelected) F2WCyanPrimary else Color.White.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            speedValue = sp
                                            onSpeedSelected(sp)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${sp}X",
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Imekamilika", color = F2WCyanPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitlesAndAudioDialog(
    selectedAudioTrack: String,
    onAudioTrackSelected: (String) -> Unit,
    selectedSubtitle: String,
    onSubtitleSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Audio, 1: Subtitles
    var audioBoostEnabled by remember { mutableStateOf(false) }
    var audioBoostLevel by remember { mutableFloatStateOf(100f) } // 100% to 200%
    var subtitleSize by remember { mutableStateOf("Medium") }
    var subtitleDelayMs by remember { mutableIntStateOf(0) }

    val audioTracks = listOf(
        "Track 1: English (Stereo 2.0)",
        "Track 2: Kiswahili (Stereo Dub)",
        "Track 3: Hindi / Multi-Audio",
        "Track 4: Audio Description (Visually Impaired)"
    )

    val subtitleTracks = listOf(
        "Hakuna (Off)",
        "English (Embedded CC)",
        "Kiswahili (Tafsiri ya Kiswahili)",
        "French (Français)",
        "Chagua faili la .SRT kutoka kwenye simu..."
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1F22),
        title = {
            Text(
                text = "Sauti na Manukuu (Audio & Subtitles)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Tab Selection (Audio vs Subtitle)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF2B2D31),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = F2WCyanPrimary
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lugha ya Sauti", fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ClosedCaption, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Manukuu (CC)", fontSize = 13.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // Audio Tracks Section
                    Text(
                        text = "Chagua Mkondo wa Sauti (Audio Track):",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    audioTracks.forEach { track ->
                        val isSelected = selectedAudioTrack == track
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAudioTrackSelected(track) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onAudioTrackSelected(track) },
                                colors = RadioButtonDefaults.colors(selectedColor = F2WCyanPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = track,
                                color = if (isSelected) F2WCyanPrimary else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    // Smart Audio Boost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Kiongeza Sauti (Audio Boost 200%)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Inafaa kwa video zenye sauti ndogo sana",
                                color = F2WTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = audioBoostEnabled,
                            onCheckedChange = { audioBoostEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = F2WCyanPrimary
                            )
                        )
                    }
                } else {
                    // Subtitles Section
                    Text(
                        text = "Chagua Lugha ya Manukuu (Subtitles):",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    subtitleTracks.forEach { sub ->
                        val isSelected = selectedSubtitle == sub
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSubtitleSelected(sub) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSubtitleSelected(sub) },
                                colors = RadioButtonDefaults.colors(selectedColor = F2WCyanPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sub,
                                color = if (isSelected) F2WCyanPrimary else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    // Subtitle Size Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ukubwa wa Maandishi:",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Kidogo", "Kati", "Kubwa").forEach { size ->
                                val isChosen = (subtitleSize == size)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isChosen) F2WCyanPrimary else Color(0xFF2B2D31))
                                        .clickable { subtitleSize = size }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = size,
                                        color = if (isChosen) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Sawa", color = F2WCyanPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun SleepTimerDialog(
    selectedMinutes: Int,
    onSelectTimer: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val options = listOf(
        0 to "Zima (Off)",
        15 to "Dakika 15",
        30 to "Dakika 30",
        45 to "Dakika 45",
        60 to "Saa 1 (Dakika 60)",
        90 to "Saa 1 na Nusu (Dakika 90)",
        -1 to "Mwisho wa Video Hii"
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1F22),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = F2WCyanPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kipima Muda cha Kulala (Sleep Timer)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Player itajizima kiotomatiki baada ya muda huu:",
                    color = F2WTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                options.forEach { (mins, label) ->
                    val isSelected = selectedMinutes == mins
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectTimer(mins)
                                onDismissRequest()
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onSelectTimer(mins)
                                onDismissRequest()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = F2WCyanPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = if (isSelected) F2WCyanPrimary else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Funga", color = F2WCyanPrimary)
            }
        }
    )
}

@Composable
fun XPlayerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean = false,
    activeColor: Color = Color(0xFF00E676),
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) activeColor
                    else Color.White.copy(alpha = 0.12f)
                )
                .border(
                    width = 1.dp,
                    color = if (isSelected) activeColor else Color.White.copy(alpha = 0.15f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoMoreOptionsSheet(
    isNightMode: Boolean = false,
    onNightModeToggle: () -> Unit = {},
    isAudioOnlyMode: Boolean = false,
    onAudioOnlyToggle: () -> Unit = {},
    repeatMode: String = "Order",
    onRepeatModeSelect: (String) -> Unit = {},
    brightnessPercent: Int = 50,
    onBrightnessChange: (Int) -> Unit = {},
    volumePercent: Int = 80,
    onVolumeChange: (Int) -> Unit = {},
    decoderMode: String = "HW Decoder",
    onDecoderModeSelect: (String) -> Unit = {},
    isFavorite: Boolean = false,
    onFavoriteToggle: () -> Unit = {},
    isAbRepeatActive: Boolean = false,
    onAbRepeatClick: () -> Unit = {},
    onAudioTrackClick: () -> Unit = {},
    onSubtitleClick: () -> Unit = {},
    onPopupPlayClick: () -> Unit = {},
    onCastClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    onVideoDetailsClick: () -> Unit = {},
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xDC111215), // Translucent dark backdrop matching XPlayer screenshots!
        scrimColor = Color.Black.copy(alpha = 0.35f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Row 1: Primary Action Buttons Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                XPlayerActionButton(
                    icon = Icons.Filled.Audiotrack,
                    label = "Audio Track",
                    onClick = {
                        onAudioTrackClick()
                        onDismissRequest()
                    }
                )
                XPlayerActionButton(
                    icon = Icons.Filled.ClosedCaption,
                    label = "Subtitle",
                    onClick = {
                        onSubtitleClick()
                        onDismissRequest()
                    }
                )
                XPlayerActionButton(
                    icon = Icons.Filled.Headset,
                    label = "Background Play",
                    isSelected = isAudioOnlyMode,
                    onClick = onAudioOnlyToggle
                )
                XPlayerActionButton(
                    icon = Icons.Filled.OpenInNew,
                    label = "Pop-up Play",
                    onClick = {
                        onPopupPlayClick()
                        onDismissRequest()
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 2: Secondary Action Buttons Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                XPlayerActionButton(
                    icon = Icons.Filled.Cast,
                    label = "Cast",
                    onClick = {
                        onCastClick()
                        onDismissRequest()
                    }
                )
                XPlayerActionButton(
                    icon = Icons.Filled.DeleteOutline,
                    label = "Delete",
                    onClick = {
                        onDeleteClick()
                        onDismissRequest()
                    }
                )
                XPlayerActionButton(
                    icon = Icons.Filled.BookmarkBorder,
                    label = "Bookmark",
                    onClick = {
                        onBookmarkClick()
                        onDismissRequest()
                    }
                )
                XPlayerActionButton(
                    icon = Icons.Filled.Favorite,
                    label = "Favorites",
                    isSelected = isFavorite,
                    onClick = onFavoriteToggle
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(14.dp))

            // Play option Section
            Text(
                text = "Play option",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp, bottom = 12.dp)
            )

            // Row 3: Play Option Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                XPlayerActionButton(
                    icon = Icons.Filled.Repeat,
                    label = "AB Repeat",
                    isSelected = isAbRepeatActive,
                    onClick = onAbRepeatClick
                )
                XPlayerActionButton(
                    icon = Icons.Filled.GraphicEq,
                    label = "Equalizer",
                    onClick = {
                        onEqualizerClick()
                        onDismissRequest()
                    }
                )
                XPlayerActionButton(
                    icon = Icons.Filled.AccessTime,
                    label = "Timer",
                    onClick = {
                        onSleepTimerClick()
                        onDismissRequest()
                    }
                )
                XPlayerActionButton(
                    icon = Icons.Filled.Nightlight,
                    label = "Visual Enhancer",
                    isSelected = isNightMode,
                    onClick = onNightModeToggle
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(14.dp))

            // Repeat Mode Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Repeat Mode",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = repeatMode,
                    color = Color(0xFF00E676),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Repeat Mode Selection Row (5 circular pill options)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val repeatModes = listOf(
                    Triple("Order", Icons.AutoMirrored.Filled.List, "Order"),
                    Triple("Repeat One", Icons.Filled.RepeatOne, "Repeat One"),
                    Triple("Shuffle", Icons.Filled.Shuffle, "Shuffle"),
                    Triple("Repeat All", Icons.Filled.Repeat, "Repeat All"),
                    Triple("Off", Icons.Filled.Loop, "Loop")
                )

                repeatModes.forEach { (modeKey, iconVec, label) ->
                    val isSel = (repeatMode.equals(modeKey, ignoreCase = true) || (repeatMode == "Off" && modeKey == "Off"))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isSel) Color(0xFF00E676) else Color.White.copy(alpha = 0.12f))
                            .clickable { onRepeatModeSelect(modeKey) }
                    ) {
                        Icon(
                            imageVector = iconVec,
                            contentDescription = label,
                            tint = if (isSel) Color.Black else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(14.dp))

            // Brightness Section
            Text(
                text = "Brightness",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.WbSunny,
                    contentDescription = "Brightness",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Slider(
                    value = brightnessPercent.toFloat(),
                    onValueChange = { onBrightnessChange(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E676),
                        activeTrackColor = Color(0xFF00E676),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "$brightnessPercent",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Volume Section
            Text(
                text = "Volume",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (volumePercent == 0) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Volume",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Slider(
                    value = volumePercent.toFloat(),
                    onValueChange = { onVolumeChange(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E676),
                        activeTrackColor = Color(0xFF00E676),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "$volumePercent",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(14.dp))

            // Decoder Section
            Text(
                text = "Decoder",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val decoders = listOf("HW Decoder", "SW Decoder")
                decoders.forEach { dec ->
                    val isSel = decoderMode.contains("HW", ignoreCase = true) == dec.contains("HW", ignoreCase = true)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSel) Color(0xFF00E676) else Color.White.copy(alpha = 0.12f))
                            .clickable { onDecoderModeSelect(dec) }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = dec,
                            color = if (isSel) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistQueueBottomSheet(
    videos: List<VideoItem>,
    currentVideoId: String,
    onVideoSelected: (VideoItem) -> Unit,
    onQueueReordered: (List<VideoItem>) -> Unit = {},
    onDismissRequest: () -> Unit
) {
    var queueList by remember(videos) { mutableStateOf(videos.toMutableList()) }
    var itemForReorder by remember { mutableStateOf<Pair<Int, VideoItem>?>(null) }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex in queueList.indices && toIndex in queueList.indices && fromIndex != toIndex) {
            val updated = queueList.toMutableList()
            val item = updated.removeAt(fromIndex)
            updated.add(toIndex, item)
            queueList = updated
            onQueueReordered(updated)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1F22),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Orodha ya Video (Playlist Queue • ${queueList.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Shikilia (hold) kupanga video inayofuata kuchezwa",
                        color = F2WCyanPrimary,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.4f))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(queueList, key = { _, v -> v.id }) { index, video ->
                    val isPlaying = video.id == currentVideoId
                    var dragAccumulator by remember { mutableStateOf(0f) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isPlaying) F2WCyanPrimary.copy(alpha = 0.18f)
                                else Color(0xFF262930)
                            )
                            .border(
                                1.dp,
                                if (isPlaying) F2WCyanPrimary.copy(alpha = 0.7f)
                                else F2WCardBorder.copy(alpha = 0.25f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onVideoSelected(video)
                                onDismissRequest()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPlaying) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Playing",
                                tint = F2WCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Text(
                                text = "${index + 1}",
                                color = F2WTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = video.title,
                                color = if (isPlaying) F2WCyanPrimary else Color.White,
                                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${video.resolution} • ${video.durationText} • ${video.sizeText}",
                                color = F2WTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Move Up & Down Controls
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { moveItem(index, index - 1) },
                                enabled = index > 0,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Sogeza Juu",
                                    tint = if (index > 0) Color.White else Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { moveItem(index, index + 1) },
                                enabled = index < queueList.lastIndex,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Sogeza Chini",
                                    tint = if (index < queueList.lastIndex) Color.White else Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Hold / Drag Handle
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { itemForReorder = Pair(index, video) }
                                    .pointerInput(video.id) {
                                        detectDragGestures(
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragAccumulator += dragAmount.y
                                                if (dragAccumulator > 35f) {
                                                    dragAccumulator = 0f
                                                    moveItem(index, index + 1)
                                                } else if (dragAccumulator < -35f) {
                                                    dragAccumulator = 0f
                                                    moveItem(index, index - 1)
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DragHandle,
                                    contentDescription = "Shikilia kubadili mpangilio",
                                    tint = if (isPlaying) F2WCyanPrimary else Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Reorder Action Dialog
    itemForReorder?.let { (idx, vid) ->
        val currentPlayIndex = queueList.indexOfFirst { it.id == currentVideoId }
        AlertDialog(
            onDismissRequest = { itemForReorder = null },
            containerColor = Color(0xFF1E1F22),
            title = {
                Text(
                    text = "Mpangilio wa Video",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "\"${vid.title}\" (Ipo nafasi #${idx + 1})",
                        color = F2WTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Option 1: Play Next (right after current playing video)
                    if (currentPlayIndex != -1 && idx != currentPlayIndex + 1 && currentPlayIndex < queueList.lastIndex) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val targetIdx = if (idx > currentPlayIndex) currentPlayIndex + 1 else currentPlayIndex
                                    moveItem(idx, targetIdx)
                                    itemForReorder = null
                                }
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = F2WCyanPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Icheze Inayofuata (Play Next)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }

                    // Option 2: Move to Top
                    if (idx > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    moveItem(idx, 0)
                                    itemForReorder = null
                                }
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = F2WCyanPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Peleka Juu Kabisa (Namba 1)", color = Color.White, fontSize = 14.sp)
                        }
                    }

                    // Option 3: Move to Bottom
                    if (idx < queueList.lastIndex) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    moveItem(idx, queueList.lastIndex)
                                    itemForReorder = null
                                }
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = F2WCyanPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Peleka Mwisho (Namba ${queueList.size})", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { itemForReorder = null }) {
                    Text("Funga", color = F2WCyanPrimary)
                }
            }
        )
    }
}

@Composable
fun EqualizerDialog(
    onDismissRequest: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(true) }
    var volumeBoost by remember { mutableFloatStateOf(100f) } // 100% - 200%
    var bassBoost by remember { mutableFloatStateOf(60f) } // 0 - 100%
    var virtualizer by remember { mutableFloatStateOf(45f) } // 0 - 100%
    var selectedPreset by remember { mutableStateOf("Bass Boost") }

    val presets = listOf("Normal", "Bass Boost", "Vocal", "Rock", "Pop", "Movie", "Classical", "Hip Hop")

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1F22),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = F2WCyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Equalizer & Bass",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = F2WCyanPrimary
                    )
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Volume Booster (100% to 200%)
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Volume Booster", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                            Text("${volumeBoost.toInt()}%", color = F2WCyanPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = volumeBoost,
                            onValueChange = { volumeBoost = it },
                            valueRange = 100f..200f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = F2WCyanPrimary,
                                activeTrackColor = F2WCyanPrimary
                            )
                        )
                    }
                }

                // Bass Boost
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bass Boost", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                            Text("${bassBoost.toInt()}%", color = F2WCyanPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = bassBoost,
                            onValueChange = { bassBoost = it },
                            valueRange = 0f..100f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = F2WCyanPrimary,
                                activeTrackColor = F2WCyanPrimary
                            )
                        )
                    }
                }

                // Virtualizer / 3D Surround
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("3D Virtualizer / Surround", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                            Text("${virtualizer.toInt()}%", color = F2WCyanPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = virtualizer,
                            onValueChange = { virtualizer = it },
                            valueRange = 0f..100f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = F2WCyanPrimary,
                                activeTrackColor = F2WCyanPrimary
                            )
                        )
                    }
                }

                // Sound Presets
                item {
                    Text(
                        text = "Vipimo Maalum (Presets):",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val chunked = presets.chunked(4)
                        chunked.forEach { rowPresets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowPresets.forEach { preset ->
                                    val isSelected = preset == selectedPreset
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) F2WCyanPrimary else Color(0xFF262930))
                                            .clickable(enabled = isEnabled) { selectedPreset = preset }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = preset,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Imekamilika", color = F2WCyanPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun AudioTrackSelectionDialog(
    availableTracks: List<String>,
    selectedTrack: String,
    onTrackSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1F22),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Audiotrack,
                    contentDescription = null,
                    tint = F2WCyanPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lugha ya Sauti (Audio Track)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(availableTracks) { _, track ->
                    val isSelected = track == selectedTrack
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) F2WCyanPrimary.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                onTrackSelected(track)
                                onDismissRequest()
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onTrackSelected(track)
                                onDismissRequest()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = F2WCyanPrimary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = track,
                            color = if (isSelected) Color.White else Color(0xFF949BA4),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.5.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Funga", color = F2WCyanPrimary)
            }
        }
    )
}

