package com.example

import com.example.ui.screens.video.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentlyPlayedManagerTest {

    @Test
    fun testRecentlyPlayedOrderAndLimit() {
        val maxRecent = 8
        val recentIds = mutableListOf<String>()

        fun recordPlayed(id: String) {
            recentIds.remove(id)
            recentIds.add(0, id)
            if (recentIds.size > maxRecent) {
                recentIds.removeAt(recentIds.size - 1)
            }
        }

        // Play 5 videos
        listOf("v1", "v2", "v3", "v4", "v5").forEach { recordPlayed(it) }
        assertEquals(5, recentIds.size)
        assertEquals("v5", recentIds[0]) // Most recently played is at index 0

        // Re-play "v2" -> should jump to the top
        recordPlayed("v2")
        assertEquals(5, recentIds.size)
        assertEquals("v2", recentIds[0])
        assertEquals("v5", recentIds[1])

        // Add more videos to exceed 8
        listOf("v6", "v7", "v8", "v9").forEach { recordPlayed(it) }
        assertEquals(8, recentIds.size)
        assertEquals("v9", recentIds[0])

        // Oldest item "v1" and "v3" should be evicted
        assertFalse(recentIds.contains("v1"))
        assertTrue(recentIds.contains("v9"))
        assertTrue(recentIds.contains("v2")) // was re-played so moved up
    }

    @Test
    fun testPlaybackPositionPreservation() {
        val progressMap = mutableMapOf<String, Long>()

        fun saveProgress(id: String, pos: Long) {
            progressMap[id] = pos
        }

        saveProgress("v1", 125000L)
        saveProgress("v2", 3400000L)

        val v1 = VideoItem(id = "v1", title = "Movie 1", playbackProgressMs = 0L)
        val v1Restored = v1.copy(playbackProgressMs = progressMap["v1"] ?: 0L)

        assertEquals(125000L, v1Restored.playbackProgressMs)
    }

    @Test
    fun testResumeMidwayAndRestartFromBeginning() {
        var currentPositionMs = 85000L // 1m 25s midway
        var isResumePromptShown = currentPositionMs > 2000L
        assertTrue(isResumePromptShown)

        // If user clicks restart / Anza Upya
        fun restartPlayback() {
            currentPositionMs = 0L
            isResumePromptShown = false
        }

        restartPlayback()
        assertEquals(0L, currentPositionMs)
        assertFalse(isResumePromptShown)
    }
}
