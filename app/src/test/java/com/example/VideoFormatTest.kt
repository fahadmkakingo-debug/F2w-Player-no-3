package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFormatTest {

    @Test
    fun testFormatDetection() {
        fun detectFormat(title: String, uri: String = ""): String {
            val titleLower = title.lowercase()
            val uriLower = uri.lowercase()
            return when {
                titleLower.endsWith(".dd0") || uriLower.endsWith(".dd0") || titleLower.contains(".dd0") -> "DD0"
                titleLower.contains("mkv") || uriLower.endsWith(".mkv") -> "MKV"
                titleLower.contains("avi") || uriLower.endsWith(".avi") -> "AVI"
                titleLower.contains("webm") || uriLower.endsWith(".webm") -> "WEBM"
                titleLower.contains("mov") || uriLower.endsWith(".mov") -> "MOV"
                else -> "MP4"
            }
        }

        assertEquals("DD0", detectFormat("mama.dd0"))
        assertEquals("DD0", detectFormat("Video_Sample.dd0", "file:///storage/emulated/0/Download/Video_Sample.dd0"))
        assertEquals("MKV", detectFormat("Avengers.Endgame.2019.mkv"))
        assertEquals("MP4", detectFormat("Tutorial_Flutter_2024.mp4"))
        assertEquals("AVI", detectFormat("Wildlife_Documentary_Africa.avi"))
        assertEquals("WEBM", detectFormat("Nature_Safari.webm"))
    }

    @Test
    fun testDd0FileNamePreservation() {
        fun cleanFileName(name: String): String {
            if (name.endsWith(".dd0", ignoreCase = true)) {
                return name
            }
            return name
                .substringBeforeLast(".")
                .replace(".", " ")
                .replace("_", " ")
                .replace("-", " ")
                .trim()
        }

        assertEquals("mama.dd0", cleanFileName("mama.dd0"))
        assertEquals("Tutorial Lesson.dd0", cleanFileName("Tutorial Lesson.dd0"))
        assertEquals("mama", cleanFileName("mama.mp4"))
    }

    @Test
    fun testDd0MimeTypePlaybackTarget() {
        fun resolveMimeTypeForMedia(title: String, uriString: String): String {
            return if (title.endsWith(".dd0", ignoreCase = true) ||
                uriString.endsWith(".dd0", ignoreCase = true)
            ) {
                "video/mp4"
            } else {
                "video/default"
            }
        }

        assertEquals("video/mp4", resolveMimeTypeForMedia("mama.dd0", "file:///storage/emulated/0/Movies/mama.dd0"))
        assertTrue(resolveMimeTypeForMedia("mama.dd0", "").contains("mp4"))
    }
}

