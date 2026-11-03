package com.coverit.app.detect.text

object Around {

    fun sentenceBefore(text: String, end: Int, max: Int): String {
        val window = text.substring((end - max).coerceAtLeast(0), end)
        val cut = maxOf(window.lastIndexOf(". "), window.lastIndexOf('\n'), window.lastIndexOf(';'))
        return if (cut >= 0) window.substring(cut + 1) else window
    }

    fun lineBefore(text: String, end: Int, max: Int = 40): String {
        val window = text.substring((end - max).coerceAtLeast(0), end)
        val cut = window.lastIndexOf('\n')
        return if (cut >= 0) window.substring(cut + 1) else window
    }

    fun trim(text: String, range: IntRange): IntRange {
        var first = range.first
        var last = range.last
        while (first <= last && text[first].isWhitespace()) first++
        while (last >= first && (text[last].isWhitespace() || text[last] in ".,;:/")) last--
        return first..last
    }
}
