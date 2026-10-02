package com.example.data.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.data.database.F2WDatabase
import com.example.data.database.VideoMediaEntity
import com.example.ui.screens.video.VideoFolder
import com.example.ui.screens.video.VideoItem
import com.example.util.permission.MediaPermissionManager
import com.example.util.permission.MediaPermissionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ScanProgressState(
    val isScanning: Boolean = false,
    val scannedCount: Int = 0,
    val totalCount: Int = 0,
    val statusMessage: String = "",
    val isPermissionDenied: Boolean = false,
    val isCancelled: Boolean = false
) {
    val progressFraction: Float
        get() = if (totalCount > 0) (scannedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
}

class LocalVideoScanner(private val context: Context) {
    private val appContext = context.applicationContext
    private val database = F2WDatabase.getInstance(appContext)
    private val videoDao = database.videoDao()

    private val progressPrefs =
        appContext.getSharedPreferences("f2w_playback_progress", Context.MODE_PRIVATE)

    private val scanScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val scanMutex = Mutex()
    private var activeScanJob: Job? = null

    private val _scanProgressState = MutableStateFlow(ScanProgressState())
    val scanProgressState: StateFlow<ScanProgressState> = _scanProgressState.asStateFlow()

    private val _allVideosState = MutableStateFlow<List<VideoItem>>(emptyList())
    val allVideosFlow: StateFlow<List<VideoItem>> = _allVideosState.asStateFlow()

    init {
        // Immediately load persistent Room database cache so UI displays instantly on app launch
        scanScope.launch {
            try {
                val cached = videoDao.getAllVideosSnapshot().map { it.toVideoItem() }
                if (cached.isNotEmpty()) {
                    _allVideosState.value = cached
                }
            } catch (_: Exception) {}

            // Reactively observe Room Flow for any additions, modifications, or deletions
            videoDao.getAllVideos().collect { entities ->
                val items = entities.map { it.toVideoItem() }
                if (items.isNotEmpty() || _allVideosState.value.isEmpty()) {
                    _allVideosState.value = items
                }
            }
        }
    }

    companion object {
        private val YEAR_PATTERN: Pattern = Pattern.compile("\\b(19\\d\\d|20\\d\\d)\\b")
        private val DATE_FORMAT_YEAR = SimpleDateFormat("yyyy", Locale.US)

        @Volatile
        private var INSTANCE: LocalVideoScanner? = null

        fun getInstance(context: Context): LocalVideoScanner {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalVideoScanner(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Triggers background synchronization with MediaStore.
     * If a sync is already running and not forceFullRescan, lets it complete.
     * Never clears the existing video library.
     */
    fun startScan(forceFullRescan: Boolean = false) {
        if (activeScanJob?.isActive == true && !forceFullRescan) {
            return
        }
        activeScanJob?.cancel()
        activeScanJob = scanScope.launch {
            scanDeviceVideosIncremental(forceFullRescan)
        }
    }

    /**
     * Cancels the currently active scan worker.
     */
    fun cancelScan() {
        activeScanJob?.cancel()
        _scanProgressState.value = ScanProgressState(
            isScanning = false,
            isCancelled = true,
            statusMessage = "Scan cancelled by user."
        )
    }

    /**
     * Primary background scanning method that:
     * 1. Checks permissions safely without freezing UI.
     * 2. Performs fast incremental diffing with Room Database.
     * 3. Retains existing cached videos while syncing in background.
     * 4. Only queries and updates changed/new videos.
     * 5. Removes only records that no longer exist on storage.
     */
    suspend fun scanDeviceVideosIncremental(forceFullRescan: Boolean = false): List<VideoItem> =
        withContext(Dispatchers.IO) {
            scanMutex.withLock {
                val hasAccess = MediaPermissionManager.hasMediaAccess(appContext, MediaPermissionType.VIDEO)
                if (!hasAccess) {
                    _scanProgressState.value = ScanProgressState(
                        isScanning = false,
                        isPermissionDenied = true,
                        statusMessage = "Storage permission required to scan videos."
                    )
                    return@withContext _allVideosState.value.ifEmpty {
                        videoDao.getAllVideosSnapshot().map { it.toVideoItem() }
                    }
                }

                _scanProgressState.value = ScanProgressState(
                    isScanning = true,
                    statusMessage = "Discovering video files..."
                )

                try {
                    // Step 1: Lightweight indexing of MediaStore IDs & modified timestamps
                    val lightProjection = arrayOf(
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DATE_MODIFIED,
                        MediaStore.Video.Media.SIZE
                    )

                    val mediaStoreMap = HashMap<String, Long>()
                    val mediaStoreIds = HashSet<String>()

                    val cursorLight = appContext.contentResolver.query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        lightProjection,
                        null,
                        null,
                        "${MediaStore.Video.Media.DATE_ADDED} DESC"
                    )

                    cursorLight?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                        val dateModCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)

                        while (cursor.moveToNext()) {
                            ensureActive()
                            val id = cursor.getLong(idCol).toString()
                            val dateMod = if (dateModCol != -1) cursor.getLong(dateModCol) else 0L
                            mediaStoreMap[id] = dateMod
                            mediaStoreIds.add(id)
                        }
                    }

                    val dbTimestamps = videoDao.getAllVideoTimestamps().associateBy { it.id }

                    // Step 2: Delete removed files from DB only if MediaStore query was valid
                    val removedIds = dbTimestamps.keys.filter { !mediaStoreIds.contains(it) }
                    if (removedIds.isNotEmpty()) {
                        videoDao.deleteVideosByIds(removedIds)
                    }

                    // Determine which items need full details fetched
                    val itemsToFetch = if (forceFullRescan) {
                        mediaStoreIds.toList()
                    } else {
                        mediaStoreMap.filter { (id, dateMod) ->
                            val cached = dbTimestamps[id]
                            cached == null || cached.dateModified != dateMod
                        }.keys.toList()
                    }

                    // If no additions or modifications, we are done!
                    if (itemsToFetch.isEmpty() && removedIds.isEmpty() && dbTimestamps.isNotEmpty()) {
                        _scanProgressState.value = ScanProgressState(
                            isScanning = false,
                            scannedCount = mediaStoreIds.size,
                            totalCount = mediaStoreIds.size,
                            statusMessage = "Media library is up to date (${mediaStoreIds.size} videos)."
                        )
                        return@withContext videoDao.getAllVideosSnapshot().map { it.toVideoItem() }
                    }

                    val totalToScan = mediaStoreIds.size
                    _scanProgressState.value = ScanProgressState(
                        isScanning = true,
                        scannedCount = 0,
                        totalCount = itemsToFetch.size,
                        statusMessage = "Reading video details..."
                    )

                    // Step 3: Fetch full details for new/modified videos
                    val fullProjection = arrayOf(
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

                    val allProgressMap = progressPrefs.all
                    val entitiesToInsert = mutableListOf<VideoMediaEntity>()
                    var processedCount = 0
                    val fetchSet = itemsToFetch.toSet()

                    // If only a small set of videos changed, query by selection for maximum speed
                    val chunks = if (itemsToFetch.size in 1..400) {
                        itemsToFetch.chunked(100)
                    } else {
                        listOf(null) // Query all and filter in cursor
                    }

                    for (chunk in chunks) {
                        ensureActive()
                        val selection = chunk?.let { ids ->
                            "${MediaStore.Video.Media._ID} IN (${ids.joinToString(",")})"
                        }

                        val fullCursor = appContext.contentResolver.query(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            fullProjection,
                            selection,
                            null,
                            "${MediaStore.Video.Media.DATE_ADDED} DESC"
                        )

                        fullCursor?.use { cursor ->
                            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                            val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                            val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                            val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                            val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                            val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                            val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                            val dateAddCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                            val dateModCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                            val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                            while (cursor.moveToNext()) {
                                ensureActive()
                                val idNum = cursor.getLong(idCol)
                                val id = idNum.toString()

                                if (selection == null && !fetchSet.contains(id)) {
                                    continue
                                }

                                val rawName = if (nameCol != -1) cursor.getString(nameCol) else null
                                val rawTitle = if (titleCol != -1) cursor.getString(titleCol) else null
                                val durationMs = if (durCol != -1) cursor.getLong(durCol) else 0L
                                val sizeBytes = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                                val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                                val height = if (heightCol != -1) cursor.getInt(heightCol) else 0
                                val dateAddedRaw = if (dateAddCol != -1) cursor.getLong(dateAddCol) else 0L
                                val dateModifiedRaw = if (dateModCol != -1) cursor.getLong(dateModCol) else 0L
                                val dateAdded = if (dateAddedRaw > 0L) dateAddedRaw else dateModifiedRaw
                                val folderName = if (bucketCol != -1) cursor.getString(bucketCol) ?: "Internal Storage" else "Internal Storage"

                                val fileName = rawName ?: rawTitle ?: "Video_$id"
                                val cleanTitle = cleanFileName(fileName)

                                // Year detection
                                val matcher = YEAR_PATTERN.matcher(fileName)
                                val year = if (matcher.find()) {
                                    matcher.group(1)
                                } else if (dateAdded > 0) {
                                    try {
                                        synchronized(DATE_FORMAT_YEAR) {
                                            DATE_FORMAT_YEAR.format(Date(dateAdded * 1000L))
                                        }
                                    } catch (_: Exception) {
                                        null
                                    }
                                } else {
                                    null
                                }

                                // Resolution badge (4K, 1080P, 720P, SD)
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

                                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, idNum)
                                val savedProgress = (allProgressMap["progress_$id"] as? Long) ?: 0L

                                entitiesToInsert.add(
                                    VideoMediaEntity(
                                        id = id,
                                        title = cleanTitle,
                                        durationText = formatDuration(durationMs),
                                        durationMs = durationMs,
                                        playbackProgressMs = savedProgress,
                                        sizeBytes = sizeBytes,
                                        sizeText = formatFileSize(sizeBytes),
                                        resolution = resolution,
                                        year = year,
                                        folderName = folderName,
                                        uriString = uri.toString(),
                                        dateAdded = dateAdded,
                                        dateModified = dateModifiedRaw
                                    )
                                )

                                if (entitiesToInsert.size >= 100) {
                                    videoDao.insertOrUpdateVideos(entitiesToInsert)
                                    processedCount += entitiesToInsert.size
                                    entitiesToInsert.clear()
                                    _scanProgressState.value = ScanProgressState(
                                        isScanning = true,
                                        scannedCount = processedCount,
                                        totalCount = itemsToFetch.size,
                                        statusMessage = "Scanned $processedCount of ${itemsToFetch.size} videos..."
                                    )
                                }
                            }
                        }
                    }

                    if (entitiesToInsert.isNotEmpty()) {
                        videoDao.insertOrUpdateVideos(entitiesToInsert)
                        processedCount += entitiesToInsert.size
                        entitiesToInsert.clear()
                    }

                    _scanProgressState.value = ScanProgressState(
                        isScanning = false,
                        scannedCount = totalToScan,
                        totalCount = totalToScan,
                        statusMessage = "Found $totalToScan videos."
                    )

                    val finalSnapshot = videoDao.getAllVideosSnapshot().map { it.toVideoItem() }
                    _allVideosState.value = finalSnapshot
                    return@withContext finalSnapshot
                } catch (e: CancellationException) {
                    _scanProgressState.value = ScanProgressState(
                        isScanning = false,
                        isCancelled = true,
                        statusMessage = "Scan cancelled."
                    )
                    throw e
                } catch (e: Exception) {
                    _scanProgressState.value = ScanProgressState(
                        isScanning = false,
                        statusMessage = "Scan completed with warnings: ${e.localizedMessage ?: "Unknown error"}"
                    )
                    return@withContext videoDao.getAllVideosSnapshot().map { it.toVideoItem() }
                }
            }
        }

    /**
     * Returns the persistent snapshot from database without forcing a full MediaStore rescan.
     */
    suspend fun scanDeviceVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val cached = videoDao.getAllVideosSnapshot().map { it.toVideoItem() }
        if (cached.isNotEmpty()) {
            cached
        } else {
            scanDeviceVideosIncremental(forceFullRescan = false)
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
                    videos = items.sortedWith { a, b ->
                        com.example.util.media.NaturalOrderComparator.compare(a.title, b.title)
                    }
                )
            }
            .sortedByDescending { it.videoCount }
    }

    suspend fun deleteVideoFromDb(id: String) = withContext(Dispatchers.IO) {
        videoDao.deleteVideoById(id)
    }

    suspend fun updateVideoTitle(id: String, newTitle: String) = withContext(Dispatchers.IO) {
        videoDao.updateVideoTitle(id, newTitle)
    }

    suspend fun updateFavorite(id: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        videoDao.updateFavorite(id, isFavorite)
    }

    suspend fun updatePlaybackProgress(id: String, progressMs: Long) = withContext(Dispatchers.IO) {
        videoDao.updatePlaybackProgress(id, progressMs)
        progressPrefs.edit().putLong("progress_$id", progressMs).apply()
    }

    private fun cleanFileName(name: String): String {
        return name
            .substringBeforeLast(".")
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
