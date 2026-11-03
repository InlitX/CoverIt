package com.coverit.app.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition

object TextExtractor {

    fun extract(doc: PDDocument): List<PageText> =
        (0 until doc.numberOfPages).map { extractPage(doc, it) }

    private fun extractPage(doc: PDDocument, index: Int): PageText {
        val page = doc.getPage(index)
        val crop = page.cropBox
        val rotated = page.rotation % 180 != 0
        val width = if (rotated) crop.height else crop.width
        val height = if (rotated) crop.width else crop.height
        val collector = Collector()
        collector.startPage = index + 1
        collector.endPage = index + 1
        collector.getText(doc)
        return PageText(index, width, height, collector.text.toString(), collector.boxes)
    }

    private class Collector : PDFTextStripper() {

        val text = StringBuilder()
        val boxes = mutableListOf<Box?>()

        init {
            sortByPosition = true
        }

        override fun writeString(string: String, textPositions: MutableList<TextPosition>) {
            for (tp in textPositions) {
                val glyphHeight = maxOf(tp.height, tp.fontSizeInPt * 0.8f)
                val box = Box(tp.x, tp.y - glyphHeight, tp.x + tp.width, tp.y + glyphHeight * 0.25f)
                for (c in tp.unicode ?: continue) {
                    text.append(c)
                    boxes += box
                }
            }
        }

        override fun writeWordSeparator() = separator(' ')

        override fun writeLineSeparator() = separator('\n')

        override fun writePageEnd() = separator('\n')

        private fun separator(c: Char) {
            text.append(c)
            boxes += null
        }
    }
}
