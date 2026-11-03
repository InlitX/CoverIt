package com.coverit.app.doc

import android.graphics.RectF
import com.coverit.app.detect.Kind
import com.coverit.app.image.CodeScanner
import com.coverit.app.image.FaceFinder
import com.coverit.app.image.Images
import com.coverit.app.image.Ocr
import com.coverit.app.office.OfficeDoc
import com.coverit.app.pdf.Box
import com.coverit.app.pdf.PageImages
import com.coverit.app.pdf.PageText
import com.coverit.app.pdf.TextExtractor
import com.coverit.app.redact.Style
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File

object Loader {

    private const val SCAN_WIDTH = 1100
    private const val IMAGE_SCAN_SIDE = 2000
    private const val OCR_WIDTH = 1700
    private const val MAX_OCR_PAGES = 12

    fun load(file: File, type: DocType, name: String, config: DetectionConfig, style: Style, recordId: String? = null): Session {
        val s = when (type.family) {
            Family.PDF -> loadPdf(file, name, config, style)
            Family.IMAGE -> loadImage(file, type, name, config, style)
            Family.OFFICE -> loadOffice(file, type, name, config)
        }
        return if (recordId == null) s else Session(s.name, s.source, s.marks.toList(), s.style, recordId)
    }

    private fun findText(texts: List<PageText>, config: DetectionConfig, firstId: Long = 0L): MutableList<Mark> {
        var id = firstId
        val marks = mutableListOf<Mark>()
        val names = config.namesIn(texts.map { it.text })
        for (t in texts) {
            for (f in config.find(t.text, names)) {
                val boxes = t.boxesFor(f.start, f.end).map { it.padded(1.5f) }
                if (boxes.isNotEmpty()) marks += Mark(id++, t.index, f.kind, f.value.trim(), boxes, f.covered)
            }
        }
        return marks
    }

    private fun loadOffice(file: File, type: DocType, name: String, config: DetectionConfig): Session {
        val source = TextSource(file, type, OfficeDoc.open(file, type.office!!))
        val marks = findText(source.pages.map { it.text!! }, config)
        return Session(name, source, marks, Style.BLACK)
    }

    private fun loadPdf(file: File, name: String, config: DetectionConfig, style: Style): Session {
        val (extracted, pictured) = PDDocument.load(file).use { TextExtractor.extract(it) to PageImages.pages(it) }
        val reader = PdfSource(file, extracted.map { Page(it.index, it.width, it.height, it) })
        var scanned = 0
        val texts = extracted.map { t ->
            if (t.text.any { it.isLetterOrDigit() } || scanned >= MAX_OCR_PAGES || !config.searches) return@map t
            scanned++
            val bitmap = reader.render(t.index, OCR_WIDTH)
            val read = Ocr.read(bitmap, t.index, t.width / bitmap.width)
            bitmap.recycle()
            read ?: t
        }
        reader.close()
        val pages = texts.map { Page(it.index, it.width, it.height, it) }
        val source = PdfSource(file, pages)
        val marks = findText(texts, config)
        var id = (marks.maxOfOrNull { it.id } ?: -1L) + 1
        val codes = Kind.CODE in config.enabled
        val faces = Kind.FACE in config.enabled
        if (codes || faces) {
            for (p in pages) {
                if (!codes && p.index !in pictured) continue
                val bitmap = source.render(p.index, SCAN_WIDTH)
                val scale = p.width / bitmap.width
                fun box(r: RectF) = Box(r.left * scale, r.top * scale, r.right * scale, r.bottom * scale)
                if (codes) CodeScanner.scan(bitmap).forEach { c -> marks += Mark(id++, p.index, Kind.CODE, c.value, listOf(box(c.area)), true) }
                if (faces && p.index in pictured) FaceFinder.find(bitmap).forEach { marks += Mark(id++, p.index, Kind.FACE, "", listOf(box(it)), true) }
                bitmap.recycle()
            }
        }
        return Session(name, source, marks.sortedWith(compareBy({ it.page }, { it.boxes.first().top })), style)
    }

    private fun loadImage(file: File, type: DocType, name: String, config: DetectionConfig, style: Style): Session {
        val bitmap = Images.load(file)
        val scan = scanImage(bitmap, config)
        val source = ImageSource(file, type, bitmap, scan.text)
        return Session(name, source, scan.marks, style)
    }

    class ImageScan(val text: PageText?, val marks: List<Mark>)

    fun scanImage(bitmap: android.graphics.Bitmap, config: DetectionConfig): ImageScan {
        val text = if (config.searches) Ocr.read(bitmap) else null
        val marks = text?.let { findText(listOf(it), config) } ?: mutableListOf()
        val side = maxOf(bitmap.width, bitmap.height)
        val probe = if (side > IMAGE_SCAN_SIDE) Images.scaled(bitmap, bitmap.width * IMAGE_SCAN_SIDE / side) else bitmap
        val scale = bitmap.width.toFloat() / probe.width
        fun box(r: RectF) = Box(r.left * scale, r.top * scale, r.right * scale, r.bottom * scale)
        var id = (marks.maxOfOrNull { it.id } ?: -1L) + 1
        if (Kind.CODE in config.enabled) {
            CodeScanner.scan(probe).forEach { c -> marks += Mark(id++, 0, Kind.CODE, c.value, listOf(box(c.area)), true) }
        }
        if (Kind.FACE in config.enabled) {
            FaceFinder.find(probe).forEach { marks += Mark(id++, 0, Kind.FACE, "", listOf(box(it)), true) }
        }
        if (probe !== bitmap) probe.recycle()
        return ImageScan(text, marks)
    }
}
