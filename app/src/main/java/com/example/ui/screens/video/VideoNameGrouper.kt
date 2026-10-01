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

    private val containerExtensionsRegex =
        "\\.(mp4|mkv|avi|mov|webm|flv|wmv|m4v|3gp|ts)$".toRegex(RegexOption.IGNORE_CASE)
    private val delimitersRegex = "[_\\[\\]()]".toRegex()
    private val yearsRegex = "\\b(19\\d{2}|20\\d{2})\\b".toRegex()
    private val qualityLabelsRegex =
        "\\b(4K|1080p|720p|480p|BluRay|WEBRip|HDR|IMAX|Remastered|x264|x265|HEVC|AAC)\\b".toRegex(RegexOption.IGNORE_CASE)
    private val whitespaceRegex = "\\s+".toRegex()
    private val trailingDigitsRegex = "\\s+\\d+$".toRegex()
    private val tokenSplitRegex = "[\\s:,-]+".toRegex()

    /**
     * Efficiently groups videos by detecting franchise / movie series titles in O(N) time.
     * Safe for large libraries with thousands of videos.
     */
    fun groupVideosByName(videos: List<VideoItem>): List<VideoNameGroup> {
        if (videos.isEmpty()) return emptyList()

        // Fast cluster mapping using extracted group key
        val clusterMap = LinkedHashMap<String, MutableList<VideoItem>>()
        val rawPrefixMap = HashMap<String, String>()

        for (video in videos) {
            val cleanTitle = cleanFileName(video.title)
            val rawPrefix = extractPrefix(cleanTitle)
            val clusterKey = normalizeKey(rawPrefix)

            val list = clusterMap.getOrPut(clusterKey) { mutableListOf() }
            list.add(video)

            if (!rawPrefixMap.containsKey(clusterKey) || rawPrefix.length > (rawPrefixMap[clusterKey]?.length ?: 0)) {
                rawPrefixMap[clusterKey] = rawPrefix
            }
        }

        val groups = ArrayList<VideoNameGroup>(clusterMap.size)
        var index = 0

        for ((key, videoList) in clusterMap) {
            val groupTitle = if (videoList.size > 1) {
                formatTitleCase(rawPrefixMap[key] ?: key)
            } else {
                videoList.first().title
            }

            groups.add(
                VideoNameGroup(
                    id = "group_${key.replace("\\s+".toRegex(), "_")}_${index++}",
                    title = groupTitle,
                    videos = videoList.sortedBy { it.title.lowercase(Locale.ROOT) }
                )
            )
        }

        // Multi-video groups first, then alphabetically
        return groups.sortedWith(
            compareByDescending<VideoNameGroup> { it.isMultiVideo }
                .thenBy { it.title.lowercase(Locale.ROOT) }
        )
    }

    private fun cleanFileName(name: String): String {
        var clean = name
            .replace(containerExtensionsRegex, "")
            .replace(delimitersRegex, " ")
            .replace(yearsRegex, "")
            .replace(qualityLabelsRegex, "")
            .replace(whitespaceRegex, " ")
            .trim()

        return if (clean.isBlank()) name else clean
    }

    private fun extractPrefix(title: String): String {
        val colonOrDash = title.split(":", "-", "—")
        val candidate = colonOrDash.firstOrNull()?.trim() ?: title
        return candidate.replace(trailingDigitsRegex, "").trim()
    }

    private fun normalizeKey(str: String): String {
        val tokens = str.lowercase(Locale.ROOT)
            .split(tokenSplitRegex)
            .filter { it.isNotBlank() && it !in commonStopWords }

        return if (tokens.isNotEmpty()) {
            tokens.joinToString(" ")
        } else {
            str.lowercase(Locale.ROOT).replace(whitespaceRegex, " ").trim()
        }
    }

    private fun formatTitleCase(text: String): String {
        return text.split(whitespaceRegex).joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }
        }
    }
}
