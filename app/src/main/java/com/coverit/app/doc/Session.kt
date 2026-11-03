package com.coverit.app.doc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.coverit.app.detect.Kind
import com.coverit.app.pdf.Box
import com.coverit.app.pdf.PageText
import com.coverit.app.redact.Style

class Session(
    name: String,
    val source: Source,
    found: List<Mark>,
    style: Style,
    val recordId: String? = null,
) {
    var name by mutableStateOf(name)
        private set

    val marks = mutableStateListOf<Mark>().apply { addAll(found) }
    var style by mutableStateOf(style)
        private set
    var watermark by mutableStateOf<String?>(null)
        private set

    var removed by mutableStateOf<Set<Int>>(emptySet())
        private set

    val canRemovePages get() = source is PdfSource && source.pages.size > 1

    val canWatermark get() = source !is TextSource

    private var nextId = (found.maxOfOrNull { it.id } ?: 0L) + 1

    private val known = HashMap<Long, Mark>().apply { found.forEach { put(it.id, it) } }

    private class MarkState(val id: Long, val value: String, val boxes: List<Box>, val checked: Boolean, val stroke: com.coverit.app.redact.BrushLine?)
    private class Snapshot(val marks: List<MarkState>, val style: Style, val framing: Framing?, val watermark: String?, val removed: Set<Int>)

    private val undoStack = mutableStateListOf<Snapshot>()
    private val redoStack = mutableStateListOf<Snapshot>()

    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()

    val edited get() = undoStack.isNotEmpty()

    private fun snapshot() = Snapshot(marks.map { MarkState(it.id, it.value, it.boxes, it.checked, it.stroke) }, style, (source as? ImageSource)?.framing, watermark, removed)

    fun checkpoint() {
        undoStack += snapshot()
        if (undoStack.size > 200) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun undo() {
        val last = undoStack.removeLastOrNull() ?: return
        redoStack += snapshot()
        restore(last)
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack += snapshot()
        restore(next)
    }

    private fun restore(s: Snapshot) {
        val list = s.marks.mapNotNull { st ->
            known[st.id]?.also {
                it.value = st.value
                it.boxes = st.boxes
                it.checked = st.checked
                it.stroke = st.stroke
            }
        }
        if (s.framing != null) (source as? ImageSource)?.show(s.framing)
        marks.clear()
        marks.addAll(list)
        style = s.style
        watermark = s.watermark
        removed = s.removed
    }

    fun changeWatermark(text: String?) {
        val clean = text?.trim()?.takeIf { it.isNotEmpty() }
        if (clean == watermark) return
        checkpoint()
        watermark = clean
    }

    fun toggleRemoved(page: Int) {
        if (!canRemovePages) return
        val next = if (page in removed) removed - page else removed + page
        if (next.size >= source.pages.size) return
        checkpoint()
        removed = next
    }

    fun restoreFinish(watermark: String?, removed: Set<Int>) {
        this.watermark = watermark
        this.removed = if (canRemovePages) removed.filter { it in source.pages.indices }.toSet() else emptySet()
    }

    fun rename(typed: String) {
        val clean = typed.trim().ifBlank { return }
        val ext = name.substringAfterLast('.', "")
        name = if (ext.isEmpty() || clean.endsWith(".$ext", ignoreCase = true)) clean else "$clean.$ext"
    }

    fun reframe(framing: Framing, built: android.graphics.Bitmap, found: List<Mark> = emptyList(), text: PageText? = null) {
        val photo = source as? ImageSource ?: return
        val from = photo.framing
        if (framing == from) return
        checkpoint()
        photo.show(framing, built, text)
        val kept = marks.toList().mapNotNull { m ->
            val boxes = m.boxes.mapNotNull { photo.move(it, from, framing) }
            if (boxes.isEmpty()) {
                null
            } else {
                m.also {
                    it.boxes = boxes
                    it.stroke = null
                }
            }
        }
        marks.clear()
        marks.addAll(kept)
        for (f in found) {
            if (marks.none { m -> m.boxes.any { a -> f.boxes.any { b -> a.intersects(b) } } }) {
                add(Mark(nextId++, 0, f.kind, f.value, f.boxes, f.checked))
            }
        }
    }

    fun restoreFraming(framing: Framing) {
        (source as? ImageSource)?.show(framing)
    }

    private fun add(mark: Mark): Mark {
        known[mark.id] = mark
        marks += mark
        return mark
    }

    fun changeStyle(value: Style) {
        if (value == style) return
        checkpoint()
        style = value
    }

    fun toggle(mark: Mark, checked: Boolean) {
        if (mark.checked == checked) return
        checkpoint()
        mark.checked = checked
    }

    fun uncover(mark: Mark) {
        checkpoint()
        if (mark.kind == Kind.MANUAL) marks.remove(mark) else mark.checked = false
    }

    fun setKind(kinds: Set<Kind>, checked: Boolean) {
        val affected = marks.filter { it.kind in kinds && it.checked != checked }
        if (affected.isEmpty()) return
        checkpoint()
        affected.forEach { it.checked = checked }
    }

    fun replaceAll(saved: List<Mark>) {
        marks.clear()
        saved.forEach { add(it) }
        nextId = maxOf(nextId, (saved.maxOfOrNull { it.id } ?: 0L) + 1)
    }

    fun restoreStyle(value: Style) {
        style = value
    }

    val chosen get() = marks.filter { it.checked && it.page !in removed }

    val scannedPages get() = if (source is PdfSource) source.pages.filter { !it.hasText }.map { it.index } else emptyList()

    val hasText get() = source.pages.any { it.hasText }

    fun lasso(page: Int, points: FloatArray): Mark? {
        if (points.size < 4) return null
        checkpoint()
        val text = source.pages[page].text?.takeIf { source.pages[page].hasText }
        if (text != null) {
            val picked = BooleanArray(text.text.length)
            text.boxes.forEachIndexed { i, box ->
                if (box == null || text.text[i].isWhitespace()) return@forEachIndexed
                val cx = (box.left + box.right) / 2
                val cy = (box.top + box.bottom) / 2
                picked[i] = inside(points, cx, cy) || crosses(points, box)
            }
            val ranges = wordRanges(text, picked)
            if (ranges.isNotEmpty()) return select(page, text, ranges)
        }
        var bounds = Box(points[0], points[1], points[0], points[1])
        for (i in points.indices step 2) bounds = bounds.union(Box(points[i], points[i + 1], points[i], points[i + 1]))
        if (bounds.right - bounds.left < 4f && bounds.bottom - bounds.top < 4f) {
            undoStack.removeLastOrNull()
            return null
        }
        return add(Mark(nextId++, page, Kind.MANUAL, "", listOf(bounds), true))
    }

    fun wordAt(page: Int, x: Float, y: Float): Mark? {
        val text = source.pages[page].text ?: return null
        val hit = text.boxes.indexOfFirst { it != null && x in it.left..it.right && y in it.top..it.bottom }
        if (hit < 0 || text.text[hit].isWhitespace()) return null
        checkpoint()
        val picked = BooleanArray(text.text.length).also { it[hit] = true }
        return select(page, text, wordRanges(text, picked))
    }

    fun countUncovered(regex: Regex): Int = occurrences(regex).count { (page, boxes) -> covering(page, boxes)?.checked != true }

    fun coverAll(regex: Regex, kind: Kind): Int {
        val hits = occurrences(regex)
        if (hits.none { (page, boxes) -> covering(page, boxes)?.checked != true }) return 0
        checkpoint()
        var count = 0
        // Only marks from before count as already there: a box just added for one match must not
        // swallow the next match a few letters away.
        val before = marks.toList()
        for ((page, boxes, value) in hits) {
            val existing = covering(page, boxes, before)
            if (existing != null) {
                if (!existing.checked) {
                    existing.checked = true
                    count++
                }
            } else {
                add(Mark(nextId++, page, kind, value, boxes, true))
                count++
            }
        }
        return count
    }

    fun beginReshape() = checkpoint()

    fun reshape(mark: Mark, index: Int, box: Box) {
        if (index !in mark.boxes.indices) return
        mark.stroke = null
        mark.boxes = mark.boxes.toMutableList().also { it[index] = box }
        textUnder(mark.page, mark.boxes)?.let { mark.value = it }
    }

    private fun textUnder(page: Int, boxes: List<Box>): String? {
        val p = source.pages[page]
        val text = p.text?.takeIf { p.hasText } ?: return null
        val under = StringBuilder()
        text.boxes.forEachIndexed { i, b ->
            val c = text.text[i]
            val inside = b != null && boxes.any { m ->
                val cx = (b.left + b.right) / 2
                val cy = (b.top + b.bottom) / 2
                cx in m.left..m.right && cy in m.top..m.bottom
            }
            if (inside && !c.isWhitespace()) under.append(c) else if (under.isNotEmpty() && under.last() != ' ') under.append(' ')
        }
        return under.toString().trim()
    }

    fun area(page: Int, box: Box): Mark? {
        if (box.right - box.left < 3f || box.bottom - box.top < 3f) return null
        checkpoint()
        return add(Mark(nextId++, page, Kind.MANUAL, textUnder(page, listOf(box)).orEmpty(), listOf(box), true))
    }

    fun brush(page: Int, points: FloatArray, radius: Float): Mark? {
        if (points.size < 2 || radius <= 0f) return null
        val p = source.pages[page]
        val cell = radius * 0.35f
        val cols = (p.width / cell).toInt() + 1
        val rows = (p.height / cell).toInt() + 1
        val on = BooleanArray(cols * rows)
        val reach = (radius / cell).toInt() + 1
        fun dab(x: Float, y: Float) {
            val cx = (x / cell).toInt()
            val cy = (y / cell).toInt()
            for (gy in (cy - reach)..(cy + reach)) for (gx in (cx - reach)..(cx + reach)) {
                if (gx !in 0 until cols || gy !in 0 until rows) continue
                val dx = (gx + 0.5f) * cell - x
                val dy = (gy + 0.5f) * cell - y
                if (dx * dx + dy * dy <= radius * radius) on[gy * cols + gx] = true
            }
        }
        dab(points[0], points[1])
        for (i in 2 until points.size step 2) {
            val ax = points[i - 2]
            val ay = points[i - 1]
            val bx = points[i]
            val by = points[i + 1]
            val steps = (kotlin.math.hypot(bx - ax, by - ay) / (cell / 2)).toInt().coerceAtLeast(1)
            for (s in 1..steps) dab(ax + (bx - ax) * s / steps, ay + (by - ay) * s / steps)
        }
        val boxes = mutableListOf<Box>()
        var open = mutableMapOf<Pair<Int, Int>, Int>()
        for (gy in 0..rows) {
            val runs = mutableListOf<Pair<Int, Int>>()
            if (gy < rows) {
                var gx = 0
                while (gx < cols) {
                    if (!on[gy * cols + gx]) {
                        gx++
                        continue
                    }
                    val start = gx
                    while (gx < cols && on[gy * cols + gx]) gx++
                    runs += start to gx
                }
            }
            val next = mutableMapOf<Pair<Int, Int>, Int>()
            for (run in runs) next[run] = open.remove(run) ?: gy
            for ((run, top) in open) {
                boxes += Box(run.first * cell, top * cell, minOf(run.second * cell, p.width), minOf(gy * cell, p.height))
            }
            open = next
        }
        if (boxes.isEmpty()) return null
        checkpoint()
        return add(Mark(nextId++, page, Kind.MANUAL, textUnder(page, boxes).orEmpty(), boxes, true, com.coverit.app.redact.BrushLine(points, radius)))
    }

    fun coverPage(page: Int): Mark? {
        val p = source.pages[page]
        return area(page, Box(0f, 0f, p.width, p.height))
    }

    private fun wholePage(page: Int, box: Box): Boolean {
        val p = source.pages[page]
        return box.left <= 0.5f && box.top <= 0.5f && box.right >= p.width - 0.5f && box.bottom >= p.height - 0.5f
    }

    fun pageCovered(page: Int): Boolean = marks.any { m -> m.page == page && m.checked && m.boxes.any { wholePage(page, it) } }

    fun uncoverPage(page: Int) {
        val whole = marks.filter { m -> m.page == page && m.boxes.any { wholePage(page, it) } }
        if (whole.isEmpty()) return
        checkpoint()
        marks.removeAll(whole)
    }

    fun remove(mark: Mark) {
        checkpoint()
        marks.remove(mark)
    }

    fun setAll(checked: Boolean) {
        if (marks.none { it.checked != checked }) return
        checkpoint()
        marks.forEach { it.checked = checked }
    }

    private data class Hit(val page: Int, val boxes: List<Box>, val value: String)

    private fun occurrences(regex: Regex): List<Hit> = source.pages.mapNotNull { it.text }.flatMap { t ->
        regex.findAll(t.text).mapNotNull { m ->
            val boxes = t.boxesFor(m.range.first, m.range.last + 1).map { it.padded(1.5f) }
            if (boxes.isEmpty()) null else Hit(t.index, boxes, m.value.trim())
        }.toList()
    }

    private fun covering(page: Int, boxes: List<Box>, among: List<Mark> = marks) =
        among.firstOrNull { m -> m.page == page && m.boxes.any { a -> boxes.any { b -> sameSpot(a, b) } } }

    private fun sameSpot(a: Box, b: Box): Boolean {
        val w = minOf(a.right, b.right) - maxOf(a.left, b.left)
        val h = minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)
        return w > 0f && h >= minOf(a.bottom - a.top, b.bottom - b.top) * 0.5f
    }

    private fun select(page: Int, text: PageText, ranges: List<IntRange>): Mark? {
        val boxes = ranges.flatMap { text.boxesFor(it.first, it.last + 1) }.map { it.padded(1.5f) }
        if (boxes.isEmpty()) return null
        val value = ranges.joinToString(" ") { text.text.substring(it.first, it.last + 1) }.replace(Regex("\\s+"), " ").trim()
        val existing = covering(page, boxes)
        if (existing != null && existing.boxes.size == boxes.size && existing.value.equals(value, ignoreCase = true)) {
            existing.checked = true
            return existing
        }
        return add(Mark(nextId++, page, Kind.MANUAL, value, boxes, true))
    }

    private fun wordRanges(text: PageText, picked: BooleanArray): List<IntRange> {
        val s = text.text
        val grown = picked.copyOf()
        for (i in s.indices) {
            if (!picked[i]) continue
            var a = i
            while (a > 0 && !s[a - 1].isWhitespace() && text.boxes[a - 1] != null) a--
            var b = i
            while (b < s.length - 1 && !s[b + 1].isWhitespace() && text.boxes[b + 1] != null) b++
            for (k in a..b) grown[k] = true
        }
        val out = mutableListOf<IntRange>()
        var start = -1
        var lastPicked = -1
        for (i in 0..s.length) {
            val on = i < s.length && grown[i]
            if (on) {
                if (start < 0) start = i
                lastPicked = i
            } else if (start >= 0 && (i == s.length || !s[i].isWhitespace() || s[i] == '\n')) {
                out += start..lastPicked
                start = -1
            }
        }
        return out
    }

    private fun inside(points: FloatArray, x: Float, y: Float): Boolean {
        var hit = false
        val n = points.size / 2
        var j = n - 1
        for (i in 0 until n) {
            val xi = points[i * 2]
            val yi = points[i * 2 + 1]
            val xj = points[j * 2]
            val yj = points[j * 2 + 1]
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) hit = !hit
            j = i
        }
        return hit
    }

    private fun crosses(points: FloatArray, box: Box): Boolean {
        for (i in points.indices step 2) {
            if (points[i] in box.left..box.right && points[i + 1] in box.top..box.bottom) return true
        }
        return false
    }
}
