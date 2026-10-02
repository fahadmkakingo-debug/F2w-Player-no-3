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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GraphicEq
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
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
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
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("player_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Rudi Nyuma",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Video Title
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
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
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.HighQuality,
                        contentDescription = "Decoder",
                        tint = F2WCyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = decoderMode,
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 2. Subtitles & Audio [CC]
            IconButton(
                onClick = onSubtitlesClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ClosedCaption,
                    contentDescription = "Subtitles na Sauti",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 3. Playlist Queue
            IconButton(
                onClick = onPlaylistClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.QueueMusic,
                    contentDescription = "Orodha ya Video",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 4. More Options Sheet
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Chaguo Zaidi",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
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
    isNightMode: Boolean,
    isMirrored: Boolean,
    abRepeatStateText: String,
    aspectRatioText: String,
    speedText: String,
    decoderMode: String,
    onInteraction: () -> Unit = {},
    onOrientationToggle: () -> Unit,
    onMuteToggle: () -> Unit,
    onBackgroundAudioToggle: () -> Unit,
    onPipClick: () -> Unit,
    onNightModeToggle: () -> Unit,
    onSpeedClick: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onABRepeatClick: () -> Unit,
    onScreenshotClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onAudioTrackClick: () -> Unit,
    onSubtitlesClick: () -> Unit,
    onMirrorToggle: () -> Unit,
    onLockClick: () -> Unit,
    onDecoderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Keep controls alive while user is scrolling or touching this row
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            onInteraction()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .displayCutoutPadding()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        onInteraction()
                    }
                }
            }
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Rotation Lock
        QuickPillButton(
            icon = Icons.Filled.ScreenRotation,
            label = if (isOrientationLocked) "Locked" else "Rotate",
            isActive = isOrientationLocked,
            onClick = {
                onInteraction()
                onOrientationToggle()
            },
            contentDescription = "Mzunguko wa Skrini"
        )

        // 2. Quick Mute
        QuickPillButton(
            icon = if (isMuted) Icons.Filled.VolumeMute else Icons.Filled.VolumeUp,
            label = if (isMuted) "Muted" else "Mute",
            isActive = isMuted,
            onClick = {
                onInteraction()
                onMuteToggle()
            },
            contentDescription = "Sauti"
        )

        // 3. Background Play (Headphones)
        QuickPillButton(
            icon = Icons.Filled.Headphones,
            label = "BG Play",
            isActive = isBackgroundAudio,
            onClick = {
                onInteraction()
                onBackgroundAudioToggle()
            },
            contentDescription = "Background Audio Play"
        )

        // 4. Pop-up / Floating Window (PiP)
        QuickPillButton(
            icon = Icons.Filled.PictureInPictureAlt,
            label = "Pop-up",
            isActive = false,
            onClick = {
                onInteraction()
                onPipClick()
            },
            contentDescription = "Pop-up / Floating Window (PiP)"
        )

        // 5. Night Mode / Eye Protection
        QuickPillButton(
            icon = Icons.Filled.DarkMode,
            label = "Night",
            isActive = isNightMode,
            onClick = {
                onInteraction()
                onNightModeToggle()
            },
            contentDescription = "Kinga ya Macho (Night Mode)"
        )

        // 6. Playback Speed Button (e.g. "1.0X", "1.5X")
        QuickPillButton(
            icon = Icons.Filled.Speed,
            label = speedText,
            isActive = speedText != "1X" && speedText != "1.0X",
            onClick = {
                onInteraction()
                onSpeedClick()
            },
            contentDescription = "Kasi ya Video"
        )

        // 7. Aspect Ratio Switcher (Fit, 16:9, Stretch, Fill, etc.)
        QuickPillButton(
            icon = Icons.Filled.AspectRatio,
            label = aspectRatioText,
            isActive = aspectRatioText != "Fit",
            onClick = {
                onInteraction()
                onAspectRatioClick()
            },
            contentDescription = "Ukubwa wa Skrini (Aspect Ratio)"
        )

        // 8. Sleep Timer
        QuickPillButton(
            icon = Icons.Filled.Timer,
            label = "Timer",
            isActive = false,
            onClick = {
                onInteraction()
                onSleepTimerClick()
            },
            contentDescription = "Kipima Muda cha Kulala"
        )

        // 9. A-B Repeat
        QuickPillButton(
            icon = if (abRepeatStateText.isNotEmpty()) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            label = if (abRepeatStateText.isNotEmpty()) "Repeat $abRepeatStateText" else "A-B Loop",
            isActive = abRepeatStateText.isNotEmpty(),
            onClick = {
                onInteraction()
                onABRepeatClick()
            },
            contentDescription = "Kurudia Sehemu A-B"
        )

        // 10. Screenshot / Capture Screen
        QuickPillButton(
            icon = Icons.Filled.CameraAlt,
            label = "Capture",
            isActive = false,
            onClick = {
                onInteraction()
                onScreenshotClick()
            },
            contentDescription = "Piga Picha ya Skrini (Screenshot)"
        )

        // 11. Equalizer / Sound Boost
        QuickPillButton(
            icon = Icons.Filled.GraphicEq,
            label = "Equalizer",
            isActive = false,
            onClick = {
                onInteraction()
                onEqualizerClick()
            },
            contentDescription = "Equalizer na Bass Boost"
        )

        // 12. Audio Track / Language Switcher
        QuickPillButton(
            icon = Icons.Filled.Audiotrack,
            label = "Audio",
            isActive = false,
            onClick = {
                onInteraction()
                onAudioTrackClick()
            },
            contentDescription = "Lugha ya Sauti"
        )

        // 13. Subtitles & Closed Captions
        QuickPillButton(
            icon = Icons.Filled.ClosedCaption,
            label = "Subtitles",
            isActive = false,
            onClick = {
                onInteraction()
                onSubtitlesClick()
            },
            contentDescription = "Manukuu (Subtitles)"
        )

        // 14. Mirror / Flip Video
        QuickPillButton(
            icon = Icons.Filled.Flip,
            label = if (isMirrored) "Mirrored" else "Mirror",
            isActive = isMirrored,
            onClick = {
                onInteraction()
                onMirrorToggle()
            },
            contentDescription = "Geuza Video (Mirror Mode)"
        )

        // 15. Screen Lock (Kid Lock)
        QuickPillButton(
            icon = Icons.Filled.Lock,
            label = "Lock",
            isActive = false,
            onClick = {
                onInteraction()
                onLockClick()
            },
            contentDescription = "Funga Skrini (Kid Lock)"
        )

        // 16. Decoder (HW / SW)
        QuickPillButton(
            icon = Icons.Filled.HighQuality,
            label = decoderMode,
            isActive = decoderMode == "HW",
            onClick = {
                onInteraction()
                onDecoderClick()
            },
            contentDescription = "Decoder Mode (HW/SW)"
        )
    }
}

@Composable
private fun QuickPillButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isActive) F2WCyanPrimary.copy(alpha = 0.28f)
                else Color(0x991E1F24)
            )
            .border(
                1.dp,
                if (isActive) F2WCyanPrimary else Color.White.copy(alpha = 0.22f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) F2WCyanPrimary else Color.White,
                modifier = Modifier.size(15.dp)
            )
            if (label.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    color = if (isActive) F2WCyanPrimary else Color.White,
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
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

                // Sleek Volcanic Magma Fire & Frosted Glass TimeBar (SeekBar)
                VolcanicFireGlassTimeBar(
                    progress = progress.coerceIn(0f, 1f),
                    onSeek = onSeek,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
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

/**
 * Sleek 3D Volcanic Magma Fire & Frosted Glass TimeBar (SeekBar).
 * - Unplayed segment: Frosted crystal glass tube with specular light reflection and depth.
 * - Active played segment: Animated molten volcanic lava & fire flames flowing and pulsating with progress.
 * - Leading edge: Blazing molten lava orb with burning plasma aura and floating ember sparks.
 * - Height: Refined sleek 7dp-8dp bar with an ergonomic 34dp touch-and-drag hit area.
 */
@Composable
fun VolcanicFireGlassTimeBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "volcanic_fire_timebar")

    // Magma flow motion
    val magmaFlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "magma_flow"
    )

    // Flame heat flicker & pulsation
    val flameFlicker by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_flicker"
    )

    // Floating embers drift
    val emberProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ember_drift"
    )

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val currentProgress = if (isDragging) dragFraction else progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(34.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val target = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek(target)
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val target = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragFraction = target
                        onSeek(target)
                    },
                    onDragEnd = {
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val target = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragFraction = target
                        onSeek(target)
                    }
                )
            }
            .testTag("player_seekbar"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            if (canvasWidth <= 0f) return@Canvas

            val trackHeight = 7.dp.toPx()
            val trackY = (canvasHeight - trackHeight) / 2f
            val cornerRadius = trackHeight / 2f
            val activeWidth = (canvasWidth * currentProgress).coerceIn(0f, canvasWidth)

            // 1. Unplayed Segment: Frosted Crystal Glass Tube Channel
            val glassRect = RoundRect(
                left = 0f,
                top = trackY,
                right = canvasWidth,
                bottom = trackY + trackHeight,
                radiusX = cornerRadius,
                radiusY = cornerRadius
            )
            val glassPath = Path().apply { addRoundRect(glassRect) }

            // Glass background fill
            val glassBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x55FFFFFF), // top glass specular highlight
                    Color(0x22A0C4E8), // translucent frosted core
                    Color(0x35000000)  // subtle glass underside shadow
                ),
                startY = trackY,
                endY = trackY + trackHeight
            )
            drawPath(path = glassPath, brush = glassBrush)

            // Glass outer rim bevel border
            val glassBorderBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.50f),
                    Color(0x20FFFFFF),
                    Color.Black.copy(alpha = 0.50f)
                ),
                startY = trackY,
                endY = trackY + trackHeight
            )
            drawPath(path = glassPath, brush = glassBorderBrush, style = Stroke(width = 1.dp.toPx()))

            // 2. Active Segment: Blazing Volcanic Magma Lava & Fire Flames
            if (activeWidth > 0.5f) {
                val activeRect = RoundRect(
                    left = 0f,
                    top = trackY,
                    right = activeWidth,
                    bottom = trackY + trackHeight,
                    radiusX = cornerRadius,
                    radiusY = cornerRadius
                )
                val activePath = Path().apply { addRoundRect(activeRect) }

                // Volcanic Magma Core Gradient with animated flow shift
                val flowShift = magmaFlow * 120f
                val activeFireBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF800020), // Deep Magma Crimson
                        Color(0xFFD00000), // Fiery Ruby Red
                        Color(0xFFFF3D00), // Intense Lava Orange
                        Color(0xFFFF9100), // Molten Volcanic Amber
                        Color(0xFFFFDD00), // Golden Flame
                        Color(0xFFFFFBEA)  // White-hot plasma tip
                    ),
                    start = Offset(0f - flowShift, trackY),
                    end = Offset(activeWidth + (120f - flowShift), trackY + trackHeight)
                )
                drawPath(path = activePath, brush = activeFireBrush)

                // Lava Plasma Current Overlay (sinusoidal fiery ripples)
                val plasmaPath = Path().apply {
                    moveTo(0f, trackY + trackHeight / 2f)
                    for (x in 0..activeWidth.toInt() step 6) {
                        val nx = x / activeWidth.coerceAtLeast(1f)
                        val wave = sin((nx * 4f * PI.toFloat()) + (magmaFlow * 2f * PI.toFloat())) * (trackHeight * 0.28f * flameFlicker)
                        lineTo(x.toFloat(), trackY + trackHeight / 2f + wave)
                    }
                }
                drawPath(
                    path = plasmaPath,
                    color = Color(0x99FFF176),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Top intense flame glow shine
                val topGlowPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = 0f,
                            top = trackY,
                            right = activeWidth,
                            bottom = trackY + (trackHeight * 0.45f),
                            radiusX = cornerRadius,
                            radiusY = cornerRadius
                        )
                    )
                }
                drawPath(
                    path = topGlowPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f * flameFlicker.coerceAtMost(1f)),
                            Color.Transparent
                        ),
                        startY = trackY,
                        endY = trackY + (trackHeight * 0.45f)
                    )
                )

                // 3. Leading Fire Head / Molten Volcano Orb & Embers at current position
                val headCenterX = activeWidth
                val headCenterY = trackY + trackHeight / 2f

                // Outer Heat Halo / Flame Glow
                val haloRadius = (12.dp.toPx()) * (if (isDragging) 1.25f else flameFlicker)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xD0FF5400),
                            Color(0x80FF0000),
                            Color.Transparent
                        ),
                        center = Offset(headCenterX, headCenterY),
                        radius = haloRadius
                    ),
                    radius = haloRadius,
                    center = Offset(headCenterX, headCenterY)
                )

                // Molten Core Orb
                val coreRadius = (if (isDragging) 7.5.dp.toPx() else 5.5.dp.toPx()) * flameFlicker
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFFFE082),
                            Color(0xFFFF3D00)
                        ),
                        center = Offset(headCenterX - 1f, headCenterY - 1f),
                        radius = coreRadius
                    ),
                    radius = coreRadius,
                    center = Offset(headCenterX, headCenterY)
                )

                // Flying Fiery Ember Sparks drifting from the flame head
                val sparkOffsets = listOf(
                    Pair(-8f, -6f),
                    Pair(-14f, 4f),
                    Pair(-20f, -8f),
                    Pair(-6f, 7f)
                )
                sparkOffsets.forEachIndexed { i, spark ->
                    val sparkDrift = emberProgress
                    val sx = headCenterX + spark.first - (sparkDrift * 16f)
                    val sy = headCenterY + spark.second - (sin((sparkDrift + i) * PI.toFloat()) * 6f)
                    if (sx > 0f) {
                        val sparkAlpha = (1f - sparkDrift).coerceIn(0f, 1f) * flameFlicker.coerceAtMost(1f)
                        drawCircle(
                            color = Color(0xFFFFD54F).copy(alpha = sparkAlpha),
                            radius = (1.8f * (1f - sparkDrift * 0.4f)).coerceAtLeast(0.6f),
                            center = Offset(sx, sy)
                        )
                    }
                }
            }
        }
    }
}

