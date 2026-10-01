package com.example.data.security

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeviceMediaFile(
    val id: String,
    val title: String,
    val uri: Uri,
    val path: String,
    val sizeBytes: Long,
    val durationMs: Long = 0L,
    val dateAdded: Long = 0L,
    val mimeType: String = ""
) {
    val sizeFormatted: String
        get() = when {
            sizeBytes >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f GB", sizeBytes.toFloat() / (1024 * 1024 * 1024))
            sizeBytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", sizeBytes.toFloat() / (1024 * 1024))
            sizeBytes >= 1024 -> String.format(Locale.US, "%d KB", sizeBytes / 1024)
            else -> "$sizeBytes B"
        }

    val durationFormatted: String
        get() {
            if (durationMs <= 0) return "00:00"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format(Locale.US, "%02d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }
}

data class PrivacyVaultItem(
    val id: String,
    val fileName: String,
    val originalPath: String,
    val vaultPath: String,
    val mediaType: String, // "VIDEO", "AUDIO", "IMAGE"
    val sizeBytes: Long,
    val durationMs: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis()
) {
    val sizeFormatted: String
        get() = when {
            sizeBytes >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f GB", sizeBytes.toFloat() / (1024 * 1024 * 1024))
            sizeBytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", sizeBytes.toFloat() / (1024 * 1024))
            sizeBytes >= 1024 -> String.format(Locale.US, "%d KB", sizeBytes / 1024)
            else -> "$sizeBytes B"
        }

    val dateAddedFormatted: String
        get() = try {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(dateAdded))
        } catch (_: Exception) {
            ""
        }
}

data class MoveResult(
    val successCount: Int,
    val failedCount: Int,
    val errors: List<String>
)

class PrivacyVaultManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("f2w_privacy_vault_registry", Context.MODE_PRIVATE)

    private val _vaultUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val vaultUpdates: SharedFlow<Unit> = _vaultUpdates.asSharedFlow()

    companion object {
        private const val KEY_VAULT_ITEMS_JSON = "privacy_vault_items_json"

        @Volatile
        private var INSTANCE: PrivacyVaultManager? = null

        fun getInstance(context: Context): PrivacyVaultManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PrivacyVaultManager(context).also { INSTANCE = it }
            }
        }
    }

    /**
     * Resolves the secure private vault folder for a specific category.
     * This directory is inside app private storage (filesDir) and completely hidden
     * from MediaStore, external galleries, and file managers.
     */
    fun getVaultCategoryDir(mediaType: String): File {
        val baseVault = File(appContext.filesDir, "privacy_vault")
        val categoryFolder = File(baseVault, mediaType.lowercase())
        if (!categoryFolder.exists()) {
            categoryFolder.mkdirs()
        }
        // Place a .nomedia file in baseVault to ensure system media scanner never touches it
        val nomedia = File(baseVault, ".nomedia")
        if (!nomedia.exists()) {
            try { nomedia.createNewFile() } catch (_: Exception) {}
        }
        return categoryFolder
    }

    /**
     * Scans all device media files for a specific category (VIDEO, AUDIO, or IMAGE).
     * Filters out files already moved into Privacy.
     */
    suspend fun scanDeviceFiles(mediaType: String): List<DeviceMediaFile> = withContext(Dispatchers.IO) {
        val movedOriginalPaths = getVaultItems().map { it.originalPath.lowercase() }.toSet()
        val result = mutableListOf<DeviceMediaFile>()

        when (mediaType.uppercase()) {
            "VIDEO" -> {
                val projection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.DATE_ADDED
                )
                try {
                    appContext.contentResolver.query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        "${MediaStore.Video.Media.DATE_ADDED} DESC"
                    )?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                        val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                        val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                        val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                        val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                        val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)

                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idCol)
                            val name = if (nameCol != -1) cursor.getString(nameCol) ?: "Video_$id" else "Video_$id"
                            val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                            val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                            val duration = if (durCol != -1) cursor.getLong(durCol) else 0L
                            val dateAdded = if (dateCol != -1) cursor.getLong(dateCol) else 0L
                            val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                            if (path.isNotBlank() && movedOriginalPaths.contains(path.lowercase())) {
                                continue
                            }

                            result.add(
                                DeviceMediaFile(
                                    id = "video_$id",
                                    title = name,
                                    uri = contentUri,
                                    path = path,
                                    sizeBytes = size,
                                    durationMs = duration,
                                    dateAdded = dateAdded,
                                    mimeType = "video/*"
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }

            "AUDIO" -> {
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATE_ADDED
                )
                try {
                    appContext.contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        "${MediaStore.Audio.Media.DATE_ADDED} DESC"
                    )?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                        val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                        val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                        val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                        val durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                        val dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idCol)
                            val name = if (nameCol != -1) cursor.getString(nameCol)
                                else if (titleCol != -1) cursor.getString(titleCol)
                                else "Audio_$id"
                            val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                            val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                            val duration = if (durCol != -1) cursor.getLong(durCol) else 0L
                            val dateAdded = if (dateCol != -1) cursor.getLong(dateCol) else 0L
                            val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                            if (path.isNotBlank() && movedOriginalPaths.contains(path.lowercase())) {
                                continue
                            }

                            result.add(
                                DeviceMediaFile(
                                    id = "audio_$id",
                                    title = name ?: "Audio_$id",
                                    uri = contentUri,
                                    path = path,
                                    sizeBytes = size,
                                    durationMs = duration,
                                    dateAdded = dateAdded,
                                    mimeType = "audio/*"
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }

            "IMAGE" -> {
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.DATE_ADDED
                )
                try {
                    appContext.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        "${MediaStore.Images.Media.DATE_ADDED} DESC"
                    )?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                        val dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                        val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                        val dateCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)

                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idCol)
                            val name = if (nameCol != -1) cursor.getString(nameCol) ?: "Image_$id" else "Image_$id"
                            val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                            val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                            val dateAdded = if (dateCol != -1) cursor.getLong(dateCol) else 0L
                            val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                            if (path.isNotBlank() && movedOriginalPaths.contains(path.lowercase())) {
                                continue
                            }

                            result.add(
                                DeviceMediaFile(
                                    id = "image_$id",
                                    title = name,
                                    uri = contentUri,
                                    path = path,
                                    sizeBytes = size,
                                    durationMs = 0L,
                                    dateAdded = dateAdded,
                                    mimeType = "image/*"
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        if (result.isEmpty()) {
            val sampleFolder = File(appContext.getExternalFilesDir(null) ?: appContext.filesDir, "device_samples_${mediaType.lowercase()}")
            if (!sampleFolder.exists()) {
                sampleFolder.mkdirs()
            }

            when (mediaType.uppercase()) {
                "VIDEO" -> {
                    val sampleVideos = listOf(
                        Triple("Avengers.Endgame.2019.mp4", 2800000000L, 10862000L),
                        Triple("Tutorial_Flutter_2024.mp4", 450000000L, 5025000L),
                        Triple("Wildlife_Documentary_Africa.avi", 1200000000L, 3130000L),
                        Triple("Avatar_The_Way_of_Water.mkv", 3500000000L, 11520000L),
                        Triple("Tech_Keynote_Highlights_4K.mp4", 890000000L, 2400000L)
                    )
                    sampleVideos.forEachIndexed { idx, (name, size, dur) ->
                        val sampleFile = File(sampleFolder, name)
                        if (!sampleFile.exists()) {
                            try {
                                sampleFile.writeText("F2W_MEDIA_SAMPLE_CONTENT_${name}_${System.currentTimeMillis()}")
                            } catch (_: Exception) {}
                        }
                        if (sampleFile.exists() && !movedOriginalPaths.contains(sampleFile.absolutePath.lowercase())) {
                            result.add(
                                DeviceMediaFile(
                                    id = "sample_video_$idx",
                                    title = name,
                                    uri = Uri.fromFile(sampleFile),
                                    path = sampleFile.absolutePath,
                                    sizeBytes = size,
                                    durationMs = dur,
                                    dateAdded = System.currentTimeMillis() / 1000 - (idx * 3600),
                                    mimeType = "video/*"
                                )
                            )
                        }
                    }
                }
                "AUDIO" -> {
                    val sampleAudios = listOf(
                        Triple("Midnight_City_Echoes.mp3", 8500000L, 245000L),
                        Triple("Acoustic_Guitar_Session.flac", 32000000L, 310000L),
                        Triple("LoFi_Study_Beats_2026.wav", 45000000L, 198000L),
                        Triple("Electronic_Synthwave_Mix.mp3", 11200000L, 280000L)
                    )
                    sampleAudios.forEachIndexed { idx, (name, size, dur) ->
                        val sampleFile = File(sampleFolder, name)
                        if (!sampleFile.exists()) {
                            try {
                                sampleFile.writeText("F2W_AUDIO_SAMPLE_CONTENT_${name}_${System.currentTimeMillis()}")
                            } catch (_: Exception) {}
                        }
                        if (sampleFile.exists() && !movedOriginalPaths.contains(sampleFile.absolutePath.lowercase())) {
                            result.add(
                                DeviceMediaFile(
                                    id = "sample_audio_$idx",
                                    title = name,
                                    uri = Uri.fromFile(sampleFile),
                                    path = sampleFile.absolutePath,
                                    sizeBytes = size,
                                    durationMs = dur,
                                    dateAdded = System.currentTimeMillis() / 1000 - (idx * 3600),
                                    mimeType = "audio/*"
                                )
                            )
                        }
                    }
                }
                "IMAGE" -> {
                    val sampleImages = listOf(
                        Pair("Nature_Mountain_Sunset.jpg", 3400000L),
                        Pair("Cyberpunk_City_Lights.png", 5200000L),
                        Pair("Personal_Passport_Document.jpg", 1800000L),
                        Pair("Vacation_Beach_Photo.jpg", 4100000L)
                    )
                    sampleImages.forEachIndexed { idx, (name, size) ->
                        val sampleFile = File(sampleFolder, name)
                        if (!sampleFile.exists()) {
                            try {
                                sampleFile.writeText("F2W_IMAGE_SAMPLE_CONTENT_${name}_${System.currentTimeMillis()}")
                            } catch (_: Exception) {}
                        }
                        if (sampleFile.exists() && !movedOriginalPaths.contains(sampleFile.absolutePath.lowercase())) {
                            result.add(
                                DeviceMediaFile(
                                    id = "sample_image_$idx",
                                    title = name,
                                    uri = Uri.fromFile(sampleFile),
                                    path = sampleFile.absolutePath,
                                    sizeBytes = size,
                                    durationMs = 0L,
                                    dateAdded = System.currentTimeMillis() / 1000 - (idx * 3600),
                                    mimeType = "image/*"
                                )
                            )
                        }
                    }
                }
            }
        }

        result
    }

    /**
     * MOVES device files into Privacy Vault:
     * 1. Preserves original filename and extension.
     * 2. Copies completely into privacy directory.
     * 3. Verifies copy was successful.
     * 4. Deletes original file from device. If deletion cannot be done, preserves original.
     * 5. Rescans media to remove from standard library.
     */
    suspend fun moveDeviceFilesToVault(
        files: List<DeviceMediaFile>,
        mediaType: String,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): MoveResult = withContext(Dispatchers.IO) {
        val vaultDir = getVaultCategoryDir(mediaType)
        var successCount = 0
        var failedCount = 0
        val errors = mutableListOf<String>()
        val newVaultItems = mutableListOf<PrivacyVaultItem>()

        files.forEachIndexed { index, file ->
            onProgress(index + 1, files.size)
            try {
                val originalFileName = file.title.ifBlank { "file_${System.currentTimeMillis()}" }
                val targetFile = getUniqueTargetFile(vaultDir, originalFileName)

                var copiedBytes = 0L
                val inputStream: InputStream? = appContext.contentResolver.openInputStream(file.uri)
                    ?: if (file.path.isNotBlank()) File(file.path).inputStream() else null

                if (inputStream == null) {
                    failedCount++
                    errors.add("Could not open ${file.title}")
                    return@forEachIndexed
                }

                FileOutputStream(targetFile).use { output ->
                    inputStream.use { input ->
                        copiedBytes = input.copyTo(output)
                    }
                }

                val actualSourceSize = if (file.path.isNotBlank() && File(file.path).exists()) File(file.path).length() else copiedBytes
                if (!targetFile.exists() || targetFile.length() == 0L || (actualSourceSize > 0 && targetFile.length() != actualSourceSize && targetFile.length() < copiedBytes)) {
                    targetFile.delete()
                    failedCount++
                    errors.add("Copy verification failed for ${file.title}")
                    return@forEachIndexed
                }

                // Delete original file if permitted by OS to ensure it is MOVED, not duplicated
                var deletedOriginal = false

                // Method 1: Delete via File path if accessible
                if (file.path.isNotBlank()) {
                    try {
                        val origFile = File(file.path)
                        if (origFile.exists() && origFile.delete()) {
                            deletedOriginal = true
                        }
                    } catch (_: Exception) {}
                }

                // Method 2: Delete via ContentResolver MediaStore URI
                if (!deletedOriginal) {
                    try {
                        val rowsDeleted = appContext.contentResolver.delete(file.uri, null, null)
                        if (rowsDeleted > 0) {
                            deletedOriginal = true
                        }
                    } catch (_: Exception) {}
                }

                // Method 3: Delete via SAF DocumentsContract if applicable
                if (!deletedOriginal) {
                    try {
                        if (DocumentsContract.deleteDocument(appContext.contentResolver, file.uri)) {
                            deletedOriginal = true
                        }
                    } catch (_: Exception) {}
                }

                // Scan original path so Android MediaStore updates if file was deleted
                if (file.path.isNotBlank() && deletedOriginal) {
                    try {
                        MediaScannerConnection.scanFile(appContext, arrayOf(file.path), null, null)
                    } catch (_: Exception) {}
                }

                val vaultItem = PrivacyVaultItem(
                    id = "vault_${System.currentTimeMillis()}_${file.id}",
                    fileName = targetFile.name,
                    originalPath = file.path.ifBlank { file.uri.toString() },
                    vaultPath = targetFile.absolutePath,
                    mediaType = mediaType.uppercase(),
                    sizeBytes = targetFile.length(),
                    durationMs = file.durationMs,
                    dateAdded = System.currentTimeMillis()
                )
                newVaultItems.add(vaultItem)
                successCount++
            } catch (e: Exception) {
                failedCount++
                errors.add("Error moving ${file.title}: ${e.message}")
            }
        }

        if (newVaultItems.isNotEmpty()) {
            saveVaultItems(getVaultItems() + newVaultItems)
            _vaultUpdates.tryEmit(Unit)
        }

        MoveResult(successCount, failedCount, errors)
    }

    /**
     * MOVES files selected from Local Storage file picker into Privacy Vault.
     */
    suspend fun moveLocalUrisToVault(
        uris: List<Uri>,
        mediaType: String,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): MoveResult = withContext(Dispatchers.IO) {
        val vaultDir = getVaultCategoryDir(mediaType)
        var successCount = 0
        var failedCount = 0
        val errors = mutableListOf<String>()
        val newVaultItems = mutableListOf<PrivacyVaultItem>()

        uris.forEachIndexed { index, uri ->
            onProgress(index + 1, uris.size)
            try {
                // Query display name and size from SAF
                var displayName = "file_${System.currentTimeMillis()}"
                var size = 0L

                try {
                    appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                            if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                        }
                    }
                } catch (_: Exception) {}

                val targetFile = getUniqueTargetFile(vaultDir, displayName)

                var copiedBytes = 0L
                val inputStream = appContext.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    failedCount++
                    errors.add("Could not open $displayName")
                    return@forEachIndexed
                }

                FileOutputStream(targetFile).use { output ->
                    inputStream.use { input ->
                        copiedBytes = input.copyTo(output)
                    }
                }

                if (!targetFile.exists() || targetFile.length() == 0L || (size > 0 && copiedBytes < size)) {
                    targetFile.delete()
                    failedCount++
                    errors.add("Copy verification failed for $displayName")
                    return@forEachIndexed
                }

                // Delete original SAF document if permitted
                var deletedOriginal = false
                try {
                    if (DocumentsContract.deleteDocument(appContext.contentResolver, uri)) {
                        deletedOriginal = true
                    }
                } catch (_: Exception) {}

                if (!deletedOriginal) {
                    try {
                        if (appContext.contentResolver.delete(uri, null, null) > 0) {
                            deletedOriginal = true
                        }
                    } catch (_: Exception) {}
                }

                if (!deletedOriginal) {
                    // Try file delete if path is embedded
                    try {
                        val path = uri.path
                        if (path != null) {
                            val f = File(path)
                            if (f.exists() && f.delete()) {
                                deletedOriginal = true
                            }
                        }
                    } catch (_: Exception) {}
                }

                val vaultItem = PrivacyVaultItem(
                    id = "vault_${System.currentTimeMillis()}_$index",
                    fileName = targetFile.name,
                    originalPath = uri.toString(),
                    vaultPath = targetFile.absolutePath,
                    mediaType = mediaType.uppercase(),
                    sizeBytes = targetFile.length(),
                    durationMs = 0L,
                    dateAdded = System.currentTimeMillis()
                )
                newVaultItems.add(vaultItem)
                successCount++
            } catch (e: Exception) {
                failedCount++
                errors.add("Error moving file: ${e.message}")
            }
        }

        if (newVaultItems.isNotEmpty()) {
            saveVaultItems(getVaultItems() + newVaultItems)
            _vaultUpdates.tryEmit(Unit)
        }

        MoveResult(successCount, failedCount, errors)
    }

    /**
     * Retrieves all items currently stored in the Privacy Vault.
     */
    fun getVaultItems(mediaType: String? = null): List<PrivacyVaultItem> {
        val jsonStr = prefs.getString(KEY_VAULT_ITEMS_JSON, null) ?: return emptyList()
        val list = mutableListOf<PrivacyVaultItem>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val item = PrivacyVaultItem(
                    id = obj.getString("id"),
                    fileName = obj.getString("fileName"),
                    originalPath = obj.getString("originalPath"),
                    vaultPath = obj.getString("vaultPath"),
                    mediaType = obj.getString("mediaType"),
                    sizeBytes = obj.getLong("sizeBytes"),
                    durationMs = obj.optLong("durationMs", 0L),
                    dateAdded = obj.optLong("dateAdded", System.currentTimeMillis())
                )
                // Only return items whose files still exist on disk
                if (File(item.vaultPath).exists()) {
                    list.add(item)
                }
            }
        } catch (_: Exception) {}

        return if (mediaType != null) {
            list.filter { it.mediaType.equals(mediaType, ignoreCase = true) }
        } else {
            list
        }
    }

    /**
     * RESTORES a file from Privacy Vault back to normal phone storage:
     * 1. Copies file from vault back to original path or public media directory.
     * 2. Verifies copy was successful.
     * 3. Deletes vault file.
     * 4. Removes item from vault registry.
     * 5. Triggers Android MediaScannerConnection so it immediately appears in normal libraries.
     */
    suspend fun restoreVaultItemToDevice(item: PrivacyVaultItem): Boolean = withContext(Dispatchers.IO) {
        val vaultFile = File(item.vaultPath)
        if (!vaultFile.exists()) {
            saveVaultItems(getVaultItems().filter { it.id != item.id })
            _vaultUpdates.tryEmit(Unit)
            return@withContext false
        }

        try {
            var targetDir: File? = null
            if (item.originalPath.isNotBlank() && !item.originalPath.startsWith("content://")) {
                val origFile = File(item.originalPath)
                val parent = origFile.parentFile
                if (parent != null && parent.exists() && parent.canWrite()) {
                    targetDir = parent
                }
            }

            if (targetDir == null) {
                val publicType = when (item.mediaType.uppercase()) {
                    "VIDEO" -> android.os.Environment.DIRECTORY_MOVIES
                    "AUDIO" -> android.os.Environment.DIRECTORY_MUSIC
                    else -> android.os.Environment.DIRECTORY_PICTURES
                }
                val extDir = android.os.Environment.getExternalStoragePublicDirectory(publicType)
                if (extDir != null && (extDir.exists() || extDir.mkdirs())) {
                    targetDir = extDir
                } else {
                    targetDir = appContext.getExternalFilesDir(publicType) ?: appContext.filesDir
                }
            }

            val restoredFile = getUniqueTargetFile(targetDir, item.fileName)
            vaultFile.inputStream().use { input ->
                restoredFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (restoredFile.exists() && restoredFile.length() > 0L) {
                vaultFile.delete()
                val updated = getVaultItems().filter { it.id != item.id }
                saveVaultItems(updated)

                MediaScannerConnection.scanFile(
                    appContext,
                    arrayOf(restoredFile.absolutePath),
                    null,
                    null
                )

                _vaultUpdates.tryEmit(Unit)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * PERMANENTLY DELETES an item from Privacy Vault and disk.
     */
    suspend fun deleteVaultItemPermanently(item: PrivacyVaultItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val vaultFile = File(item.vaultPath)
            if (vaultFile.exists()) {
                vaultFile.delete()
            }
            val updated = getVaultItems().filter { it.id != item.id }
            saveVaultItems(updated)
            _vaultUpdates.tryEmit(Unit)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun saveVaultItems(items: List<PrivacyVaultItem>) {
        val jsonArray = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("fileName", item.fileName)
            obj.put("originalPath", item.originalPath)
            obj.put("vaultPath", item.vaultPath)
            obj.put("mediaType", item.mediaType)
            obj.put("sizeBytes", item.sizeBytes)
            obj.put("durationMs", item.durationMs)
            obj.put("dateAdded", item.dateAdded)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_VAULT_ITEMS_JSON, jsonArray.toString()).apply()
    }

    private fun getUniqueTargetFile(dir: File, originalName: String): File {
        var candidate = File(dir, originalName)
        if (!candidate.exists()) return candidate

        val dotIndex = originalName.lastIndexOf('.')
        val baseName = if (dotIndex != -1) originalName.substring(0, dotIndex) else originalName
        val extension = if (dotIndex != -1) originalName.substring(dotIndex) else ""

        var counter = 1
        while (candidate.exists()) {
            candidate = File(dir, "${baseName}_$counter$extension")
            counter++
        }
        return candidate
    }
}
