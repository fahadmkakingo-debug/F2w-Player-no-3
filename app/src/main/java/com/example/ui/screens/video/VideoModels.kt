package com.example.ui.screens.video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.ui.graphics.vector.ImageVector

data class VideoItem(
    val id: String,
    val title: String,
    val durationText: String = "00:00",
    val durationMs: Long = 0L,
    val playbackProgressMs: Long = 0L,
    val sizeText: String = "0 MB",
    val resolution: String = "1080P", // "4K", "1080P", "720P", "SD"
    val year: String? = null,
    val folderName: String = "Internal Storage",
    val uriString: String = "",
    val dateAdded: Long = 0L,
    val lastPlayedTime: Long = 0L
) {
    val progressFraction: Float
        get() = if (durationMs > 0 && playbackProgressMs > 0) {
            (playbackProgressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val isPartiallyWatched: Boolean
        get() = progressFraction in 0.03f..0.96f
}

data class VideoFolder(
    val id: String,
    val name: String,
    val videoCount: Int = 0,
    val path: String = "",
    val videos: List<VideoItem> = emptyList()
) {
    val primaryVideo: VideoItem? get() = videos.firstOrNull()
}

enum class VideoFilterMode(
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    ALL_VIDEO(
        label = "All Video",
        icon = Icons.Filled.PlayArrow,
        testTag = "filter_all_video"
    ),
    GROUP_BY_NAME(
        label = "Group by Name",
        icon = Icons.Filled.SortByAlpha,
        testTag = "filter_group_by_name"
    ),
    GROUP_BY_FOLDER(
        label = "Group by Folder",
        icon = Icons.Filled.Folder,
        testTag = "filter_group_by_folder"
    ),
    RECENTLY_ADDED(
        label = "Recently Added",
        icon = Icons.Filled.FiberNew,
        testTag = "filter_recently_added"
    ),
    RECENTLY_PLAYED(
        label = "Recently Played",
        icon = Icons.Filled.History,
        testTag = "filter_recently_played"
    )
}
