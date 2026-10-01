package com.example.util.media

import java.util.Comparator

object NaturalOrderComparator : Comparator<String> {

    private val chunkRegex = "(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)".toRegex()

    override fun compare(s1: String?, s2: String?): Int {
        if (s1 == null && s2 == null) return 0
        if (s1 == null) return -1
        if (s2 == null) return 1

        val chunks1 = s1.split(chunkRegex)
        val chunks2 = s2.split(chunkRegex)

        val minSize = minOf(chunks1.size, chunks2.size)
        for (i in 0 until minSize) {
            val c1 = chunks1[i]
            val c2 = chunks2[i]

            val n1 = c1.toLongOrNull()
            val n2 = c2.toLongOrNull()

            val result = if (n1 != null && n2 != null) {
                n1.compareTo(n2)
            } else {
                c1.compareTo(c2, ignoreCase = true)
            }

            if (result != 0) return result
        }

        return chunks1.size.compareTo(chunks2.size)
    }
}
