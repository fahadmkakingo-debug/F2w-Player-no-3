package com.example.data.media

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.screens.video.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecentlyPlayedManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("f2w_playback_progress", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_RECENT_ORDER = "recently_played_video_ids"
        const val MAX_RECENT_VIDEOS = 8

        @Volatile
        private var INSTANCE: RecentlyPlayedManager? = null

        fun getInstance(context: Context): RecentlyPlayedManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RecentlyPlayedManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _recentlyPlayedIds = MutableStateFlow<List<String>>(loadRecentIds())
    val recentlyPlayedIds: StateFlow<List<String>> = _recentlyPlayedIds.asStateFlow()

    fun loadRecentIds(): List<String> {
        val raw = prefs.getString(KEY_RECENT_ORDER, null)
        if (raw.isNullOrBlank()) {
            // Initial seed from sample videos that have recorded progress
            val defaultSeeds = listOf("demo_avengers_endgame", "demo_avatar_1", "demo_avatar_2", "demo_7")
            return defaultSeeds
        }
        return raw.split(",").filter { it.isNotBlank() }.take(MAX_RECENT_VIDEOS)
    }

    private fun saveRecentIds(ids: List<String>) {
        val limited = ids.take(MAX_RECENT_VIDEOS)
        prefs.edit().putString(KEY_RECENT_ORDER, limited.joinToString(",")).apply()
        _recentlyPlayedIds.value = limited
    }

    /**
     * Record video playback. Moves the video to the top of Recently Played (index 0).
     * Enforces maximum of 8 videos; the 9th oldest is automatically removed from this list.
     * Does not delete the video from device or other lists.
     */
    fun recordVideoPlayed(videoId: String, positionMs: Long = 0L) {
        if (videoId.isBlank()) return

        val currentList = loadRecentIds().toMutableList()
        currentList.remove(videoId)
        currentList.add(0, videoId) // Move to top

        saveRecentIds(currentList)

        if (positionMs > 0L) {
            savePlaybackPosition(videoId, positionMs)
        }
    }

    fun savePlaybackPosition(videoId: String, positionMs: Long) {
        if (videoId.isBlank()) return
        prefs.edit().putLong("progress_$videoId", positionMs).apply()
    }

    fun getPlaybackPosition(videoId: String): Long {
        return prefs.getLong("progress_$videoId", 0L)
    }

    /**
     * Map IDs to VideoItems preserving the exact most-recently-watched-to-oldest order.
     * Injects the saved playback position into each VideoItem.
     */
    fun getRecentlyPlayedVideos(allVideos: List<VideoItem>): List<VideoItem> {
        val recentIds = loadRecentIds()
        val videoMap = allVideos.associateBy { it.id }

        return recentIds.mapNotNull { id ->
            val video = videoMap[id] ?: DemoVideoData.sampleVideos.find { it.id == id }
            video?.let {
                val savedProgress = getPlaybackPosition(id).let { pos ->
                    if (pos > 0L) pos else it.playbackProgressMs
                }
                it.copy(playbackProgressMs = savedProgress)
            }
        }.take(MAX_RECENT_VIDEOS)
    }
}
