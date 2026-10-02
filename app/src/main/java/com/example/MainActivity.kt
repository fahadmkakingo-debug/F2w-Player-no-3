package com.example
 
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.video.VideoPlaybackManager
import com.example.service.VideoPlaybackService
import com.example.ui.MainScreen
import com.example.ui.theme.F2WPlayerTheme
 
val LocalPipState = compositionLocalOf { false }
 
class MainActivity : ComponentActivity() {
    private var isPipActive by mutableStateOf(false)
    var isVideoPlayingActive: Boolean = false
    var shouldAutoEnterPip: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.util.media.VideoThumbnailHelper.initialize(this)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            F2WPlayerTheme {
                MainScreen(
                    isInPipMode = isPipActive,
                    onRequestPip = { enterPipMode() },
                    onToggleOrientation = { toggleOrientation() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            VideoPlaybackService.ACTION_OPEN_FULLSCREEN -> {
                VideoPlaybackManager.getInstance(applicationContext).openFullScreen()
            }
            Intent.ACTION_VIEW -> {
                handleViewIntent(intent)
            }
        }
    }

    private fun handleViewIntent(intent: Intent) {
        val uri = intent.data ?: return
        try {
            var displayName: String? = null
            var sizeBytes = 0L

            if (uri.scheme == "content") {
                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIndex != -1) displayName = cursor.getString(nameIndex)
                            if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                        }
                    }
                } catch (_: Exception) {}
            }

            if (displayName.isNullOrBlank()) {
                val pathSegment = uri.lastPathSegment ?: ""
                displayName = pathSegment.substringAfterLast("/").ifBlank { "External Video" }
            }

            var durationMs = 0L
            var width = 0
            var height = 0
            try {
                val retriever = android.media.MediaMetadataRetriever()
                if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                    val file = java.io.File(uri.path!!)
                    if (file.exists()) {
                        val fis = java.io.FileInputStream(file)
                        try {
                            retriever.setDataSource(fis.fd)
                        } finally {
                            fis.close()
                        }
                    } else {
                        retriever.setDataSource(applicationContext, uri)
                    }
                } else if (uri.scheme == "content") {
                    val pfd = contentResolver.openFileDescriptor(uri, "r")
                    if (pfd != null) {
                        try {
                            retriever.setDataSource(pfd.fileDescriptor)
                        } finally {
                            pfd.close()
                        }
                    } else {
                        retriever.setDataSource(applicationContext, uri)
                    }
                } else {
                    retriever.setDataSource(applicationContext, uri)
                }

                val durStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durStr?.toLongOrNull() ?: 0L
                val wStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val hStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                width = wStr?.toIntOrNull() ?: 0
                height = hStr?.toIntOrNull() ?: 0
                retriever.release()
            } catch (_: Exception) {}

            val maxDim = maxOf(width, height)
            val resolution = when {
                maxDim >= 3840 -> "4K"
                maxDim >= 1920 -> "1080P"
                maxDim >= 1280 -> "720P"
                maxDim > 0 -> "480P"
                displayName.contains("4k", ignoreCase = true) || displayName.contains("2160p", ignoreCase = true) -> "4K"
                displayName.contains("1080", ignoreCase = true) -> "1080P"
                displayName.contains("720", ignoreCase = true) -> "720P"
                else -> "HD"
            }

            val durationSeconds = durationMs / 1000
            val durationText = if (durationSeconds >= 3600) {
                String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", durationSeconds / 3600, (durationSeconds % 3600) / 60, durationSeconds % 60)
            } else {
                String.format(java.util.Locale.getDefault(), "%02d:%02d", durationSeconds / 60, durationSeconds % 60)
            }

            val sizeText = if (sizeBytes > 0) {
                val mb = sizeBytes.toDouble() / (1024 * 1024)
                if (mb >= 1024) String.format(java.util.Locale.getDefault(), "%.1f GB", mb / 1024)
                else String.format(java.util.Locale.getDefault(), "%.1f MB", mb)
            } else ""

            val videoItem = com.example.ui.screens.video.VideoItem(
                id = "ext_${uri.toString().hashCode()}",
                title = displayName,
                durationText = durationText,
                durationMs = durationMs,
                playbackProgressMs = 0L,
                sizeText = sizeText,
                resolution = resolution,
                year = null,
                folderName = "External",
                uriString = uri.toString(),
                dateAdded = System.currentTimeMillis() / 1000L,
                lastPlayedTime = System.currentTimeMillis()
            )

            VideoPlaybackManager.getInstance(applicationContext).playVideo(videoItem)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
        VideoPlaybackManager.getInstance(applicationContext).onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        VideoPlaybackManager.getInstance(applicationContext).onAppBackgrounded()
    }

    fun enterPipMode(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                return enterPictureInPictureMode(params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    private fun toggleOrientation() {
        requestedOrientation = if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPipActive = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isVideoPlayingActive && shouldAutoEnterPip && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPipMode()
        }
    }
}

