package com.coverit.app.doc

import android.graphics.Bitmap
import android.os.Build
import com.coverit.app.detect.Kind
import com.coverit.app.image.CodeScanner
import com.coverit.app.image.Images
import com.coverit.app.image.Metadata
import com.coverit.app.image.Ocr
import com.coverit.app.office.OfficeDoc
import com.coverit.app.pdf.Box
import com.coverit.app.pdf.Redactor
import com.coverit.app.pdf.Verifier
import com.coverit.app.redact.Cover
import com.coverit.app.redact.Painter
import java.io.File

class Report(
    val type: DocType,
    val leaked: List<String>,
    val remaining: List<Pair<Kind, String>>,
    val leftovers: List<String>,
) {
    val clean get() = leaked.isEmpty() && leftovers.isEmpty()

    var before: List<MetaField> = emptyList()
}

object Exporter {

    fun export(session: Session, target: File, config: DetectionConfig, tag: (Kind) -> String = { "" }): Report {
        val chosen = session.chosen
        val byPage = chosen.groupBy { it.page }.mapValues { (_, marks) -> marks.flatMap { it.boxes } }
        val covers = chosen.groupBy { it.page }.mapValues { (_, marks) -> marks.flatMap { m -> m.covers(tag(m.kind)) } }
        return when (val source = session.source) {
            is PdfSource -> exportPdf(source, covers, session, chosen, target, config)
            is ImageSource -> exportImage(source, covers[0].orEmpty(), session, chosen, target, config)
            is TextSource -> exportOffice(source, byPage, chosen, target, config)
        }
    }

    private fun exportOffice(source: TextSource, byPage: Map<Int, List<Box>>, chosen: List<Mark>, target: File, config: DetectionConfig): Report {
        val doc = source.doc
        val covered = BooleanArray(doc.text.length)
        for ((page, boxes) in byPage) {
            val text = source.pages[page].text ?: continue
            val start = source.pageStart(page)
            text.boxes.forEachIndexed { i, box ->
                if (box == null) return@forEachIndexed
                val cx = (box.left + box.right) / 2
                val cy = (box.top + box.bottom) / 2
                if (boxes.any { cx in it.left..it.right && cy in it.top..it.bottom }) covered[start + i] = true
            }
        }
        val values = chosen.filter { it.kind != Kind.CODE && it.value.isNotBlank() }.map { it.value }
        doc.write(covered, values, target) { _, bytes -> Metadata.strip(bytes) }

        val written = OfficeDoc.open(target, doc.format)
        val everything = normalize(written.allText())
        val coveredText = (values + runs(doc.text, covered)).distinct()
        val leaked = coveredText.filter { v -> normalize(v).let { it.length >= 4 && everything.contains(it) } }
        val remaining = config.find(written.text).map { it.kind to it.value.trim() }.distinct()
        return Report(source.type, leaked, remaining, written.leftovers())
    }

    private fun runs(text: String, covered: BooleanArray): List<String> {
        val out = mutableListOf<String>()
        var start = -1
        for (i in 0..text.length) {
            val on = i < text.length && covered[i] && text[i] != '\n'
            if (on && start < 0) start = i
            if (!on && start >= 0) {
                out += text.substring(start, i).trim()
                start = -1
            }
        }
        return out.filter { it.isNotEmpty() }
    }

    private fun normalize(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    private fun exportPdf(
        source: PdfSource,
        byPage: Map<Int, List<Cover>>,
        session: Session,
        chosen: List<Mark>,
        target: File,
        config: DetectionConfig,
    ): Report {
        Redactor.redact(source.file, source.pages.map { it.text!! }, byPage, session.style, target, session.watermark, session.removed)
        val values = chosen.filter { it.kind != Kind.CODE && it.value.isNotBlank() }.map { it.value }
        val check = Verifier.check(target, values) { config.find(it) }
        return Report(DocType.PDF, check.leaked, check.remaining.map { it.kind to it.value }, check.leftovers)
    }

    private fun exportImage(source: ImageSource, boxes: List<Cover>, session: Session, chosen: List<Mark>, target: File, config: DetectionConfig): Report {
        val bitmap = source.bitmap.copy(Bitmap.Config.ARGB_8888, true)
        boxes.forEach { Painter.cover(bitmap, it, 1f, session.style) }
        session.watermark?.let { Painter.watermark(bitmap, it) }
        target.outputStream().use { out ->
            when (source.type) {
                DocType.PNG -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                DocType.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 92, out)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 92, out)
                }
                else -> bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
        }
        bitmap.recycle()

        val covered = chosen.filter { it.kind == Kind.CODE }.map { it.value }.toSet()
        val reread = Images.load(target)
        val codes = if (Kind.CODE in config.enabled) CodeScanner.scan(reread).map { it.value } else emptyList()
        val read = if (config.searches) Ocr.read(reread)?.text.orEmpty() else ""
        reread.recycle()
        val everything = normalize(read)
        val values = chosen.filter { it.kind != Kind.CODE && it.value.isNotBlank() }.map { it.value }
        val leaked = (codes.filter { it in covered } + values.filter { v -> normalize(v).let { it.length >= 4 && everything.contains(it) } }).distinct()
        val remaining = (codes.filter { it !in covered }.map { Kind.CODE to it } + config.find(read).map { it.kind to it.value.trim() }).distinct()
        val leftovers = if (Images.hasMetadata(target)) listOf("EXIF") else emptyList()
        return Report(source.type, leaked, remaining, leftovers)
    }
}
