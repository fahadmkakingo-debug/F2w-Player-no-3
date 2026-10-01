@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WVioletAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XPlayerTopBar(
    title: String,
    decoderMode: String,
    onBackClick: () -> Unit,
    onDecoderClick: () -> Unit,
    onSubtitlesClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.95f),
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .displayCutoutPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("player_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Rudi Nyuma",
                    tint = Color.White
                )
            }

            // Video Title
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )

            // Right Action Icons
            // 1. Decoder (HW / SW)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(onClick = onDecoderClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.HighQuality,
                        contentDescription = "Decoder",
                        tint = F2WCyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = decoderMode,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 2. Subtitles & Audio [CC]
            IconButton(onClick = onSubtitlesClick) {
                Icon(
                    imageVector = Icons.Filled.ClosedCaption,
                    contentDescription = "Subtitles na Sauti",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 3. Playlist Queue
            IconButton(onClick = onPlaylistClick) {
                Icon(
                    imageVector = Icons.Filled.QueueMusic,
                    contentDescription = "Orodha ya Video",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 4. More Options
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Chaguo Zaidi",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun XPlayerQuickControlsRow(
    isOrientationLocked: Boolean,
    isMuted: Boolean,
    isBackgroundAudio: Boolean,
    aspectRatioText: String,
    speedText: String,
    onOrientationToggle: () -> Unit,
    onMuteToggle: () -> Unit,
    onBackgroundAudioToggle: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onPipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .displayCutoutPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Rotation Lock
        QuickCircleButton(
            icon = Icons.Filled.ScreenRotation,
            isActive = isOrientationLocked,
            onClick = onOrientationToggle,
            contentDescription = "Mzunguko wa Skrini"
        )

        // 2. Quick Mute
        QuickCircleButton(
            icon = if (isMuted) Icons.Filled.VolumeMute else Icons.Filled.VolumeUp,
            isActive = isMuted,
            onClick = onMuteToggle,
            contentDescription = "Sauti"
        )

        // 3. Background Play (Headphones)
        QuickCircleButton(
            icon = Icons.Filled.Headphones,
            isActive = isBackgroundAudio,
            onClick = onBackgroundAudioToggle,
            contentDescription = "Background Play (Audio)"
        )

        // 4. Pop-up / Floating Window (PiP)
        QuickCircleButton(
            icon = Icons.Filled.PictureInPictureAlt,
            isActive = false,
            onClick = onPipClick,
            contentDescription = "Pop-up / Floating Window (PiP)"
        )

        // 5. Playback Speed Button (e.g. "1.0X", "1.5X")
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (speedText != "1X" && speedText != "1.0X") F2WCyanPrimary.copy(alpha = 0.25f) else Color(0x661E1E1E))
                .border(
                    1.dp,
                    if (speedText != "1X" && speedText != "1.0X") F2WCyanPrimary else Color.White.copy(alpha = 0.3f),
                    CircleShape
                )
                .clickable(onClick = onSpeedClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = speedText,
                color = if (speedText != "1X" && speedText != "1.0X") F2WCyanPrimary else Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 6. Aspect Ratio Switcher (Fit, 16:9, Stretch)
        Box(
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x661E1E1E))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .clickable(onClick = onAspectRatioClick)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AspectRatio,
                    contentDescription = "Aspect Ratio",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = aspectRatioText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun QuickCircleButton(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isActive) F2WCyanPrimary.copy(alpha = 0.35f) else Color(0x661E1E1E))
            .border(
                1.dp,
                if (isActive) F2WCyanPrimary else Color.White.copy(alpha = 0.3f),
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) F2WCyanPrimary else Color.White,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
fun XPlayerScreenshotButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable(onClick = onClick)
            .testTag("player_screenshot_btn"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CameraAlt,
            contentDescription = "Piga Picha ya Skrini",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun XPlayerBottomBar(
    currentTimeText: String,
    totalDurationText: String,
    progress: Float,
    isPlaying: Boolean,
    isLocked: Boolean,
    onSeek: (Float) -> Unit,
    onLockToggle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPipToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.7f),
                        Color.Black.copy(alpha = 0.98f)
                    )
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Time & Seekbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Current Position Text
                Text(
                    text = currentTimeText,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(end = 8.dp)
                )

                // Seekbar Slider without tall thumb line (smooth touch-and-drag directly on track)
                Slider(
                    value = progress.coerceIn(0f, 1f),
                    onValueChange = onSeek,
                    thumb = {},
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Transparent,
                        activeTrackColor = Color(0xFFC0157B),
                        inactiveTrackColor = Color(0xFF7E8088)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_seekbar")
                )

                // Total Duration Text
                Text(
                    text = totalDurationText,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 2: Action Controls (Kid Lock, Prev, Big Play/Pause, Next, Pop-up Window)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Kid Lock / Screen Lock Button
                IconButton(onClick = onLockToggle) {
                    Icon(
                        imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = "Kid Lock / Funga Skrini",
                        tint = if (isLocked) F2WCyanPrimary else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 2. Previous Video Button
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Video Iliyopita",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // 3. Center Big Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White),
                            onClick = onPlayPause
                        )
                        .testTag("player_play_pause_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Simamisha" else "Cheza",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // 4. Next Video Button
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Video Inayofuata",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // 5. Pop-up / Floating Window (PiP)
                IconButton(onClick = onPipToggle) {
                    Icon(
                        imageVector = Icons.Filled.PictureInPictureAlt,
                        contentDescription = "Pop-up / Floating Window (PiP)",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun XPlayerLockedFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.8f))
            .border(1.5.dp, F2WCyanPrimary, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("player_unlock_floating_btn"),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Fungua Skrini",
                tint = F2WCyanPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Gusa kufungua (Unlock)",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AudioModeVisualizer(
    title: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "audio_anim")
    val height1 by transition.animateFloat(
        initialValue = 12f,
        targetValue = 48f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val height2 by transition.animateFloat(
        initialValue = 20f,
        targetValue = 64f,
        animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val height3 by transition.animateFloat(
        initialValue = 8f,
        targetValue = 36f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val height4 by transition.animateFloat(
        initialValue = 16f,
        targetValue = 54f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h4"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1012)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(F2WCyanPrimary.copy(alpha = 0.15f))
                    .border(2.dp, F2WCyanPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Headphones,
                    contentDescription = null,
                    tint = F2WCyanPrimary,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Modi ya Sauti Tu (Inaokoa betri na skrini)",
                color = F2WTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Animated Equalizer Bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.height(70.dp)
            ) {
                listOf(height1, height2, height4, height3, height2, height1, height4).forEach { h ->
                    val animH = if (isPlaying) h else 8f
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(animH.dp)
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
    }
}

/**
 * A compact, pill-shaped overlay that matches XPlayer:
 * "✕  Continue playing from where you stopped.  |  Start Over"
 * Automatically dismisses after 5 seconds while video playback continues uninterrupted.
 */
@Composable
fun ResumePlaybackPromptOverlay(
    visible: Boolean,
    onRestartClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 2 },
        exit = fadeOut(tween(250)) + slideOutVertically(tween(300)) { it / 2 },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.88f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("resume_playback_prompt")
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ✕ Dismiss icon
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 14.dp),
                            onClick = onDismissClick
                        )
                        .testTag("dismiss_resume_prompt_btn")
                )

                // Message Text
                Text(
                    text = "Continue playing from where you stopped.",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Vertical Divider Line "|"
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(13.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                )

                // "Start Over" button
                Text(
                    text = "Start Over",
                    color = Color(0xFFE040FB), // Magenta accent matching XPlayer
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onRestartClick
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("restart_video_btn")
                )
            }
        }
    }
}

