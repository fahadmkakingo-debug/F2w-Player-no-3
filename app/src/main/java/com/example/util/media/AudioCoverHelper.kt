package com.example.util.media

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object AudioCoverHelper {

    private val memoryCache = ConcurrentHashMap<String, Bitmap?>()

    fun getAlbumArtUri(albumId: Long): Uri {
        return ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
    }

    suspend fun getAudioCoverBitmap(context: Context, audioUriString: String): Bitmap? = withContext(Dispatchers.IO) {
        if (audioUriString.isBlank()) return@withContext null
        if (memoryCache.containsKey(audioUriString)) {
            return@withContext memoryCache[audioUriString]
        }

        var bitmap: Bitmap? = null
        val retriever = MediaMetadataRetriever()
        try {
            when {
                audioUriString.startsWith("content://") || audioUriString.startsWith("file://") -> {
                    retriever.setDataSource(context, Uri.parse(audioUriString))
                }
                File(audioUriString).exists() -> {
                    retriever.setDataSource(audioUriString)
                }
            }
            val artBytes = retriever.embeddedPicture
            if (artBytes != null && artBytes.isNotEmpty()) {
                val opts = BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, opts)
            }
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        if (bitmap != null) {
            memoryCache[audioUriString] = bitmap
        }
        return@withContext bitmap
    }
}
