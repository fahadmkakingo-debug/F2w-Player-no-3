package com.example.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.media.DemoVideoData
import com.example.data.media.LocalVideoScanner
import com.example.ui.components.F2WTopBar
import com.example.ui.navigation.FloatingNavBar
import com.example.ui.navigation.NavTab
import com.example.ui.screens.audio.AudioScreen
import com.example.ui.screens.player.InAppFloatingPlayer
import com.example.ui.screens.player.XVideoPlayerScreen
import com.example.ui.screens.playlist.PlaylistScreen
import com.example.ui.screens.privacy.PrivacyScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.theme.ThemePickerScreen
import com.example.ui.screens.video.VideoItem
import com.example.ui.screens.video.VideoScreen
import com.example.ui.screens.video.VideoSearchOverlayScreen
import com.example.ui.theme.F2WBackground
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurface
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WTextTertiary
import com.example.ui.theme.F2WVioletAccent
import com.example.util.permission.MediaPermissionManager
import com.example.util.permission.MediaPermissionType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isInPipMode: Boolean = false,
    onRequestPip: () -> Unit = {},
    onToggleOrientation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val scanner = remember { LocalVideoScanner.getInstance(context) }
    val allScannedVideos by scanner.allVideosFlow.collectAsState(initial = emptyList())

    // Request permissions on first launch only
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        scanner.startScan(forceFullRescan = false)
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("f2w_app_prefs", Context.MODE_PRIVATE)
        val hasRequestedBefore = prefs.getBoolean("has_prompted_media_permissions", false)
        val hasAccess = MediaPermissionManager.hasMediaAccess(context, MediaPermissionType.ALL_MEDIA)
        if (!hasAccess && !hasRequestedBefore) {
            prefs.edit().putBoolean("has_prompted_media_permissions", true).apply()
            val required = MediaPermissionManager.getRequiredPermissions(MediaPermissionType.ALL_MEDIA)
            permissionLauncher.launch(required)
        } else if (hasAccess) {
            scanner.startScan(forceFullRescan = false)
        }
    }

    var selectedTab by remember { mutableStateOf(NavTab.VIDEO) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showSettingsPage by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var isListView by remember { mutableStateOf(false) }
    var activePlayingVideo by remember { mutableStateOf<VideoItem?>(null) }
    var currentVideoPlaylist by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var isFloatingMiniPlayer by remember { mutableStateOf(false) }

    val audioManager = remember { com.example.data.audio.AudioPlaybackManager.getInstance(context) }
    val isFullScreenAudioOpen by audioManager.isFullScreenOpen.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Fullscreen Audio Player
    if (isFullScreenAudioOpen) {
        com.example.ui.screens.audio.AudioPlayerFullScreen(
            audioManager = audioManager,
            onBack = { audioManager.closeFullScreen() }
        )
        return
    }

    // Fullscreen XPlayer when a video is clicked and not in mini-player mode
    if (activePlayingVideo != null && (!isFloatingMiniPlayer || isInPipMode)) {
        XVideoPlayerScreen(
            video = activePlayingVideo!!,
            allVideos = currentVideoPlaylist,
            isInPipMode = isInPipMode,
            onRequestPip = {
                onRequestPip()
                isFloatingMiniPlayer = true
            },
            onToggleOrientation = onToggleOrientation,
            onBack = {
                activePlayingVideo = null
                isFloatingMiniPlayer = false
            },
            onVideoChange = { activePlayingVideo = it }
        )
        return
    }

    if (showThemePicker) {
        ThemePickerScreen(
            onBack = { showThemePicker = false }
        )
        return
    }

    if (showSettingsPage) {
        SettingsScreen(
            onBack = { showSettingsPage = false }
        )
        return
    }

    // Back handling: If on secondary tab, return to Video tab
    BackHandler(enabled = selectedTab != NavTab.VIDEO) {
        selectedTab = NavTab.VIDEO
    }

    val subtitle = when (selectedTab) {
        NavTab.VIDEO -> "Local Media Player"
        NavTab.AUDIO -> "Hi-Res Audio Player"
        NavTab.PLAYLIST -> "Media Playlists"
        NavTab.PRIVACY -> "Private Media Vault"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = F2WBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selectedTab != NavTab.AUDIO) {
                F2WTopBar(
                    subtitle = subtitle,
                    onSearchClick = { showSearchDialog = true },
                    isListView = isListView,
                    onToggleViewMode = { isListView = !isListView },
                    onThemeClick = { showThemePicker = true },
                    onRefreshClick = {
                        scanner.startScan(forceFullRescan = true)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Refreshing media storage...")
                        }
                    },
                    onEqualiserClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Equaliser: Audio enhancements ready")
                        }
                    },
                    onSettingsClick = { showSettingsPage = true }
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                com.example.ui.screens.audio.AudioMiniPlayerBar(
                    audioManager = audioManager,
                    onExpand = { audioManager.openFullScreen() },
                    onOpenQueue = { audioManager.openFullScreen() }
                )
                FloatingNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Content Area with Smooth Animation
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "screen_tab_transition",
                modifier = Modifier.fillMaxSize()
            ) { targetTab ->
                when (targetTab) {
                    NavTab.VIDEO -> VideoScreen(
                        onScanRequest = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Local storage scanner initialized for video discovery.")
                            }
                        },
                        isListView = isListView,
                        onToggleViewMode = { isListView = !isListView },
                        onVideoClick = { clickedVideo ->
                            activePlayingVideo = clickedVideo
                        }
                    )
                    NavTab.AUDIO -> AudioScreen(
                        onScanRequest = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Local storage scanner initialized for audio discovery.")
                            }
                        }
                    )
                    NavTab.PLAYLIST -> PlaylistScreen(
                        onPlayVideoPlaylist = { playlistVideos, startIndex ->
                            currentVideoPlaylist = playlistVideos
                            activePlayingVideo = playlistVideos.getOrNull(startIndex) ?: playlistVideos.firstOrNull()
                        }
                    )
                    NavTab.PRIVACY -> PrivacyScreen()
                }
            }

            // In-App Floating Mini-Player when minimized
            if (activePlayingVideo != null && isFloatingMiniPlayer) {
                InAppFloatingPlayer(
                    video = activePlayingVideo!!,
                    onExpand = { isFloatingMiniPlayer = false },
                    onClose = {
                        activePlayingVideo = null
                        isFloatingMiniPlayer = false
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 12.dp, end = 16.dp)
                )
            }
        }
    }

    // Search Screen Overlay
    if (showSearchDialog) {
        VideoSearchOverlayScreen(
            allVideos = allScannedVideos,
            onBack = { showSearchDialog = false },
            onVideoClick = { clickedVideo ->
                currentVideoPlaylist = allScannedVideos
                activePlayingVideo = clickedVideo
                showSearchDialog = false
            }
        )
    }
}
