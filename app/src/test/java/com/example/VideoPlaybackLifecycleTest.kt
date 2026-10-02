package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.video.VideoPlaybackManager
import com.example.ui.screens.video.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VideoPlaybackLifecycleTest {

    private fun createTestVideo(id: String, title: String): VideoItem {
        return VideoItem(
            id = id,
            title = title,
            durationText = "05:00",
            durationMs = 300000L,
            playbackProgressMs = 0L,
            sizeText = "10 MB",
            resolution = "1080P",
            year = "2024",
            folderName = "Movies",
            uriString = "content://media/external/video/media/$id"
        )
    }

    @Test
    fun testVideoSequence_PlayBackPlayBackMultipleVideos() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = VideoPlaybackManager.getInstance(context)
        manager.setBackgroundAudioEnabled(false)

        val video1 = createTestVideo("1", "Video 1")
        val video2 = createTestVideo("2", "Video 2")
        val video3 = createTestVideo("3", "Video 3")
        val video4 = createTestVideo("4", "Video 4")
        val playlist = listOf(video1, video2, video3, video4)

        // 1. Play Video 1
        manager.playVideo(video1, playlist)
        assertEquals("Video 1", manager.currentVideo.value?.title)
        assertTrue(manager.isFullScreenOpen.value)
        assertNotNull(manager.getOrCreatePlayer())

        // 2. Back to library
        manager.closeFullScreen()
        assertNull(manager.currentVideo.value)
        assertFalse(manager.isFullScreenOpen.value)

        // 3. Play Video 2
        manager.playVideo(video2, playlist)
        assertEquals("Video 2", manager.currentVideo.value?.title)
        assertTrue(manager.isFullScreenOpen.value)

        // 4. Back to library
        manager.closeFullScreen()
        assertNull(manager.currentVideo.value)
        assertFalse(manager.isFullScreenOpen.value)

        // 5. Play Video 3
        manager.playVideo(video3, playlist)
        assertEquals("Video 3", manager.currentVideo.value?.title)
        assertTrue(manager.isFullScreenOpen.value)

        // 6. Back to library
        manager.closeFullScreen()
        assertNull(manager.currentVideo.value)
        assertFalse(manager.isFullScreenOpen.value)

        // 7. Play Video 4
        manager.playVideo(video4, playlist)
        assertEquals("Video 4", manager.currentVideo.value?.title)
        assertTrue(manager.isFullScreenOpen.value)

        // 8. Back to library and return to Video 1
        manager.closeFullScreen()
        manager.playVideo(video1, playlist)
        assertEquals("Video 1", manager.currentVideo.value?.title)
        assertTrue(manager.isFullScreenOpen.value)
    }

    @Test
    fun testBackgroundAudioPersistence_WhenGoingBack() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = VideoPlaybackManager.getInstance(context)

        val video = createTestVideo("5", "Sample Background Video")
        manager.setBackgroundAudioEnabled(true)
        manager.playVideo(video)

        assertTrue(manager.isFullScreenOpen.value)
        assertEquals("Sample Background Video", manager.currentVideo.value?.title)

        // User goes back while background audio is ENABLED
        manager.closeFullScreen()

        // Fullscreen closes, but video and mini player remain active!
        assertFalse(manager.isFullScreenOpen.value)
        assertEquals("Sample Background Video", manager.currentVideo.value?.title)
        assertTrue(manager.isMiniPlayerVisible.value)

        // Dismissing mini player stops playback
        manager.dismissMiniPlayer()
        assertNull(manager.currentVideo.value)
        assertFalse(manager.isMiniPlayerVisible.value)
    }
}
