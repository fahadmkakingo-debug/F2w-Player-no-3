package com.example.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.ui.screens.video.VideoItem

@Entity(
    tableName = "videos",
    indices = [
        Index(value = ["dateAdded"]),
        Index(value = ["folderName"]),
        Index(value = ["title"])
    ]
)
data class VideoMediaEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val durationText: String,
    val durationMs: Long,
    val playbackProgressMs: Long,
    val sizeBytes: Long,
    val sizeText: String,
    val resolution: String,
    val year: String?,
    val folderName: String,
    val uriString: String,
    val dateAdded: Long,
    val dateModified: Long,
    val lastPlayedTime: Long = 0L,
    val isFavorite: Boolean = false
) {
    fun toVideoItem(): VideoItem {
        return VideoItem(
            id = id,
            title = title,
            durationText = durationText,
            durationMs = durationMs,
            playbackProgressMs = playbackProgressMs,
            sizeText = sizeText,
            resolution = resolution,
            year = year,
            folderName = folderName,
            uriString = uriString,
            dateAdded = dateAdded,
            lastPlayedTime = lastPlayedTime
        )
    }

    companion object {
        fun fromVideoItem(
            item: VideoItem,
            sizeBytes: Long = 0L,
            dateModified: Long = 0L,
            isFavorite: Boolean = false
        ): VideoMediaEntity {
            return VideoMediaEntity(
                id = item.id,
                title = item.title,
                durationText = item.durationText,
                durationMs = item.durationMs,
                playbackProgressMs = item.playbackProgressMs,
                sizeBytes = sizeBytes,
                sizeText = item.sizeText,
                resolution = item.resolution,
                year = item.year,
                folderName = item.folderName,
                uriString = item.uriString,
                dateAdded = item.dateAdded,
                dateModified = dateModified,
                lastPlayedTime = item.lastPlayedTime,
                isFavorite = isFavorite
            )
        }
    }
}
