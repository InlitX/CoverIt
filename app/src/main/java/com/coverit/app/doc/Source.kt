package com.coverit.app.doc

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.coverit.app.image.Images
import com.coverit.app.office.OfficeFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.coverit.app.pdf.Box
import com.coverit.app.pdf.PageText
import java.io.Closeable
import java.io.File

enum class Family { PDF, IMAGE, OFFICE }

enum class DocType(val extension: String, val mime: String, val family: Family, val office: OfficeFormat? = null) {
    PDF("pdf", "application/pdf", Family.PDF),
    JPEG("jpg", "image/jpeg", Family.IMAGE),
    PNG("png", "image/png", Family.IMAGE),
    WEBP("webp", "image/webp", Family.IMAGE),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", Family.OFFICE, OfficeFormat.DOCX),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", Family.OFFICE, OfficeFormat.XLSX),
    PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", Family.OFFICE, OfficeFormat.PPTX),
    ODT("odt", "application/vnd.oasis.opendocument.text", Family.OFFICE, OfficeFormat.ODT),
    ODS("ods", "application/vnd.oasis.opendocument.spreadsheet", Family.OFFICE, OfficeFormat.ODS),
    ODP("odp", "application/vnd.oasis.opendocument.presentation", Family.OFFICE, OfficeFormat.ODP),
    TXT("txt", "text/plain", Family.OFFICE, OfficeFormat.TEXT),
    CSV("csv", "text/csv", Family.OFFICE, OfficeFormat.TEXT),
    MD("md", "text/markdown", Family.OFFICE, OfficeFormat.TEXT),
    TSV("tsv", "text/tab-separated-values", Family.OFFICE, OfficeFormat.TEXT),
    JSON("json", "application/json", Family.OFFICE, OfficeFormat.TEXT),
    XML("xml", "text/xml", Family.OFFICE, OfficeFormat.TEXT),
    HTML("html", "text/html", Family.OFFICE, OfficeFormat.TEXT),
    LOG("log", "text/x-log", Family.OFFICE, OfficeFormat.TEXT),
    VCF("vcf", "text/vcard", Family.OFFICE, OfficeFormat.TEXT),
    ICS("ics", "text/calendar", Family.OFFICE, OfficeFormat.TEXT),
    EML("eml", "message/rfc822", Family.OFFICE, OfficeFormat.TEXT),
    ;

    val monospace get() = this in setOf(CSV, TSV, JSON, XML, LOG)

    companion object {

        val pickable = arrayOf(
            "application/pdf",
            "image/*",
            "text/*", "application/json", "application/xml", "message/rfc822",
            "text/plain", "text/csv", "text/comma-separated-values", "text/markdown", "text/x-markdown",
            "application/msword", "application/vnd.ms-excel", "application/vnd.ms-powerpoint", "application/rtf", "text/rtf",
        ) + entries.filter { it.family == Family.OFFICE }.map { it.mime }

        fun of(mime: String?, name: String): DocType {
            val ext = name.substringAfterLast('.', "").lowercase()
            val m = mime?.lowercase().orEmpty()
            entries.firstOrNull { it.family == Family.OFFICE && (it.extension == ext) }?.let { return it }
            ALIASES[ext]?.let { return it }
            entries.firstOrNull { it.family == Family.OFFICE && it.mime == m && ext !in IMAGE_EXT }?.let { return it }
            return when {
                m == "application/pdf" || ext == "pdf" -> PDF
                ext in setOf("doc", "xls", "ppt", "rtf", "pages", "numbers", "key", "wpd") ||
                    m in setOf("application/msword", "application/vnd.ms-excel", "application/vnd.ms-powerpoint", "application/rtf", "text/rtf") -> throw Unsupported(ext.ifEmpty { m })
                ext == "webp" || m == "image/webp" -> WEBP
                ext in setOf("png", "gif", "bmp") || m in setOf("image/png", "image/gif", "image/bmp", "image/x-ms-bmp") -> PNG
                m.startsWith("image/") || ext in IMAGE_EXT -> JPEG
                m == "text/comma-separated-values" -> CSV
                m == "text/plain" -> TXT
                m == "application/xml" -> XML
                m == "text/x-vcard" -> VCF
                else -> throw Unsupported(ext.ifEmpty { m })
            }
        }

        private val ALIASES = mapOf("htm" to HTML, "xhtml" to HTML, "vcard" to VCF, "markdown" to MD, "text" to TXT, "ini" to TXT, "yaml" to TXT, "yml" to TXT, "srt" to TXT, "vtt" to TXT)

        private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "avif", "gif", "bmp")
    }
}

class Unsupported(val format: String) : Exception(format)

class Page(val index: Int, val width: Float, val height: Float, val text: PageText?, val version: Int = 0) {
    val hasText get() = text != null && text.text.any { it.isLetterOrDigit() }
}

sealed class Source(val file: File, val type: DocType) : Closeable {
    abstract val pages: List<Page>
    abstract fun render(page: Int, widthPx: Int): Bitmap
}

class PdfSource(file: File, override val pages: List<Page>) : Source(file, DocType.PDF) {

    private val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(fd)

    @Synchronized
    override fun render(page: Int, widthPx: Int): Bitmap = renderer.openPage(page).use {
        val height = (widthPx.toLong() * it.height / it.width).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        it.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap
    }

    override fun close() {
        renderer.close()
        fd.close()
    }
}

data class Framing(val turns: Int = 0, val crop: Box? = null) {
    val isPlain get() = turns % 4 == 0 && crop == null
}

class ImageSource(file: File, type: DocType, private val raw: Bitmap, private val rawText: PageText? = null) : Source(file, type) {

    var framing by mutableStateOf(Framing())
        private set

    var bitmap: Bitmap = raw
        private set

    override var pages by mutableStateOf(listOf(Page(0, raw.width.toFloat(), raw.height.toFloat(), rawText)))
        private set

    private var version = 0

    private var spare: Triple<Framing, Bitmap, PageText?>? = null

    private val small by lazy { Images.scaled(raw, minOf(raw.width, 1400)) }

    fun preview(turns: Int): Bitmap =
        if (turns % 4 == 0) small else Bitmap.createBitmap(small, 0, 0, small.width, small.height, Matrix().apply { postRotate(90f * (turns % 4)) }, true)

    fun turnedSize(turns: Int): Pair<Int, Int> = if (turns % 2 == 0) raw.width to raw.height else raw.height to raw.width

    fun build(f: Framing): Bitmap {
        if (f == framing) return bitmap
        spare?.let { (sf, sb, _) -> if (sf == f) return sb }
        if (f.isPlain) return raw
        val turned = if (f.turns % 4 == 0) {
            raw
        } else {
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(90f * (f.turns % 4)) }, true)
        }
        val c = f.crop ?: return turned
        val l = c.left.toInt().coerceIn(0, turned.width - 1)
        val t = c.top.toInt().coerceIn(0, turned.height - 1)
        val w = (c.right.toInt() - l).coerceIn(1, turned.width - l)
        val h = (c.bottom.toInt() - t).coerceIn(1, turned.height - t)
        return Bitmap.createBitmap(turned, l, t, w, h)
    }

    fun show(f: Framing, built: Bitmap = build(f), text: PageText? = knownText(f)) {
        if (f == framing) return
        spare = Triple(framing, bitmap, pages[0].text)
        framing = f
        bitmap = built
        version++
        pages = listOf(Page(0, built.width.toFloat(), built.height.toFloat(), text, version))
    }

    private fun knownText(f: Framing): PageText? = when {
        f.isPlain -> rawText
        spare?.first == f -> spare?.third
        else -> null
    }

    fun move(box: Box, from: Framing, to: Framing): Box? {
        var b = from.crop?.let { Box(box.left + it.left, box.top + it.top, box.right + it.left, box.bottom + it.top) } ?: box
        var (w, h) = turnedSize(from.turns)
        repeat((4 - from.turns % 4) % 4) {
            b = Box(h - b.bottom, b.left, h - b.top, b.right)
            val t = w
            w = h
            h = t
        }
        repeat(to.turns % 4) {
            b = Box(h - b.bottom, b.left, h - b.top, b.right)
            val t = w
            w = h
            h = t
        }
        val c = to.crop ?: return b
        val cut = Box(maxOf(b.left, c.left) - c.left, maxOf(b.top, c.top) - c.top, minOf(b.right, c.right) - c.left, minOf(b.bottom, c.bottom) - c.top)
        return cut.takeIf { it.right - it.left >= 2f && it.bottom - it.top >= 2f }
    }

    override fun render(page: Int, widthPx: Int): Bitmap = Images.scaled(bitmap, widthPx)

    override fun close() = Unit
}
