package com.example.data.media

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class SearchHistoryManager private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("f2w_search_history_prefs", Context.MODE_PRIVATE)

    private val defaultHistory = listOf("muuza", "halo", "hel", "dra", "big")

    private val _historyList = MutableStateFlow(loadHistory())
    val historyList: StateFlow<List<String>> = _historyList.asStateFlow()

    private fun loadHistory(): List<String> {
        val rawJson = prefs.getString("search_history_json", null)
        if (rawJson.isNull_or_empty()) {
            // Save initial defaults
            saveHistoryToPrefs(defaultHistory)
            return defaultHistory
        }
        return try {
            val array = JSONArray(rawJson)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val item = array.optString(i)
                if (item.isNotBlank()) {
                    list.add(item)
                }
            }
            if (list.isEmpty()) defaultHistory else list
        } catch (_: Exception) {
            defaultHistory
        }
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()

    private fun saveHistoryToPrefs(list: List<String>) {
        val array = JSONArray()
        list.forEach { array.put(it) }
        prefs.edit().putString("search_history_json", array.toString()).apply()
    }

    fun addSearchQuery(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return

        val current = _historyList.value.toMutableList()
        current.removeAll { it.equals(cleanQuery, ignoreCase = true) }
        current.add(0, cleanQuery)

        // Limit history size to 20 items
        val trimmed = current.take(20)
        _historyList.value = trimmed
        saveHistoryToPrefs(trimmed)
    }

    fun removeHistoryItem(query: String) {
        val current = _historyList.value.toMutableList()
        current.removeAll { it.equals(query.trim(), ignoreCase = true) }
        _historyList.value = current
        saveHistoryToPrefs(current)
    }

    fun clearHistory() {
        _historyList.value = emptyList()
        saveHistoryToPrefs(emptyList())
    }

    companion object {
        @Volatile
        private var instance: SearchHistoryManager? = null

        fun getInstance(context: Context): SearchHistoryManager {
            return instance ?: synchronized(this) {
                instance ?: SearchHistoryManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
