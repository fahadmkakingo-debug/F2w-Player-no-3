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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoMoreOptionsSheet(
    isNightMode: Boolean,
    onNightModeToggle: () -> Unit,
    isAudioOnlyMode: Boolean,
    onAudioOnlyToggle: () -> Unit,
    repeatMode: String,
    onRepeatModeCycle: () -> Unit,
    onEqualizerClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onVideoDetailsClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1F22),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Chaguo Zaidi za Kichezaji (More Smart Options)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Audio Only Background Mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onAudioOnlyToggle()
                        onDismissRequest()
                    }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Audiotrack, contentDescription = null, tint = F2WCyanPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Modi ya Sauti Tu (Audio-Only Mode)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Zima picha kusikiliza sauti na kutunza chaji", color = F2WTextSecondary, fontSize = 11.5.sp)
                    }
                }
                Switch(
                    checked = isAudioOnlyMode,
                    onCheckedChange = {
                        onAudioOnlyToggle()
                        onDismissRequest()
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = F2WCyanPrimary)
                )
            }

            // 2. Night Mode / Eye Protection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onNightModeToggle()
                    }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Nightlight, contentDescription = null, tint = Color(0xFFFFD54F))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Kinga ya Macho (Night Mode / Eye Care)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Punguza mwanga mkali gizani", color = F2WTextSecondary, fontSize = 11.5.sp)
                    }
                }
                Switch(
                    checked = isNightMode,
                    onCheckedChange = { onNightModeToggle() },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFFFFD54F))
                )
            }

            // 3. Repeat Mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onRepeatModeCycle() }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = F2WVioletAccent)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Kurudia Video (Repeat Mode)", color = Color.White, fontSize = 14.sp)
                }
                Text(text = repeatMode, color = F2WCyanPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // 4. Equalizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onEqualizerClick()
                        onDismissRequest()
                    }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = F2WCyanPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Kiboresha Sauti (Equalizer & Bass)", color = Color.White, fontSize = 14.sp)
                    Text("Bass Boost, Virtualizer na Sound Presets", color = F2WTextSecondary, fontSize = 11.5.sp)
                }
            }

            // 5. Sleep Timer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onSleepTimerClick()
                        onDismissRequest()
                    }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Kipima Muda cha Kulala (Sleep Timer)", color = Color.White, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
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
