package com.coverit.app.pdf

class PageText(
    val index: Int,
    val width: Float,
    val height: Float,
    val text: String,
    val boxes: List<Box?>,
) {

    fun boxesFor(start: Int, end: Int): List<Box> {
        val lines = mutableListOf<Box>()
        for (i in start until end) {
            val box = boxes[i] ?: continue
            val last = lines.lastOrNull()
            if (last != null && sameLine(last, box)) {
                lines[lines.size - 1] = last.union(box)
            } else {
                lines += box
            }
        }
        return lines
    }

    fun words(): List<Pair<String, Box>> {
        val out = mutableListOf<Pair<String, Box>>()
        var start = -1
        for (i in 0..text.length) {
            val inWord = i < text.length && boxes[i] != null && !text[i].isWhitespace()
            if (inWord && start < 0) start = i
            if (!inWord && start >= 0) {
                boxesFor(start, i).firstOrNull()?.let { out += text.substring(start, i) to it }
                start = -1
            }
        }
        return out
    }

    private fun sameLine(a: Box, b: Box) =
        b.left >= a.left - 1f && kotlin.math.abs(a.bottom - b.bottom) < maxOf(a.height, b.height) / 2
}
