package com.example.data.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.MainActivity
import com.example.data.playlist.PlaylistItemModel
import com.example.util.media.AudioCoverHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AudioRepeatMode {
    OFF, ALL, ONE
}

class AudioPlaybackManager private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var sleepCountDownTimer: CountDownTimer? = null

    private var exoPlayer: ExoPlayer? = null

    // State Flows
    private val _currentTrack = MutableStateFlow<PlaylistItemModel?>(null)
    val currentTrack: StateFlow<PlaylistItemModel?> = _currentTrack.asStateFlow()

    private val _currentCoverBitmap = MutableStateFlow<Bitmap?>(null)
    val currentCoverBitmap: StateFlow<Bitmap?> = _currentCoverBitmap.asStateFlow()

    private val _playlist = MutableStateFlow<List<PlaylistItemModel>>(emptyList())
    val playlist: StateFlow<List<PlaylistItemModel>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(1L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(AudioRepeatMode.ALL)
    val repeatMode: StateFlow<AudioRepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isMiniPlayerVisible = MutableStateFlow(false)
    val isMiniPlayerVisible: StateFlow<Boolean> = _isMiniPlayerVisible.asStateFlow()

    private val _isFullScreenOpen = MutableStateFlow(false)
    val isFullScreenOpen: StateFlow<Boolean> = _isFullScreenOpen.asStateFlow()

    private val _favoriteTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteTrackIds: StateFlow<Set<String>> = _favoriteTrackIds.asStateFlow()

    private val _sleepTimerRemainingMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingMinutes: StateFlow<Int?> = _sleepTimerRemainingMinutes.asStateFlow()

    init {
        initPlayer()
        loadFavorites()
    }

    private fun initPlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(appContext)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                        if (playing) {
                            startProgressUpdates()
                        } else {
                            stopProgressUpdates()
                        }
                        updateNotification()
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        _isBuffering.value = state == Player.STATE_BUFFERING
                        if (state == Player.STATE_READY) {
                            val dur = duration.coerceAtLeast(1L)
                            _durationMs.value = dur
                            _currentPositionMs.value = currentPosition
                            updateNotification()
                        } else if (state == Player.STATE_ENDED) {
                            handleTrackEnded()
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _isPlaying.value = false
                        _isBuffering.value = false
                    }
                })
            }
    }

    fun playPlaylist(list: List<PlaylistItemModel>, startIndex: Int = 0) {
        if (list.isEmpty()) return
        _playlist.value = list
        val validIndex = startIndex.coerceIn(0, list.lastIndex)
        _currentIndex.value = validIndex
        playTrackInternal(list[validIndex])
    }

    fun playSingle(track: PlaylistItemModel) {
        val existingIndex = _playlist.value.indexOfFirst { it.id == track.id }
        if (existingIndex != -1) {
            _currentIndex.value = existingIndex
            playTrackInternal(track)
        } else {
            val newList = listOf(track) + _playlist.value
            _playlist.value = newList
            _currentIndex.value = 0
            playTrackInternal(track)
        }
    }

    private fun playTrackInternal(track: PlaylistItemModel) {
        _currentTrack.value = track
        _isMiniPlayerVisible.value = true
        _currentPositionMs.value = 0L
        _durationMs.value = track.durationMs.coerceAtLeast(1000L)

        // Load album cover
        scope.launch {
            val bmp = AudioCoverHelper.getAudioCoverBitmap(appContext, track.uriString)
            _currentCoverBitmap.value = bmp
            updateNotification()
        }

        try {
            val mediaUri = Uri.parse(track.uriString)
            val mediaItem = MediaItem.fromUri(mediaUri)
            exoPlayer?.apply {
                setMediaItem(mediaItem)
                setPlaybackParameters(PlaybackParameters(_playbackSpeed.value))
                prepare()
                play()
            }
            _isPlaying.value = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_IDLE && _currentTrack.value != null) {
                    _currentTrack.value?.let { playTrackInternal(it) }
                } else {
                    player.play()
                }
            }
        }
    }

    fun playNext() {
        val list = _playlist.value
        if (list.isEmpty()) return

        val nextIndex = if (_isShuffle.value && list.size > 1) {
            var r = (0 until list.size).random()
            while (r == _currentIndex.value) {
                r = (0 until list.size).random()
            }
            r
        } else {
            (_currentIndex.value + 1) % list.size
        }
        _currentIndex.value = nextIndex
        playTrackInternal(list[nextIndex])
    }

    fun playPrevious() {
        val list = _playlist.value
        if (list.isEmpty()) return

        // If played more than 3 seconds, replay track
        if ((exoPlayer?.currentPosition ?: 0L) > 3000L) {
            seekTo(0L)
            return
        }

        val prevIndex = if (_currentIndex.value - 1 < 0) list.lastIndex else _currentIndex.value - 1
        _currentIndex.value = prevIndex
        playTrackInternal(list[prevIndex])
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _durationMs.value)
        _currentPositionMs.value = clamped
        exoPlayer?.seekTo(clamped)
    }

    fun seekBy(deltaMs: Long) {
        val current = exoPlayer?.currentPosition ?: _currentPositionMs.value
        val target = (current + deltaMs).coerceIn(0L, _durationMs.value)
        seekTo(target)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.setPlaybackParameters(PlaybackParameters(speed))
    }

    fun cyclePlaybackSpeed() {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val currentIndex = speeds.indexOfFirst { kotlin.math.abs(it - _playbackSpeed.value) < 0.05f }
        val nextIndex = if (currentIndex == -1) 2 else (currentIndex + 1) % speeds.size
        setPlaybackSpeed(speeds[nextIndex])
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        val next = when (_repeatMode.value) {
            AudioRepeatMode.ALL -> AudioRepeatMode.ONE
            AudioRepeatMode.ONE -> AudioRepeatMode.OFF
            AudioRepeatMode.OFF -> AudioRepeatMode.ALL
        }
        _repeatMode.value = next
        exoPlayer?.repeatMode = when (next) {
            AudioRepeatMode.ONE -> Player.REPEAT_MODE_ONE
            AudioRepeatMode.ALL -> Player.REPEAT_MODE_ALL
            AudioRepeatMode.OFF -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleFavorite(trackId: String) {
        val current = _favoriteTrackIds.value.toMutableSet()
        if (current.contains(trackId)) {
            current.remove(trackId)
        } else {
            current.add(trackId)
        }
        _favoriteTrackIds.value = current
        saveFavorites(current)
    }

    fun isFavorite(trackId: String): Boolean {
        return _favoriteTrackIds.value.contains(trackId)
    }

    private fun loadFavorites() {
        val prefs = appContext.getSharedPreferences("f2w_audio_prefs", Context.MODE_PRIVATE)
        val set = prefs.getStringSet("favorite_tracks", emptySet()) ?: emptySet()
        _favoriteTrackIds.value = set
    }

    private fun saveFavorites(set: Set<String>) {
        val prefs = appContext.getSharedPreferences("f2w_audio_prefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("favorite_tracks", set).apply()
    }

    fun setSleepTimer(minutes: Int) {
        cancelSleepTimer()
        _sleepTimerRemainingMinutes.value = minutes
        val totalMs = minutes * 60 * 1000L
        sleepCountDownTimer = object : CountDownTimer(totalMs, 60000L) {
            override fun onTick(millisUntilFinished: Long) {
                _sleepTimerRemainingMinutes.value = (millisUntilFinished / 60000L).toInt() + 1
            }

            override fun onFinish() {
                _sleepTimerRemainingMinutes.value = null
                exoPlayer?.pause()
                _isPlaying.value = false
            }
        }.start()
    }

    fun cancelSleepTimer() {
        sleepCountDownTimer?.cancel()
        sleepCountDownTimer = null
        _sleepTimerRemainingMinutes.value = null
    }

    fun openFullScreen() {
        _isFullScreenOpen.value = true
    }

    fun closeFullScreen() {
        _isFullScreenOpen.value = false
    }

    fun dismissMiniPlayer() {
        exoPlayer?.stop()
        _isPlaying.value = false
        _isMiniPlayerVisible.value = false
        _isFullScreenOpen.value = false
        clearNotification()
    }

    private fun handleTrackEnded() {
        when (_repeatMode.value) {
            AudioRepeatMode.ONE -> {
                seekTo(0L)
                exoPlayer?.play()
            }
            AudioRepeatMode.ALL -> {
                playNext()
            }
            AudioRepeatMode.OFF -> {
                if (_currentIndex.value < _playlist.value.lastIndex) {
                    playNext()
                } else {
                    _isPlaying.value = false
                }
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPositionMs.value = player.currentPosition
                        val dur = player.duration
                        if (dur > 0L) {
                            _durationMs.value = dur
                        }
                    }
                }
                delay(400)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    // Media Notification
    private val NOTIF_CHANNEL_ID = "f2w_audio_playback_channel"
    private val NOTIF_ID = 4481

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "F2W Audio Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for background music playback in F2W Player"
                setShowBadge(false)
            }
            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val track = _currentTrack.value ?: return
        createNotificationChannel()

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pOpen = PendingIntent.getActivity(
            appContext,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifBuilder = NotificationCompat.Builder(appContext, NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(track.title)
            .setContentText(track.subtitle.ifBlank { "F2W Audio Player" })
            .setContentIntent(pOpen)
            .setOngoing(_isPlaying.value)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val coverBmp = _currentCoverBitmap.value
        if (coverBmp != null) {
            notifBuilder.setLargeIcon(coverBmp)
        }

        try {
            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID, notifBuilder.build())
        } catch (_: Exception) {}
    }

    private fun clearNotification() {
        try {
            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIF_ID)
        } catch (_: Exception) {}
    }

    companion object {
        @Volatile
        private var instance: AudioPlaybackManager? = null

        fun getInstance(context: Context): AudioPlaybackManager {
            return instance ?: synchronized(this) {
                instance ?: AudioPlaybackManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
