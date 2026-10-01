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

    @Test
    fun testAdultingEpisodesGrouping() {
        val videos = listOf(
            VideoItem(id = "1", title = "Adulting S01 Ep01"),
            VideoItem(id = "2", title = "Adulting S01 Ep03"),
            VideoItem(id = "3", title = "Adulting S01 Ep05"),
            VideoItem(id = "4", title = "Adulting S01 Ep06"),
            VideoItem(id = "5", title = "Adulting S01 Ep10")
        )

        val groups = VideoNameGrouper.groupVideosByName(videos)
        assertEquals("All Adulting episodes should be clustered into 1 group", 1, groups.size)
        val adultingGroup = groups.first()
        assertTrue("Group title should contain Adulting", adultingGroup.title.contains("Adulting"))
        assertEquals("Adulting group should contain all 5 episodes", 5, adultingGroup.videos.size)
    }

    @Test
    fun testSwahiliConsecutiveAlphabetPrefixGrouping() {
        val videos = listOf(
            VideoItem(id = "1", title = "babaya wake"),
            VideoItem(id = "2", title = "baba huyu"),
            VideoItem(id = "3", title = "dunia 01"),
            VideoItem(id = "4", title = "dunia no2"),
            VideoItem(id = "5", title = "Single Unrelated Movie")
        )

        val groups = VideoNameGrouper.groupVideosByName(videos)

        val babaGroup = groups.find { it.title.startsWith("Baba", ignoreCase = true) }
        assertTrue("Baba group should be found", babaGroup != null)
        assertEquals(2, babaGroup?.videos?.size)

        val duniaGroup = groups.find { it.title.startsWith("Dunia", ignoreCase = true) }
        assertTrue("Dunia group should be found", duniaGroup != null)
        assertEquals(2, duniaGroup?.videos?.size)

        val singleGroup = groups.find { it.title.contains("Single") }
        assertTrue("Single video should remain 1", singleGroup != null)
        assertEquals(1, singleGroup?.videos?.size)
    }

    @Test(timeout = 2000)
    fun testLargeVideoLibraryGroupingPerformanceNoAnr() {
        val largeList = mutableListOf<VideoItem>()
        for (i in 1..3000) {
            val franchise = when (i % 5) {
                0 -> "Avatar Episode $i"
                1 -> "Dune Part $i"
                2 -> "Spider-Man: Series $i"
                3 -> "Fast and Furious $i"
                else -> "Random Movie File $i"
            }
            largeList.add(
                VideoItem(
                    id = "vid_$i",
                    title = franchise,
                    durationText = "01:45:00",
                    folderName = "Movies"
                )
            )
        }

        val startTime = System.currentTimeMillis()
        val groups = VideoNameGrouper.groupVideosByName(largeList)
        val elapsed = System.currentTimeMillis() - startTime

        assertTrue("Grouping 3000 videos must complete in under 500ms, elapsed: ${elapsed}ms", elapsed < 500)
        assertTrue("Groups must not be empty", groups.isNotEmpty())
    }
}
