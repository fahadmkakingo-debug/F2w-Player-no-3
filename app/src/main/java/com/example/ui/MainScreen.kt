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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.example.ui.screens.welcome.WelcomeScreen
import com.example.ui.theme.F2WBackground
import com.example.ui.theme.LiveThemeBackground
import com.example.ui.theme.ThemeManager
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
    val currentTheme by ThemeManager.getInstance(context).currentTheme.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val scanner = remember { LocalVideoScanner.getInstance(context) }
    // Observe persistent Room cache immediately on startup - never starts empty if DB has items
    val allScannedVideos by scanner.allVideosFlow.collectAsState()

    val prefs = remember { context.getSharedPreferences("f2w_app_prefs", Context.MODE_PRIVATE) }
    var showWelcomeScreen by rememberSaveable { mutableStateOf(true) }
    val hasCompletedFirstLaunchOnboarding = remember { prefs.getBoolean("has_completed_first_launch_onboarding", false) }
    var showFirstLaunchPermissionScreen by rememberSaveable { mutableStateOf(!hasCompletedFirstLaunchOnboarding) }

    // Start background sync immediately if permission already granted
    LaunchedEffect(Unit) {
        if (MediaPermissionManager.hasMediaAccess(context, MediaPermissionType.ALL_MEDIA)) {
            scanner.startScan(forceFullRescan = false)
        }
    }

    // 1. Welcome Screen (Splash screen on launch)
    if (showWelcomeScreen) {
        WelcomeScreen(
            onContinue = {
                showWelcomeScreen = false
            }
        )
        return
    }

    // 2. First Launch Black Screen Permission Onboarding (Exclusively on First Launch)
    if (showFirstLaunchPermissionScreen) {
        com.example.ui.screens.welcome.FirstLaunchPermissionScreen(
            onComplete = {
                prefs.edit().putBoolean("has_completed_first_launch_onboarding", true).apply()
                showFirstLaunchPermissionScreen = false
                scanner.startScan(forceFullRescan = false)
            }
        )
        return
    }

    var selectedTab by remember { mutableStateOf(NavTab.VIDEO) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showSettingsPage by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showEqualizerScreen by remember { mutableStateOf(false) }
    var isListView by remember { mutableStateOf(false) }
    val videoGridState = rememberLazyGridState()

    val videoManager = remember { com.example.data.video.VideoPlaybackManager.getInstance(context) }
    val currentPlayingVideo by videoManager.currentVideo.collectAsState()
    val isFullScreenVideoOpen by videoManager.isFullScreenOpen.collectAsState()

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

    if (showThemePicker) {
        ThemePickerScreen(
            onBack = { showThemePicker = false }
        )
        return
    }

    if (showSettingsPage) {
        SettingsScreen(
            onBack = { showSettingsPage = false },
            onNavigateToPrivacy = {
                showSettingsPage = false
                selectedTab = NavTab.PRIVACY
            }
        )
        return
    }

    if (showEqualizerScreen) {
        com.example.ui.screens.equalizer.EqualizerScreen(
            onBack = { showEqualizerScreen = false }
        )
        return
    }

    var isPrivacyUnlocked by remember { mutableStateOf(false) }

    // Back handling: If on secondary tab, return to Video tab & relock Privacy
    BackHandler(enabled = selectedTab != NavTab.VIDEO) {
        selectedTab = NavTab.VIDEO
        isPrivacyUnlocked = false
    }

    val subtitle = when (selectedTab) {
        NavTab.VIDEO -> "Local Media Player"
        NavTab.AUDIO -> "Hi-Res Audio Player"
        NavTab.PLAYLIST -> "Media Playlists"
        NavTab.PRIVACY -> "Private Media Vault"
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Dynamic Live Theme Animated Canvas Layer
        if (currentTheme.isLiveAnimated) {
            LiveThemeBackground(
                theme = currentTheme,
                modifier = Modifier.fillMaxSize()
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (currentTheme.isLiveAnimated) Color.Transparent else F2WBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (selectedTab == NavTab.VIDEO) {
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
                            showEqualizerScreen = true
                        },
                        onSortClick = {
                            showSortDialog = true
                        },
                        onSettingsClick = { showSettingsPage = true }
                    )
                }
            },
            bottomBar = {
                if (selectedTab != NavTab.PRIVACY || !isPrivacyUnlocked) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        com.example.ui.screens.video.VideoMiniPlayerBar(
                            videoManager = videoManager,
                            onExpand = { videoManager.openFullScreen() }
                        )
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
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = (innerPadding.calculateBottomPadding() - 34.dp).coerceAtLeast(0.dp)
                    )
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
                            videos = allScannedVideos,
                            onScanRequest = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Local storage scanner initialized.")
                                }
                            },
                            isListView = isListView,
                            onToggleViewMode = { isListView = !isListView },
                            showSortDialog = showSortDialog,
                            onDismissSortDialog = { showSortDialog = false },
                            onVideoClick = { clickedVideo, playlist ->
                                videoManager.playVideo(clickedVideo, playlist)
                            },
                            gridState = videoGridState
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
                                val startVideo = playlistVideos.getOrNull(startIndex) ?: playlistVideos.firstOrNull()
                                if (startVideo != null) {
                                    videoManager.playVideo(startVideo, playlistVideos)
                                }
                            }
                        )
                        NavTab.PRIVACY -> PrivacyScreen(
                            onBackToHome = {
                                selectedTab = NavTab.VIDEO
                                isPrivacyUnlocked = false
                            },
                            onVaultUnlockedChanged = { unlocked ->
                                isPrivacyUnlocked = unlocked
                            }
                        )
                    }
                }
            }
        }

        // Fullscreen XPlayer Overlay: Stays on top without destroying VideoScreen scroll state
        if (isFullScreenVideoOpen && currentPlayingVideo != null) {
            val playerQueue = if (videoManager.playlist.value.isNotEmpty()) videoManager.playlist.value else allScannedVideos
            androidx.compose.runtime.key(currentPlayingVideo!!.id) {
                XVideoPlayerScreen(
                    video = currentPlayingVideo!!,
                    allVideos = playerQueue,
                    isInPipMode = isInPipMode,
                    onRequestPip = {
                        onRequestPip()
                    },
                    onToggleOrientation = onToggleOrientation,
                    onBack = {
                        videoManager.closeFullScreen()
                    },
                    onVideoChange = { nextVideo ->
                        videoManager.playVideo(nextVideo, playerQueue)
                    }
                )
            }
        }

        // Search Screen Overlay
        if (showSearchDialog) {
            VideoSearchOverlayScreen(
                allVideos = allScannedVideos,
                onBack = { showSearchDialog = false },
                onVideoClick = { clickedVideo ->
                    videoManager.playVideo(clickedVideo, allScannedVideos)
                    showSearchDialog = false
                }
            )
        }
    }
}
