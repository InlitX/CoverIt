package com.coverit.app.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.coverit.app.image.Metadata
import com.coverit.app.redact.Cover
import com.coverit.app.redact.Painter
import com.coverit.app.redact.Style
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.cos.COSStream
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.RenderingMode
import com.tom_roush.pdfbox.util.Matrix
import java.io.File

object Redactor {

    private const val DPI = 200f
    private const val JPEG_QUALITY = 0.88f

    private val pageKeysToDrop = listOf("Annots", "AA", "Metadata", "PieceInfo", "B", "Thumb")

    fun redact(source: File, pages: List<PageText>, redactions: Map<Int, List<Cover>>, style: Style, target: File, watermark: String? = null, skip: Set<Int> = emptySet()) {
        PDDocument.load(source).use { src ->
            ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    PDDocument().use { out ->
                        for (page in pages) {
                            if (page.index in skip) continue
                            val boxes = redactions[page.index].orEmpty()
                            if (boxes.isEmpty() && watermark == null) {
                                copyPage(out, src.getPage(page.index))
                            } else {
                                flattenPage(out, renderer, page, boxes, style, watermark)
                            }
                        }
                        out.save(target)
                    }
                }
            }
        }
    }

    private fun copyPage(out: PDDocument, page: PDPage) {
        val copy = out.importPage(page)
        val dict = copy.cosObject
        pageKeysToDrop.forEach { dict.removeItem(COSName.getPDFName(it)) }
        val resources = copy.resources ?: return
        for (name in resources.xObjectNames) {
            val xObject = resources.getXObject(name)
            if (xObject is PDImageXObject) {
                xObject.cosObject.removeItem(COSName.METADATA)
                stripJpeg(xObject.cosObject)
            }
        }
    }

    private fun stripJpeg(stream: COSStream) {
        if (stream.filters != COSName.DCT_DECODE) return
        runCatching {
            val raw = stream.createRawInputStream().use { it.readBytes() }
            val clean = Metadata.strip(raw)
            if (clean.size < raw.size) stream.createRawOutputStream().use { it.write(clean) }
        }
    }

    private fun flattenPage(out: PDDocument, renderer: PdfRenderer, page: PageText, covers: List<Cover>, style: Style, watermark: String?) {
        val scale = DPI / 72f
        val bitmap = Bitmap.createBitmap(
            (page.width * scale).toInt(),
            (page.height * scale).toInt(),
            Bitmap.Config.ARGB_8888,
        )
        bitmap.eraseColor(Color.WHITE)
        renderer.openPage(page.index).use {
            it.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
        }
        covers.forEach { Painter.cover(bitmap, it, scale, style) }
        watermark?.let { Painter.watermark(bitmap, it) }
        val boxes = covers.flatMap { it.cells }

        val newPage = PDPage(PDRectangle(page.width, page.height))
        out.addPage(newPage)
        val image = JPEGFactory.createFromImage(out, bitmap, JPEG_QUALITY)
        bitmap.recycle()

        PDPageContentStream(out, newPage).use { cs ->
            cs.drawImage(image, 0f, 0f, page.width, page.height)
            val font = PDType1Font.HELVETICA
            cs.beginText()
            cs.setRenderingMode(RenderingMode.NEITHER)
            for ((word, box) in page.words()) {
                if (boxes.any { it.intersects(box) }) continue
                val text = encodable(font, word)
                val size = box.height * 0.8f
                val natural = font.getStringWidth(text) / 1000f * size
                if (natural <= 0f) continue
                cs.setFont(font, size)
                cs.setHorizontalScaling(100f * (box.right - box.left) / natural)
                cs.setTextMatrix(Matrix.getTranslateInstance(box.left, page.height - (box.bottom - box.height * 0.2f)))
                cs.showText(text)
            }
            cs.endText()
        }
    }

    private fun encodable(font: PDFont, word: String) = buildString {
        for (c in word) {
            val s = c.toString()
            append(if (runCatching { font.encode(s) }.isSuccess) s else "?")
        }
    }
}
