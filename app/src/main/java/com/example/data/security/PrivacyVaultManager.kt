package com.example.data.security

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
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
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.crypto.SecretKey

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

class PrivacyVaultManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("f2w_privacy_vault_registry", Context.MODE_PRIVATE)

    private val _vaultUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val vaultUpdates: SharedFlow<Unit> = _vaultUpdates.asSharedFlow()

    @Volatile
    private var activeVaultKey: SecretKey? = null

    @Volatile
    private var cachedVaultItems: List<PrivacyVaultItem>? = null

    @Volatile
    private var cachedVaultPathsSet: Set<String>? = null

    companion object {
        private const val KEY_SALT_HEX = "vault_salt_hex"
        private const val KEY_VERIFIER_HASH = "vault_verifier_hash"

        @Volatile
        private var INSTANCE: PrivacyVaultManager? = null

        fun getInstance(context: Context): PrivacyVaultManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PrivacyVaultManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Resolves persistent device storage folder for F2W Private Vault.
     * Stored in /storage/emulated/0/F2W/PrivateVault so app uninstalls leave encrypted media intact.
     */
    fun getPersistentVaultRootDir(): File {
        val candidates = listOf(
            File(Environment.getExternalStorageDirectory(), "F2W/PrivateVault"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "F2W/PrivateVault"),
            File(appContext.getExternalFilesDir(null), "F2W/PrivateVault"),
            File(appContext.filesDir, "privacy_vault")
        )

        for (candidate in candidates) {
            try {
                if (!candidate.exists()) {
                    candidate.mkdirs()
                }
                if (candidate.exists() && candidate.canWrite()) {
                    val nomedia = File(candidate, ".nomedia")
                    if (!nomedia.exists()) {
                        nomedia.createNewFile()
                    }
                    return candidate
                }
            } catch (_: Exception) {}
        }

        val fallback = File(appContext.filesDir, "privacy_vault")
        fallback.mkdirs()
        try { File(fallback, ".nomedia").createNewFile() } catch (_: Exception) {}
        return fallback
    }

    fun getVaultCategoryDir(mediaType: String): File {
        val root = getPersistentVaultRootDir()
        val categoryFolder = File(root, mediaType.lowercase())
        if (!categoryFolder.exists()) {
            categoryFolder.mkdirs()
        }
        val nomedia = File(categoryFolder, ".nomedia")
        if (!nomedia.exists()) {
            try { nomedia.createNewFile() } catch (_: Exception) {}
        }
        return categoryFolder
    }

    /**
     * Checks if a persistent vault header exists on disk (e.g. after reinstall).
     */
    fun isExistingVaultDetected(): Boolean {
        val headerFile = File(getPersistentVaultRootDir(), "vault_header.json")
        return headerFile.exists() && headerFile.length() > 0
    }

    /**
     * Reads vault header JSON from persistent storage.
     */
    fun getVaultHeaderJson(): JSONObject? {
        val headerFile = File(getPersistentVaultRootDir(), "vault_header.json")
        if (!headerFile.exists()) return null
        return try {
            JSONObject(headerFile.readText())
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Initializes or updates vault header with derived master key and salt.
     */
    fun initializeVaultKey(
        pin: String,
        question: String = "",
        answer: String = ""
    ): SecretKey {
        val root = getPersistentVaultRootDir()
        val headerFile = File(root, "vault_header.json")

        var salt: ByteArray
        var questionSalt: ByteArray
        var headerObj = getVaultHeaderJson()

        if (headerObj != null && headerObj.has("salt_hex")) {
            salt = VaultCryptoManager.hexToBytes(headerObj.getString("salt_hex"))
            questionSalt = if (headerObj.has("question_salt_hex")) {
                VaultCryptoManager.hexToBytes(headerObj.getString("question_salt_hex"))
            } else {
                VaultCryptoManager.generateRandomSalt()
            }
        } else {
            salt = VaultCryptoManager.generateRandomSalt()
            questionSalt = VaultCryptoManager.generateRandomSalt()
            headerObj = JSONObject()
        }

        val key = VaultCryptoManager.deriveKey(pin, salt)
        val verifierHash = VaultCryptoManager.generateVerifierHash(key)

        headerObj.put("vault_id", "f2w_vault_${System.currentTimeMillis()}")
        headerObj.put("salt_hex", VaultCryptoManager.bytesToHex(salt))
        headerObj.put("verifier_hash", verifierHash)

        if (question.isNotBlank() && answer.isNotBlank()) {
            val qKey = VaultCryptoManager.deriveKey(answer.trim().lowercase(), questionSalt)
            val qVerifier = VaultCryptoManager.generateVerifierHash(qKey)
            headerObj.put("question", question.trim())
            headerObj.put("question_salt_hex", VaultCryptoManager.bytesToHex(questionSalt))
            headerObj.put("question_verifier_hash", qVerifier)
        }

        try {
            headerFile.writeText(headerObj.toString())
        } catch (_: Exception) {}

        prefs.edit()
            .putString(KEY_SALT_HEX, VaultCryptoManager.bytesToHex(salt))
            .putString(KEY_VERIFIER_HASH, verifierHash)
            .apply()

        activeVaultKey = key
        return key
    }

    /**
     * Unlocks existing vault with user entered PIN or Security Answer.
     */
    fun unlockVaultWithPinOrAnswer(input: String, isAnswer: Boolean = false): Boolean {
        val header = getVaultHeaderJson() ?: return false
        val saltHex = if (isAnswer) header.optString("question_salt_hex") else header.optString("salt_hex")
        val verifier = if (isAnswer) header.optString("question_verifier_hash") else header.optString("verifier_hash")

        if (saltHex.isBlank() || verifier.isBlank()) return false

        val salt = VaultCryptoManager.hexToBytes(saltHex)
        val derivedKey = VaultCryptoManager.deriveKey(input.trim().let { if (isAnswer) it.lowercase() else it }, salt)

        if (VaultCryptoManager.verifyKey(derivedKey, verifier)) {
            if (!isAnswer) {
                activeVaultKey = derivedKey
            } else {
                activeVaultKey = derivedKey
            }
            cachedVaultItems = null
            cachedVaultPathsSet = null
            _vaultUpdates.tryEmit(Unit)
            return true
        }
        return false
    }

    fun getActiveVaultKey(): SecretKey? = activeVaultKey

    fun isVaultUnlocked(): Boolean = activeVaultKey != null

    fun lockVault() {
        activeVaultKey = null
        cachedVaultItems = null
        cachedVaultPathsSet = null
        _vaultUpdates.tryEmit(Unit)
    }

    private fun buildVaultPathsSet(items: List<PrivacyVaultItem>): Set<String> {
        val set = HashSet<String>()
        for (item in items) {
            if (item.originalPath.isNotBlank()) set.add(item.originalPath.lowercase().trim())
            if (item.vaultPath.isNotBlank()) set.add(item.vaultPath.lowercase().trim())
            if (item.fileName.isNotBlank()) set.add(item.fileName.lowercase().trim())
            if (item.id.isNotBlank()) set.add(item.id.lowercase().trim())
        }
        return set
    }

    /**
     * Scans unencrypted device media files (Videos, Audio, or Images) from MediaStore.
     * Filters out files already moved into Privacy Vault.
     */
    suspend fun scanDeviceFiles(mediaType: String): List<DeviceMediaFile> = withContext(Dispatchers.IO) {
        val result = mutableListOf<DeviceMediaFile>()
        val uri = when (mediaType.uppercase()) {
            "VIDEO" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "AUDIO" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED
        )

        try {
            appContext.contentResolver.query(uri, projection, null, null, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
                val idIdx = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val pathIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val sizeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = if (idIdx != -1) cursor.getLong(idIdx).toString() else ""
                    val title = if (nameIdx != -1) cursor.getString(nameIdx) ?: "Media" else "Media"
                    val path = if (pathIdx != -1) cursor.getString(pathIdx) ?: "" else ""
                    val sizeBytes = if (sizeIdx != -1) cursor.getLong(sizeIdx) else 0L
                    val dateAdded = if (dateIdx != -1) cursor.getLong(dateIdx) else 0L
                    val contentUri = ContentUris.withAppendedId(uri, id.toLongOrNull() ?: 0L)

                    if (!isPathOrUriInVault(path, id)) {
                        result.add(
                            DeviceMediaFile(
                                id = id,
                                title = title,
                                uri = contentUri,
                                path = path,
                                sizeBytes = sizeBytes,
                                durationMs = 0L,
                                dateAdded = dateAdded,
                                mimeType = ""
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        result
    }

    /**
     * Checks if a file path, URI string, title, or ID belongs to an item currently stored in Privacy Vault.
     * Uses fast O(1) in-memory set comparison.
     */
    fun isPathOrUriInVault(uriOrPath: String?, id: String? = null): Boolean {
        if (uriOrPath.isNullOrBlank()) return false
        val lower = uriOrPath.lowercase().trim()
        if (lower.contains("privacy_vault") || lower.contains("privatevault")) return true

        val pathsSet = cachedVaultPathsSet ?: run {
            val items = getVaultItems()
            val set = buildVaultPathsSet(items)
            cachedVaultPathsSet = set
            set
        }

        if (pathsSet.isEmpty()) return false
        if (pathsSet.contains(lower)) return true
        if (id != null && pathsSet.contains(id.lowercase().trim())) return true

        for (itemPath in pathsSet) {
            if (itemPath.length > 3 && (lower == itemPath || lower.endsWith(itemPath) || itemPath.endsWith(lower))) {
                return true
            }
        }
        return false
    }

    /**
     * MOVES device files into Privacy Vault using AES-256-GCM encryption.
     */
    suspend fun moveDeviceFilesToVault(
        files: List<DeviceMediaFile>,
        mediaType: String,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): MoveResult = withContext(Dispatchers.IO) {
        val key = activeVaultKey
        if (key == null) {
            return@withContext MoveResult(0, files.size, listOf("Vault is locked. Please unlock first."))
        }

        val vaultDir = getVaultCategoryDir(mediaType)
        var successCount = 0
        var failedCount = 0
        val errors = mutableListOf<String>()
        val newVaultItems = mutableListOf<PrivacyVaultItem>()

        files.forEachIndexed { index, file ->
            onProgress(index + 1, files.size)
            try {
                val originalFileName = file.title.ifBlank { "file_${System.currentTimeMillis()}" }
                val targetEncryptedFile = File(vaultDir, "media_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.f2w")

                val inputStream: InputStream? = appContext.contentResolver.openInputStream(file.uri)
                    ?: if (file.path.isNotBlank()) File(file.path).inputStream() else null

                if (inputStream == null) {
                    failedCount++
                    errors.add("Could not open ${file.title}")
                    return@forEachIndexed
                }

                val encryptedSize = inputStream.use { input ->
                    VaultCryptoManager.encryptStreamToFile(input, targetEncryptedFile, key)
                }

                if (!targetEncryptedFile.exists() || encryptedSize <= 12) {
                    targetEncryptedFile.delete()
                    failedCount++
                    errors.add("Encryption failed for ${file.title}")
                    return@forEachIndexed
                }

                var deletedOriginal = false

                if (file.path.isNotBlank()) {
                    try {
                        val origFile = File(file.path)
                        if (origFile.exists() && origFile.delete()) {
                            deletedOriginal = true
                        }
                    } catch (_: Exception) {}
                }

                if (!deletedOriginal) {
                    try {
                        if (appContext.contentResolver.delete(file.uri, null, null) > 0) {
                            deletedOriginal = true
                        }
                    } catch (_: Exception) {}
                }

                if (!deletedOriginal) {
                    try {
                        if (DocumentsContract.deleteDocument(appContext.contentResolver, file.uri)) {
                            deletedOriginal = true
                        }
                    } catch (_: Exception) {}
                }

                if (file.path.isNotBlank()) {
                    try {
                        MediaScannerConnection.scanFile(appContext, arrayOf(file.path), null, null)
                    } catch (_: Exception) {}
                }

                val vaultItem = PrivacyVaultItem(
                    id = "vault_${System.currentTimeMillis()}_${file.id}",
                    fileName = originalFileName,
                    originalPath = file.path.ifBlank { file.uri.toString() },
                    vaultPath = targetEncryptedFile.absolutePath,
                    mediaType = mediaType.uppercase(),
                    sizeBytes = targetEncryptedFile.length(),
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
     * MOVES files selected from Local Storage file picker into Privacy Vault using AES-256-GCM.
     */
    suspend fun moveLocalUrisToVault(
        uris: List<Uri>,
        mediaType: String,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): MoveResult = withContext(Dispatchers.IO) {
        val key = activeVaultKey
        if (key == null) {
            return@withContext MoveResult(0, uris.size, listOf("Vault is locked. Please unlock first."))
        }

        val vaultDir = getVaultCategoryDir(mediaType)
        var successCount = 0
        var failedCount = 0
        val errors = mutableListOf<String>()
        val newVaultItems = mutableListOf<PrivacyVaultItem>()

        uris.forEachIndexed { index, uri ->
            onProgress(index + 1, uris.size)
            try {
                var displayName = "file_${System.currentTimeMillis()}"
                try {
                    appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex != -1) {
                            displayName = cursor.getString(nameIndex) ?: displayName
                        }
                    }
                } catch (_: Exception) {}

                val targetEncryptedFile = File(vaultDir, "media_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.f2w")
                val inputStream = appContext.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    failedCount++
                    errors.add("Could not open $displayName")
                    return@forEachIndexed
                }

                val encryptedSize = inputStream.use { input ->
                    VaultCryptoManager.encryptStreamToFile(input, targetEncryptedFile, key)
                }

                if (!targetEncryptedFile.exists() || encryptedSize <= 12) {
                    targetEncryptedFile.delete()
                    failedCount++
                    errors.add("Encryption failed for $displayName")
                    return@forEachIndexed
                }

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

                val vaultItem = PrivacyVaultItem(
                    id = "vault_${System.currentTimeMillis()}_$index",
                    fileName = displayName,
                    originalPath = uri.toString(),
                    vaultPath = targetEncryptedFile.absolutePath,
                    mediaType = mediaType.uppercase(),
                    sizeBytes = targetEncryptedFile.length(),
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
     * Decrypts encrypted vault item on-the-fly to a temporary cache file for ExoPlayer / Image loading.
     */
    fun getDecryptedTempFile(item: PrivacyVaultItem): File? {
        val key = activeVaultKey ?: return null
        val encryptedFile = File(item.vaultPath)
        if (!encryptedFile.exists()) return null

        val dotIdx = item.fileName.lastIndexOf('.')
        val ext = if (dotIdx != -1) item.fileName.substring(dotIdx) else ".tmp"

        val tempDir = File(appContext.cacheDir, "vault_temp_play")
        if (!tempDir.exists()) tempDir.mkdirs()

        val tempFile = File(tempDir, "play_${item.id.replace("[^a-zA-Z0-9]".toRegex(), "_")}$ext")

        try {
            if (tempFile.exists() && tempFile.length() > 0) {
                return tempFile
            }
            VaultCryptoManager.decryptFileToTempFile(encryptedFile, tempFile, key)
            tempFile.deleteOnExit()
            return tempFile
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Retrieves all items currently stored in the Privacy Vault.
     */
    fun getVaultItems(mediaType: String? = null): List<PrivacyVaultItem> {
        val cached = cachedVaultItems
        val list = if (cached != null) {
            cached
        } else {
            val loaded = loadVaultItemsFromStorage()
            cachedVaultItems = loaded
            cachedVaultPathsSet = buildVaultPathsSet(loaded)
            loaded
        }

        return if (mediaType != null) {
            list.filter { it.mediaType.equals(mediaType, ignoreCase = true) }
        } else {
            list
        }
    }

    private fun loadVaultItemsFromStorage(): List<PrivacyVaultItem> {
        val key = activeVaultKey
        val itemsFile = File(getPersistentVaultRootDir(), "vault_items.enc")

        val list = mutableListOf<PrivacyVaultItem>()

        if (itemsFile.exists() && key != null) {
            try {
                val encHex = itemsFile.readText()
                val jsonStr = VaultCryptoManager.decryptString(encHex, key)
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
                    if (File(item.vaultPath).exists()) {
                        list.add(item)
                    }
                }
            } catch (_: Exception) {}
        } else {
            val legacyJson = prefs.getString("privacy_vault_items_json", null)
            if (!legacyJson.isNullOrBlank()) {
                try {
                    val jsonArray = JSONArray(legacyJson)
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
                        if (File(item.vaultPath).exists()) {
                            list.add(item)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        return list
    }

    /**
     * RESTORES a file from Privacy Vault back to normal phone storage:
     * 1. Decrypts file using AES-256-GCM.
     * 2. Writes decrypted file back to Movies/F2W, Music/F2W, or Pictures/F2W.
     * 3. Verifies copy was successful.
     * 4. Deletes encrypted vault file.
     * 5. Triggers MediaScannerConnection.
     */
    suspend fun restoreVaultItemToDevice(item: PrivacyVaultItem): Boolean = withContext(Dispatchers.IO) {
        val key = activeVaultKey ?: return@withContext false
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
                    "VIDEO" -> Environment.DIRECTORY_MOVIES
                    "AUDIO" -> Environment.DIRECTORY_MUSIC
                    else -> Environment.DIRECTORY_PICTURES
                }
                val extDir = Environment.getExternalStoragePublicDirectory(publicType)
                val f2wSubDir = File(extDir, "F2W")
                if (f2wSubDir.exists() || f2wSubDir.mkdirs()) {
                    targetDir = f2wSubDir
                } else {
                    targetDir = appContext.getExternalFilesDir(publicType) ?: appContext.filesDir
                }
            }

            val restoredFile = getUniqueTargetFile(targetDir, item.fileName)
            restoredFile.outputStream().use { restoredOut ->
                VaultCryptoManager.decryptFileToStream(vaultFile, restoredOut, key)
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
        cachedVaultItems = items
        cachedVaultPathsSet = buildVaultPathsSet(items)

        val key = activeVaultKey
        val itemsFile = File(getPersistentVaultRootDir(), "vault_items.enc")

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

        if (key != null) {
            try {
                val encHex = VaultCryptoManager.encryptString(jsonArray.toString(), key)
                itemsFile.writeText(encHex)
            } catch (_: Exception) {}
        }
        prefs.edit().putString("privacy_vault_items_json", jsonArray.toString()).apply()
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
