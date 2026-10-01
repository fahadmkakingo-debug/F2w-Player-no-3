package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.F2WDatabase
import com.example.data.database.VideoMediaEntity
import com.example.ui.screens.video.VideoItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalVideoScannerTest {

    private lateinit var db: F2WDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, F2WDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testRoomVideoEntityInsertAndRetrieval() = runBlocking {
        val videoDao = db.videoDao()

        val sampleVideos = (1..500).map { i ->
            VideoMediaEntity(
                id = "vid_$i",
                title = "Sample Video $i",
                durationText = "12:30",
                durationMs = 750000L,
                playbackProgressMs = 12000L,
                sizeBytes = 104857600L,
                sizeText = "100 MB",
                resolution = "1080P",
                year = "2024",
                folderName = "Movies",
                uriString = "content://media/external/video/media/$i",
                dateAdded = 1700000000L + i,
                dateModified = 1700000000L + i
            )
        }

        // Batch insert
        videoDao.insertOrUpdateVideos(sampleVideos)

        val count = videoDao.getVideoCount()
        assertEquals(500, count)

        val snapshot = videoDao.getAllVideosSnapshot()
        assertEquals(500, snapshot.size)
        assertEquals("Sample Video 500", snapshot.first().title) // Sorted by dateAdded DESC

        // Test incremental update
        val updatedVideo = snapshot.first().copy(title = "Updated Avatar 500")
        videoDao.insertOrUpdateVideo(updatedVideo)

        val retrieved = videoDao.getVideoById("vid_500")
        assertNotNull(retrieved)
        assertEquals("Updated Avatar 500", retrieved?.title)

        // Test batch deletion
        videoDao.deleteVideosByIds(listOf("vid_1", "vid_2", "vid_3"))
        assertEquals(497, videoDao.getVideoCount())
    }
}
