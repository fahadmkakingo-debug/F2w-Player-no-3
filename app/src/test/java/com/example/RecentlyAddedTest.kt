package com.example

import com.example.ui.screens.video.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentlyAddedTest {

    @Test
    fun testRecentlyAddedSortsNewestToOldest() {
        val v1 = VideoItem(id = "1", title = "Old Movie", dateAdded = 1000L)
        val v2 = VideoItem(id = "2", title = "Recent Movie", dateAdded = 5000L)
        val v3 = VideoItem(id = "3", title = "Latest Movie", dateAdded = 9000L)
        val v4 = VideoItem(id = "4", title = "Medium Movie", dateAdded = 3000L)

        val videos = listOf(v1, v2, v3, v4)

        val sorted = videos.sortedWith(
            compareByDescending<VideoItem> { it.dateAdded }
                .thenByDescending { it.id.toLongOrNull() ?: 0L }
        )

        assertEquals("Latest Movie", sorted[0].title)
        assertEquals("Recent Movie", sorted[1].title)
        assertEquals("Medium Movie", sorted[2].title)
        assertEquals("Old Movie", sorted[3].title)
    }
}
