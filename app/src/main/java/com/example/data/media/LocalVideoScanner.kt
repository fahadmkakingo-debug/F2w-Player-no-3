package com.example.data.media

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.data.database.F2WDatabase
import com.example.data.database.VideoMediaEntity
import com.example.data.security.PrivacyVaultManager
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
import java.io.File
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
        val vaultManager = PrivacyVaultManager.getInstance(appContext)

        // Immediately load persistent Room database cache filtered against Privacy Vault
        scanScope.launch {
            try {
                val cached = videoDao.getAllVideosSnapshot().map { it.toVideoItem() }.filter {
                    !vaultManager.isPathOrUriInVault(it.uriString, it.id) && !vaultManager.isPathOrUriInVault(it.title)
                }
                if (cached.isNotEmpty()) {
                    _allVideosState.value = cached
                }
            } catch (_: Exception) {}

            // Reactively observe Room Flow for any additions, modifications, or deletions
            videoDao.getAllVideos().collect { entities ->
                val items = entities.map { it.toVideoItem() }.filter {
                    !vaultManager.isPathOrUriInVault(it.uriString, it.id) && !vaultManager.isPathOrUriInVault(it.title)
                }
                _allVideosState.value = items
            }
        }

        // Listen for Privacy Vault updates to immediately purge vault items from state
        scanScope.launch {
            vaultManager.vaultUpdates.collect {
                try {
                    val filtered = _allVideosState.value.filter {
                        !vaultManager.isPathOrUriInVault(it.uriString, it.id) && !vaultManager.isPathOrUriInVault(it.title)
                    }
                    _allVideosState.value = filtered
                    startScan(forceFullRescan = true)
                } catch (_: Exception) {}
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
     * Triggers background synchronization with MediaStore and custom .dd0 files.
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
     * 2. Scans both standard MediaStore videos and custom .dd0 video files.
     * 3. Performs fast incremental diffing with Room Database.
     * 4. Retains existing cached videos while syncing in background.
     * 5. Preserves .dd0 file extension in the displayed title.
     * 6. Removes only records that no longer exist on storage.
     */
    suspend fun scanDeviceVideosIncremental(forceFullRescan: Boolean = false): List<VideoItem> =
        withContext(Dispatchers.IO) {
            scanMutex.withLock {
                val vaultManager = PrivacyVaultManager.getInstance(appContext)
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
                    statusMessage = "Discovering video files and .dd0 media..."
                )

                try {
                    // Step 1: Lightweight indexing of standard MediaStore video files
                    val lightProjection = arrayOf(
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DATE_MODIFIED,
                        MediaStore.Video.Media.SIZE
                    )

                    val mediaStoreMap = HashMap<String, Long>()
                    val validMediaIds = HashSet<String>()

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
                            validMediaIds.add(id)
                        }
                    }

                    // Step 1b: Discover custom .dd0 video files from device storage and MediaStore Files
                    val discoveredDd0Files = scanStorageAndMediaStoreForDd0Files()
                    val dd0FilesToProcess = mutableListOf<File>()

                    for (file in discoveredDd0Files) {
                        ensureActive()
                        val dd0Id = getStableIdForDd0File(file)
                        val lastMod = file.lastModified() / 1000L
                        validMediaIds.add(dd0Id)
                        mediaStoreMap[dd0Id] = lastMod
                    }

                    val dbTimestamps = videoDao.getAllVideoTimestamps().associateBy { it.id }

                    // Step 2: Delete removed files from DB only if discovery was valid
                    val removedIds = dbTimestamps.keys.filter { !validMediaIds.contains(it) }
                    if (removedIds.isNotEmpty()) {
                        videoDao.deleteVideosByIds(removedIds)
                    }

                    // Determine which MediaStore items need full details fetched
                    val standardItemsToFetch = if (forceFullRescan) {
                        mediaStoreMap.keys.filter { !it.startsWith("dd0_") }
                    } else {
                        mediaStoreMap.filter { (id, dateMod) ->
                            if (id.startsWith("dd0_")) return@filter false
                            val cached = dbTimestamps[id]
                            cached == null || cached.dateModified != dateMod
                        }.keys.toList()
                    }

                    // Determine which .dd0 files need metadata extraction/update
                    for (file in discoveredDd0Files) {
                        val dd0Id = getStableIdForDd0File(file)
                        val lastMod = file.lastModified() / 1000L
                        val cached = dbTimestamps[dd0Id]
                        if (forceFullRescan || cached == null || cached.dateModified != lastMod) {
                            dd0FilesToProcess.add(file)
                        }
                    }

                    val totalChanges = standardItemsToFetch.size + dd0FilesToProcess.size

                    // If no additions or modifications and nothing removed, we are done!
                    if (totalChanges == 0 && removedIds.isEmpty() && dbTimestamps.isNotEmpty()) {
                        val upToDateSnapshot = videoDao.getAllVideosSnapshot().map { it.toVideoItem() }.filter {
                            !vaultManager.isPathOrUriInVault(it.uriString, it.id) && !vaultManager.isPathOrUriInVault(it.title)
                        }
                        _allVideosState.value = upToDateSnapshot
                        _scanProgressState.value = ScanProgressState(
                            isScanning = false,
                            scannedCount = upToDateSnapshot.size,
                            totalCount = upToDateSnapshot.size,
                            statusMessage = "Media library is up to date (${upToDateSnapshot.size} videos)."
                        )
                        return@withContext upToDateSnapshot
                    }

                    val totalToScan = validMediaIds.size
                    _scanProgressState.value = ScanProgressState(
                        isScanning = true,
                        scannedCount = 0,
                        totalCount = totalChanges,
                        statusMessage = "Reading video details and .dd0 files..."
                    )

                    val allProgressMap = progressPrefs.all
                    val entitiesToInsert = mutableListOf<VideoMediaEntity>()
                    var processedCount = 0

                    // Step 3: Fetch full details for new/modified standard MediaStore videos
                    if (standardItemsToFetch.isNotEmpty()) {
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

                        val fetchSet = standardItemsToFetch.toSet()
                        val chunks = if (standardItemsToFetch.size in 1..400) {
                            standardItemsToFetch.chunked(100)
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
                                    val cleanTitle = if (fileName.endsWith(".dd0", ignoreCase = true)) {
                                        fileName
                                    } else {
                                        cleanFileName(fileName)
                                    }

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
                                            totalCount = totalChanges,
                                            statusMessage = "Scanned $processedCount of $totalChanges videos..."
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Step 4: Process custom .dd0 video files
                    for (dd0File in dd0FilesToProcess) {
                        ensureActive()
                        val entity = createEntityForDd0File(dd0File, allProgressMap)
                        if (entity != null) {
                            entitiesToInsert.add(entity)
                        }

                        if (entitiesToInsert.size >= 100) {
                            videoDao.insertOrUpdateVideos(entitiesToInsert)
                            processedCount += entitiesToInsert.size
                            entitiesToInsert.clear()
                            _scanProgressState.value = ScanProgressState(
                                isScanning = true,
                                scannedCount = processedCount,
                                totalCount = totalChanges,
                                statusMessage = "Scanned $processedCount of $totalChanges videos..."
                            )
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
                        statusMessage = "Found $totalToScan videos (including .dd0 files)."
                    )

                    val finalSnapshot = videoDao.getAllVideosSnapshot().map { it.toVideoItem() }.filter {
                        !vaultManager.isPathOrUriInVault(it.uriString, it.id) && !vaultManager.isPathOrUriInVault(it.title)
                    }
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
                    val fallbackSnapshot = videoDao.getAllVideosSnapshot().map { it.toVideoItem() }.filter {
                        !vaultManager.isPathOrUriInVault(it.uriString, it.id) && !vaultManager.isPathOrUriInVault(it.title)
                    }
                    _allVideosState.value = fallbackSnapshot
                    return@withContext fallbackSnapshot
                }
            }
        }

    /**
     * Discovers all .dd0 video files from device storage directories and MediaStore.Files.
     */
    private fun scanStorageAndMediaStoreForDd0Files(): List<File> {
        val discoveredFiles = LinkedHashMap<String, File>()

        // 1. Query Android MediaStore.Files for any file ending in .dd0 or .DD0
        try {
            val filesUri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE
            )
            val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.dd0' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.DD0' OR ${MediaStore.Files.FileColumns.DATA} LIKE '%.dd0' OR ${MediaStore.Files.FileColumns.DATA} LIKE '%.DD0'"

            val cursor = appContext.contentResolver.query(
                filesUri,
                projection,
                selection,
                null,
                null
            )

            cursor?.use { c ->
                val dataCol = c.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val sizeCol = c.getColumnIndex(MediaStore.Files.FileColumns.SIZE)

                while (c.moveToNext()) {
                    val path = if (dataCol != -1) c.getString(dataCol) else null
                    val size = if (sizeCol != -1) c.getLong(sizeCol) else 0L
                    if (!path.isNullOrBlank() && (size > 0L || File(path).length() > 0L)) {
                        val file = File(path)
                        if (file.exists() && file.isFile && file.name.endsWith(".dd0", ignoreCase = true)) {
                            val canonical = try { file.canonicalPath } catch (_: Exception) { file.absolutePath }
                            discoveredFiles[canonical] = file
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Direct fast targeted scan of public media directories (max depth 2, max 80 dirs, max 1000ms timeout)
        try {
            val searchRoots = LinkedHashSet<File>()
            listOf(
                Environment.DIRECTORY_MOVIES,
                Environment.DIRECTORY_DOWNLOADS,
                Environment.DIRECTORY_DCIM,
                Environment.DIRECTORY_DOCUMENTS,
                Environment.DIRECTORY_PICTURES
            ).forEach { dirType ->
                try {
                    Environment.getExternalStoragePublicDirectory(dirType)?.let {
                        if (it.exists() && it.isDirectory) searchRoots.add(it)
                    }
                } catch (_: Exception) {}
            }

            val startTime = System.currentTimeMillis()
            var dirCount = 0
            val visitedDirs = HashSet<String>()

            fun walk(dir: File, depth: Int) {
                if (depth > 2 || dirCount > 80 || System.currentTimeMillis() - startTime > 1000) return
                if (!dir.exists() || !dir.isDirectory) return

                val canonical = try { dir.canonicalPath } catch (_: Exception) { dir.absolutePath }
                if (visitedDirs.contains(canonical)) return
                visitedDirs.add(canonical)
                dirCount++

                val dirName = dir.name
                if (dirName.startsWith(".") || dirName.equals("lost.dir", ignoreCase = true)) return

                val pathLower = dir.absolutePath.lowercase()
                if (pathLower.contains("/android/data") || pathLower.contains("/android/obb") || pathLower.contains("privatevault")) return

                val children = try { dir.listFiles() } catch (_: Exception) { null } ?: return
                for (child in children) {
                    if (child.isDirectory) {
                        walk(child, depth + 1)
                    } else if (child.isFile && child.name.endsWith(".dd0", ignoreCase = true) && child.length() > 0) {
                        val childCanonical = try { child.canonicalPath } catch (_: Exception) { child.absolutePath }
                        discoveredFiles[childCanonical] = child
                    }
                }
            }

            for (root in searchRoots) {
                if (System.currentTimeMillis() - startTime > 1000) break
                walk(root, 0)
            }
        } catch (_: Exception) {}

        return discoveredFiles.values.toList()
    }

    private fun getStableIdForDd0File(file: File): String {
        val canonical = try { file.canonicalPath } catch (_: Exception) { file.absolutePath }
        val hash = canonical.hashCode()
        return if (hash < 0) "dd0_n${-hash}" else "dd0_$hash"
    }

    private fun createEntityForDd0File(file: File, allProgressMap: Map<String, *>): VideoMediaEntity? {
        if (!file.exists() || !file.isFile || file.length() <= 0L) return null

        val id = getStableIdForDd0File(file)
        val fileName = file.name // Keep exact filename with .dd0 extension, e.g. "mama.dd0"
        val folderName = file.parentFile?.name ?: "Internal Storage"
        val sizeBytes = file.length()
        val sizeText = formatFileSize(sizeBytes)
        val lastMod = file.lastModified() / 1000L
        val uriString = Uri.fromFile(file).toString()

        var durationMs = 0L
        var width = 0
        var height = 0

        try {
            val retriever = MediaMetadataRetriever()
            val fis = java.io.FileInputStream(file)
            try {
                retriever.setDataSource(fis.fd)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durStr?.toLongOrNull() ?: 0L
                val wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                width = wStr?.toIntOrNull() ?: 0
                height = hStr?.toIntOrNull() ?: 0
            } finally {
                fis.close()
            }
            retriever.release()
        } catch (_: Exception) {}

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

        val matcher = YEAR_PATTERN.matcher(fileName)
        val year = if (matcher.find()) {
            matcher.group(1)
        } else if (lastMod > 0) {
            try {
                synchronized(DATE_FORMAT_YEAR) {
                    DATE_FORMAT_YEAR.format(Date(file.lastModified()))
                }
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        val savedProgress = (allProgressMap["progress_$id"] as? Long) ?: 0L

        return VideoMediaEntity(
            id = id,
            title = fileName, // Preserves the exact name e.g. mama.dd0
            durationText = formatDuration(durationMs),
            durationMs = durationMs,
            playbackProgressMs = savedProgress,
            sizeBytes = sizeBytes,
            sizeText = sizeText,
            resolution = resolution,
            year = year,
            folderName = folderName,
            uriString = uriString,
            dateAdded = lastMod,
            dateModified = lastMod
        )
    }

    /**
     * Returns the persistent snapshot from database without forcing a full MediaStore rescan.
     */
    suspend fun scanDeviceVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val vaultManager = PrivacyVaultManager.getInstance(appContext)
        val cached = videoDao.getAllVideosSnapshot().map { it.toVideoItem() }.filter {
            !vaultManager.isPathOrUriInVault(it.uriString, it.id) && !vaultManager.isPathOrUriInVault(it.title)
        }
        if (cached.isNotEmpty()) {
            _allVideosState.value = cached
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
        if (name.endsWith(".dd0", ignoreCase = true)) {
            return name
        }
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
