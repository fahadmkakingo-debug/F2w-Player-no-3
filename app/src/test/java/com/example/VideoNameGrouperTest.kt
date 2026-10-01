package com.example

import com.example.ui.screens.video.VideoItem
import com.example.ui.screens.video.VideoNameGrouper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoNameGrouperTest {

    @Test
    fun testAvatarFranchiseGrouping() {
        val videos = listOf(
            VideoItem(id = "1", title = "Avatar 1"),
            VideoItem(id = "2", title = "Avatar 2: The Way of Water"),
            VideoItem(id = "3", title = "Avatar 3: Fire and Ash"),
            VideoItem(id = "4", title = "Oppenheimer Special Edition"),
            VideoItem(id = "5", title = "Interstellar IMAX Remastered")
        )

        val groups = VideoNameGrouper.groupVideosByName(videos)

        // Find Avatar group
        val avatarGroup = groups.find { it.title.startsWith("Avatar") }
        assertTrue("Avatar group should be found", avatarGroup != null)
        assertEquals(3, avatarGroup?.videos?.size)

        // Oppenheimer should remain as individual group
        val oppenheimerGroup = groups.find { it.title.contains("Oppenheimer") }
        assertTrue("Oppenheimer group should be found", oppenheimerGroup != null)
        assertEquals(1, oppenheimerGroup?.videos?.size)

        // Interstellar should remain as individual group
        val interstellarGroup = groups.find { it.title.contains("Interstellar") }
        assertTrue("Interstellar group should be found", interstellarGroup != null)
        assertEquals(1, interstellarGroup?.videos?.size)
    }

    @Test
    fun testDuneGrouping() {
        val videos = listOf(
            VideoItem(id = "1", title = "Dune: Part One"),
            VideoItem(id = "2", title = "Dune: Part Two"),
            VideoItem(id = "3", title = "Spider-Man: Into the Spider-Verse"),
            VideoItem(id = "4", title = "Spider-Man: Across the Spider-Verse")
        )

        val groups = VideoNameGrouper.groupVideosByName(videos)

        val duneGroup = groups.find { it.title.equals("Dune", ignoreCase = true) }
        assertTrue("Dune group should exist", duneGroup != null)
        assertEquals(2, duneGroup?.videos?.size)

        val spidermanGroup = groups.find { it.title.contains("Spider", ignoreCase = true) }
        assertTrue("Spider-Man group should exist", spidermanGroup != null)
        assertEquals(2, spidermanGroup?.videos?.size)
    }
}
