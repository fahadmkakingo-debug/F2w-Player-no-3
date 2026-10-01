package com.example

import com.example.data.playlist.PlaylistItemModel
import com.example.data.playlist.PlaylistMediaType
import com.example.data.playlist.UserPlaylist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistTest {

    @Test
    fun testAudioAndVideoPlaylistSeparation() {
        val playlists = listOf(
            UserPlaylist("1", "Workout Beats", PlaylistMediaType.AUDIO),
            UserPlaylist("2", "Chill Lounge", PlaylistMediaType.AUDIO),
            UserPlaylist("3", "Action Movies", PlaylistMediaType.VIDEO),
            UserPlaylist("4", "Travel Vlogs", PlaylistMediaType.VIDEO)
        )

        val audioPlaylists = playlists.filter { it.type == PlaylistMediaType.AUDIO }
        val videoPlaylists = playlists.filter { it.type == PlaylistMediaType.VIDEO }

        assertEquals(2, audioPlaylists.size)
        assertEquals(2, videoPlaylists.size)
        assertTrue(audioPlaylists.all { it.type == PlaylistMediaType.AUDIO })
        assertTrue(videoPlaylists.all { it.type == PlaylistMediaType.VIDEO })
    }

    @Test
    fun testCreateNewPlaylistWithName() {
        fun createPlaylist(name: String, type: PlaylistMediaType): UserPlaylist {
            val validName = name.trim().ifBlank {
                if (type == PlaylistMediaType.AUDIO) "New Audio Playlist" else "New Video Playlist"
            }
            return UserPlaylist(
                id = "pl_test_1",
                name = validName,
                type = type,
                items = emptyList()
            )
        }

        val audioPl = createPlaylist("Bongo Flava Hits", PlaylistMediaType.AUDIO)
        assertEquals("Bongo Flava Hits", audioPl.name)
        assertEquals(PlaylistMediaType.AUDIO, audioPl.type)

        val videoPl = createPlaylist("Filamu za Mapenzi", PlaylistMediaType.VIDEO)
        assertEquals("Filamu za Mapenzi", videoPl.name)
        assertEquals(PlaylistMediaType.VIDEO, videoPl.type)

        // Blank name falls back to default
        val defaultAudio = createPlaylist("   ", PlaylistMediaType.AUDIO)
        assertEquals("New Audio Playlist", defaultAudio.name)
    }

    @Test
    fun testAddAndRemoveMediaFromPlaylist() {
        val playlist = UserPlaylist(
            id = "pl_100",
            name = "My Summer Mix",
            type = PlaylistMediaType.AUDIO,
            items = emptyList()
        )

        val item1 = PlaylistItemModel("m1", "Track 1", "uri1", "03:30", 210000L)
        val item2 = PlaylistItemModel("m2", "Track 2", "uri2", "04:15", 255000L)

        // Add items
        var currentItems = playlist.items + listOf(item1, item2)
        assertEquals(2, currentItems.size)
        assertEquals("Track 1", currentItems[0].title)
        assertEquals("Track 2", currentItems[1].title)

        // Prevent duplicate additions
        val duplicateCandidate = listOf(item1, PlaylistItemModel("m3", "Track 3", "uri3"))
        val existingIds = currentItems.map { it.id }.toSet()
        val newToAdd = duplicateCandidate.filter { !existingIds.contains(it.id) }
        currentItems = currentItems + newToAdd
        assertEquals(3, currentItems.size)

        // Remove item from playlist
        currentItems = currentItems.filter { it.id != "m1" }
        assertEquals(2, currentItems.size)
        assertFalse(currentItems.any { it.id == "m1" })
        assertTrue(currentItems.any { it.id == "m2" })
        assertTrue(currentItems.any { it.id == "m3" })
    }

    @Test
    fun testSequentialAutoPlayOrder() {
        val items = listOf(
            PlaylistItemModel("v1", "Intro Video", "uri1"),
            PlaylistItemModel("v2", "Main Story Part 1", "uri2"),
            PlaylistItemModel("v3", "Main Story Part 2", "uri3"),
            PlaylistItemModel("v4", "Outro Credits", "uri4")
        )

        // When v1 finishes (index 0), sequential next is v2 (index 1)
        fun getNextSequentialIndex(currentIndex: Int, totalSize: Int): Int? {
            return if (currentIndex < totalSize - 1) currentIndex + 1 else null
        }

        val next0 = getNextSequentialIndex(0, items.size)
        assertEquals(1, next0)
        assertEquals("Main Story Part 1", items[next0!!].title)

        val next1 = getNextSequentialIndex(1, items.size)
        assertEquals(2, next1)
        assertEquals("Main Story Part 2", items[next1!!].title)

        val next2 = getNextSequentialIndex(2, items.size)
        assertEquals(3, next2)
        assertEquals("Outro Credits", items[next2!!].title)

        // At end of playlist (index 3), no next item unless repeat is on
        assertEquals(null, getNextSequentialIndex(3, items.size))
    }

    @Test
    fun testTotalDurationCalculation() {
        val items = listOf(
            PlaylistItemModel("t1", "Track 1", "uri1", durationMs = 120000L), // 2 min
            PlaylistItemModel("t2", "Track 2", "uri2", durationMs = 180000L)  // 3 min
        )
        val playlist = UserPlaylist("p1", "Test", PlaylistMediaType.AUDIO, items = items)
        assertEquals("5m 0s", playlist.totalDurationText)
    }

    @Test
    fun testConvertVideoToMp3Model() {
        val videoTitle = "Wildlife_Documentary_Africa.avi"
        val cleanTitle = videoTitle.substringBeforeLast(".")
        val mp3Title = "$cleanTitle.mp3"

        assertEquals("Wildlife_Documentary_Africa.mp3", mp3Title)

        val audioModel = PlaylistItemModel(
            id = "converted_123",
            title = mp3Title,
            uriString = "content://media/video_123",
            durationText = "52:10",
            durationMs = 3130000L,
            sizeText = "48 MB",
            subtitle = "Converted from Video",
            mediaType = PlaylistMediaType.AUDIO
        )

        assertEquals("Wildlife_Documentary_Africa.mp3", audioModel.title)
        assertEquals(PlaylistMediaType.AUDIO, audioModel.mediaType)
        assertEquals("Converted from Video", audioModel.subtitle)
    }

    @Test
    fun testAddVideoToExistingPlaylist() {
        var playlist = UserPlaylist(
            id = "pl_vids",
            name = "Movie Clips",
            type = PlaylistMediaType.VIDEO,
            items = emptyList()
        )

        val newVideo = PlaylistItemModel(
            id = "v_africa",
            title = "Wildlife_Documentary_Africa.avi",
            uriString = "uri_africa",
            durationText = "52:10",
            durationMs = 3130000L,
            mediaType = PlaylistMediaType.VIDEO
        )

        playlist = playlist.copy(items = playlist.items + newVideo)
        assertEquals(1, playlist.items.size)
        assertEquals("Wildlife_Documentary_Africa.avi", playlist.items[0].title)
    }
}
