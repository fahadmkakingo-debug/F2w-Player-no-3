package com.example.data.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import coil.Coil
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.data.audio.AudioPlaybackManager
import com.example.data.media.RecentlyPlayedManager
import com.example.data.settings.VideoSettingsPreferences
import com.example.service.VideoPlaybackService
import com.example.ui.screens.video.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VideoPlaybackManager private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var exoPlayer: ExoPlayer? = null

    // State flows
    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    private val _playlist = MutableStateFlow<List<VideoItem>>(emptyList())
    val playlist: StateFlow<List<VideoItem>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(1000L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow("Off") // "Off", "Repeat One", "Repeat All"
    val repeatMode: StateFlow<String> = _repeatMode.asStateFlow()

    private val _isBackgroundAudioEnabled = MutableStateFlow(false)
    val isBackgroundAudioEnabled: StateFlow<Boolean> = _isBackgroundAudioEnabled.asStateFlow()

    private val _isFullScreenOpen = MutableStateFlow(false)
    val isFullScreenOpen: StateFlow<Boolean> = _isFullScreenOpen.asStateFlow()

    private val _isMiniPlayerVisible = MutableStateFlow(false)
    val isMiniPlayerVisible: StateFlow<Boolean> = _isMiniPlayerVisible.asStateFlow()

    private val _currentThumbnailBitmap = MutableStateFlow<Bitmap?>(null)
    val currentThumbnailBitmap: StateFlow<Bitmap?> = _currentThumbnailBitmap.asStateFlow()

    // Subtitle Management States
    private val _isSubtitlesEnabled = MutableStateFlow(true)
    val isSubtitlesEnabled: StateFlow<Boolean> = _isSubtitlesEnabled.asStateFlow()

    private val _currentSubtitleName = MutableStateFlow("None")
    val currentSubtitleName: StateFlow<String> = _currentSubtitleName.asStateFlow()

    private val _availableSubtitles = MutableStateFlow<List<SubtitleTrackOption>>(emptyList())
    val availableSubtitles: StateFlow<List<SubtitleTrackOption>> = _availableSubtitles.asStateFlow()

    init {
        val prefs = VideoSettingsPreferences(appContext)
        if (prefs.rememberBackgroundPlay) {
            _isBackgroundAudioEnabled.value = prefs.isBackgroundPlayEnabled
        } else {
            _isBackgroundAudioEnabled.value = false
        }
    }

    fun releasePlayer() {
        val player = exoPlayer
        if (player != null) {
            try {
                player.clearVideoSurface()
                player.stop()
                player.clearMediaItems()
                player.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            exoPlayer = null
        }
    }

    fun getOrCreatePlayer(): ExoPlayer {
        if (exoPlayer == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            exoPlayer = ExoPlayer.Builder(appContext)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build().apply {
                    playWhenReady = true
                }
            setupPlayerListener(exoPlayer!!)
        }
        return exoPlayer!!
    }

    private fun setupPlayerListener(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    startProgressTracking()
                    // Stop/pause music audio playback to avoid overlapping sounds
                    AudioPlaybackManager.getInstance(appContext).pause()
                } else {
                    stopProgressTracking()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    _durationMs.value = player.duration.coerceAtLeast(1000L)
                    val sessionId = player.audioSessionId
                    if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != 0) {
                        com.example.data.audio.EqualizerManager.getInstance(appContext).bindAudioSession(sessionId)
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    handlePlaybackEnded()
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
                    com.example.data.audio.EqualizerManager.getInstance(appContext).bindAudioSession(audioSessionId)
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateAvailableSubtitles(tracks)
            }
        })
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _currentPositionMs.value = player.currentPosition
                    if (player.duration > 0) {
                        _durationMs.value = player.duration
                    }
                    val video = _currentVideo.value
                    if (video != null && player.currentPosition > 0L) {
                        RecentlyPlayedManager.getInstance(appContext).savePlaybackPosition(
                            video.id,
                            player.currentPosition
                        )
                    }
                }
                delay(400)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun handlePlaybackEnded() {
        val prefs = VideoSettingsPreferences(appContext)
        when (_repeatMode.value) {
            "Repeat One" -> {
                exoPlayer?.seekTo(0L)
                exoPlayer?.play()
            }
            "Repeat All" -> {
                playNext(forceLoop = true)
            }
            else -> {
                if (prefs.autoPlayNext) {
                    playNext(forceLoop = false)
                } else {
                    _isPlaying.value = false
                    if (_isBackgroundAudioEnabled.value) {
                        VideoPlaybackService.update(appContext)
                    }
                }
            }
        }
    }

    fun playVideo(
        video: VideoItem,
        newPlaylist: List<VideoItem> = listOf(video),
        startPositionMs: Long? = null
    ) {
        // Pause audio playback if currently active
        AudioPlaybackManager.getInstance(appContext).pause()

        _currentVideo.value = video
        _playlist.value = if (newPlaylist.isNotEmpty()) newPlaylist else listOf(video)
        _currentIndex.value = _playlist.value.indexOfFirst { it.id == video.id }.coerceAtLeast(0)

        // Release any existing player to ensure a pristine player instance and clean surface attachment
        releasePlayer()
        val player = getOrCreatePlayer()

        val playableUri = try {
            val uriStr = video.uriString
            if (uriStr.startsWith("content://") || uriStr.startsWith("file://") || uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                Uri.parse(uriStr)
            } else if (uriStr.startsWith("/")) {
                Uri.fromFile(java.io.File(uriStr))
            } else if (uriStr.isNotBlank()) {
                Uri.parse(uriStr)
            } else {
                Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
            }
        } catch (e: Exception) {
            Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
        }

        val mediaItemBuilder = MediaItem.Builder().setUri(playableUri)
        if (video.title.endsWith(".dd0", ignoreCase = true) ||
            video.uriString.endsWith(".dd0", ignoreCase = true) ||
            playableUri.path?.endsWith(".dd0", ignoreCase = true) == true
        ) {
            mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
        }
        val localSub = detectLocalSubtitle(video)
        if (localSub != null) {
            val subConfig = MediaItem.SubtitleConfiguration.Builder(localSub.first)
                .setMimeType(getSubtitleMimeType(localSub.second))
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .setLanguage("und")
                .setLabel(localSub.second)
                .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
            _currentSubtitleName.value = localSub.second
        } else {
            _currentSubtitleName.value = "None"
        }

        player.setMediaItem(mediaItemBuilder.build())
        player.prepare()

        val recentlyPlayed = RecentlyPlayedManager.getInstance(appContext)
        val resumePos = startPositionMs ?: recentlyPlayed.getPlaybackPosition(video.id).let { pos ->
            if (pos > 0L) pos else video.playbackProgressMs
        }

        if (resumePos > 2000L) {
            player.seekTo(resumePos)
            _currentPositionMs.value = resumePos
        } else {
            _currentPositionMs.value = 0L
        }

        recentlyPlayed.recordVideoPlayed(video.id, resumePos)
        player.play()
        _isPlaying.value = true
        _isFullScreenOpen.value = true
        _isMiniPlayerVisible.value = false

        loadThumbnailBitmap(video)
    }

    private fun loadThumbnailBitmap(video: VideoItem) {
        scope.launch(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(appContext)
                    .data(video.uriString)
                    .videoFrameMillis(1500L)
                    .size(256, 256)
                    .allowHardware(false) // Must be software bitmap for NotificationCompat
                    .build()
                val result = Coil.imageLoader(appContext).execute(request)
                val bmp = (result.drawable as? BitmapDrawable)?.bitmap
                _currentThumbnailBitmap.value = bmp
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        val validMs = positionMs.coerceIn(0L, _durationMs.value)
        exoPlayer?.seekTo(validMs)
        _currentPositionMs.value = validMs
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    fun setRepeatMode(mode: String) {
        _repeatMode.value = mode
    }

    fun playNext(forceLoop: Boolean = false) {
        val list = _playlist.value
        if (list.isEmpty()) return
        val nextIdx = _currentIndex.value + 1
        if (nextIdx < list.size) {
            playVideo(list[nextIdx], list, startPositionMs = 0L)
        } else if (forceLoop && list.isNotEmpty()) {
            playVideo(list[0], list, startPositionMs = 0L)
        }
    }

    fun playPrevious() {
        val list = _playlist.value
        if (list.isEmpty()) return
        val prevIdx = _currentIndex.value - 1
        if (prevIdx >= 0) {
            playVideo(list[prevIdx], list, startPositionMs = 0L)
        } else {
            // Seek to beginning of current
            seekTo(0L)
        }
    }

    fun toggleBackgroundAudio(): Boolean {
        val newVal = !_isBackgroundAudioEnabled.value
        setBackgroundAudioEnabled(newVal)
        return newVal
    }

    fun setBackgroundAudioEnabled(enabled: Boolean) {
        _isBackgroundAudioEnabled.value = enabled
        val prefs = VideoSettingsPreferences(appContext)
        if (prefs.rememberBackgroundPlay) {
            prefs.isBackgroundPlayEnabled = enabled
        }
    }

    fun openFullScreen() {
        _isFullScreenOpen.value = true
        _isMiniPlayerVisible.value = false
    }

    fun closeFullScreen() {
        _isFullScreenOpen.value = false
        stopPlayback()
    }

    fun dismissMiniPlayer() {
        stopPlayback()
    }

    fun stopPlayback() {
        val player = exoPlayer
        if (player != null) {
            val video = _currentVideo.value
            val pos = player.currentPosition
            if (video != null && pos > 0L) {
                RecentlyPlayedManager.getInstance(appContext).savePlaybackPosition(video.id, pos)
            }
        }
        releasePlayer()
        _currentVideo.value = null
        _isPlaying.value = false
        _isFullScreenOpen.value = false
        _isMiniPlayerVisible.value = false
        _currentThumbnailBitmap.value = null
        stopProgressTracking()
        VideoPlaybackService.stop(appContext)
    }

    /**
     * Called when the app moves to background (e.g. Home button pressed or app switched).
     */
    fun onAppBackgrounded() {
        if (_currentVideo.value != null && _isPlaying.value) {
            if (_isBackgroundAudioEnabled.value) {
                // Background play is ENABLED by user in settings:
                // Keep playing video audio and start foreground service with media notification!
                VideoPlaybackService.start(appContext)
            } else {
                // Background play is NOT enabled:
                // PAUSE IMMEDIATELY
                pause()
                VideoPlaybackService.stop(appContext)
            }
        }
    }

    /**
     * Called when the app returns to foreground.
     */
    fun onAppForegrounded() {
        if (_isFullScreenOpen.value) {
            // Dismiss notification when watching inside the app
            VideoPlaybackService.stop(appContext)
        }
    }

    // ------------------------------------------------------------------------
    // Subtitle Management & Selection
    // ------------------------------------------------------------------------

    private fun getSubtitleMimeType(pathOrUri: String): String {
        val lower = pathOrUri.lowercase()
        return when {
            lower.endsWith(".vtt") -> MimeTypes.TEXT_VTT
            lower.endsWith(".ass") || lower.endsWith(".ssa") -> MimeTypes.TEXT_SSA
            lower.endsWith(".ttml") || lower.endsWith(".xml") -> MimeTypes.APPLICATION_TTML
            else -> MimeTypes.APPLICATION_SUBRIP
        }
    }

    private fun detectLocalSubtitle(video: VideoItem): Pair<Uri, String>? {
        val uriStr = video.uriString
        try {
            val filePath = when {
                uriStr.startsWith("file://") -> uriStr.removePrefix("file://")
                uriStr.startsWith("/") -> uriStr
                uriStr.startsWith("content://") -> {
                    val proj = arrayOf(android.provider.MediaStore.Video.Media.DATA)
                    appContext.contentResolver.query(Uri.parse(uriStr), proj, null, null, null)?.use { cursor ->
                        val col = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.DATA)
                        if (col != -1 && cursor.moveToFirst()) cursor.getString(col) else null
                    }
                }
                else -> null
            }

            if (filePath != null) {
                val file = java.io.File(filePath)
                val parent = file.parentFile
                val baseName = file.nameWithoutExtension
                if (parent != null && parent.exists() && parent.isDirectory) {
                    val candidateExtensions = listOf(".srt", ".vtt", ".ass", ".ssa", ".en.srt", ".sw.srt")
                    for (ext in candidateExtensions) {
                        val candidate = java.io.File(parent, "$baseName$ext")
                        if (candidate.exists() && candidate.isFile && candidate.length() > 0) {
                            return Pair(Uri.fromFile(candidate), candidate.name)
                        }
                    }
                    val matchingFiles = parent.listFiles { f ->
                        f.isFile && (f.name.endsWith(".srt", true) || f.name.endsWith(".vtt", true)) &&
                                f.name.startsWith(baseName, true)
                    }
                    if (!matchingFiles.isNullOrEmpty()) {
                        val first = matchingFiles.first()
                        return Pair(Uri.fromFile(first), first.name)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun updateAvailableSubtitles(tracks: Tracks) {
        val list = mutableListOf<SubtitleTrackOption>()
        var foundSelectedName: String? = null

        for ((groupIndex, group) in tracks.groups.withIndex()) {
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val label = format.label
                        ?: format.language?.uppercase()
                        ?: "Track ${list.size + 1}"
                    val isSelected = group.isTrackSelected(i)
                    if (isSelected) {
                        foundSelectedName = label
                    }
                    list.add(
                        SubtitleTrackOption(
                            id = "track_${groupIndex}_$i",
                            name = label,
                            isSelected = isSelected,
                            groupIndex = groupIndex,
                            trackIndex = i,
                            isEmbedded = true
                        )
                    )
                }
            }
        }

        // Preserve external subtitles
        val externalSubs = _availableSubtitles.value.filter { !it.isEmbedded }
        val combined = externalSubs + list
        _availableSubtitles.value = combined

        if (!_isSubtitlesEnabled.value) {
            _currentSubtitleName.value = "None"
        } else if (foundSelectedName != null) {
            _currentSubtitleName.value = foundSelectedName
        } else if (combined.isNotEmpty()) {
            // Auto-select first subtitle track
            val first = combined.first()
            selectSubtitleTrack(first)
        } else {
            _currentSubtitleName.value = "None"
        }
    }

    fun addExternalSubtitle(subtitleUri: Uri, displayName: String) {
        val player = exoPlayer ?: return
        val currentVideoItem = _currentVideo.value ?: return
        val currentPos = player.currentPosition
        val wasPlaying = player.isPlaying

        val mimeType = getSubtitleMimeType(displayName.ifEmpty { subtitleUri.toString() })
        val subConfig = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
            .setMimeType(mimeType)
            .setLanguage("und")
            .setLabel(displayName)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val currentItem = player.currentMediaItem
        val existingConfigs = currentItem?.localConfiguration?.subtitleConfigurations ?: emptyList()
        val updatedConfigs = existingConfigs.filter { it.uri != subtitleUri } + subConfig

        val newMediaItem = if (currentItem != null) {
            currentItem.buildUpon()
                .setSubtitleConfigurations(updatedConfigs)
                .build()
        } else {
            MediaItem.Builder()
                .setUri(currentVideoItem.uriString)
                .setSubtitleConfigurations(updatedConfigs)
                .build()
        }

        player.setMediaItem(newMediaItem, currentPos)
        player.prepare()
        if (wasPlaying) {
            player.play()
        }

        _isSubtitlesEnabled.value = true
        _currentSubtitleName.value = displayName

        val newOption = SubtitleTrackOption(
            id = "ext_${System.currentTimeMillis()}",
            name = displayName,
            isSelected = true,
            isEmbedded = false,
            uri = subtitleUri
        )
        val updatedList = _availableSubtitles.value.toMutableList()
        updatedList.removeAll { it.name == displayName }
        updatedList.add(0, newOption)
        _availableSubtitles.value = updatedList

        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
    }

    fun setSubtitlesEnabled(enabled: Boolean) {
        _isSubtitlesEnabled.value = enabled
        exoPlayer?.let { player ->
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
                .build()
        }
        if (!enabled) {
            _currentSubtitleName.value = "None"
        } else {
            val selected = _availableSubtitles.value.firstOrNull { it.isSelected } ?: _availableSubtitles.value.firstOrNull()
            _currentSubtitleName.value = selected?.name ?: "Embedded Subtitle"
        }
    }

    fun selectSubtitleTrack(track: SubtitleTrackOption) {
        val player = exoPlayer ?: return
        _isSubtitlesEnabled.value = true
        _currentSubtitleName.value = track.name

        if (track.groupIndex >= 0 && track.trackIndex >= 0) {
            val groups = player.currentTracks.groups
            if (track.groupIndex < groups.size) {
                val group = groups[track.groupIndex]
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(
                        TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
                    )
                    .build()
            }
        } else if (track.uri != null) {
            addExternalSubtitle(track.uri, track.name)
        }

        _availableSubtitles.value = _availableSubtitles.value.map {
            it.copy(isSelected = (it.id == track.id))
        }
    }

    fun disableSubtitles() {
        setSubtitlesEnabled(false)
    }

    companion object {
        @Volatile
        private var instance: VideoPlaybackManager? = null

        fun getInstance(context: Context): VideoPlaybackManager {
            return instance ?: synchronized(this) {
                instance ?: VideoPlaybackManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

data class SubtitleTrackOption(
    val id: String,
    val name: String,
    val isSelected: Boolean = false,
    val groupIndex: Int = -1,
    val trackIndex: Int = -1,
    val isEmbedded: Boolean = true,
    val uri: Uri? = null
)
