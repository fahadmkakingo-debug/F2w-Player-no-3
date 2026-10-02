package com.example.data.playlist

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.provider.MediaStore
import com.example.data.media.DemoVideoData
import com.example.data.security.PrivacyVaultManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class PlaylistMediaType {
    AUDIO,
    VIDEO
}

data class PlaylistItemModel(
    val id: String,
    val title: String,
    val uriString: String,
    val durationText: String = "00:00",
    val durationMs: Long = 0L,
    val sizeText: String = "",
    val subtitle: String = "",
    val mediaType: PlaylistMediaType = PlaylistMediaType.VIDEO,
    val album: String = "Unknown Album",
    val artist: String = "Unknown Artist",
    val folder: String = "Music",
    val albumId: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis()
)

data class UserPlaylist(
    val id: String,
    val name: String,
    val type: PlaylistMediaType,
    val createdAt: Long = System.currentTimeMillis(),
    val items: List<PlaylistItemModel> = emptyList()
) {
    val totalDurationText: String
        get() {
            val totalMs = items.sumOf { it.durationMs }
            if (totalMs <= 0L) return "${items.size} items"
            val totalSeconds = totalMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                "${hours}h ${minutes % 60}m"
            } else {
                "${minutes}m ${seconds}s"
            }
        }
}

class PlaylistManager private constructor(private val appContext: Context) {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("f2w_user_playlists_prefs", Context.MODE_PRIVATE)

    private val _playlistUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playlistUpdates: SharedFlow<Unit> = _playlistUpdates.asSharedFlow()

    companion object {
        private const val KEY_PLAYLISTS_JSON = "key_stored_playlists_json"

        @Volatile
        private var INSTANCE: PlaylistManager? = null

        fun getInstance(context: Context): PlaylistManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlaylistManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        // Seed default playlists if completely empty on first launch
        if (getAllPlaylists().isEmpty()) {
            seedDefaultPlaylists()
        }
    }

    private fun seedDefaultPlaylists() {
        val initialPlaylists = listOf(
            UserPlaylist(
                id = "pl_default_audio",
                name = "Nyimbo Zangu",
                type = PlaylistMediaType.AUDIO,
                items = emptyList()
            ),
            UserPlaylist(
                id = "pl_default_video",
                name = "Video Zangu",
                type = PlaylistMediaType.VIDEO,
                items = emptyList()
            )
        )
        saveAllPlaylists(initialPlaylists)
    }

    @Synchronized
    fun getAllPlaylists(): List<UserPlaylist> {
        val raw = prefs.getString(KEY_PLAYLISTS_JSON, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            val result = mutableListOf<UserPlaylist>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val name = obj.optString("name", "Untitled")
                val typeStr = obj.optString("type", PlaylistMediaType.VIDEO.name)
                val type = try {
                    PlaylistMediaType.valueOf(typeStr)
                } catch (_: Exception) {
                    PlaylistMediaType.VIDEO
                }
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                val itemsArray = obj.optJSONArray("items") ?: JSONArray()
                val itemsList = mutableListOf<PlaylistItemModel>()
                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    itemsList.add(
                        PlaylistItemModel(
                            id = itemObj.optString("id", UUID.randomUUID().toString()),
                            title = itemObj.optString("title", "Unknown"),
                            uriString = itemObj.optString("uriString", ""),
                            durationText = itemObj.optString("durationText", "00:00"),
                            durationMs = itemObj.optLong("durationMs", 0L),
                            sizeText = itemObj.optString("sizeText", ""),
                            subtitle = itemObj.optString("subtitle", ""),
                            mediaType = type,
                            dateAdded = itemObj.optLong("dateAdded", System.currentTimeMillis())
                        )
                    )
                }

                result.add(
                    UserPlaylist(
                        id = id,
                        name = name,
                        type = type,
                        createdAt = createdAt,
                        items = itemsList
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getPlaylists(type: PlaylistMediaType): List<UserPlaylist> {
        return getAllPlaylists().filter { it.type == type }
    }

    fun getPlaylistById(id: String): UserPlaylist? {
        return getAllPlaylists().firstOrNull { it.id == id }
    }

    @Synchronized
    fun createPlaylist(name: String, type: PlaylistMediaType): UserPlaylist {
        val trimmed = name.trim().ifBlank {
            if (type == PlaylistMediaType.AUDIO) "New Audio Playlist" else "New Video Playlist"
        }
        val newPlaylist = UserPlaylist(
            id = "pl_${type.name.lowercase()}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
            name = trimmed,
            type = type,
            createdAt = System.currentTimeMillis(),
            items = emptyList()
        )
        val current = getAllPlaylists().toMutableList()
        current.add(0, newPlaylist)
        saveAllPlaylists(current)
        _playlistUpdates.tryEmit(Unit)
        return newPlaylist
    }

    @Synchronized
    fun deletePlaylist(playlistId: String): Boolean {
        val current = getAllPlaylists().toMutableList()
        val removed = current.removeAll { it.id == playlistId }
        if (removed) {
            saveAllPlaylists(current)
            _playlistUpdates.tryEmit(Unit)
        }
        return removed
    }

    @Synchronized
    fun renamePlaylist(playlistId: String, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return false
        val current = getAllPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index == -1) return false
        current[index] = current[index].copy(name = trimmed)
        saveAllPlaylists(current)
        _playlistUpdates.tryEmit(Unit)
        return true
    }

    @Synchronized
    fun addItemsToPlaylist(playlistId: String, newItems: List<PlaylistItemModel>): Boolean {
        if (newItems.isEmpty()) return false
        val current = getAllPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index == -1) return false

        val existingPlaylist = current[index]
        val existingIds = existingPlaylist.items.map { it.id }.toSet()
        val filteredNew = newItems.filter { !existingIds.contains(it.id) }
        if (filteredNew.isEmpty()) return false

        val updatedItems = existingPlaylist.items + filteredNew
        current[index] = existingPlaylist.copy(items = updatedItems)
        saveAllPlaylists(current)
        _playlistUpdates.tryEmit(Unit)
        return true
    }

    @Synchronized
    fun removeItemFromPlaylist(playlistId: String, itemId: String): Boolean {
        val current = getAllPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index == -1) return false

        val existingPlaylist = current[index]
        val updatedItems = existingPlaylist.items.filter { it.id != itemId }
        current[index] = existingPlaylist.copy(items = updatedItems)
        saveAllPlaylists(current)
        _playlistUpdates.tryEmit(Unit)
        return true
    }

    @Synchronized
    fun updatePlaylistItemsOrder(playlistId: String, newItemsOrder: List<PlaylistItemModel>): Boolean {
        val current = getAllPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index == -1) return false

        current[index] = current[index].copy(items = newItemsOrder)
        saveAllPlaylists(current)
        _playlistUpdates.tryEmit(Unit)
        return true
    }

    @Synchronized
    fun movePlaylistItem(playlistId: String, fromIndex: Int, toIndex: Int): Boolean {
        val current = getAllPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index == -1) return false

        val existingPlaylist = current[index]
        val items = existingPlaylist.items.toMutableList()
        if (fromIndex !in items.indices || toIndex !in items.indices) return false

        val moved = items.removeAt(fromIndex)
        items.add(toIndex, moved)
        current[index] = existingPlaylist.copy(items = items)
        saveAllPlaylists(current)
        _playlistUpdates.tryEmit(Unit)
        return true
    }

    @Synchronized
    fun saveConvertedMp3(
        title: String,
        durationText: String,
        durationMs: Long,
        sizeText: String,
        uriString: String
    ): UserPlaylist {
        val cleanTitle = if (title.endsWith(".mp4", ignoreCase = true) ||
            title.endsWith(".avi", ignoreCase = true) ||
            title.endsWith(".mkv", ignoreCase = true) ||
            title.endsWith(".mov", ignoreCase = true) ||
            title.endsWith(".dd0", ignoreCase = true)
        ) {
            title.substringBeforeLast(".")
        } else title

        val audioItem = PlaylistItemModel(
            id = "converted_audio_${System.currentTimeMillis()}",
            title = "$cleanTitle.mp3",
            uriString = uriString,
            durationText = durationText,
            durationMs = durationMs,
            sizeText = sizeText.ifBlank { "4.8 MB" },
            subtitle = "Converted from Video",
            mediaType = PlaylistMediaType.AUDIO
        )

        val allPlaylists = getAllPlaylists().toMutableList()
        var convertedPlaylist = allPlaylists.firstOrNull { it.id == "pl_converted_audios" }
        if (convertedPlaylist == null) {
            convertedPlaylist = UserPlaylist(
                id = "pl_converted_audios",
                name = "Converted MP3s",
                type = PlaylistMediaType.AUDIO,
                items = listOf(audioItem)
            )
            allPlaylists.add(0, convertedPlaylist)
        } else {
            val updatedItems = listOf(audioItem) + convertedPlaylist.items.filter { it.title != audioItem.title }
            val idx = allPlaylists.indexOfFirst { it.id == "pl_converted_audios" }
            convertedPlaylist = convertedPlaylist.copy(items = updatedItems)
            allPlaylists[idx] = convertedPlaylist
        }
        saveAllPlaylists(allPlaylists)
        _playlistUpdates.tryEmit(Unit)
        return convertedPlaylist
    }

    private fun saveAllPlaylists(list: List<UserPlaylist>) {
        val jsonArray = JSONArray()
        list.forEach { pl ->
            val obj = JSONObject()
            obj.put("id", pl.id)
            obj.put("name", pl.name)
            obj.put("type", pl.type.name)
            obj.put("createdAt", pl.createdAt)

            val itemsArray = JSONArray()
            pl.items.forEach { item ->
                val itemObj = JSONObject()
                itemObj.put("id", item.id)
                itemObj.put("title", item.title)
                itemObj.put("uriString", item.uriString)
                itemObj.put("durationText", item.durationText)
                itemObj.put("durationMs", item.durationMs)
                itemObj.put("sizeText", item.sizeText)
                itemObj.put("subtitle", item.subtitle)
                itemObj.put("dateAdded", item.dateAdded)
                itemsArray.put(itemObj)
            }
            obj.put("items", itemsArray)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_PLAYLISTS_JSON, jsonArray.toString()).apply()
    }

    /**
     * Retrieve all available videos that can be added to playlists.
     */
    fun getAvailableVideos(): List<PlaylistItemModel> {
        val vaultManager = PrivacyVaultManager.getInstance(appContext)
        val vaultPaths = vaultManager.getVaultItems().map { it.originalPath.lowercase() }.toSet()

        val result = mutableListOf<PlaylistItemModel>()

        // 1. Local Device MediaStore videos
        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION
            )
            appContext.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = if (nameCol != -1) cursor.getString(nameCol)
                    else if (titleCol != -1) cursor.getString(titleCol)
                    else "Video_$id"
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val duration = if (durCol != -1) cursor.getLong(durCol) else 0L

                    if (path.isNotBlank() && vaultPaths.contains(path.lowercase())) continue

                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val durText = formatDuration(duration)
                    val szText = formatSize(size)

                    result.add(
                        PlaylistItemModel(
                            id = "device_vid_$id",
                            title = name ?: "Video_$id",
                            uriString = uri.toString(),
                            durationText = durText,
                            durationMs = duration,
                            sizeText = szText,
                            subtitle = if (path.isNotBlank()) File(path).parentFile?.name ?: "Videos" else "Videos",
                            mediaType = PlaylistMediaType.VIDEO
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return result
    }

    /**
     * Retrieve all available audio files that can be added to playlists.
     */
    fun getAvailableAudios(): List<PlaylistItemModel> {
        val vaultManager = PrivacyVaultManager.getInstance(appContext)
        val vaultPaths = vaultManager.getVaultItems().map { it.originalPath.lowercase() }.toSet()

        val result = mutableListOf<PlaylistItemModel>()

        // Device MediaStore Audio
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID
            )
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
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = if (titleCol != -1 && !cursor.getString(titleCol).isNullOrBlank()) cursor.getString(titleCol)
                    else if (nameCol != -1) cursor.getString(nameCol)
                    else "Audio_$id"
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val duration = if (durCol != -1) cursor.getLong(durCol) else 0L
                    val artist = if (artistCol != -1 && !cursor.getString(artistCol).isNullOrBlank() && cursor.getString(artistCol) != "<unknown>") {
                        cursor.getString(artistCol)
                    } else "Unknown Artist"
                    val album = if (albumCol != -1 && !cursor.getString(albumCol).isNullOrBlank() && cursor.getString(albumCol) != "<unknown>") {
                        cursor.getString(albumCol)
                    } else "Unknown Album"
                    val albumId = if (albumIdCol != -1) cursor.getLong(albumIdCol) else 0L

                    if (path.isNotBlank() && vaultPaths.contains(path.lowercase())) continue

                    val folderName = try {
                        if (path.isNotBlank()) {
                            File(path).parentFile?.name ?: "Music"
                        } else "Music"
                    } catch (_: Exception) {
                        "Music"
                    }

                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val durText = formatDuration(duration)
                    val szText = formatSize(size)

                    result.add(
                        PlaylistItemModel(
                            id = "device_audio_$id",
                            title = name ?: "Audio_$id",
                            uriString = uri.toString(),
                            durationText = durText,
                            durationMs = duration,
                            sizeText = szText,
                            subtitle = artist,
                            mediaType = PlaylistMediaType.AUDIO,
                            album = album,
                            artist = artist,
                            folder = folderName,
                            albumId = albumId
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return result
    }

    private fun formatDuration(ms: Long): String {
        if (ms <= 0L) return "00:00"
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        val h = m / 60
        return if (h > 0) {
            String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m % 60, s)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", m, s)
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f GB", bytes.toFloat() / (1024 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes.toFloat() / (1024 * 1024))
            bytes >= 1024 -> String.format(java.util.Locale.US, "%d KB", bytes / 1024)
            else -> "$bytes B"
        }
    }
}
