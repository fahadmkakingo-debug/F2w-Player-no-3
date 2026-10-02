package com.example.ui.screens.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.media.RecentlyPlayedManager
import com.example.ui.screens.video.VideoItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun XVideoPlayerScreen(
    video: VideoItem,
    allVideos: List<VideoItem> = listOf(video),
    isInPipMode: Boolean = false,
    onRequestPip: () -> Unit = {},
    onToggleOrientation: () -> Unit = {},
    onBack: () -> Unit,
    onVideoChange: (VideoItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val recentlyPlayedManager = remember { RecentlyPlayedManager.getInstance(context) }

    var currentVideo by remember { mutableStateOf(video) }
    var currentPlaylist by remember(allVideos) { mutableStateOf(allVideos) }

    // ExoPlayer Instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Player States
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(video.durationMs.coerceAtLeast(1000L)) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isBackgroundAudio by remember { mutableStateOf(false) }
    var isOrientationLocked by remember { mutableStateOf(false) }
    var isAudioOnlyMode by remember { mutableStateOf(false) }
    var isNightMode by remember { mutableStateOf(false) }
    var decoderMode by remember { mutableStateOf("HW") }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var selectedAudioTrack by remember { mutableStateOf("Track 1: English (Stereo 2.0)") }
    var selectedSubtitle by remember { mutableStateOf("Hakuna (Off)") }
    var repeatMode by remember { mutableStateOf("Off") }
    var sleepTimerMinutes by remember { mutableIntStateOf(0) }
    var aspectRatioMode by remember { mutableStateOf("Fit") } // "Fit", "16:9", "Fill", "Zoom"

    // Gesture Overlay States
    var volumePercent by remember {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        mutableIntStateOf(((currentVol.toFloat() / maxVol) * 100).toInt())
    }
    var showVolumeOverlay by remember { mutableStateOf(false) }

    var brightnessPercent by remember { mutableIntStateOf(70) }
    var showBrightnessOverlay by remember { mutableStateOf(false) }

    var showSeekOverlay by remember { mutableStateOf(false) }
    var seekDiffText by remember { mutableStateOf("") }
    var seekTargetText by remember { mutableStateOf("") }

    // Double Tap Overlay States
    var showDoubleTapRewind by remember { mutableStateOf(false) }
    var showDoubleTapForward by remember { mutableStateOf(false) }
    var showDoubleTapPlayPause by remember { mutableStateOf(false) }
    var isLongPressSpeedActive by remember { mutableStateOf(false) }

    // Dialog States
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSubtitleAudioDialog by remember { mutableStateOf(false) }
    var showPlaylistQueue by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    // Resume Playback Prompt States
    var showResumePrompt by remember { mutableStateOf(false) }
    var resumedPositionMs by remember { mutableLongStateOf(0L) }
    var resumePromptKey by remember { mutableIntStateOf(0) }

    // Reference to PlayerView for aspect ratio adjustments
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    // Load Media Item into ExoPlayer
    LaunchedEffect(currentVideo) {
        val playableUri = if (currentVideo.uriString.startsWith("content://") ||
            currentVideo.uriString.startsWith("file://") ||
            currentVideo.uriString.endsWith(".mp4") ||
            currentVideo.uriString.endsWith(".mkv")
        ) {
            Uri.parse(currentVideo.uriString)
        } else {
            Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
        }

        val mediaItem = MediaItem.fromUri(playableUri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        // Resume from saved playback position if available
        val savedPos = recentlyPlayedManager.getPlaybackPosition(currentVideo.id).let { pos ->
            if (pos > 0L) pos else currentVideo.playbackProgressMs
        }
        if (savedPos > 2000L) {
            exoPlayer.seekTo(savedPos)
            currentPositionMs = savedPos
            resumedPositionMs = savedPos
            showResumePrompt = true
            resumePromptKey++
        } else {
            showResumePrompt = false
        }

        // Move to the top of Recently Played
        recentlyPlayedManager.recordVideoPlayed(currentVideo.id, savedPos)

        // Start playback immediately without waiting for user confirmation
        exoPlayer.play()
    }

    // Auto-dismiss the resume prompt after 5 seconds while video playback continues uninterrupted
    LaunchedEffect(showResumePrompt, resumePromptKey) {
        if (showResumePrompt) {
            delay(5000)
            showResumePrompt = false
        }
    }

    // Sync Player Position & State periodically
    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(1000L)
                } else if (playbackState == Player.STATE_ENDED) {
                    if (repeatMode == "Repeat One") {
                        exoPlayer.seekTo(0L)
                        exoPlayer.play()
                    } else if (repeatMode == "Repeat All") {
                        val currentIndex = currentPlaylist.indexOfFirst { it.id == currentVideo.id }
                        val nextVideo = if (currentIndex != -1 && currentIndex < currentPlaylist.size - 1) {
                            currentPlaylist[currentIndex + 1]
                        } else {
                            currentPlaylist.firstOrNull() ?: currentVideo
                        }
                        currentVideo = nextVideo
                        onVideoChange(nextVideo)
                    } else {
                        // Sequential order playback: if there is a next video in the queue, auto-play it!
                        val currentIndex = currentPlaylist.indexOfFirst { it.id == currentVideo.id }
                        if (currentIndex != -1 && currentIndex < currentPlaylist.size - 1) {
                            val nextVideo = currentPlaylist[currentIndex + 1]
                            currentVideo = nextVideo
                            onVideoChange(nextVideo)
                        }
                    }
                }
            }
        }
        exoPlayer.addListener(listener)

        while (true) {
            if (exoPlayer.isPlaying) {
                currentPositionMs = exoPlayer.currentPosition
                if (exoPlayer.duration > 0) {
                    durationMs = exoPlayer.duration
                }
                if (currentPositionMs > 0L) {
                    recentlyPlayedManager.savePlaybackPosition(currentVideo.id, currentPositionMs)
                }
            }
            delay(400)
        }
    }

    // Sleep Timer countdown
    LaunchedEffect(sleepTimerMinutes) {
        if (sleepTimerMinutes > 0) {
            delay(sleepTimerMinutes * 60 * 1000L)
            exoPlayer.pause()
            Toast.makeText(context, "Kipima Muda cha Kulala: Video imesimamishwa", Toast.LENGTH_LONG).show()
        }
    }

    val window = activity?.window
    val insetsController = remember(window) {
        window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }

    // Dynamic immersive mode: when controls are hidden, hide system bars cleanly so video is full screen
    // When controls are visible, show system bars, with safe insets padding so notification panel never covers controls
    LaunchedEffect(areControlsVisible, isLocked, isInPipMode) {
        if (!isInPipMode) {
            insetsController?.let { controller ->
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (areControlsVisible && !isLocked) {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    // Auto-hide controls after 4 seconds of inactivity when not locked
    LaunchedEffect(areControlsVisible, isPlaying, isLocked) {
        if (areControlsVisible && isPlaying && !isLocked) {
            delay(4000)
            areControlsVisible = false
        }
    }

    // Clean up player on exit
    DisposableEffect(Unit) {
        onDispose {
            if (currentPositionMs > 0L) {
                recentlyPlayedManager.savePlaybackPosition(currentVideo.id, currentPositionMs)
            }
            exoPlayer.release()
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            // Reset screen brightness
            activity?.window?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = -1.0f
                win.attributes = lp
            }
        }
    }

    // Back button handling: If locked, unlock first; otherwise exit or pip
    BackHandler {
        if (isLocked) {
            isLocked = false
            areControlsVisible = true
            Toast.makeText(context, "Kid Lock imeondolewa (Screen Unlocked)", Toast.LENGTH_SHORT).show()
        } else {
            onBack()
        }
    }

    fun seekToPosition(targetMs: Long) {
        val validMs = targetMs.coerceIn(0L, durationMs)
        exoPlayer.seekTo(validMs)
        currentPositionMs = validMs
    }

    fun skipNextVideo() {
        val currentIndex = currentPlaylist.indexOfFirst { it.id == currentVideo.id }
        if (currentIndex != -1 && currentIndex < currentPlaylist.size - 1) {
            val nextVideo = currentPlaylist[currentIndex + 1]
            currentVideo = nextVideo
            onVideoChange(nextVideo)
        } else if (currentPlaylist.isNotEmpty()) {
            val first = currentPlaylist.first()
            currentVideo = first
            onVideoChange(first)
        }
    }

    fun skipPreviousVideo() {
        if (currentPositionMs > 5000L) {
            seekToPosition(0L)
        } else {
            val currentIndex = currentPlaylist.indexOfFirst { it.id == currentVideo.id }
            if (currentIndex > 0) {
                val prevVideo = currentPlaylist[currentIndex - 1]
                currentVideo = prevVideo
                onVideoChange(prevVideo)
            } else {
                seekToPosition(0L)
            }
        }
    }

    fun updateScreenBrightness(percent: Int) {
        brightnessPercent = percent.coerceIn(0, 100)
        activity?.window?.let { window ->
            val lp = window.attributes
            lp.screenBrightness = (brightnessPercent / 100f).coerceIn(0.01f, 1.0f)
            window.attributes = lp
        }
    }

    fun cycleAspectRatio() {
        val modes = listOf("Fit", "16:9", "Fill", "Zoom")
        val nextIndex = (modes.indexOf(aspectRatioMode) + 1) % modes.size
        aspectRatioMode = modes[nextIndex]

        playerViewRef?.let { pv ->
            when (aspectRatioMode) {
                "Fit" -> pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                "16:9" -> pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                "Fill" -> pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                "Zoom" -> pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        }
        Toast.makeText(context, "Aspect Ratio: $aspectRatioMode", Toast.LENGTH_SHORT).show()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("xplayer_fullscreen_container")
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()

        // 1. AndroidView Video Player Surface (ExoPlayer) or Audio Mode
        if (isAudioOnlyMode) {
            AudioModeVisualizer(
                title = currentVideo.title,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        playerViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Night Mode Tint Overlay
        if (isNightMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF884000).copy(alpha = 0.25f))
            )
        }

        // When in Android OS PiP mode, hide all control overlays for a clean video feed
        if (!isInPipMode) {
            // 2. Gesture Detector Layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isLocked) {
                        if (isLocked) {
                            detectTapGestures(
                                onTap = {
                                    areControlsVisible = !areControlsVisible
                                }
                            )
                        } else {
                            detectTapGestures(
                                onTap = {
                                    areControlsVisible = !areControlsVisible
                                },
                                onLongPress = {
                                    // 2.0x Fast-forward speed while holding
                                    isLongPressSpeedActive = true
                                    exoPlayer.setPlaybackSpeed(2.0f)
                                },
                                onPress = {
                                    tryAwaitRelease()
                                    if (isLongPressSpeedActive) {
                                        isLongPressSpeedActive = false
                                        exoPlayer.setPlaybackSpeed(currentSpeed)
                                    }
                                },
                                onDoubleTap = { offset ->
                                    val leftThreshold = totalWidth * 0.35f
                                    val rightThreshold = totalWidth * 0.65f

                                    if (offset.x < leftThreshold) {
                                        // Left Double Tap: Rewind 10s
                                        seekToPosition(currentPositionMs - 10000L)
                                        showDoubleTapRewind = true
                                        coroutineScope.launch {
                                            delay(700)
                                            showDoubleTapRewind = false
                                        }
                                    } else if (offset.x > rightThreshold) {
                                        // Right Double Tap: Fast Forward 10s
                                        seekToPosition(currentPositionMs + 10000L)
                                        showDoubleTapForward = true
                                        coroutineScope.launch {
                                            delay(700)
                                            showDoubleTapForward = false
                                        }
                                    } else {
                                        // Center Double Tap: Play / Pause
                                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                        showDoubleTapPlayPause = true
                                        coroutineScope.launch {
                                            delay(700)
                                            showDoubleTapPlayPause = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                    .pointerInput(isLocked) {
                        if (!isLocked) {
                            var dragType = 0 // 1: Brightness (left), 2: Volume (right), 3: Seek (horizontal)
                            var initialTouchX = 0f
                            var initialPosition = 0L

                            detectDragGestures(
                                onDragStart = { offset ->
                                    initialTouchX = offset.x
                                    initialPosition = currentPositionMs
                                    dragType = 0
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (dragType == 0) {
                                        if (Math.abs(dragAmount.x) > Math.abs(dragAmount.y) && Math.abs(dragAmount.x) > 12f) {
                                            dragType = 3 // Horizontal Seek
                                        } else if (Math.abs(dragAmount.y) > 12f) {
                                            dragType = if (initialTouchX < totalWidth / 2f) 1 else 2 // 1: Brightness, 2: Volume
                                        }
                                    }

                                    when (dragType) {
                                        1 -> {
                                            // Brightness on left
                                            val delta = (-dragAmount.y / totalHeight * 100).toInt()
                                            updateScreenBrightness(brightnessPercent + delta)
                                            showBrightnessOverlay = true
                                        }
                                        2 -> {
                                            // Volume on right
                                            val delta = (-dragAmount.y / totalHeight * 100).toInt()
                                            volumePercent = (volumePercent + delta).coerceIn(0, 100)
                                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                            val newVol = ((volumePercent.toFloat() / 100f) * maxVol).toInt()
                                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                            showVolumeOverlay = true
                                        }
                                        3 -> {
                                            // Seek horizontal
                                            val seekSeconds = (dragAmount.x / totalWidth * 90).toInt()
                                            val targetMs = (currentPositionMs + seekSeconds * 1000L).coerceIn(0L, durationMs)
                                            seekToPosition(targetMs)
                                            val diff = (targetMs - initialPosition) / 1000
                                            seekDiffText = if (diff >= 0) "+${diff}s" else "${diff}s"
                                            seekTargetText = "${formatTime(targetMs)} / ${formatTime(durationMs)}"
                                            showSeekOverlay = true
                                        }
                                    }
                                },
                                onDragEnd = {
                                    coroutineScope.launch {
                                        delay(800)
                                        showBrightnessOverlay = false
                                        showVolumeOverlay = false
                                        showSeekOverlay = false
                                    }
                                }
                            )
                        }
                    }
            )

            // 3. Gesture Overlays (Volume, Brightness, Seek)
            VolumeGestureOverlay(
                volumePercent = volumePercent,
                visible = showVolumeOverlay,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
            )

            BrightnessGestureOverlay(
                brightnessPercent = brightnessPercent,
                visible = showBrightnessOverlay,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
            )

            SeekGestureOverlay(
                targetTimeText = seekTargetText,
                diffText = seekDiffText,
                visible = showSeekOverlay,
                modifier = Modifier.align(Alignment.Center)
            )

            // Double Tap Ripple Bubbles
            DoubleTapSeekBubble(
                isForward = false,
                visible = showDoubleTapRewind,
                seconds = 10,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 36.dp)
            )

            DoubleTapSeekBubble(
                isForward = true,
                visible = showDoubleTapForward,
                seconds = 10,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 36.dp)
            )

            DoubleTapPlayPauseBubble(
                isPlaying = isPlaying,
                visible = showDoubleTapPlayPause,
                modifier = Modifier.align(Alignment.Center)
            )

            LongPressSpeedOverlay(
                visible = isLongPressSpeedActive,
                speed = 2.0f,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 28.dp)
            )

            // 4. Floating Screenshot Button on Right
            AnimatedVisibility(
                visible = areControlsVisible && !isLocked,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                XPlayerScreenshotButton(
                    onClick = {
                        Toast.makeText(context, "Screenshot imehifadhiwa! (Frame Captured)", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 5. Controls Overlay (Top Bar, Quick Controls, Bottom Controls)
            AnimatedVisibility(
                visible = areControlsVisible && !isLocked,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top: Top Bar & Quick Controls Row
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        XPlayerTopBar(
                            title = currentVideo.title,
                            decoderMode = decoderMode,
                            onBackClick = onBack,
                            onDecoderClick = {
                                decoderMode = if (decoderMode == "HW") "SW" else "HW"
                                Toast.makeText(context, "Decoder: $decoderMode (Hardware/Software)", Toast.LENGTH_SHORT).show()
                            },
                            onSubtitlesClick = { showSubtitleAudioDialog = true },
                            onPlaylistClick = { showPlaylistQueue = true },
                            onMoreClick = { showMoreSheet = true }
                        )

                        XPlayerQuickControlsRow(
                            isOrientationLocked = isOrientationLocked,
                            isMuted = isMuted,
                            isBackgroundAudio = isBackgroundAudio,
                            aspectRatioText = aspectRatioMode,
                            speedText = if (currentSpeed == 1.0f) "1.0X" else "${currentSpeed}X",
                            onOrientationToggle = {
                                isOrientationLocked = !isOrientationLocked
                                onToggleOrientation()
                                Toast.makeText(
                                    context,
                                    if (isOrientationLocked) "Screen Rotation: Imefungwa" else "Screen Rotation: Mzunguko Wazi",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onMuteToggle = {
                                isMuted = !isMuted
                                if (isMuted) {
                                    exoPlayer.volume = 0f
                                } else {
                                    exoPlayer.volume = 1f
                                }
                                Toast.makeText(context, if (isMuted) "Sauti Imesimamishwa (Muted)" else "Sauti Imerudishwa (Unmuted)", Toast.LENGTH_SHORT).show()
                            },
                            onBackgroundAudioToggle = {
                                isBackgroundAudio = !isBackgroundAudio
                                Toast.makeText(
                                    context,
                                    if (isBackgroundAudio) "Background Play: Imewashwa (Inacheza simu ikifungwa)" else "Background Play: Imezimwa",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onAspectRatioClick = { cycleAspectRatio() },
                            onSpeedClick = { showSpeedDialog = true },
                            onPipClick = {
                                onRequestPip()
                                Toast.makeText(context, "Inafungua Pop-up Window (PiP)...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // Bottom: Bottom Bar (Time, Slider, Controls)
                    val progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
                    XPlayerBottomBar(
                        currentTimeText = formatTime(currentPositionMs),
                        totalDurationText = formatTime(durationMs),
                        progress = progress,
                        isPlaying = isPlaying,
                        isLocked = isLocked,
                        onSeek = { fraction ->
                            seekToPosition((fraction * durationMs).toLong())
                        },
                        onLockToggle = {
                            isLocked = true
                            areControlsVisible = false
                            Toast.makeText(context, "Kid Lock: Skrini imefungwa kuzuia kuguswa", Toast.LENGTH_SHORT).show()
                        },
                        onPrevious = { skipPreviousVideo() },
                        onPlayPause = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        onNext = { skipNextVideo() },
                        onPipToggle = {
                            onRequestPip()
                            Toast.makeText(context, "Inafungua Pop-up Window (PiP)...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            // 6. When Screen is Locked: Floating Unlock Button on bottom-center
            if (isLocked) {
                AnimatedVisibility(
                    visible = areControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 28.dp)
                ) {
                    XPlayerLockedFloatingButton(
                        onClick = {
                            isLocked = false
                            areControlsVisible = true
                            Toast.makeText(context, "Kid Lock imeondolewa (Screen Unlocked)", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // 7. Resume Playback Overlay (Auto-dismisses in 5s or restarts from 00:00)
            ResumePlaybackPromptOverlay(
                visible = showResumePrompt,
                onRestartClick = {
                    seekToPosition(0L)
                    recentlyPlayedManager.savePlaybackPosition(currentVideo.id, 0L)
                    showResumePrompt = false
                },
                onDismissClick = {
                    showResumePrompt = false
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (areControlsVisible && !isLocked) 84.dp else 24.dp)
            )
        }
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        PlaybackSpeedDialog(
            currentSpeed = currentSpeed,
            onSpeedSelected = { speed ->
                currentSpeed = speed
                exoPlayer.setPlaybackSpeed(speed)
                Toast.makeText(context, "Kasi ya Video: ${speed}X", Toast.LENGTH_SHORT).show()
            },
            onDismissRequest = { showSpeedDialog = false }
        )
    }

    // Subtitles & Audio Track Dialog
    if (showSubtitleAudioDialog) {
        SubtitlesAndAudioDialog(
            selectedAudioTrack = selectedAudioTrack,
            onAudioTrackSelected = { track ->
                selectedAudioTrack = track
                Toast.makeText(context, "Sauti: $track", Toast.LENGTH_SHORT).show()
            },
            selectedSubtitle = selectedSubtitle,
            onSubtitleSelected = { sub ->
                selectedSubtitle = sub
                Toast.makeText(context, "Manukuu: $sub", Toast.LENGTH_SHORT).show()
            },
            onDismissRequest = { showSubtitleAudioDialog = false }
        )
    }

    // Playlist Queue Bottom Sheet
    if (showPlaylistQueue) {
        PlaylistQueueBottomSheet(
            videos = currentPlaylist,
            currentVideoId = currentVideo.id,
            onVideoSelected = { videoItem ->
                currentVideo = videoItem
                onVideoChange(videoItem)
            },
            onQueueReordered = { updatedQueue ->
                currentPlaylist = updatedQueue
            },
            onDismissRequest = { showPlaylistQueue = false }
        )
    }

    // More Options Bottom Sheet
    if (showMoreSheet) {
        VideoMoreOptionsSheet(
            isNightMode = isNightMode,
            onNightModeToggle = {
                isNightMode = !isNightMode
                Toast.makeText(context, if (isNightMode) "Night Mode Imewashwa" else "Night Mode Imezimwa", Toast.LENGTH_SHORT).show()
            },
            isAudioOnlyMode = isAudioOnlyMode,
            onAudioOnlyToggle = {
                isAudioOnlyMode = !isAudioOnlyMode
                Toast.makeText(context, if (isAudioOnlyMode) "Modi ya Sauti Tu Imewashwa" else "Video Imerudi Kawaida", Toast.LENGTH_SHORT).show()
            },
            repeatMode = repeatMode,
            onRepeatModeCycle = {
                repeatMode = when (repeatMode) {
                    "Off" -> "Repeat One"
                    "Repeat One" -> "Repeat All"
                    else -> "Off"
                }
                Toast.makeText(context, "Kurudia Video: $repeatMode", Toast.LENGTH_SHORT).show()
            },
            onEqualizerClick = {
                Toast.makeText(context, "Equalizer: Bass Boost & Audio Enhancer Imewashwa", Toast.LENGTH_SHORT).show()
            },
            onSleepTimerClick = { showSleepTimerDialog = true },
            onVideoDetailsClick = {
                Toast.makeText(context, "${currentVideo.title} • ${currentVideo.resolution} • ${currentVideo.sizeText}", Toast.LENGTH_LONG).show()
            },
            onDismissRequest = { showMoreSheet = false }
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            selectedMinutes = sleepTimerMinutes,
            onSelectTimer = { mins ->
                sleepTimerMinutes = mins
                if (mins > 0) {
                    Toast.makeText(context, "Sleep Timer: Video itajizima baada ya dakika $mins", Toast.LENGTH_SHORT).show()
                } else if (mins == -1) {
                    Toast.makeText(context, "Sleep Timer: Video itajizima baada ya kumalizika", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Sleep Timer Imezimwa", Toast.LENGTH_SHORT).show()
                }
            },
            onDismissRequest = { showSleepTimerDialog = false }
        )
    }
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
