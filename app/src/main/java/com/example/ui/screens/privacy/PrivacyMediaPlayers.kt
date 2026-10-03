@file:OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)

package com.example.ui.screens.privacy

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.security.PrivacyVaultItem
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

/**
 * Full in-vault video player screen.
 * Plays the private encrypted video directly inside Privacy without leaking it to external players.
 */
@Composable
fun PrivacyVideoPlayerScreen(
    item: PrivacyVaultItem,
    onClose: () -> Unit,
    onRestore: (() -> Unit)? = null,
    onDeletePermanently: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)

    val context = LocalContext.current
    val vaultManager = remember { com.example.data.security.PrivacyVaultManager.getInstance(context) }

    var videoFile by remember { mutableStateOf<File?>(null) }
    var isDecrypting by remember { mutableStateOf(true) }

    LaunchedEffect(item.vaultPath) {
        isDecrypting = true
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            videoFile = vaultManager.getDecryptedTempFile(item)
            isDecrypting = false
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(item.durationMs) }
    var showControls by remember { mutableStateOf(true) }

    val exoPlayer = remember(videoFile) {
        videoFile?.let { file ->
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                prepare()
                playWhenReady = true
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    if (exoPlayer?.duration ?: 0 > 0) {
                        durationMs = exoPlayer?.duration ?: 0L
                    }
                }
            }
        }
        exoPlayer?.addListener(listener)

        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
        }
    }

    // Progress update loop
    LaunchedEffect(exoPlayer) {
        while (exoPlayer != null) {
            if (exoPlayer.isPlaying) {
                currentPositionMs = exoPlayer.currentPosition
                if (exoPlayer.duration > 0) {
                    durationMs = exoPlayer.duration
                }
            }
            delay(500)
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { showControls = !showControls }
            )
            .testTag("privacy_video_player_screen")
    ) {
        if (isDecrypting || exoPlayer == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = F2WCyanPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ina-decrypt faili ya video...",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // Video View
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("privacy_video_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.fileName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = F2WCyanPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Encrypted Vault Video • ${item.sizeFormatted}",
                                color = F2WCyanPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (onRestore != null) {
                        IconButton(
                            onClick = onRestore,
                            modifier = Modifier.testTag("privacy_video_restore_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Restore,
                                contentDescription = "Restore to Device",
                                tint = F2WVioletAccent
                            )
                        }
                    }

                    if (onDeletePermanently != null) {
                        IconButton(
                            onClick = onDeletePermanently,
                            modifier = Modifier.testTag("privacy_video_delete_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteForever,
                                contentDescription = "Delete Permanently",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }

                // Center Play/Pause & Replay/Forward
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val current = exoPlayer?.currentPosition ?: 0L
                            val newPos = (current - 10000).coerceAtLeast(0)
                            exoPlayer?.seekTo(newPos)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(F2WCyanPrimary)
                            .clickable {
                                if (isPlaying) exoPlayer?.pause() else exoPlayer?.play()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF070B12),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val current = exoPlayer?.currentPosition ?: 0L
                            val newPos = (current + 10000).coerceAtMost(durationMs)
                            exoPlayer?.seekTo(newPos)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Seekbar & Time (Play Position Bar)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(currentPositionMs),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Slider(
                        value = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                        onValueChange = { fraction ->
                            val seekMs = (fraction * durationMs).toLong()
                            currentPositionMs = seekMs
                            exoPlayer?.seekTo(seekMs)
                        },
                        thumb = {},
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Transparent,
                            activeTrackColor = Color(0xFFC0157B),
                            inactiveTrackColor = Color(0xFF7E8088)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("privacy_video_seekbar")
                    )

                    Text(
                        text = formatTime(durationMs),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dedicated in-vault audio player dialog.
 * Plays private music/audio files with real-time progress and player controls.
 */
@Composable
fun PrivacyAudioPlayerDialog(
    item: PrivacyVaultItem,
    onClose: () -> Unit,
    onRestore: (() -> Unit)? = null,
    onDeletePermanently: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val vaultManager = remember { com.example.data.security.PrivacyVaultManager.getInstance(context) }

    var audioFile by remember { mutableStateOf<File?>(null) }
    var isDecrypting by remember { mutableStateOf(true) }

    LaunchedEffect(item.vaultPath) {
        isDecrypting = true
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            audioFile = vaultManager.getDecryptedTempFile(item)
            isDecrypting = false
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(item.durationMs) }

    val exoPlayer = remember(audioFile) {
        audioFile?.let { file ->
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                prepare()
                playWhenReady = true
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    if (exoPlayer?.duration ?: 0 > 0) {
                        durationMs = exoPlayer?.duration ?: 0L
                    }
                }
            }
        }
        exoPlayer?.addListener(listener)

        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (exoPlayer != null) {
            if (exoPlayer.isPlaying) {
                currentPositionMs = exoPlayer.currentPosition
                if (exoPlayer.duration > 0) {
                    durationMs = exoPlayer.duration
                }
            }
            delay(500)
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Color(0xFF131722),
        icon = {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                F2WVioletAccent.copy(alpha = 0.35f),
                                Color(0xFF181C2C)
                            )
                        )
                    )
                    .border(1.5.dp, F2WVioletAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Audiotrack,
                    contentDescription = null,
                    tint = F2WVioletAccent,
                    modifier = Modifier.size(44.dp)
                )
            }
        },
        title = {
            Text(
                text = item.fileName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = F2WVioletAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Encrypted Vault Audio • ${item.sizeFormatted}",
                        color = F2WVioletAccent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Slider
                Slider(
                    value = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                    onValueChange = { fraction ->
                        val seekMs = (fraction * durationMs).toLong()
                        currentPositionMs = seekMs
                        exoPlayer?.seekTo(seekMs)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = F2WVioletAccent,
                        activeTrackColor = F2WVioletAccent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentPositionMs), color = Color.White, fontSize = 11.5.sp)
                    Text(formatTime(durationMs), color = F2WTextSecondary, fontSize = 11.5.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val current = exoPlayer?.currentPosition ?: 0L
                            val newPos = (current - 10000).coerceAtLeast(0)
                            exoPlayer?.seekTo(newPos)
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Rewind",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(F2WVioletAccent)
                            .clickable {
                                if (isPlaying) exoPlayer?.pause() else exoPlayer?.play()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val current = exoPlayer?.currentPosition ?: 0L
                            val newPos = (current + 10000).coerceAtMost(durationMs)
                            exoPlayer?.seekTo(newPos)
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Forward",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onRestore != null) {
                        TextButton(
                            onClick = onRestore,
                            modifier = Modifier.testTag("audio_player_restore_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Restore,
                                contentDescription = null,
                                tint = F2WVioletAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore", color = F2WVioletAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (onDeletePermanently != null) {
                        TextButton(
                            onClick = onDeletePermanently,
                            modifier = Modifier.testTag("audio_player_delete_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteForever,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                TextButton(onClick = onClose) {
                    Text("Close", color = F2WTextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

/**
 * Fullscreen Image Viewer for private photos.
 */
@Composable
fun PrivacyImageViewerDialog(
    item: PrivacyVaultItem,
    onClose: () -> Unit,
    onRestore: (() -> Unit)? = null,
    onDeletePermanently: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val vaultManager = remember { com.example.data.security.PrivacyVaultManager.getInstance(context) }

    var imageFile by remember { mutableStateOf<File?>(null) }
    var isDecrypting by remember { mutableStateOf(true) }

    LaunchedEffect(item.vaultPath) {
        isDecrypting = true
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            imageFile = vaultManager.getDecryptedTempFile(item)
            isDecrypting = false
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Color(0xFF0C0F17),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Encrypted Vault Image • ${item.sizeFormatted}",
                        color = Color(0xFF10B981),
                        fontSize = 11.5.sp
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isDecrypting || imageFile == null) {
                    CircularProgressIndicator(color = F2WCyanPrimary)
                } else {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = item.fileName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onRestore != null) {
                        TextButton(
                            onClick = onRestore,
                            modifier = Modifier.testTag("image_viewer_restore_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Restore,
                                contentDescription = null,
                                tint = F2WVioletAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore", color = F2WVioletAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (onDeletePermanently != null) {
                        TextButton(
                            onClick = onDeletePermanently,
                            modifier = Modifier.testTag("image_viewer_delete_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteForever,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                TextButton(onClick = onClose) {
                    Text("Done", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

/**
 * Bottom Sheet presenting operations for a vault item:
 * 1. Play / View
 * 2. Restore to Device / Remove from Privacy
 * 3. Delete Permanently
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyItemActionBottomSheet(
    item: PrivacyVaultItem,
    onDismissRequest: () -> Unit,
    onPlayOrView: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = F2WSurfaceElevated,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF384055))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp, top = 6.dp)
                .testTag("privacy_item_action_sheet")
        ) {
            // Header info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF10131C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.mediaType.uppercase()) {
                            "VIDEO" -> Icons.Filled.Videocam
                            "AUDIO" -> Icons.Filled.Audiotrack
                            else -> Icons.Filled.Visibility
                        },
                        contentDescription = null,
                        tint = when (item.mediaType.uppercase()) {
                            "VIDEO" -> F2WCyanPrimary
                            "AUDIO" -> F2WVioletAccent
                            else -> Color(0xFF10B981)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.mediaType} • ${item.sizeFormatted}",
                        color = F2WTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Action 1: Play / View
            ActionMenuItem(
                title = if (item.mediaType.equals("IMAGE", ignoreCase = true)) "View Photo" else "Play ${item.mediaType.lowercase().replaceFirstChar { it.uppercase() }}",
                subtitle = "View or play directly inside encrypted vault",
                icon = if (item.mediaType.equals("IMAGE", ignoreCase = true)) Icons.Filled.Visibility else Icons.Filled.PlayArrow,
                iconColor = F2WCyanPrimary,
                testTag = "action_play_view_btn",
                onClick = {
                    onDismissRequest()
                    onPlayOrView()
                }
            )

            // Action 2: Restore to Device (Remove from Privacy)
            ActionMenuItem(
                title = "Restore to Device (Rejesha kwenye Simu)",
                subtitle = "Move back to original phone storage so it reappears in your normal files",
                icon = Icons.Filled.Restore,
                iconColor = F2WVioletAccent,
                testTag = "action_restore_btn",
                onClick = {
                    onDismissRequest()
                    onRestore()
                }
            )

            // Action 3: Delete Permanently
            ActionMenuItem(
                title = "Delete Permanently (Futa Kabisa)",
                subtitle = "Erase permanently from device and vault. Cannot be recovered",
                icon = Icons.Filled.DeleteForever,
                iconColor = Color(0xFFEF4444),
                testTag = "action_delete_permanently_btn",
                onClick = {
                    onDismissRequest()
                    onDeletePermanently()
                }
            )
        }
    }
}

@Composable
private fun ActionMenuItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = iconColor),
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = F2WTextSecondary,
                fontSize = 11.5.sp
            )
        }
    }
}

/**
 * Confirmation dialog before restoring file back to normal storage.
 */
@Composable
fun PrivacyRestoreConfirmDialog(
    item: PrivacyVaultItem,
    isRestoring: Boolean,
    onDismissRequest: () -> Unit,
    onConfirmRestore: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isRestoring) onDismissRequest() },
        containerColor = F2WSurfaceElevated,
        icon = {
            Icon(
                imageVector = Icons.Filled.Restore,
                contentDescription = null,
                tint = F2WVioletAccent,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Restore to Device Storage?",
                color = F2WTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.5.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Do you want to restore \"${item.fileName}\" back to your normal phone storage?",
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "After restoring, the file will be removed from Privacy Vault and will become visible again in your phone gallery and media player.",
                    color = F2WTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                if (isRestoring) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = F2WVioletAccent,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Restoring file to storage...", color = F2WVioletAccent, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmRestore,
                colors = ButtonDefaults.buttonColors(containerColor = F2WVioletAccent),
                enabled = !isRestoring,
                modifier = Modifier.testTag("confirm_restore_btn")
            ) {
                Text(
                    text = if (isRestoring) "Restoring..." else "Restore File",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (!isRestoring) {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel", color = F2WTextSecondary)
                }
            }
        }
    )
}

/**
 * Confirmation dialog before permanently deleting file.
 */
@Composable
fun PrivacyDeletePermanentlyConfirmDialog(
    item: PrivacyVaultItem,
    isDeleting: Boolean,
    onDismissRequest: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismissRequest() },
        containerColor = F2WSurfaceElevated,
        icon = {
            Icon(
                imageVector = Icons.Filled.DeleteForever,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Delete File Permanently?",
                color = Color(0xFFEF4444),
                fontWeight = FontWeight.Bold,
                fontSize = 16.5.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to permanently delete \"${item.fileName}\"?",
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This file will be completely erased from your phone and Privacy Vault. This action CANNOT be undone.",
                    color = Color(0xFFFCA5A5),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                if (isDeleting) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Deleting file permanently...", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                enabled = !isDeleting,
                modifier = Modifier.testTag("confirm_delete_permanently_btn")
            ) {
                Text(
                    text = if (isDeleting) "Deleting..." else "Delete Permanently",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (!isDeleting) {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel", color = F2WTextSecondary)
                }
            }
        }
    )
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
