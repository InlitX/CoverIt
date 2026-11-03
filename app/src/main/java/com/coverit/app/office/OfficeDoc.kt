package com.coverit.app.office

import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

enum class OfficeFormat { DOCX, XLSX, PPTX, ODT, ODS, ODP, TEXT }

class Segment(val part: String, val node: XText, val cell: XElement? = null, val value: XElement? = null)

const val BLOCK = '█'

class OfficeDoc internal constructor(
    val format: OfficeFormat,
    private val entries: List<PartEntry>,
    private val xml: Map<String, XmlDoc>,
    private val plain: PlainText?,
    val segments: List<Segment>,
    val text: String,
    val owner: IntArray,
    val offset: IntArray,
) {

    class PartEntry(val name: String, val data: ByteArray, val stored: Boolean)

    class PlainText(val charset: Charset, val bom: ByteArray)

    fun write(covered: BooleanArray, values: Collection<String>, target: File, media: (String, ByteArray) -> ByteArray = { _, b -> b }) {
        val perSegment = arrayOfNulls<BooleanArray>(segments.size)
        for (i in covered.indices) {
            if (!covered[i] || owner[i] < 0) continue
            val s = owner[i]
            val flags = perSegment[s] ?: BooleanArray(segments[s].node.text.length).also { perSegment[s] = it }
            flags[offset[i]] = true
        }
        val runs = mutableSetOf<String>()
        perSegment.forEachIndexed { s, flags -> if (flags != null) runs += coveredRuns(segments[s].node.text, flags) }
        val blanker = Blanker(runs + values)

        if (plain != null) {
            val seg = segments.single()
            val redacted = blanker.blank(redact(seg.node.text, perSegment[0]))
            target.outputStream().use {
                it.write(plain.bom)
                it.write(redacted.toByteArray(plain.charset))
            }
            return
        }

        val newText = HashMap<XText, String>()
        val edits = xml.mapValues { (_, doc) -> XmlEdits(doc) }
        segments.forEachIndexed { s, seg ->
            val flags = perSegment[s] ?: return@forEachIndexed
            val redacted = redact(seg.node.text, flags)
            val cell = seg.cell
            val value = seg.value
            if (cell != null && value != null) {
                // A number can't hold blocks: the cell becomes an inline text cell.
                val e = edits.getValue(seg.part)
                e.replace(cell.start, cell.tagEnd, inlineCellTag(xml.getValue(seg.part).raw.substring(cell.start, cell.tagEnd)))
                e.replace(value.start, value.end, "<is><t>" + Entities.encode(redacted) + "</t></is>")
            } else {
                if (cell != null) {
                    edits.getValue(seg.part).replace(cell.start, cell.tagEnd, stringCellTag(xml.getValue(seg.part).raw.substring(cell.start, cell.tagEnd)))
                }
                newText[seg.node] = redacted
            }
        }

        val dropped = mutableSetOf<String>()
        Cleaner.clean(format, xml, edits, dropped)

        for ((part, doc) in xml) {
            val e = edits.getValue(part)
            visitText(doc.root) { t ->
                val current = newText[t] ?: t.text
                val blanked = blanker.blank(current)
                if (blanked != t.text) e.setText(t, blanked)
            }
            doc.root.descendants().plus(doc.root).forEach { el ->
                el.attrs.forEach { a ->
                    if (a.local in BLANKED_ATTRS) {
                        val blanked = blanker.blank(a.value)
                        if (blanked != a.value) e.setAttr(a, blanked)
                    }
                }
            }
        }

        target.outputStream().use { out ->
            writeZip(out) { entry ->
                when {
                    entry.name in dropped -> null
                    entry.name in xml -> edits.getValue(entry.name).let { if (it.isEmpty) entry.data else it.apply().toByteArray(Charsets.UTF_8) }
                    isMedia(entry.name) -> runCatching { media(entry.name, entry.data) }.getOrDefault(entry.data)
                    else -> entry.data
                }
            }
        }
    }

    fun allText(): String = buildString {
        append(text).append('\n')
        for (doc in xml.values) {
            visitText(doc.root) { append(it.text).append(' ') }
            doc.root.descendants().forEach { el -> el.attrs.forEach { if (it.local in BLANKED_ATTRS) append(it.value).append(' ') } }
        }
    }

    fun leftovers(): List<String> = Cleaner.leftovers(format, xml, entries.map { it.name })

    fun properties(): List<Pair<String, String>> = Cleaner.properties(xml)

    private fun writeZip(out: OutputStream, content: (PartEntry) -> ByteArray?) {
        ZipOutputStream(out).use { zip ->
            for (entry in entries) {
                val data = content(entry) ?: continue
                val z = ZipEntry(entry.name)
                if (entry.stored) {
                    z.method = ZipEntry.STORED
                    z.size = data.size.toLong()
                    z.compressedSize = data.size.toLong()
                    z.crc = CRC32().apply { update(data) }.value
                }
                zip.putNextEntry(z)
                zip.write(data)
                zip.closeEntry()
            }
        }
    }

    companion object {

        private const val MAX_TEXT = 2_000_000
        private const val MAX_UNZIPPED = 400L * 1024 * 1024
        private const val MAX_ENTRIES = 20_000

        val BLANKED_ATTRS = setOf("Target", "descr", "title", "name", "value", "string-value", "href", "instr", "displayName", "author", "initials")

        fun open(file: File, format: OfficeFormat): OfficeDoc =
            if (format == OfficeFormat.TEXT) openPlain(file) else openZip(file, format)

        private fun openPlain(file: File): OfficeDoc {
            val bytes = file.readBytes()
            val (bom, charset) = when {
                bytes.startsWith(0xEF, 0xBB, 0xBF) -> bytes.copyOf(3) to Charsets.UTF_8
                bytes.startsWith(0xFF, 0xFE) -> bytes.copyOf(2) to Charsets.UTF_16LE
                bytes.startsWith(0xFE, 0xFF) -> bytes.copyOf(2) to Charsets.UTF_16BE
                else -> ByteArray(0) to (if (isUtf8(bytes)) Charsets.UTF_8 else Charset.forName("windows-1252"))
            }
            val content = String(bytes, bom.size, bytes.size - bom.size, charset)
            require(content.length <= MAX_TEXT) { "too big" }
            val node = XText(0, content.length, content)
            val view = ViewBuilder()
            view.seg("#plain", node)
            return view.build(OfficeFormat.TEXT, emptyList(), emptyMap(), PlainText(charset, bom))
        }

        private fun openZip(file: File, format: OfficeFormat): OfficeDoc {
            val entries = mutableListOf<PartEntry>()
            ZipFile(file).use { zip ->
                var total = 0L
                val list = zip.entries().toList()
                require(list.size <= MAX_ENTRIES) { "too many entries" }
                for (z in list) {
                    if (z.isDirectory) continue
                    val data = zip.getInputStream(z).use { it.readBytes() }
                    total += data.size
                    require(total <= MAX_UNZIPPED) { "too big" }
                    entries += PartEntry(z.name, data, z.method == ZipEntry.STORED)
                }
            }
            val xml = LinkedHashMap<String, XmlDoc>()
            for (e in entries) {
                if (isXml(e.name)) runCatching { XmlDoc(String(e.data, Charsets.UTF_8).removePrefix("﻿")) }.onSuccess { xml[e.name] = it }
            }
            val view = ViewBuilder()
            when (format) {
                OfficeFormat.DOCX -> Readers.docx(xml, view)
                OfficeFormat.XLSX -> Readers.xlsx(xml, view)
                OfficeFormat.PPTX -> Readers.pptx(xml, view)
                OfficeFormat.ODT, OfficeFormat.ODS, OfficeFormat.ODP -> Readers.odf(xml, view)
                OfficeFormat.TEXT -> Unit
            }
            require(view.length <= MAX_TEXT) { "too big" }
            return view.build(format, entries, xml, null)
        }

        private fun ByteArray.startsWith(vararg b: Int) = size >= b.size && b.indices.all { this[it] == b[it].toByte() }

        private fun isUtf8(bytes: ByteArray) = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
            true
        } catch (_: CharacterCodingException) {
            false
        }

        private fun isXml(name: String) = name.endsWith(".xml") || name.endsWith(".rels") || name.endsWith(".vml")

        private fun isMedia(name: String) =
            (name.contains("/media/") || name.startsWith("Pictures/")) &&
                name.substringAfterLast('.').lowercase() in setOf("jpg", "jpeg", "png", "webp")

        fun visitText(e: XElement, visit: (XText) -> Unit) {
            for (c in e.children) {
                when (c) {
                    is XText -> visit(c)
                    is XElement -> visitText(c, visit)
                }
            }
        }

        fun redact(text: String, flags: BooleanArray?): String {
            if (flags == null) return text
            val chars = text.toCharArray()
            for (i in chars.indices) if (flags[i] && !chars[i].isWhitespace()) chars[i] = BLOCK
            return String(chars)
        }

        private fun coveredRuns(text: String, flags: BooleanArray): List<String> {
            val out = mutableListOf<String>()
            var start = -1
            for (i in 0..text.length) {
                val on = i < text.length && flags[i]
                if (on && start < 0) start = i
                if (!on && start >= 0) {
                    out += text.substring(start, i).trim()
                    start = -1
                }
            }
            return out
        }

        private fun stringCellTag(tag: String): String {
            val stripped = tag
                .replace(Regex("""\s+(office:(value|date-value|time-value|boolean-value|currency)|calcext:value-type|table:formula)\s*=\s*("[^"]*"|'[^']*')"""), "")
                .replace(Regex("""office:value-type\s*=\s*("[^"]*"|'[^']*')"""), "office:value-type=\"string\"")
            if (!tag.contains("calcext:value-type")) return stripped
            val close = if (stripped.endsWith("/>")) stripped.length - 2 else stripped.length - 1
            return stripped.substring(0, close) + " calcext:value-type=\"string\"" + stripped.substring(close)
        }

        private fun inlineCellTag(tag: String): String {
            val withoutType = tag.replace(Regex("""\s+t\s*=\s*("[^"]*"|'[^']*')"""), "")
            val close = if (withoutType.endsWith("/>")) withoutType.length - 2 else withoutType.length - 1
            return withoutType.substring(0, close) + " t=\"inlineStr\"" + withoutType.substring(close)
        }
    }
}

class ViewBuilder {
    private val sb = StringBuilder()
    private var owner = IntArray(4096)
    private var offset = IntArray(4096)
    private val segments = mutableListOf<Segment>()
    private val indexOf = HashMap<XText, Int>()

    val length get() = sb.length

    fun seg(part: String, node: XText, cell: XElement? = null, value: XElement? = null) {
        val index = indexOf.getOrPut(node) {
            segments += Segment(part, node, cell, value)
            segments.size - 1
        }
        node.text.forEachIndexed { i, c -> push(if (c == '\r') ' ' else c, index, i) }
    }

    fun sep(s: String) {
        for (c in s) {
            if (c == '\n') {
                while (sb.isNotEmpty() && owner[sb.length - 1] < 0 && sb[sb.length - 1].let { it == ' ' || it == '·' }) sb.setLength(sb.length - 1)
            }
            if (c == '\n' && sb.length >= 2 && sb[sb.length - 1] == '\n' && sb[sb.length - 2] == '\n') continue
            if (c == '\n' && sb.isEmpty()) continue
            push(c, -1, 0)
        }
    }

    fun endsWith(c: Char) = sb.isNotEmpty() && sb[sb.length - 1] == c

    private fun push(c: Char, seg: Int, off: Int) {
        val n = sb.length
        if (n == owner.size) {
            owner = owner.copyOf(n * 2)
            offset = offset.copyOf(n * 2)
        }
        owner[n] = seg
        offset[n] = off
        sb.append(c)
    }

    internal fun build(
        format: OfficeFormat,
        entries: List<OfficeDoc.PartEntry>,
        xml: Map<String, XmlDoc>,
        plain: OfficeDoc.PlainText?,
    ): OfficeDoc {
        val n = sb.length
        return OfficeDoc(format, entries, xml, plain, segments.toList(), sb.toString(), owner.copyOf(n), offset.copyOf(n))
    }
}

class Blanker(values: Collection<String>) {

    private val patterns: List<Regex> = values
        .map { it.trim() }
        .filter { v -> v.count { it.isLetterOrDigit() } >= 4 && v.any { it != BLOCK } }
        .distinct()
        .sortedByDescending { it.length }
        .map { v ->
            val digits = v.filter { it.isDigit() }
            if (digits.length >= 6 && v.all { it.isDigit() || it.isWhitespace() || it in ".-/+()" }) {
                Regex(digits.toList().joinToString("""[\s.\-/]*"""))
            } else {
                Regex(Regex.escape(v).replace(" ", """\E\s+\Q"""), RegexOption.IGNORE_CASE)
            }
        }

    val isEmpty get() = patterns.isEmpty()

    fun blank(s: String): String {
        if (patterns.isEmpty() || s.isEmpty()) return s
        var out = s
        for (p in patterns) {
            out = p.replace(out) { m -> m.value.map { if (it.isWhitespace()) it else BLOCK }.joinToString("") }
        }
        return out
    }
}
