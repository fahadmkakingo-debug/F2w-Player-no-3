package com.example.util.media

import android.content.Context
import android.net.Uri
import android.os.Build
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.videoFrameMillis

object VideoThumbnailHelper {

    @Volatile
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (!isInitialized) {
                try {
                    val loader = buildImageLoader(context)
                    Coil.setImageLoader(loader)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                isInitialized = true
            }
        }
    }

    fun buildImageLoader(context: Context): ImageLoader {
        val appContext = context.applicationContext
        return ImageLoader.Builder(appContext)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(appContext.cacheDir.resolve("f2w_video_thumbs"))
                    .maxSizePercent(0.08)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    fun buildThumbnailRequest(
        context: Context,
        uriString: String,
        frameMillis: Long = 1000L
    ): ImageRequest {
        val dataObj: Any = when {
            uriString.startsWith("file://") -> {
                try {
                    val path = Uri.parse(uriString).path ?: uriString.removePrefix("file://")
                    java.io.File(path)
                } catch (_: Exception) {
                    Uri.parse(uriString)
                }
            }
            uriString.startsWith("content://") -> Uri.parse(uriString)
            uriString.startsWith("/") -> java.io.File(uriString)
            else -> uriString
        }

        return ImageRequest.Builder(context)
            .data(dataObj)
            .decoderFactory(VideoFrameDecoder.Factory())
            .videoFrameMillis(frameMillis)
            .crossfade(true)
            .build()
    }
}
