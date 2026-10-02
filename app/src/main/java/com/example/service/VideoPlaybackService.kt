package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.video.VideoPlaybackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service managing background video audio playback
 * and persistent interactive media notifications in the notification drawer.
 */
class VideoPlaybackService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var isForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observePlaybackState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val videoManager = VideoPlaybackManager.getInstance(applicationContext)

        when (intent?.action) {
            ACTION_TOGGLE -> videoManager.togglePlayPause()
            ACTION_PLAY -> if (!videoManager.isPlaying.value) videoManager.togglePlayPause()
            ACTION_PAUSE -> if (videoManager.isPlaying.value) videoManager.togglePlayPause()
            ACTION_NEXT -> videoManager.playNext()
            ACTION_PREV -> videoManager.playPrevious()
            ACTION_STOP -> {
                videoManager.stopPlayback()
                stopForegroundInternal()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, ACTION_UPDATE -> {
                updateNotificationState()
            }
        }

        updateNotificationState()
        return START_STICKY
    }

    private fun observePlaybackState() {
        val videoManager = VideoPlaybackManager.getInstance(applicationContext)

        serviceScope.launch {
            videoManager.currentVideo.collectLatest {
                updateNotificationState()
            }
        }

        serviceScope.launch {
            videoManager.isPlaying.collectLatest {
                updateNotificationState()
            }
        }

        serviceScope.launch {
            videoManager.currentThumbnailBitmap.collectLatest {
                updateNotificationState()
            }
        }
    }

    private fun updateNotificationState() {
        val videoManager = VideoPlaybackManager.getInstance(applicationContext)
        val video = videoManager.currentVideo.value

        if (video == null) {
            stopForegroundInternal()
            stopSelf()
            return
        }

        val isPlaying = videoManager.isPlaying.value
        val thumbnailBmp = videoManager.currentThumbnailBitmap.value
        val notification = buildNotification(video.title, isPlaying, thumbnailBmp)

        if (!isForeground) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                isForeground = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundInternal() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            isForeground = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildNotification(
        title: String,
        isPlaying: Boolean,
        thumbnailBitmap: Bitmap?
    ): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_FULLSCREEN
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pOpen = PendingIntent.getActivity(
            this,
            10,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Previous (Kulidisha nyuma)
        val pPrev = PendingIntent.getService(
            this,
            11,
            Intent(this, VideoPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Play / Pause (Pausi / Cheza)
        val pToggle = PendingIntent.getService(
            this,
            12,
            Intent(this, VideoPlaybackService::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Next
        val pNext = PendingIntent.getService(
            this,
            13,
            Intent(this, VideoPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 4: Close / Stop (Kuikata kabisa video isiendelee)
        val pStop = PendingIntent.getService(
            this,
            14,
            Intent(this, VideoPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseText = if (isPlaying) "Pause" else "Play"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText("F2W Video • Background Play")
            .setContentIntent(pOpen)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .addAction(android.R.drawable.ic_media_previous, "Previous", pPrev)
            .addAction(playPauseIcon, playPauseText, pToggle)
            .addAction(android.R.drawable.ic_media_next, "Next", pNext)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pStop)

        if (thumbnailBitmap != null) {
            builder.setLargeIcon(thumbnailBitmap)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "F2W Video Background Play",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background video controls for F2W Player"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopForegroundInternal()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "f2w_video_playback_channel"
        const val NOTIFICATION_ID = 5592

        const val ACTION_START = "com.example.action.video.START"
        const val ACTION_TOGGLE = "com.example.action.video.TOGGLE"
        const val ACTION_PLAY = "com.example.action.video.PLAY"
        const val ACTION_PAUSE = "com.example.action.video.PAUSE"
        const val ACTION_PREV = "com.example.action.video.PREV"
        const val ACTION_NEXT = "com.example.action.video.NEXT"
        const val ACTION_STOP = "com.example.action.video.STOP"
        const val ACTION_UPDATE = "com.example.action.video.UPDATE"
        const val ACTION_OPEN_FULLSCREEN = "com.example.action.video.OPEN_FULLSCREEN"

        fun start(context: Context) {
            val intent = Intent(context, VideoPlaybackService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun update(context: Context) {
            val intent = Intent(context, VideoPlaybackService::class.java).apply {
                action = ACTION_UPDATE
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VideoPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
