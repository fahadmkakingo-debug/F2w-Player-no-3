package com.example.ui.screens.audio

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.AudioPlaybackManager
import com.example.data.audio.AudioRepeatMode
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WTextTertiary
import com.example.ui.theme.F2WVioletAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerFullScreen(
    audioManager: AudioPlaybackManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    val currentTrack by audioManager.currentTrack.collectAsState()
    val isPlaying by audioManager.isPlaying.collectAsState()
    val currentPosMs by audioManager.currentPositionMs.collectAsState()
    val durationMs by audioManager.durationMs.collectAsState()
    val playbackSpeed by audioManager.playbackSpeed.collectAsState()
    val repeatMode by audioManager.repeatMode.collectAsState()
    val isShuffle by audioManager.isShuffle.collectAsState()
    val coverBitmap by audioManager.currentCoverBitmap.collectAsState()
    val favoriteIds by audioManager.favoriteTrackIds.collectAsState()
    val sleepMinutes by audioManager.sleepTimerRemainingMinutes.collectAsState()
    val playlist by audioManager.playlist.collectAsState()
    val currentIndex by audioManager.currentIndex.collectAsState()

    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showTrackDetailsDialog by remember { mutableStateOf(false) }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderValue by remember { mutableFloatStateOf(0f) }

    val track = currentTrack ?: run {
        onBack()
        return
    }

    val isFavorited = favoriteIds.contains(track.id)

    fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_spin_angle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1E212B),
                        Color(0xFF141720),
                        Color(0xFF0D0F15)
                    )
                )
            )
            .testTag("audio_player_fullscreen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar: Back Arrow, Equalizer, More Options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("fullscreen_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Rudi",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showEqualizerDialog = true },
                        modifier = Modifier.testTag("fullscreen_equalizer_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Equalizer,
                            contentDescription = "Equalizer",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.testTag("fullscreen_more_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Track Details (Taarifa)") },
                                onClick = {
                                    showMoreMenu = false
                                    showTrackDetailsDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sleep Timer (${sleepMinutes?.let { "${it}m" } ?: "Off"})") },
                                onClick = {
                                    showMoreMenu = false
                                    showSleepTimerDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Audio (Shiriki)") },
                                onClick = {
                                    showMoreMenu = false
                                    Toast.makeText(context, "Sharing: ${track.title}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Large Circular Rotating Vinyl / Album Art Disc
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .aspectRatio(1f)
                    .shadow(elevation = 24.dp, shape = CircleShape, ambientColor = F2WVioletAccent, spotColor = F2WCyanPrimary)
                    .clip(CircleShape)
                    .background(Color(0xFF0A0C10))
                    .border(6.dp, Color(0xFF1E2433), CircleShape)
                    .rotate(if (isPlaying) rotationAngle else 0f),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl outer ridges
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFF282F42), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFF282F42).copy(alpha = 0.6f), CircleShape)
                )

                // Center Album Art Cover
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF131720)),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverBitmap != null) {
                        Image(
                            bitmap = coverBitmap!!.asImageBitmap(),
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            F2WCyanPrimary.copy(alpha = 0.35f),
                                            F2WVioletAccent.copy(alpha = 0.45f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = F2WCyanPrimary,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }

                    // Center spindle hole
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0A0C10))
                            .border(2.dp, Color(0xFF333A4D), CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Track Info Row: Sleep Timer, Title & Artist, Favorite Heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { showSleepTimerDialog = true },
                    modifier = Modifier.size(42.dp).testTag("fullscreen_timer_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = "Sleep Timer",
                        tint = if (sleepMinutes != null) F2WCyanPrimary else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.subtitle.ifBlank { "Unknown Artist" },
                        color = F2WTextSecondary,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                IconButton(
                    onClick = { audioManager.toggleFavorite(track.id) },
                    modifier = Modifier.size(42.dp).testTag("fullscreen_fav_btn")
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorited) Color(0xFFEF4444) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Progress Slider Bar
            val activeProgress = if (isDraggingSlider) {
                dragSliderValue
            } else {
                if (durationMs > 0) (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
            }

            val displayedTimeMs = if (isDraggingSlider) (dragSliderValue * durationMs).toLong() else currentPosMs

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = activeProgress,
                    onValueChange = {
                        isDraggingSlider = true
                        dragSliderValue = it
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        audioManager.seekTo((dragSliderValue * durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF10B981),
                        activeTrackColor = Color(0xFF10B981),
                        inactiveTrackColor = Color(0xFF333A4D)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fullscreen_audio_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(displayedTimeMs),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatTime(durationMs),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Main Playback Controls: Shuffle, Prev, Play/Pause, Next, Queue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { audioManager.toggleShuffle() },
                    modifier = Modifier.size(44.dp).testTag("fullscreen_shuffle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) Color(0xFF10B981) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = { audioManager.playPrevious() },
                    modifier = Modifier.size(52.dp).testTag("fullscreen_prev_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { audioManager.togglePlayPause() }
                        .testTag("fullscreen_play_pause_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color(0xFF0F141C),
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = { audioManager.playNext() },
                    modifier = Modifier.size(52.dp).testTag("fullscreen_next_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier.size(44.dp).testTag("fullscreen_queue_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Bottom Secondary Controls Row: Rewind 10s, Speed Pill (1.0x), Repeat, Forward 10s
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { audioManager.seekBy(-10000L) },
                    modifier = Modifier.size(44.dp).testTag("fullscreen_rewind_10_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay10,
                        contentDescription = "Rudisha nyuma sekunde 10",
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF222838))
                        .border(1.dp, F2WCardBorder, RoundedCornerShape(20.dp))
                        .clickable { audioManager.cyclePlaybackSpeed() }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("fullscreen_speed_pill")
                ) {
                    Text(
                        text = "${playbackSpeed}X",
                        color = Color(0xFF10B981),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { audioManager.toggleRepeat() },
                    modifier = Modifier.size(44.dp).testTag("fullscreen_repeat_btn")
                ) {
                    Icon(
                        imageVector = if (repeatMode == AudioRepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat Mode",
                        tint = if (repeatMode != AudioRepeatMode.OFF) Color(0xFF10B981) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = { audioManager.seekBy(10000L) },
                    modifier = Modifier.size(44.dp).testTag("fullscreen_forward_10_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Forward10,
                        contentDescription = "Mbele sekunde 10",
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }

    // Queue / Playlist Modal Bottom Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = Color(0xFF181C26)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Playing Queue (${playlist.size} Tracks)",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(playlist) { index, item ->
                        val isCurrent = index == currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) Color(0x3310B981) else Color(0xFF1E2330))
                                .border(
                                    1.dp,
                                    if (isCurrent) Color(0xFF10B981) else F2WCardBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    audioManager.playPlaylist(playlist, index)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = if (isCurrent) Color(0xFF10B981) else F2WTextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(28.dp)
                                )
                                Column {
                                    Text(
                                        text = item.title,
                                        color = if (isCurrent) Color(0xFF10B981) else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.subtitle.ifBlank { "Audio" },
                                        color = F2WTextSecondary,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                text = item.durationText,
                                color = F2WTextTertiary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        val timerOptions = listOf(15, 30, 45, 60, 90)
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            containerColor = Color(0xFF1C212D),
            title = {
                Text(
                    text = "Sleep Timer",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (sleepMinutes != null) "Active Timer: $sleepMinutes minutes remaining" else "Turn off playback automatically:",
                        color = F2WTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    timerOptions.forEach { mins ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    audioManager.setSleepTimer(mins)
                                    showSleepTimerDialog = false
                                    Toast.makeText(context, "Sleep timer set for $mins minutes", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$mins Minutes",
                                color = if (sleepMinutes == mins) Color(0xFF10B981) else Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (sleepMinutes != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                audioManager.cancelSleepTimer()
                                showSleepTimerDialog = false
                                Toast.makeText(context, "Sleep timer cancelled", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Turn Off Timer", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Close", color = F2WCyanPrimary)
                }
            }
        )
    }

    // Equalizer Screen (matches XPlayer exactly)
    if (showEqualizerDialog) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            com.example.ui.screens.equalizer.EqualizerScreen(
                onBack = { showEqualizerDialog = false }
            )
        }
    }

    // Track Details Dialog
    if (showTrackDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showTrackDetailsDialog = false },
            containerColor = Color(0xFF1C212D),
            title = {
                Text(
                    text = "Track Information",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow("Title", track.title)
                    DetailRow("Artist", track.subtitle.ifBlank { "Unknown" })
                    DetailRow("Duration", track.durationText)
                    DetailRow("File Size", track.sizeText)
                    DetailRow("Location", track.uriString)
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrackDetailsDialog = false }) {
                    Text("Close", color = F2WCyanPrimary)
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = F2WTextSecondary, fontSize = 11.5.sp)
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
