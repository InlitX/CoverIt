package com.coverit.app.doc

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.coverit.app.office.OfficeDoc
import com.coverit.app.pdf.Box
import com.coverit.app.pdf.PageText
import java.io.File

class TextSource(file: File, type: DocType, val doc: OfficeDoc) : Source(file, type) {

    private val layout = DocLayout(doc.text, monospace = type.monospace)

    override val pages: List<Page> = layout.pages.mapIndexed { i, p -> Page(i, PAGE_W, PAGE_H, p.text) }

    override fun render(page: Int, widthPx: Int): Bitmap = layout.render(page, widthPx)

    fun pageStart(page: Int) = layout.pages[page].start

    override fun close() = Unit

    companion object {
        const val PAGE_W = 595f
        const val PAGE_H = 842f
    }
}

class DocLayout(text: String, monospace: Boolean) {

    class LaidPage(val start: Int, val firstLine: Int, val lastLine: Int, val text: PageText)

    private val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(28, 28, 30)
        textSize = FONT * SCALE
        typeface = if (monospace) Typeface.MONOSPACE else Typeface.SANS_SERIF
    }

    private val inner = ((TextSource.PAGE_W - 2 * MARGIN) * SCALE).toInt()

    private val layout: StaticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, inner)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, 1.18f)
        .setIncludePad(false)
        .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
        .build()

    val pages: List<LaidPage> = cut(text)

    private fun cut(text: String): List<LaidPage> {
        val out = mutableListOf<LaidPage>()
        val usable = (TextSource.PAGE_H - 2 * MARGIN) * SCALE
        var first = 0
        val count = layout.lineCount
        while (first < count) {
            val top = layout.getLineTop(first)
            var last = first
            while (last + 1 < count && layout.getLineBottom(last + 1) - top <= usable) last++
            out += LaidPage(layout.getLineStart(first), first, last, pageText(out.size, text, first, last))
            first = last + 1
        }
        if (out.isEmpty()) out += LaidPage(0, 0, -1, PageText(0, TextSource.PAGE_W, TextSource.PAGE_H, "", emptyList()))
        return out
    }

    private fun pageText(index: Int, text: String, first: Int, last: Int): PageText {
        val start = layout.getLineStart(first)
        val end = layout.getLineEnd(last)
        val top = layout.getLineTop(first)
        val boxes = ArrayList<Box?>(end - start)
        for (line in first..last) {
            val lineStart = layout.getLineStart(line)
            val lineEnd = layout.getLineEnd(line)
            val y0 = (layout.getLineTop(line) - top) / SCALE + MARGIN
            val y1 = y0 + (layout.getLineDescent(line) - layout.getLineAscent(line)) / SCALE
            for (i in lineStart until lineEnd) {
                val c = text[i]
                if (c == '\n') {
                    boxes += null
                    continue
                }
                val x0 = layout.getPrimaryHorizontal(i)
                val x1 = if (i + 1 < lineEnd) layout.getPrimaryHorizontal(i + 1) else layout.getLineRight(line)
                val left = minOf(x0, x1) / SCALE + MARGIN
                val right = maxOf(x0, x1).coerceAtLeast(minOf(x0, x1) + 1f) / SCALE + MARGIN
                boxes += Box(left, y0, right, y1)
            }
        }
        return PageText(index, TextSource.PAGE_W, TextSource.PAGE_H, text.substring(start, end), boxes)
    }

    fun render(page: Int, widthPx: Int): Bitmap {
        val p = pages[page]
        val scale = widthPx / TextSource.PAGE_W
        val height = (TextSource.PAGE_H * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        if (p.lastLine < p.firstLine) return bitmap
        val canvas = Canvas(bitmap)
        val top = layout.getLineTop(p.firstLine).toFloat()
        val bottom = layout.getLineBottom(p.lastLine).toFloat()
        canvas.scale(scale, scale)
        canvas.translate(MARGIN, MARGIN)
        canvas.scale(1f / SCALE, 1f / SCALE)
        canvas.translate(0f, -top)
        canvas.clipRect(0f, top, inner.toFloat(), bottom)
        layout.draw(canvas)
        return bitmap
    }

    companion object {
        private const val MARGIN = 40f
        private const val FONT = 12f
        private const val SCALE = 4f
    }
}
