package com.example.ui.screens.playlist

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.playlist.PlaylistItemModel
import com.example.data.playlist.UserPlaylist
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurface
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WTextTertiary
import com.example.ui.theme.F2WVioletAccent
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@OptIn(UnstableApi::class)
@Composable
fun PlaylistAudioPlayerDialog(
    playlist: UserPlaylist,
    initialIndex: Int = 0,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val items = playlist.items
    if (items.isEmpty()) {
        onClose()
        return
    }

    var currentIndex by remember {
        mutableIntStateOf(initialIndex.coerceIn(0, items.size - 1))
    }
    val currentTrack = items.getOrNull(currentIndex) ?: items.first()

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(currentTrack.durationMs.coerceAtLeast(1000L)) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    fun playTrackAt(index: Int) {
        val target = items.getOrNull(index) ?: return
        currentPositionMs = 0L
        durationMs = target.durationMs.coerceAtLeast(1000L)

        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        if (target.uriString.isNotBlank()) {
            val uri = if (target.uriString.startsWith("content://") || target.uriString.startsWith("file://")) {
                Uri.parse(target.uriString)
            } else {
                Uri.fromFile(File(target.uriString))
            }
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else {
            // Sample placeholder track mode
            exoPlayer.playWhenReady = true
        }
        isPlaying = true
    }

    LaunchedEffect(currentIndex) {
        playTrackAt(currentIndex)
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    if (exoPlayer.duration > 0) {
                        durationMs = exoPlayer.duration
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    // Sequential auto-play: automatically play the next track in order!
                    if (currentIndex < items.size - 1) {
                        currentIndex++
                    } else {
                        // Loop back to start or stop
                        currentIndex = 0
                    }
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Progress tick
    LaunchedEffect(isPlaying, currentIndex) {
        while (true) {
            if (exoPlayer.isPlaying) {
                currentPositionMs = exoPlayer.currentPosition
                if (exoPlayer.duration > 0) {
                    durationMs = exoPlayer.duration
                }
            } else if (isPlaying && currentTrack.uriString.isBlank()) {
                // Simulated timer for sample audio with no actual media URI
                currentPositionMs += 500L
                if (currentPositionMs >= durationMs) {
                    // Auto-advance next
                    if (currentIndex < items.size - 1) {
                        currentIndex++
                    } else {
                        currentIndex = 0
                    }
                }
            }
            delay(400)
        }
    }

    fun skipNext() {
        if (currentIndex < items.size - 1) {
            currentIndex++
        } else {
            currentIndex = 0
        }
    }

    fun skipPrevious() {
        if (currentPositionMs > 4000L) {
            exoPlayer.seekTo(0L)
            currentPositionMs = 0L
        } else if (currentIndex > 0) {
            currentIndex--
        } else {
            exoPlayer.seekTo(0L)
            currentPositionMs = 0L
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Color(0xFF0D111A),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(F2WVioletAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QueueMusic,
                            contentDescription = null,
                            tint = F2WVioletAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = playlist.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Track ${currentIndex + 1} of ${items.size} • Sequential Order",
                            color = F2WCyanPrimary,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_audio_player_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Album Art / Visualizer disk
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF1E1035),
                                    Color(0xFF0F172A),
                                    Color(0xFF003049)
                                )
                            )
                        )
                        .border(2.dp, F2WVioletAccent.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AudioWaveBars(isPlaying = isPlaying)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Title and Subtitle
                Text(
                    text = currentTrack.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentTrack.subtitle.ifBlank { "Audio Track" },
                    color = F2WTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Play Position Seek Bar (matching the sleek magenta style without tall thumb line)
                val progress = if (durationMs > 0L) {
                    (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatAudioTime(currentPositionMs),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    Slider(
                        value = progress,
                        onValueChange = { fraction ->
                            val seekTarget = (fraction * durationMs).toLong()
                            currentPositionMs = seekTarget
                            exoPlayer.seekTo(seekTarget)
                        },
                        thumb = {},
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Transparent,
                            activeTrackColor = Color(0xFFC0157B),
                            inactiveTrackColor = Color(0xFF7E8088)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("playlist_audio_seekbar")
                    )

                    Text(
                        text = formatAudioTime(durationMs),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Controls: Prev, Big Play/Pause, Next
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Track
                    IconButton(
                        onClick = { skipPrevious() },
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("audio_player_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Play/Pause Center Button
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(F2WCyanPrimary, F2WVioletAccent))
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.White),
                                onClick = {
                                    if (isPlaying) {
                                        exoPlayer.pause()
                                        isPlaying = false
                                    } else {
                                        exoPlayer.play()
                                        isPlaying = true
                                    }
                                }
                            )
                            .testTag("audio_player_play_pause_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Next Track
                    IconButton(
                        onClick = { skipNext() },
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("audio_player_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next Track",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun AudioWaveBars(isPlaying: Boolean) {
    val transition = rememberInfiniteTransition(label = "audio_bars")
    val heights = (0 until 5).map { index ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = if (isPlaying) 1.0f else 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 350 + index * 120, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { anim ->
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height((42.dp * anim.value).coerceAtLeast(10.dp))
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(F2WCyanPrimary, F2WVioletAccent)
                        )
                    )
            )
        }
    }
}

private fun formatAudioTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    val h = m / 60
    return if (h > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", h, m % 60, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}
