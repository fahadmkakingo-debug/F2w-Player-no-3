package com.example

import com.example.ui.screens.video.VideoFolder
import com.example.ui.screens.video.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFolderTest {

    @Test
    fun testFolderGroupingByFolderName() {
        val videos = listOf(
            VideoItem(id = "1", title = "Movie A", folderName = "Movies"),
            VideoItem(id = "2", title = "Movie B", folderName = "Movies"),
            VideoItem(id = "3", title = "Clip 1", folderName = "WhatsApp Video"),
            VideoItem(id = "4", title = "Download 1", folderName = "Downloads"),
            VideoItem(id = "5", title = "Download 2", folderName = "Downloads")
        )

        val folders = videos.groupBy { it.folderName }.map { (name, list) ->
            VideoFolder(
                id = name,
                name = name,
                videoCount = list.size,
                path = "/storage/emulated/0/$name",
                videos = list
            )
        }.sortedByDescending { it.videoCount }

        assertEquals(3, folders.size)

        val moviesFolder = folders.find { it.name == "Movies" }
        assertNotNull(moviesFolder)
        assertEquals(2, moviesFolder?.videoCount)
        assertEquals(2, moviesFolder?.videos?.size)
        assertEquals("Movie A", moviesFolder?.primaryVideo?.title)

        val downloadsFolder = folders.find { it.name == "Downloads" }
        assertNotNull(downloadsFolder)
        assertEquals(2, downloadsFolder?.videoCount)

        val whatsAppFolder = folders.find { it.name == "WhatsApp Video" }
        assertNotNull(whatsAppFolder)
        assertEquals(1, whatsAppFolder?.videoCount)
    }
}
