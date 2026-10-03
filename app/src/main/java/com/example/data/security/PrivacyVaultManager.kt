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
    val originalUriString: String = "",
    val originalMediaId: String = "",
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
        private const val KEY_MASTER_SEED_HEX = "vault_master_seed_hex"

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

    @Synchronized
    fun getOrCreateVaultEncryptionKey(): SecretKey {
        activeVaultKey?.let { return it }

        var seedHex = prefs.getString(KEY_MASTER_SEED_HEX, null)
        if (seedHex.isNullOrBlank()) {
            val header = getVaultHeaderJson()
            if (header != null && header.has("master_seed_hex")) {
                seedHex = header.getString("master_seed_hex")
            }
        }

        if (seedHex.isNullOrBlank()) {
            val randomBytes = ByteArray(32)
            java.security.SecureRandom().nextBytes(randomBytes)
            seedHex = VaultCryptoManager.bytesToHex(randomBytes)
            prefs.edit().putString(KEY_MASTER_SEED_HEX, seedHex).apply()

            try {
                val header = getVaultHeaderJson() ?: JSONObject()
                header.put("master_seed_hex", seedHex)
                val root = getPersistentVaultRootDir()
                File(root, "vault_header.json").writeText(header.toString())
            } catch (_: Exception) {}
        }

        val keyBytes = VaultCryptoManager.hexToBytes(seedHex)
        val key = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
        activeVaultKey = key
        return key
    }

    fun getActiveVaultKey(): SecretKey? = activeVaultKey

    fun isVaultUnlocked(): Boolean = activeVaultKey != null

    fun lockVault() {
        activeVaultKey = null
        cachedVaultItems = null
        cachedVaultPathsSet = null
        _vaultUpdates.tryEmit(Unit)
    }

    @Volatile
    private var cachedVaultIdsSet: Set<String>? = null

    private fun buildVaultPathsSet(items: List<PrivacyVaultItem>): Set<String> {
        val set = HashSet<String>()
        val ids = HashSet<String>()
        for (item in items) {
            if (item.originalPath.isNotBlank()) {
                val orig = item.originalPath.lowercase().trim()
                set.add(orig)
                val fileNameFromPath = orig.substringAfterLast('/')
                if (fileNameFromPath.isNotBlank()) set.add(fileNameFromPath)
            }
            if (item.originalUriString.isNotBlank()) {
                val uriStr = item.originalUriString.lowercase().trim()
                set.add(uriStr)
                val uriId = uriStr.substringAfterLast('/')
                if (uriId.isNotBlank()) ids.add(uriId)
            }
            if (item.originalMediaId.isNotBlank()) {
                val mId = item.originalMediaId.trim().lowercase()
                ids.add(mId)
                set.add(mId)
            }
            if (item.vaultPath.isNotBlank()) {
                val vPath = item.vaultPath.lowercase().trim()
                set.add(vPath)
                val vFileName = vPath.substringAfterLast('/')
                if (vFileName.isNotBlank()) set.add(vFileName)
            }
            if (item.fileName.isNotBlank()) {
                val fName = item.fileName.lowercase().trim()
                set.add(fName)
                val fNameNoExt = fName.substringBeforeLast('.')
                if (fNameNoExt.isNotBlank() && fNameNoExt.length > 2) set.add(fNameNoExt)
            }
            if (item.id.isNotBlank()) {
                set.add(item.id.lowercase().trim())
            }
        }
        cachedVaultIdsSet = ids
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
        if (id != null && (pathsSet.contains(id.lowercase().trim()) || cachedVaultIdsSet?.contains(id.trim().lowercase()) == true)) return true

        val lastSegment = lower.substringAfterLast('/').substringAfterLast('%')
        if (lastSegment.isNotBlank() && pathsSet.contains(lastSegment)) return true

        for (itemPath in pathsSet) {
            if (itemPath.length > 3 && (lower == itemPath || lower.endsWith(itemPath) || itemPath.endsWith(lower))) {
                return true
            }
        }
        return false
    }

    /**
     * MOVES device files into Privacy Vault (lightweight, unencrypted, fast).
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
                val targetFile = File(vaultDir, "pv_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}_${originalFileName}")

                val inputStream: InputStream? = appContext.contentResolver.openInputStream(file.uri)
                    ?: if (file.path.isNotBlank()) File(file.path).inputStream() else null

                if (inputStream == null) {
                    failedCount++
                    errors.add("Could not open ${file.title}")
                    return@forEachIndexed
                }

                val writtenBytes = inputStream.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (!targetFile.exists() || writtenBytes <= 0) {
                    targetFile.delete()
                    failedCount++
                    errors.add("Failed to move ${file.title}")
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
                    originalUriString = file.uri.toString(),
                    originalMediaId = file.id,
                    vaultPath = targetFile.absolutePath,
                    mediaType = mediaType.uppercase(),
                    sizeBytes = targetFile.length(),
                    durationMs = file.durationMs,
                    dateAdded = System.currentTimeMillis()
                )
                newVaultItems.add(vaultItem)
                successCount++

                // Immediately purge from Room DB and scanner cache
                if (mediaType.equals("VIDEO", ignoreCase = true)) {
                    com.example.data.media.LocalVideoScanner.getInstance(appContext)
                        .removeVideoFromLibrary(file.id, file.uri.toString(), file.path)
                }
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
     * MOVES files selected from Local Storage file picker into Privacy Vault (lightweight).
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
                var displayName = "file_${System.currentTimeMillis()}"
                try {
                    appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex != -1) {
                            displayName = cursor.getString(nameIndex) ?: displayName
                        }
                    }
                } catch (_: Exception) {}

                val targetFile = File(vaultDir, "pv_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}_${displayName}")
                val inputStream = appContext.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    failedCount++
                    errors.add("Could not open $displayName")
                    return@forEachIndexed
                }

                val writtenBytes = inputStream.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (!targetFile.exists() || writtenBytes <= 0) {
                    targetFile.delete()
                    failedCount++
                    errors.add("Move failed for $displayName")
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
     * Retrieves lightweight private file for player/viewing instantly.
     */
    fun getDecryptedTempFile(item: PrivacyVaultItem): File? {
        val file = File(item.vaultPath)
        if (file.exists() && file.length() > 0) return file
        return null
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
        val list = mutableListOf<PrivacyVaultItem>()

        // 1. Try loading from vault_items.json
        val jsonFile = File(getPersistentVaultRootDir(), "vault_items.json")
        var jsonStr = if (jsonFile.exists()) jsonFile.readText() else null

        // 2. Try encrypted legacy if upgrading/downgrading
        if (jsonStr.isNullOrBlank()) {
            val key = activeVaultKey
            val itemsEncFile = File(getPersistentVaultRootDir(), "vault_items.enc")
            if (itemsEncFile.exists() && key != null) {
                try {
                    val encHex = itemsEncFile.readText()
                    jsonStr = VaultCryptoManager.decryptString(encHex, key)
                } catch (_: Exception) {}
            }
        }

        // 3. Fallback to prefs
        if (jsonStr.isNullOrBlank()) {
            jsonStr = prefs.getString("privacy_vault_items_json", null)
        }

        if (!jsonStr.isNullOrBlank()) {
            try {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val item = PrivacyVaultItem(
                        id = obj.getString("id"),
                        fileName = obj.getString("fileName"),
                        originalPath = obj.getString("originalPath"),
                        originalUriString = obj.optString("originalUriString", ""),
                        originalMediaId = obj.optString("originalMediaId", ""),
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
        return list
    }

    /**
     * RESTORES a file from Privacy Vault back to normal phone storage.
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
            obj.put("originalUriString", item.originalUriString)
            obj.put("originalMediaId", item.originalMediaId)
            obj.put("vaultPath", item.vaultPath)
            obj.put("mediaType", item.mediaType)
            obj.put("sizeBytes", item.sizeBytes)
            obj.put("durationMs", item.durationMs)
            obj.put("dateAdded", item.dateAdded)
            jsonArray.put(obj)
        }

        try {
            File(getPersistentVaultRootDir(), "vault_items.json").writeText(jsonArray.toString())
        } catch (_: Exception) {}
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
