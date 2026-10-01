package com.example.ui.screens.video

import java.util.Locale

data class VideoNameGroup(
    val id: String,
    val title: String,
    val videos: List<VideoItem>
) {
    val count: Int get() = videos.size
    val isMultiVideo: Boolean get() = videos.size > 1
    val primaryVideo: VideoItem? get() = videos.firstOrNull()
}

object VideoNameGrouper {

    private val commonStopWords = setOf(
        "the", "a", "an", "and", "or", "of", "in", "on", "at", "to", "for", "with",
        "part", "pt", "chapter", "ch", "vol", "volume", "season", "episode", "ep",
        "remastered", "edition", "extended", "cut", "special", "version", "imax",
        "4k", "1080p", "720p", "hdr", "bluray", "webrip", "x264", "x265", "hevc"
    )

    /**
     * Automatically groups a list of videos by detecting common / similar movie names
     * from their titles and filenames.
     * Group names are derived automatically from the common root name.
     * Videos without matches remain as individual groups.
     */
    fun groupVideosByName(videos: List<VideoItem>): List<VideoNameGroup> {
        if (videos.isEmpty()) return emptyList()

        val processedItems = videos.map { video ->
            val cleanTitle = cleanFileName(video.title)
            val tokens = tokenizeTitle(cleanTitle)
            VideoTitleMetadata(video, cleanTitle, tokens)
        }

        val visited = BooleanArray(processedItems.size)
        val groups = mutableListOf<VideoNameGroup>()

        for (i in processedItems.indices) {
            if (visited[i]) continue

            val current = processedItems[i]
            val matchedIndices = mutableListOf(i)

            for (j in (i + 1) until processedItems.size) {
                if (visited[j]) continue
                val candidate = processedItems[j]

                if (areTitlesSimilar(current, candidate)) {
                    matchedIndices.add(j)
                }
            }

            // Mark all matched as visited
            matchedIndices.forEach { visited[it] = true }

            val groupVideos = matchedIndices.map { processedItems[it].originalVideo }

            val groupTitle = if (groupVideos.size > 1) {
                deriveCommonGroupTitle(matchedIndices.map { processedItems[it] })
            } else {
                current.originalVideo.title
            }

            groups.add(
                VideoNameGroup(
                    id = "group_${groupTitle.lowercase().replace("\\s+".toRegex(), "_")}_$i",
                    title = groupTitle,
                    videos = groupVideos.sortedBy { it.title.lowercase() }
                )
            )
        }

        // Sort groups: multi-video collections first, then alphabetically
        return groups.sortedWith(
            compareByDescending<VideoNameGroup> { it.isMultiVideo }
                .thenBy { it.title.lowercase() }
        )
    }

    /**
     * Cleans common container extensions, brackets, year tags, and quality labels.
     */
    private fun cleanFileName(name: String): String {
        var clean = name
            .replace("\\.(mp4|mkv|avi|mov|webm|flv|wmv|m4v|3gp|ts)$".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("[_\\[\\]()]".toRegex(), " ")
            .replace("\\b(19\\d{2}|20\\d{2})\\b".toRegex(), "") // Remove years (e.g. 2023, 2024)
            .replace("\\b(4K|1080p|720p|480p|BluRay|WEBRip|HDR|IMAX|Remastered|x264|x265|HEVC|AAC)\\b".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("\\s+".toRegex(), " ")
            .trim()

        return if (clean.isBlank()) name else clean
    }

    private fun tokenizeTitle(cleanTitle: String): List<String> {
        return cleanTitle
            .lowercase(Locale.ROOT)
            .split("[\\s:,-]+".toRegex())
            .filter { it.isNotBlank() && it !in commonStopWords }
    }

    /**
     * Determines whether two video titles belong to the same franchise / movie group.
     */
    private fun areTitlesSimilar(a: VideoTitleMetadata, b: VideoTitleMetadata): Boolean {
        // 1. Direct prefix match before delimiter (e.g. "Avatar: The Way of Water" & "Avatar 1" -> "Avatar")
        val prefixA = extractPrefix(a.cleanTitle)
        val prefixB = extractPrefix(b.cleanTitle)
        if (prefixA.length >= 3 && prefixA.equals(prefixB, ignoreCase = true)) {
            return true
        }

        // 2. Token overlap: If the first 1 or 2 significant words match
        if (a.tokens.isNotEmpty() && b.tokens.isNotEmpty()) {
            if (a.tokens.first() == b.tokens.first()) {
                val firstWord = a.tokens.first()
                // If it's a substantive word (e.g. "avatar", "dune", "spiderman", "batman", "cyberpunk")
                if (firstWord.length >= 4) {
                    return true
                }
                // If two leading words match (e.g. "star wars", "spider man", "john wick")
                if (a.tokens.size >= 2 && b.tokens.size >= 2 && a.tokens[1] == b.tokens[1]) {
                    return true
                }
            }
        }

        // 3. Normalized string starting with common prefix (e.g. "Avatar 1" and "Avatar 2")
        val normA = normalizeForPrefix(a.cleanTitle)
        val normB = normalizeForPrefix(b.cleanTitle)
        val commonPrefix = normA.commonPrefixWith(normB).trim()
        if (commonPrefix.length >= 4 && (commonPrefix.length >= normA.length * 0.4f || commonPrefix.length >= normB.length * 0.4f)) {
            return true
        }

        return false
    }

    private fun extractPrefix(title: String): String {
        val parts = title.split(":", "-", "—")
        return parts.firstOrNull()?.trim()?.replace("\\s+\\d+$".toRegex(), "") ?: title
    }

    private fun normalizeForPrefix(str: String): String {
        return str.lowercase(Locale.ROOT).replace("\\s+".toRegex(), " ")
    }

    /**
     * Automatically extracts the common group title from a list of matched videos.
     */
    private fun deriveCommonGroupTitle(matches: List<VideoTitleMetadata>): String {
        // Check for common prefix before colon/dash
        val firstPrefix = extractPrefix(matches.first().cleanTitle)
        if (matches.all { extractPrefix(it.cleanTitle).equals(firstPrefix, ignoreCase = true) }) {
            return formatTitleCase(firstPrefix)
        }

        // Look for common tokens
        val tokenLists = matches.map { it.tokens }
        val commonTokens = mutableListOf<String>()
        val firstTokens = tokenLists.first()

        for (i in firstTokens.indices) {
            val token = firstTokens[i]
            if (tokenLists.all { it.size > i && it[i] == token }) {
                commonTokens.add(token)
            } else {
                break
            }
        }

        if (commonTokens.isNotEmpty()) {
            return commonTokens.joinToString(" ") { formatTitleCase(it) }
        }

        // Fallback: longest common substring of clean titles
        val longestCommon = matches.map { it.cleanTitle }
            .reduce { acc, s -> acc.commonPrefixWith(s) }
            .trim()
            .replace("[-:\\s]+$".toRegex(), "")

        return if (longestCommon.length >= 3) {
            formatTitleCase(longestCommon)
        } else {
            formatTitleCase(firstPrefix)
        }
    }

    private fun formatTitleCase(text: String): String {
        return text.split("\\s+".toRegex()).joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }
        }
    }

    private data class VideoTitleMetadata(
        val originalVideo: VideoItem,
        val cleanTitle: String,
        val tokens: List<String>
    )
}
