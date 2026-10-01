package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFormatTest {

    @Test
    fun testFormatDetection() {
        fun detectFormat(title: String, uri: String = ""): String {
            val titleLower = title.lowercase()
            val uriLower = uri.lowercase()
            return when {
                titleLower.contains("mkv") || uriLower.endsWith(".mkv") -> "MKV"
                titleLower.contains("avi") || uriLower.endsWith(".avi") -> "AVI"
                titleLower.contains("webm") || uriLower.endsWith(".webm") -> "WEBM"
                titleLower.contains("mov") || uriLower.endsWith(".mov") -> "MOV"
                else -> "MP4"
            }
        }

        assertEquals("MKV", detectFormat("Avengers.Endgame.2019.mkv"))
        assertEquals("MP4", detectFormat("Tutorial_Flutter_2024.mp4"))
        assertEquals("AVI", detectFormat("Wildlife_Documentary_Africa.avi"))
        assertEquals("WEBM", detectFormat("Nature_Safari.webm"))
    }
}
