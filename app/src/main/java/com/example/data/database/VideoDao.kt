package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class VideoIdAndTimestamp(
    val id: String,
    val dateModified: Long
)

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
    fun getAllVideos(): Flow<List<VideoMediaEntity>>

    @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
    suspend fun getAllVideosSnapshot(): List<VideoMediaEntity>

    @Query("SELECT id, dateModified FROM videos")
    suspend fun getAllVideoTimestamps(): List<VideoIdAndTimestamp>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: String): VideoMediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateVideos(videos: List<VideoMediaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateVideo(video: VideoMediaEntity)

    @Query("DELETE FROM videos WHERE id IN (:ids)")
    suspend fun deleteVideosByIds(ids: List<String>)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideoById(id: String)

    @Query("DELETE FROM videos")
    suspend fun clearAll()

    @Query("UPDATE videos SET playbackProgressMs = :progressMs WHERE id = :id")
    suspend fun updatePlaybackProgress(id: String, progressMs: Long)

    @Query("UPDATE videos SET title = :newTitle WHERE id = :id")
    suspend fun updateVideoTitle(id: String, newTitle: String)

    @Query("UPDATE videos SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: String, isFav: Boolean)

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getVideoCount(): Int
}
