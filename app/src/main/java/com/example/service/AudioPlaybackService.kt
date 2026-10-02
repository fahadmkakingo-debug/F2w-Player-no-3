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
import com.example.data.audio.AudioPlaybackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service managing continuous background audio playback
 * and persistent interactive media notifications in the notification drawer.
 */
class AudioPlaybackService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var isForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observePlaybackState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val audioManager = AudioPlaybackManager.getInstance(applicationContext)

        when (intent?.action) {
            ACTION_TOGGLE -> audioManager.togglePlayPause()
            ACTION_PLAY -> if (!audioManager.isPlaying.value) audioManager.togglePlayPause()
            ACTION_PAUSE -> if (audioManager.isPlaying.value) audioManager.togglePlayPause()
            ACTION_NEXT -> audioManager.playNext()
            ACTION_PREV -> audioManager.playPrevious()
            ACTION_STOP -> {
                audioManager.dismissMiniPlayer()
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
        val audioManager = AudioPlaybackManager.getInstance(applicationContext)

        serviceScope.launch {
            audioManager.currentTrack.collectLatest {
                updateNotificationState()
            }
        }

        serviceScope.launch {
            audioManager.isPlaying.collectLatest {
                updateNotificationState()
            }
        }

        serviceScope.launch {
            audioManager.isMiniPlayerVisible.collectLatest { isVisible ->
                if (!isVisible) {
                    stopForegroundInternal()
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(NOTIFICATION_ID)
                    stopSelf()
                } else {
                    updateNotificationState()
                }
            }
        }

        serviceScope.launch {
            audioManager.currentCoverBitmap.collectLatest {
                updateNotificationState()
            }
        }
    }

    private fun updateNotificationState() {
        val audioManager = AudioPlaybackManager.getInstance(applicationContext)
        val track = audioManager.currentTrack.value

        if (track == null || !audioManager.isMiniPlayerVisible.value) {
            stopForegroundInternal()
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
            stopSelf()
            return
        }

        val isPlaying = audioManager.isPlaying.value
        val coverBmp = audioManager.currentCoverBitmap.value
        val notification = buildNotification(track.title, track.subtitle, isPlaying, coverBmp)

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
        subtitle: String,
        isPlaying: Boolean,
        coverBitmap: Bitmap?
    ): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Previous (Kulidisha nyuma)
        val pPrev = PendingIntent.getService(
            this,
            1,
            Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Play / Pause (Pausi / Cheza)
        val pToggle = PendingIntent.getService(
            this,
            2,
            Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Next
        val pNext = PendingIntent.getService(
            this,
            3,
            Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 4: Close / Stop (Kuikata kabisa nyimbo isiendelee)
        val pStop = PendingIntent.getService(
            this,
            4,
            Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_STOP },
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
            .setContentText(subtitle.ifBlank { "F2W Audio Player" })
            .setContentIntent(pOpen)
            .setDeleteIntent(pStop)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            // Explicit user actions (icons only without text clutter)
            .addAction(android.R.drawable.ic_media_previous, "Previous", pPrev) // Index 0
            .addAction(playPauseIcon, playPauseText, pToggle) // Index 1
            .addAction(android.R.drawable.ic_media_next, "Next", pNext) // Index 2
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pStop) // Index 3
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )

        if (coverBitmap != null) {
            builder.setLargeIcon(coverBitmap)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "F2W Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background music controls for F2W Player"
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
        const val CHANNEL_ID = "f2w_audio_playback_channel"
        const val NOTIFICATION_ID = 4481

        const val ACTION_START = "com.example.action.START"
        const val ACTION_TOGGLE = "com.example.action.TOGGLE"
        const val ACTION_PLAY = "com.example.action.PLAY"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_PREV = "com.example.action.PREV"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_UPDATE = "com.example.action.UPDATE"

        fun start(context: Context) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
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

        fun stop(context: Context) {
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(NOTIFICATION_ID)
                context.stopService(Intent(context, AudioPlaybackService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
