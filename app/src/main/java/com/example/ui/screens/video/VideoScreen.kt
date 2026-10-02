package com.example.ui.screens.video

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
import com.example.data.media.LocalVideoScanner
import com.example.data.media.RecentlyPlayedManager
import com.example.data.security.PrivacyVaultManager
import com.example.ui.components.F2WEmptyState
import com.example.ui.components.permission.MediaPermissionRationaleDialog
import com.example.ui.components.permission.rememberMediaPermissionState
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurface
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WTextTertiary
import com.example.util.permission.MediaPermissionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
    onScanRequest: () -> Unit,
    modifier: Modifier = Modifier,
    videos: List<VideoItem> = emptyList(),
    folders: List<VideoFolder> = emptyList(),
    isListView: Boolean = false,
    onToggleViewMode: () -> Unit = {},
    showSortDialog: Boolean = false,
    onDismissSortDialog: () -> Unit = {},
    onVideoClick: (VideoItem, List<VideoItem>) -> Unit = { _, _ -> },
    onFolderClick: (VideoFolder) -> Unit = {},
    gridState: LazyGridState = rememberLazyGridState()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scanner = remember { LocalVideoScanner.getInstance(context) }
    val recentlyPlayedManager = remember { RecentlyPlayedManager.getInstance(context) }
    val recentlyPlayedIds by recentlyPlayedManager.recentlyPlayedIds.collectAsState()

    // Observe persistent Room cached videos flow - starts with database snapshot
    val dbVideos by scanner.allVideosFlow.collectAsState()
    val scanProgress by scanner.scanProgressState.collectAsState()

    val videoSettings = remember { com.example.data.settings.VideoSettingsPreferences(context) }
    var currentSortOption by remember {
        mutableStateOf(
            try {
                VideoSortOption.valueOf(videoSettings.videoSortOption)
            } catch (_: Exception) {
                VideoSortOption.NAME_ASC
            }
        )
    }
    var selectedFilter by remember {
        mutableStateOf(
            try {
                VideoFilterMode.valueOf(videoSettings.selectedVideoFilter)
            } catch (_: Exception) {
                VideoFilterMode.ALL_VIDEO
            }
        )
    }

    fun parseSizeToBytes(sizeText: String): Long {
        return try {
            val parts = sizeText.trim().split(" ")
            val num = parts.firstOrNull()?.toDoubleOrNull() ?: 0.0
            val unit = parts.getOrNull(1)?.uppercase() ?: ""
            when {
                unit.contains("GB") -> (num * 1024 * 1024 * 1024).toLong()
                unit.contains("MB") -> (num * 1024 * 1024).toLong()
                unit.contains("KB") -> (num * 1024).toLong()
                else -> num.toLong()
            }
        } catch (_: Exception) {
            0L
        }
    }

    fun List<VideoItem>.applySort(option: VideoSortOption): List<VideoItem> {
        return when (option) {
            VideoSortOption.NAME_ASC -> sortedBy { it.title.lowercase() }
            VideoSortOption.NAME_DESC -> sortedByDescending { it.title.lowercase() }
            VideoSortOption.DATE_NEWEST -> sortedWith(
                compareByDescending<VideoItem> { it.dateAdded }
                    .thenByDescending { it.id.toLongOrNull() ?: 0L }
            )
            VideoSortOption.DATE_OLDEST -> sortedWith(
                compareBy<VideoItem> { it.dateAdded }
                    .thenBy { it.id.toLongOrNull() ?: 0L }
            )
            VideoSortOption.SIZE_LARGEST -> sortedByDescending { parseSizeToBytes(it.sizeText) }
            VideoSortOption.SIZE_SMALLEST -> sortedBy { parseSizeToBytes(it.sizeText) }
            VideoSortOption.DURATION_LONGEST -> sortedByDescending { it.durationMs }
            VideoSortOption.DURATION_SHORTEST -> sortedBy { it.durationMs }
        }
    }

    fun List<VideoFolder>.applyFolderSort(option: VideoSortOption): List<VideoFolder> {
        return when (option) {
            VideoSortOption.NAME_ASC -> sortedBy { it.name.lowercase() }
            VideoSortOption.NAME_DESC -> sortedByDescending { it.name.lowercase() }
            VideoSortOption.DATE_NEWEST -> sortedByDescending { folder ->
                folder.videos.maxOfOrNull { it.dateAdded } ?: 0L
            }
            VideoSortOption.DATE_OLDEST -> sortedBy { folder ->
                folder.videos.minOfOrNull { it.dateAdded } ?: 0L
            }
            VideoSortOption.SIZE_LARGEST -> sortedByDescending { folder ->
                folder.videos.sumOf { parseSizeToBytes(it.sizeText) }
            }
            VideoSortOption.SIZE_SMALLEST -> sortedBy { folder ->
                folder.videos.sumOf { parseSizeToBytes(it.sizeText) }
            }
            VideoSortOption.DURATION_LONGEST -> sortedByDescending { folder ->
                folder.videos.sumOf { it.durationMs }
            }
            VideoSortOption.DURATION_SHORTEST -> sortedBy { folder ->
                folder.videos.sumOf { it.durationMs }
            }
        }
    }
    var activeVideoForMenu by remember { mutableStateOf<VideoItem?>(null) }
    var activeGroupDetail by remember { mutableStateOf<VideoNameGroup?>(null) }
    var activeFolderDetail by remember { mutableStateOf<VideoFolder?>(null) }
    val favoriteVideoIds = remember { mutableStateListOf<String>() }

    val vaultManager = remember { PrivacyVaultManager.getInstance(context) }
    var vaultUpdateTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        vaultManager.vaultUpdates.collect {
            vaultUpdateTrigger++
        }
    }

    // Media permissions state handler
    val mediaPermissionState = rememberMediaPermissionState(
        type = MediaPermissionType.VIDEO,
        onPermissionGranted = {
            scanner.startScan(forceFullRescan = false)
        }
    )

    // Debounced ContentObserver to detect newly downloaded or deleted videos
    DisposableEffect(mediaPermissionState.hasAccess) {
        if (!mediaPermissionState.hasAccess) return@DisposableEffect onDispose {}

        var scanJob: Job? = null
        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                scanJob?.cancel()
                scanJob = coroutineScope.launch {
                    delay(3000) // Debounce by 3 seconds to avoid spamming scans
                    if (!scanner.scanProgressState.value.isScanning) {
                        scanner.startScan(forceFullRescan = false)
                    }
                }
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                false,
                contentObserver
            )
        } catch (_: Exception) {}

        onDispose {
            scanJob?.cancel()
            try {
                context.contentResolver.unregisterContentObserver(contentObserver)
            } catch (_: Exception) {}
        }
    }

    val effectiveVideos = if (dbVideos.isNotEmpty()) dbVideos else videos

    val availableVideos = remember(effectiveVideos, vaultUpdateTrigger) {
        effectiveVideos.filter { v ->
            !vaultManager.isPathOrUriInVault(v.uriString, v.id) && !vaultManager.isPathOrUriInVault(v.title)
        }
    }

    // Fast O(N) grouping and folder extraction
    val nameGroups = remember(availableVideos) {
        VideoNameGrouper.groupVideosByName(availableVideos)
    }

    val computedFolders = remember(availableVideos, currentSortOption) {
        scanner.extractFolders(availableVideos).applyFolderSort(currentSortOption)
    }

    if (showSortDialog) {
        VideoSortDialog(
            selectedOption = currentSortOption,
            onOptionSelected = { newOpt ->
                currentSortOption = newOpt
                videoSettings.videoSortOption = newOpt.name
            },
            onDismissRequest = onDismissSortDialog
        )
    }

    if (activeGroupDetail != null) {
        VideoGroupDetailScreen(
            group = activeGroupDetail!!,
            onBack = { activeGroupDetail = null },
            onVideoClick = onVideoClick,
            isListView = isListView
        )
        return
    }

    if (activeFolderDetail != null) {
        VideoFolderDetailScreen(
            folder = activeFolderDetail!!,
            onBack = { activeFolderDetail = null },
            onVideoClick = onVideoClick,
            isListView = isListView
        )
        return
    }

    // Filter videos according to selected filter tab & current sort option
    val displayedVideos = remember(selectedFilter, availableVideos, recentlyPlayedIds, currentSortOption) {
        val baseList = when (selectedFilter) {
            VideoFilterMode.ALL_VIDEO -> availableVideos
            VideoFilterMode.GROUP_BY_NAME -> availableVideos
            VideoFilterMode.RECENTLY_ADDED -> availableVideos.sortedWith(
                compareByDescending<VideoItem> { it.dateAdded }
                    .thenByDescending { it.id.toLongOrNull() ?: 0L }
            )
            VideoFilterMode.RECENTLY_PLAYED -> {
                recentlyPlayedManager.getRecentlyPlayedVideos(availableVideos)
            }
            VideoFilterMode.GROUP_BY_FOLDER -> availableVideos
        }

        if (selectedFilter == VideoFilterMode.RECENTLY_ADDED || selectedFilter == VideoFilterMode.RECENTLY_PLAYED) {
            baseList
        } else {
            baseList.applySort(currentSortOption)
        }
    }

    val themeManager = remember { com.example.ui.theme.ThemeManager.getInstance(context) }
    val currentTheme by themeManager.currentTheme.collectAsState()
    val isLiveTheme = currentTheme.isLiveAnimated

    val columnsCount = if (isListView) 1 else 2

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isLiveTheme) Color.Transparent else F2WSurface)
            .testTag("video_screen_container")
    ) {
        // Fixed / Sticky Top Task Panel for Filter Pills
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isLiveTheme) Color.Transparent else F2WSurface)
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 2.dp)
        ) {
            // Row 1: [ All Video ] [ Group by Name ] [ Group by Folder ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterPillButton(
                    mode = VideoFilterMode.ALL_VIDEO,
                    isSelected = selectedFilter == VideoFilterMode.ALL_VIDEO,
                    onClick = {
                        selectedFilter = VideoFilterMode.ALL_VIDEO
                        videoSettings.selectedVideoFilter = VideoFilterMode.ALL_VIDEO.name
                    }
                )
                FilterPillButton(
                    mode = VideoFilterMode.GROUP_BY_NAME,
                    isSelected = selectedFilter == VideoFilterMode.GROUP_BY_NAME,
                    onClick = {
                        selectedFilter = VideoFilterMode.GROUP_BY_NAME
                        videoSettings.selectedVideoFilter = VideoFilterMode.GROUP_BY_NAME.name
                    }
                )
                FilterPillButton(
                    mode = VideoFilterMode.GROUP_BY_FOLDER,
                    isSelected = selectedFilter == VideoFilterMode.GROUP_BY_FOLDER,
                    onClick = {
                        selectedFilter = VideoFilterMode.GROUP_BY_FOLDER
                        videoSettings.selectedVideoFilter = VideoFilterMode.GROUP_BY_FOLDER.name
                    }
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Row 2: [ Recently Added ] [ Recently Played ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterPillButton(
                    mode = VideoFilterMode.RECENTLY_ADDED,
                    isSelected = selectedFilter == VideoFilterMode.RECENTLY_ADDED,
                    onClick = {
                        selectedFilter = VideoFilterMode.RECENTLY_ADDED
                        videoSettings.selectedVideoFilter = VideoFilterMode.RECENTLY_ADDED.name
                    }
                )
                FilterPillButton(
                    mode = VideoFilterMode.RECENTLY_PLAYED,
                    isSelected = selectedFilter == VideoFilterMode.RECENTLY_PLAYED,
                    onClick = {
                        selectedFilter = VideoFilterMode.RECENTLY_PLAYED
                        videoSettings.selectedVideoFilter = VideoFilterMode.RECENTLY_PLAYED.name
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            HorizontalDivider(
                color = F2WCardBorder.copy(alpha = 0.6f),
                thickness = 0.8.dp
            )
        }

        // Linear Progress bar during background syncing without hiding existing videos
        if (scanProgress.isScanning) {
            LinearProgressIndicator(
                progress = { scanProgress.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = F2WCyanPrimary,
                trackColor = F2WCyanPrimary.copy(alpha = 0.2f)
            )
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(columnsCount),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 2.dp,
                bottom = 12.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status bar showing active mode, video count, and scan progress
            item(span = { GridItemSpan(columnsCount) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val countText = when (selectedFilter) {
                        VideoFilterMode.GROUP_BY_FOLDER -> "${computedFolders.size} Folders"
                        VideoFilterMode.GROUP_BY_NAME -> "${nameGroups.size} Groups"
                        else -> "${displayedVideos.size} Videos"
                    }

                    Text(
                        text = "${selectedFilter.label} ($countText)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = F2WTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (scanProgress.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = F2WCyanPrimary,
                                strokeWidth = 1.5.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (scanProgress.totalCount > 0) {
                                    "Syncing (${scanProgress.scannedCount}/${scanProgress.totalCount})"
                                } else {
                                    "Syncing..."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = F2WCyanPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Cancel Scan button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF331520))
                                    .clickable { scanner.cancelScan() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .testTag("cancel_scan_btn")
                            ) {
                                Text(
                                    text = "Stop",
                                    color = Color(0xFFFF5252),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = if (isListView) "List View" else "2-Column Grid",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = F2WTextTertiary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            // Video Content Area (List or 2-Column Grid)
            if (selectedFilter == VideoFilterMode.GROUP_BY_NAME) {
                if (nameGroups.isEmpty() && !scanProgress.isScanning) {
                    item(span = { GridItemSpan(columnsCount) }) {
                        F2WEmptyState(
                            icon = Icons.Outlined.VideoLibrary,
                            title = if (mediaPermissionState.hasAccess) "No Video Groups Found" else "Permission Required",
                            description = if (mediaPermissionState.hasAccess)
                                "Videos with matching or similar movie names will be automatically grouped here in 2 columns."
                            else
                                "Storage permission is required to discover and group video files.",
                            actionLabel = if (mediaPermissionState.hasAccess) "Scan Video Za Simu" else "Grant Storage Permission",
                            actionIcon = Icons.Filled.Refresh,
                            onActionClick = {
                                if (mediaPermissionState.hasAccess) {
                                    scanner.startScan(forceFullRescan = true)
                                } else {
                                    mediaPermissionState.requestPermissions()
                                }
                                onScanRequest()
                            },
                            tipText = "Franchises like Avatar, Dune, Spider-Man are automatically clustered",
                            testTag = "groups_empty_state"
                        )
                    }
                } else {
                    items(
                        items = nameGroups,
                        key = { it.id }
                    ) { group ->
                        if (isListView) {
                            VideoNameGroupListCard(
                                group = group,
                                onClick = { activeGroupDetail = group }
                            )
                        } else {
                            VideoNameGroupCard(
                                group = group,
                                onClick = { activeGroupDetail = group }
                            )
                        }
                    }
                }
            } else if (selectedFilter == VideoFilterMode.GROUP_BY_FOLDER) {
                if (computedFolders.isEmpty() && !scanProgress.isScanning) {
                    item(span = { GridItemSpan(columnsCount) }) {
                        F2WEmptyState(
                            icon = Icons.Outlined.VideoLibrary,
                            title = if (mediaPermissionState.hasAccess) "No Video Folders Found" else "Permission Required",
                            description = if (mediaPermissionState.hasAccess)
                                "Video folders on your device (e.g. Movies, Downloads, Camera) will be displayed here in 2 columns once storage is scanned."
                            else
                                "Storage permission is required to list device video folders.",
                            actionLabel = if (mediaPermissionState.hasAccess) "Scan Video Za Simu" else "Grant Storage Permission",
                            actionIcon = Icons.Filled.Refresh,
                            onActionClick = {
                                if (mediaPermissionState.hasAccess) {
                                    scanner.startScan(forceFullRescan = true)
                                } else {
                                    mediaPermissionState.requestPermissions()
                                }
                                onScanRequest()
                            },
                            tipText = "Folders are automatically sorted and organized",
                            testTag = "folders_empty_state"
                        )
                    }
                } else {
                    items(
                        items = computedFolders,
                        key = { it.id }
                    ) { folder ->
                        if (isListView) {
                            VideoFolderListCard(
                                folder = folder,
                                onClick = { activeFolderDetail = folder }
                            )
                        } else {
                            VideoFolderCard(
                                folder = folder,
                                onClick = { activeFolderDetail = folder }
                            )
                        }
                    }
                }
            } else {
                if (displayedVideos.isEmpty() && !scanProgress.isScanning) {
                    item(span = { GridItemSpan(columnsCount) }) {
                        if (selectedFilter == VideoFilterMode.RECENTLY_PLAYED) {
                            F2WEmptyState(
                                icon = Icons.Outlined.VideoLibrary,
                                title = "Hakuna Video Zilizotazamwa Karibuni",
                                description = "Video zozote unazotazama zitahifadhiwa hapa kwa mpangilio (hadi video 8) ukiweza kuendeleza pale ulipoishia.",
                                actionLabel = "Tazama Video",
                                actionIcon = Icons.Filled.PlayArrow,
                                onActionClick = { selectedFilter = VideoFilterMode.ALL_VIDEO },
                                tipText = "Hurekodi nafasi ya video uliyotazama mara ya mwisho",
                                testTag = "recently_played_empty_state"
                            )
                        } else {
                            F2WEmptyState(
                                icon = Icons.Outlined.VideoLibrary,
                                title = if (mediaPermissionState.hasAccess) "Hakuna Video Kwenye Simu" else "Ruhusa ya Storage Inahitajika",
                                description = if (mediaPermissionState.hasAccess)
                                    "Hatujapata video zozote kwenye internal storage ya simu yako. Hakikisha umehifadhi video na ubonyeze 'Scan Video' kutafuta tena."
                                else
                                    "App inahitaji ruhusa ya kusoma video zilizo kwenye internal storage ya simu yako ili kuzionyesha na kuzicheza.",
                                actionLabel = if (mediaPermissionState.hasAccess) "Scan Video Za Simu" else "Toa Ruhusa ya Storage",
                                actionIcon = Icons.Filled.Refresh,
                                onActionClick = {
                                    if (mediaPermissionState.hasAccess) {
                                        scanner.startScan(forceFullRescan = true)
                                    } else {
                                        mediaPermissionState.requestPermissions()
                                    }
                                    onScanRequest()
                                },
                                tipText = "Inasoma MP4, MKV, AVI, WebM na format zote za video",
                                testTag = "video_empty_state"
                            )
                        }
                    }
                } else {
                    // Video cards: Horizontal List or 2-Column Grid based on user's toggle
                    items(
                        items = displayedVideos,
                        key = { it.id }
                    ) { video ->
                        if (isListView) {
                            VideoListCard(
                                video = video,
                                isFavorite = favoriteVideoIds.contains(video.id),
                                onToggleFavorite = {
                                    val newFav = !favoriteVideoIds.contains(video.id)
                                    if (newFav) {
                                        favoriteVideoIds.add(video.id)
                                    } else {
                                        favoriteVideoIds.remove(video.id)
                                    }
                                    coroutineScope.launch {
                                        scanner.updateFavorite(video.id, newFav)
                                    }
                                },
                                onClick = { onVideoClick(video, displayedVideos) },
                                onMoreOptionsClick = { activeVideoForMenu = video }
                            )
                        } else {
                            VideoThumbnailCard(
                                video = video,
                                onClick = { onVideoClick(video, displayedVideos) },
                                onMoreOptionsClick = { activeVideoForMenu = video }
                            )
                        }
                    }
                }
            }
        }

        // Smart Pop Menu with Options
        activeVideoForMenu?.let { video ->
            VideoActionSmartMenu(
                video = video,
                onDismissRequest = { activeVideoForMenu = null },
                onDeleteVideo = { deleted ->
                    coroutineScope.launch {
                        scanner.deleteVideoFromDb(deleted.id)
                    }
                },
                onRenameVideo = { target, newName ->
                    coroutineScope.launch {
                        scanner.updateVideoTitle(target.id, newName)
                    }
                },
                onLockInPrivateFolder = { locked ->
                    coroutineScope.launch {
                        scanner.deleteVideoFromDb(locked.id)
                    }
                }
            )
        }
    }

    // Permission rationale dialog if permanently denied
    MediaPermissionRationaleDialog(permissionState = mediaPermissionState)
}

@Composable
private fun FilterPillButton(
    mode: VideoFilterMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Independent Wet Glass & Dew Droplets Glistening Animation
    val dropletTransition = rememberInfiniteTransition(label = "wet_droplets_${mode.name}")
    
    // Glistening sparkle pulse on water droplets
    val gleamPulse by dropletTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600 + (mode.ordinal * 280), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gleam_pulse_${mode.name}"
    )

    // Gentle light sheen passing over the wet glass
    val wetSheenOffset by dropletTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800 + (mode.ordinal * 420), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wet_sheen_${mode.name}"
    )

    // Secondary micro-condensation sparkle
    val microDewShimmer by dropletTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200 + (mode.ordinal * 310), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micro_dew_${mode.name}"
    )

    // 3D Depth & Elevation
    val shadowElevation by animateDpAsState(
        targetValue = if (isSelected) 5.dp else 2.5.dp,
        label = "pill_elevation_${mode.name}"
    )

    val isLight = com.example.ui.theme.F2WIsLightTheme
    val textShadow = com.example.ui.theme.F2WTextShadow
    val contentColor = if (isSelected) {
        Color.White
    } else if (isLight) {
        Color(0xFF0F172A)
    } else {
        Color(0xFFE2F4FA)
    }
    val pillShape = RoundedCornerShape(percent = 50)

    // 3D Bevel Border (Light reflection top-left, darker glass rim bottom-right)
    val bevelBorderBrush = if (isSelected) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.90f),
                Color(0xFF00E5FF),
                Color(0xFF0077B6).copy(alpha = 0.60f),
                Color(0xFF03045E).copy(alpha = 0.90f)
            ),
            start = Offset(0f, 0f),
            end = Offset(200f, 80f)
        )
    } else if (isLight) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0284C7).copy(alpha = 0.80f),
                Color(0xFF38BDF8).copy(alpha = 0.50f),
                Color(0xFF94A3B8).copy(alpha = 0.40f)
            ),
            start = Offset(0f, 0f),
            end = Offset(200f, 80f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.45f),
                Color(0x6600E5FF),
                Color(0x22132D42),
                Color.Black.copy(alpha = 0.75f)
            ),
            start = Offset(0f, 0f),
            end = Offset(200f, 80f)
        )
    }

    // 3D Wet Glass Base Surface Fill
    val baseBackgroundBrush = if (isSelected) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF00B4D8), // bright wet cyan top
                Color(0xFF0077B6), // mid ocean cyan
                Color(0xFF023E8A)  // dark 3D base depth
            )
        )
    } else if (isLight) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xEEFFFFFF), // bright translucent white glass pill
                Color(0xDDEDF2F7),
                Color(0xCCDCE5ED)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xF0183852), // glossy translucent upper wet glass
                Color(0xF50F2538), // mid wet body
                Color(0xFA071522)  // bottom 3D shadow rim
            )
        )
    }

    Box(
        modifier = modifier
            .height(27.dp)
            .shadow(
                elevation = shadowElevation,
                shape = pillShape,
                spotColor = if (isSelected) Color(0x9900E5FF) else Color(0x66000000),
                ambientColor = if (isSelected) Color(0x4400E5FF) else Color(0x33000000)
            )
            .clip(pillShape)
            .background(baseBackgroundBrush)
            .border(width = 1.1.dp, brush = bevelBorderBrush, shape = pillShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .testTag(mode.testTag),
        contentAlignment = Alignment.Center
    ) {
        // 💧 Wet Glass Surface with Glistening Water Droplets Layer
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .clip(pillShape)
        ) {
            val width = size.width
            val height = size.height

            if (width <= 0 || height <= 0) return@Canvas

            // 1. Wet glass condensation mist / subtle moisture gradient
            val wetMistBrush = Brush.radialGradient(
                colors = listOf(
                    (if (isSelected) Color(0x30FFFFFF) else Color(0x2000E5FF)),
                    Color.Transparent
                ),
                center = Offset(width * 0.35f, height * 0.3f),
                radius = width * 0.6f
            )
            drawRect(brush = wetMistBrush)

            // 2. Deterministic Water Droplets (Dew Drops on glass) seeded by mode.ordinal
            val dropletPositions = listOf(
                // xFraction, yFraction, radius, gleamFactor
                listOf(0.12f, 0.32f, 2.6f, 0.9f),
                listOf(0.22f, 0.72f, 1.8f, 0.7f),
                listOf(0.38f, 0.28f, 2.2f, 0.85f),
                listOf(0.52f, 0.68f, 3.2f, 1.0f),
                listOf(0.68f, 0.30f, 2.0f, 0.75f),
                listOf(0.82f, 0.62f, 2.8f, 0.95f),
                listOf(0.92f, 0.38f, 1.6f, 0.65f),
                listOf(0.30f, 0.50f, 1.4f, 0.6f),
                listOf(0.60f, 0.42f, 1.5f, 0.6f),
                listOf(0.76f, 0.76f, 2.4f, 0.8f)
            )

            dropletPositions.forEachIndexed { index, drop ->
                // Stagger slightly per mode
                val xFrac = (drop[0] + (mode.ordinal * 0.07f)) % 0.94f + 0.03f
                val yFrac = (drop[1] + (mode.ordinal * 0.05f)) % 0.80f + 0.10f
                val baseRadius = drop[2]
                val individualGleam = drop[3]

                val cx = width * xFrac
                val cy = height * yFrac

                // Droplet dark shadow underneath on glass
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = baseRadius + 0.4f,
                    center = Offset(cx + 0.6f, cy + 0.8f)
                )

                // Translucent liquid droplet body
                val dropBodyColor = if (isSelected) {
                    Color(0x6000F5FF)
                } else {
                    Color(0x4500D4FF)
                }
                drawCircle(
                    color = dropBodyColor,
                    radius = baseRadius,
                    center = Offset(cx, cy)
                )

                // Internal light refraction bottom crescent
                val bottomRefractionColor = if (isSelected) {
                    Color.White.copy(alpha = 0.45f * gleamPulse)
                } else {
                    Color(0x8000E5FF).copy(alpha = 0.40f * gleamPulse)
                }
                drawCircle(
                    color = bottomRefractionColor,
                    radius = baseRadius * 0.55f,
                    center = Offset(cx + 0.3f, cy + (baseRadius * 0.35f))
                )

                // Glistening specular highlight pin-point on top-left of droplet
                val highlightAlpha = (0.75f + (0.25f * sin(gleamPulse * PI.toFloat() * individualGleam))).coerceIn(0f, 1f)
                drawCircle(
                    color = Color.White.copy(alpha = highlightAlpha),
                    radius = (baseRadius * 0.38f).coerceAtLeast(0.8f),
                    center = Offset(cx - (baseRadius * 0.35f), cy - (baseRadius * 0.35f))
                )
            }

            // 3. Passing Wet Glass Light Sheen Sweep
            val sheenX = wetSheenOffset * (width + height)
            val sheenStart = Offset(sheenX - 25f, 0f)
            val sheenEnd = Offset(sheenX + 25f, height)
            val sheenBrush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    (if (isSelected) Color.White.copy(alpha = 0.26f) else Color(0x3500E5FF)),
                    Color.Transparent
                ),
                start = sheenStart,
                end = sheenEnd
            )
            drawRect(brush = sheenBrush)
        }

        // Top 3D Specular Glass Glare Overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(pillShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isSelected) 0.34f else 0.18f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 32f
                    )
                )
        )

        // Content Row: Icon / NEW Badge & Label
        Row(
            modifier = Modifier.padding(horizontal = 7.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (mode == VideoFilterMode.RECENTLY_ADDED) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.28f)
                            else Color(0xFF00E5FF).copy(alpha = 0.25f)
                        )
                        .border(
                            width = 0.6.dp,
                            color = if (isSelected) Color.White.copy(alpha = 0.5f) else Color(0x6600E5FF),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 3.dp, vertical = 0.8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NEW",
                        color = contentColor,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.3.sp
                    )
                }
            } else {
                Icon(
                    imageVector = mode.icon,
                    contentDescription = mode.label,
                    tint = contentColor,
                    modifier = Modifier.size(13.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.5.dp))

            Text(
                text = mode.label,
                color = contentColor,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                style = if (textShadow != null) androidx.compose.ui.text.TextStyle(shadow = textShadow) else androidx.compose.ui.text.TextStyle.Default,
                maxLines = 1
            )
        }
    }
}
