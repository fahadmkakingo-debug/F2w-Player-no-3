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
        "the", "a", "an", "and", "or", "of", "in", "on", "at", "to", "for", "with"
    )

    private val containerExtensionsRegex =
        "\\.(mp4|mkv|avi|mov|webm|flv|wmv|m4v|3gp|ts)$".toRegex(RegexOption.IGNORE_CASE)
    private val delimitersRegex = "[_\\[\\]()]".toRegex()
    private val yearsRegex = "\\b(19\\d{2}|20\\d{2})\\b".toRegex()
    private val qualityLabelsRegex =
        "\\b(4K|2160p|1080p|720p|480p|BluRay|WEBRip|HDR|IMAX|Remastered|x264|x265|HEVC|AAC|WEB-DL|HDTV)\\b".toRegex(RegexOption.IGNORE_CASE)

    // Regex to match season/episode/part/number markers (e.g. S01 Ep01, Ep 03, Episode 4, Part 2, No 1, 01, etc.)
    private val seasonEpisodePartRegex =
        "\\b(s\\d{1,2}\\s*e\\s*p?\\d{1,3}|s\\d{1,2}|e\\s*p?\\d{1,3}|ep\\s*\\d+|episode\\s*\\d+|season\\s*\\d+|series\\s*\\d+|part\\s*\\d+|pt\\s*\\d+|ch\\s*\\d+|chapter\\s*\\d+|vol\\s*\\d+|volume\\s*\\d+|no\\.?\\s*\\d+|number\\s*\\d+)\\b".toRegex(RegexOption.IGNORE_CASE)

    private val trailingDigitsOrNumbersRegex = "(\\s+\\d+|[-_:]\\s*\\d+)$".toRegex()
    private val isolatedNumbersRegex = "\\b\\d+\\b".toRegex()
    private val whitespaceRegex = "\\s+".toRegex()
    private val tokenSplitRegex = "[\\s:,-]+".toRegex()

    /**
     * Groups videos by franchise / series / common root name in O(N) linear time using prefix buckets.
     * Prevents UI thread hangs and ANR on libraries with thousands of videos.
     * Preserves natural alphabetical order ("dunia 01", "dunia 02", "dunia 03") inside each group.
     */
    fun groupVideosByName(videos: List<VideoItem>): List<VideoNameGroup> {
        if (videos.isEmpty()) return emptyList()

        // 1. Prepare metadata for each video
        val metadataList = videos.map { video ->
            val cleanTitle = cleanFileName(video.title)
            val baseFranchise = stripSeasonEpisodeAndNumbers(cleanTitle)
            val clusterKey = extractClusterKey(cleanTitle, baseFranchise)
            val rawPrefix = extractPrefixBeforeDelimiter(cleanTitle)
            VideoItemMeta(
                video = video,
                cleanTitle = cleanTitle,
                baseFranchise = baseFranchise,
                clusterKey = clusterKey,
                rawPrefix = rawPrefix
            )
        }

        // 2. High-performance prefix bucketing:
        // Instead of quadratic O(N^2) pairwise checks across the entire library,
        // partition candidates into small prefix buckets.
        val buckets = LinkedHashMap<String, MutableList<VideoItemMeta>>()
        for (item in metadataList) {
            val key = when {
                item.clusterKey.length >= 3 -> item.clusterKey.take(3).lowercase(Locale.ROOT)
                item.cleanTitle.length >= 3 -> item.cleanTitle.take(3).lowercase(Locale.ROOT)
                else -> item.cleanTitle.lowercase(Locale.ROOT)
            }
            buckets.getOrPut(key) { mutableListOf() }.add(item)
        }

        // 3. Cluster items within buckets
        val rawGroups = mutableListOf<MutableList<VideoItemMeta>>()
        for ((_, bucketItems) in buckets) {
            val visited = BooleanArray(bucketItems.size)
            for (i in bucketItems.indices) {
                if (visited[i]) continue
                visited[i] = true

                val current = bucketItems[i]
                val currentGroup = mutableListOf(current)

                for (j in (i + 1) until bucketItems.size) {
                    if (visited[j]) continue
                    val candidate = bucketItems[j]

                    if (areItemsInSameGroup(current, candidate)) {
                        visited[j] = true
                        currentGroup.add(candidate)
                    }
                }

                rawGroups.add(currentGroup)
            }
        }

        // 4. Construct VideoNameGroup objects with proper titles and Natural Alphabetical Sorting
        val result = mutableListOf<VideoNameGroup>()
        var index = 0

        for (groupItems in rawGroups) {
            val videoList = groupItems.map { it.video }
            val groupTitle = if (groupItems.size > 1) {
                deriveGroupTitle(groupItems)
            } else {
                groupItems.first().video.title
            }

            val groupId = "group_${groupTitle.lowercase(Locale.ROOT).replace("\\s+".toRegex(), "_")}_${index++}"

            result.add(
                VideoNameGroup(
                    id = groupId,
                    title = groupTitle,
                    videos = videoList.sortedWith { a, b ->
                        com.example.util.media.NaturalOrderComparator.compare(a.title, b.title)
                    }
                )
            )
        }

        // 5. Sort multi-video collections first, then alphabetically
        return result.sortedWith(
            compareByDescending<VideoNameGroup> { it.isMultiVideo }
                .thenBy { it.title.lowercase(Locale.ROOT) }
        )
    }

    /**
     * Determines whether two video titles belong to the same franchise group.
     */
    private fun areItemsInSameGroup(a: VideoItemMeta, b: VideoItemMeta): Boolean {
        // 1. Direct cluster key match (e.g. "adulting", "dune", "dunia", "avatar")
        if (a.clusterKey.isNotBlank() && a.clusterKey.length >= 3 && a.clusterKey == b.clusterKey) {
            return true
        }

        // 2. Base franchise title match (e.g. "Spider Man", "Adulting S01")
        if (a.baseFranchise.isNotBlank() && a.baseFranchise.length >= 3 &&
            a.baseFranchise.equals(b.baseFranchise, ignoreCase = true)
        ) {
            return true
        }

        // 3. Raw prefix before colon/dash match (e.g. "Spider-Man: Into the Spider-Verse" and "Spider-Man: Across the Spider-Verse")
        if (a.rawPrefix.isNotBlank() && a.rawPrefix.length >= 3 &&
            a.rawPrefix.equals(b.rawPrefix, ignoreCase = true)
        ) {
            return true
        }

        // 4. Shared leading alphabetic characters (at least 3-4 letters matching, e.g. "babaya" and "baba" -> common prefix "baba")
        val cleanA = a.cleanTitle.lowercase(Locale.ROOT).replace(whitespaceRegex, "")
        val cleanB = b.cleanTitle.lowercase(Locale.ROOT).replace(whitespaceRegex, "")
        val commonPrefix = cleanA.commonPrefixWith(cleanB)

        if (commonPrefix.length >= 3) {
            if (commonPrefix.length >= 4 ||
                commonPrefix.length >= cleanA.takeWhile { it.isLetter() }.length ||
                commonPrefix.length >= cleanB.takeWhile { it.isLetter() }.length
            ) {
                return true
            }
        }

        // 5. First substantive token starts with common 3+ letters
        val tokenA = a.clusterKey.split(" ").firstOrNull() ?: ""
        val tokenB = b.clusterKey.split(" ").firstOrNull() ?: ""
        if (tokenA.length >= 3 && tokenB.length >= 3) {
            val tokenCommon = tokenA.commonPrefixWith(tokenB)
            if (tokenCommon.length >= 3 && (tokenCommon == tokenA || tokenCommon == tokenB || tokenCommon.length >= 4)) {
                return true
            }
        }

        return false
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

    private fun stripSeasonEpisodeAndNumbers(title: String): String {
        val withoutDelim = title.split(":", "-", "—").firstOrNull()?.trim() ?: title
        var stripped = withoutDelim
            .replace(seasonEpisodePartRegex, "")
            .replace(trailingDigitsOrNumbersRegex, "")
            .replace(isolatedNumbersRegex, "")
            .replace(whitespaceRegex, " ")
            .trim()

        return if (stripped.isBlank()) withoutDelim else stripped
    }

    private fun extractClusterKey(cleanTitle: String, baseFranchise: String): String {
        val target = if (baseFranchise.isNotBlank()) baseFranchise else cleanTitle
        val tokens = target.lowercase(Locale.ROOT)
            .split(tokenSplitRegex)
            .filter { it.isNotBlank() && it !in commonStopWords && !it.matches("\\d+".toRegex()) }

        return if (tokens.isNotEmpty()) {
            if (tokens.size >= 2 && tokens[0].length < 3) {
                "${tokens[0]} ${tokens[1]}"
            } else {
                tokens.first()
            }
        } else {
            target.lowercase(Locale.ROOT).take(5).trim()
        }
    }

    private fun extractPrefixBeforeDelimiter(title: String): String {
        val colonOrDash = title.split(":", "-", "—")
        val candidate = colonOrDash.firstOrNull()?.trim() ?: title
        return candidate.replace(trailingDigitsOrNumbersRegex, "").trim()
    }

    private fun deriveGroupTitle(items: List<VideoItemMeta>): String {
        val firstBase = items.first().baseFranchise
        if (firstBase.isNotBlank() && items.all { it.baseFranchise.equals(firstBase, ignoreCase = true) }) {
            return formatTitleCase(firstBase)
        }

        val firstPrefix = items.first().rawPrefix
        if (firstPrefix.isNotBlank() && items.all { it.rawPrefix.equals(firstPrefix, ignoreCase = true) }) {
            return formatTitleCase(firstPrefix)
        }

        val titles = items.map { it.cleanTitle }
        val commonPrefix = titles.reduce { acc, s -> acc.commonPrefixWith(s) }
            .trim()
            .replace("[-:_\\s]+$".toRegex(), "")

        if (commonPrefix.length >= 3) {
            return formatTitleCase(commonPrefix)
        }

        val fallback = if (firstBase.isNotBlank()) firstBase else items.first().clusterKey
        return formatTitleCase(fallback)
    }

    private fun formatTitleCase(text: String): String {
        return text.split(whitespaceRegex).joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }
        }
    }

    private data class VideoItemMeta(
        val video: VideoItem,
        val cleanTitle: String,
        val baseFranchise: String,
        val clusterKey: String,
        val rawPrefix: String
    )
}
