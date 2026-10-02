package com.example.ui.screens.video

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onVideoClick: (VideoItem, List<VideoItem>) -> Unit = { _, _ -> },
    onFolderClick: (VideoFolder) -> Unit = {}
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
    var selectedFilter by remember {
        mutableStateOf(
            try {
                VideoFilterMode.valueOf(videoSettings.selectedVideoFilter)
            } catch (_: Exception) {
                VideoFilterMode.ALL_VIDEO
            }
        )
    }
    var activeVideoForMenu by remember { mutableStateOf<VideoItem?>(null) }
    var activeGroupDetail by remember { mutableStateOf<VideoNameGroup?>(null) }
    var activeFolderDetail by remember { mutableStateOf<VideoFolder?>(null) }
    val favoriteVideoIds = remember { mutableStateListOf<String>() }

    val vaultManager = remember { PrivacyVaultManager.getInstance(context) }
    var movedVaultPaths by remember {
        mutableStateOf(vaultManager.getVaultItems().map { it.originalPath.lowercase() }.toSet())
    }

    LaunchedEffect(Unit) {
        vaultManager.vaultUpdates.collect {
            movedVaultPaths = vaultManager.getVaultItems().map { it.originalPath.lowercase() }.toSet()
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

    val availableVideos = remember(effectiveVideos, movedVaultPaths) {
        if (movedVaultPaths.isEmpty()) effectiveVideos
        else effectiveVideos.filter { v ->
            !movedVaultPaths.contains(v.uriString.lowercase()) &&
            !movedVaultPaths.any { path -> path.isNotBlank() && path.endsWith(v.title.lowercase()) }
        }
    }

    // Fast O(N) grouping and folder extraction
    val nameGroups = remember(availableVideos) {
        VideoNameGrouper.groupVideosByName(availableVideos)
    }

    val computedFolders = remember(availableVideos) {
        scanner.extractFolders(availableVideos)
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

    // Filter videos according to selected filter tab
    val displayedVideos = remember(selectedFilter, availableVideos, recentlyPlayedIds) {
        when (selectedFilter) {
            VideoFilterMode.ALL_VIDEO -> availableVideos
            VideoFilterMode.GROUP_BY_NAME -> availableVideos.sortedBy { it.title.lowercase() }
            VideoFilterMode.RECENTLY_ADDED -> availableVideos.sortedWith(
                compareByDescending<VideoItem> { it.dateAdded }
                    .thenByDescending { it.id.toLongOrNull() ?: 0L }
            )
            VideoFilterMode.RECENTLY_PLAYED -> {
                recentlyPlayedManager.getRecentlyPlayedVideos(availableVideos)
            }
            VideoFilterMode.GROUP_BY_FOLDER -> availableVideos
        }
    }

    val columnsCount = if (isListView) 1 else 2

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(F2WSurface)
            .testTag("video_screen_container")
    ) {
        // Fixed / Sticky Top Task Panel for Filter Pills
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(F2WSurface)
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
    val primaryColor = F2WCyanPrimary
    val surfaceElevated = F2WSurfaceElevated
    val cardBorder = F2WCardBorder
    val textSecondary = F2WTextSecondary

    val backgroundColor = if (isSelected) primaryColor else surfaceElevated
    val contentColor = if (isSelected) Color.White else textSecondary
    val borderColor = if (isSelected) primaryColor else cardBorder

    Box(
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(13.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .testTag(mode.testTag)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (mode == VideoFilterMode.RECENTLY_ADDED) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.28f) else Color(0xFF133B47))
                        .padding(horizontal = 2.5.dp, vertical = 0.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NEW",
                        color = contentColor,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.2.sp
                    )
                }
            } else {
                Icon(
                    imageVector = mode.icon,
                    contentDescription = mode.label,
                    tint = contentColor,
                    modifier = Modifier.size(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = mode.label,
                color = contentColor,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
