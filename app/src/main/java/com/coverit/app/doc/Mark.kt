package com.coverit.app.doc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.coverit.app.detect.Kind
import com.coverit.app.pdf.Box
import com.coverit.app.redact.BrushLine
import com.coverit.app.redact.Cover

@androidx.compose.runtime.Stable
class Mark(
    val id: Long,
    val page: Int,
    val kind: Kind,
    value: String,
    boxes: List<Box>,
    checked: Boolean,
    stroke: BrushLine? = null,
) {
    var checked by mutableStateOf(checked)

    var value by mutableStateOf(value)

    var boxes by mutableStateOf(boxes)

    var stroke by mutableStateOf(stroke)

    fun covers(label: String): List<Cover> {
        val line = stroke
        if (line == null || boxes.isEmpty()) return Cover.of(boxes, label)
        return listOf(Cover(boxes.reduce { a, b -> a.union(b) }, "", line, boxes))
    }
}

object Masking {

    fun mask(value: String): String = value.trim().split(Regex("\\s+")).joinToString(" ") { word ->
        when {
            word.length <= 2 -> "•".repeat(word.length)
            else -> word.first() + "•".repeat(word.length - 2) + word.last()
        }
    }
}
