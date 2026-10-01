package com.example.data.media

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.ui.screens.video.VideoFolder
import com.example.ui.screens.video.VideoItem
import com.example.util.permission.MediaPermissionManager
import com.example.util.permission.MediaPermissionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class LocalVideoScanner(private val context: Context) {
    private val progressPrefs =
        context.getSharedPreferences("f2w_playback_progress", Context.MODE_PRIVATE)

    private val scanMutex = Mutex()

    suspend fun scanDeviceVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        scanMutex.withLock {
            val videoList = mutableListOf<VideoItem>()

            // Check if permission is granted before querying MediaStore
            val hasAccess = MediaPermissionManager.hasMediaAccess(context, MediaPermissionType.VIDEO)
            if (!hasAccess) return@withContext emptyList()

            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            )

            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
            val allProgressMap = progressPrefs.all
            val yearPattern = Pattern.compile("\\b(19\\d\\d|20\\d\\d)\\b")
            val sdfYear = SimpleDateFormat("yyyy", Locale.getDefault())

            try {
                val cursor = context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )

                cursor?.use {
                    val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val displayNameCol = it.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    val titleCol = it.getColumnIndex(MediaStore.Video.Media.TITLE)
                    val durationCol = it.getColumnIndex(MediaStore.Video.Media.DURATION)
                    val sizeCol = it.getColumnIndex(MediaStore.Video.Media.SIZE)
                    val widthCol = it.getColumnIndex(MediaStore.Video.Media.WIDTH)
                    val heightCol = it.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                    val dateAddedCol = it.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                    val dateModifiedCol = it.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                    val bucketCol = it.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                    while (it.moveToNext()) {
                        val id = it.getLong(idCol)
                        val rawName = if (displayNameCol != -1) it.getString(displayNameCol) else null
                        val rawTitle = if (titleCol != -1) it.getString(titleCol) else null
                        val durationMs = if (durationCol != -1) it.getLong(durationCol) else 0L
                        val sizeBytes = if (sizeCol != -1) it.getLong(sizeCol) else 0L
                        val width = if (widthCol != -1) it.getInt(widthCol) else 0
                        val height = if (heightCol != -1) it.getInt(heightCol) else 0
                        val dateAddedRaw = if (dateAddedCol != -1) it.getLong(dateAddedCol) else 0L
                        val dateModifiedRaw = if (dateModifiedCol != -1) it.getLong(dateModifiedCol) else 0L
                        val dateAdded = if (dateAddedRaw > 0L) dateAddedRaw else dateModifiedRaw
                        val folderName = if (bucketCol != -1) it.getString(bucketCol) ?: "Internal Storage" else "Internal Storage"

                        val fileName = rawName ?: rawTitle ?: "Video_$id"
                        val cleanTitle = cleanFileName(fileName)

                        // Extract year from filename if available, or fall back to dateAdded year
                        val matcher = yearPattern.matcher(fileName)
                        val year = if (matcher.find()) {
                            matcher.group(1)
                        } else if (dateAdded > 0) {
                            try {
                                sdfYear.format(Date(dateAdded * 1000L))
                            } catch (_: Exception) {
                                null
                            }
                        } else {
                            null
                        }

                        // Compute resolution badge (4K, 1080P, 720P, SD)
                        val maxDimension = maxOf(width, height)
                        val resolution = when {
                            maxDimension >= 3840 -> "4K"
                            maxDimension >= 1920 -> "1080P"
                            maxDimension >= 1280 -> "720P"
                            maxDimension > 0 -> "480P"
                            fileName.contains("4k", ignoreCase = true) || fileName.contains("2160p", ignoreCase = true) -> "4K"
                            fileName.contains("1080", ignoreCase = true) -> "1080P"
                            fileName.contains("720", ignoreCase = true) -> "720P"
                            else -> "HD"
                        }

                        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                        val savedProgress = (allProgressMap["progress_$id"] as? Long) ?: 0L

                        videoList.add(
                            VideoItem(
                                id = id.toString(),
                                title = cleanTitle,
                                durationText = formatDuration(durationMs),
                                durationMs = durationMs,
                                playbackProgressMs = savedProgress,
                                sizeText = formatFileSize(sizeBytes),
                                resolution = resolution,
                                year = year,
                                folderName = folderName,
                                uriString = uri.toString(),
                                dateAdded = dateAdded
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // Permission or content resolver issue handled gracefully
            }

            videoList
        }
    }

    fun extractFolders(videos: List<VideoItem>): List<VideoFolder> {
        return videos
            .groupBy { it.folderName }
            .map { (folderName, items) ->
                VideoFolder(
                    id = folderName,
                    name = folderName,
                    videoCount = items.size,
                    path = folderName,
                    videos = items
                )
            }
            .sortedByDescending { it.videoCount }
    }

    private fun cleanFileName(name: String): String {
        return name
            .substringBeforeLast(".") // Remove extension (.mp4, .mkv, etc.)
            .replace(".", " ")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
    }

    private fun formatDuration(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSeconds = ms / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes.toDouble() / (1024 * 1024)
        return if (mb >= 1024) {
            String.format(Locale.getDefault(), "%.1f GB", mb / 1024)
        } else {
            String.format(Locale.getDefault(), "%.1f MB", mb)
        }
    }
}
