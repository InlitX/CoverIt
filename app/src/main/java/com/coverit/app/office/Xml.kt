package com.coverit.app.office

sealed class XNode

class XText(val start: Int, val end: Int, val text: String) : XNode()

class XAttr(val name: String, val start: Int, val end: Int, val value: String) {
    val local get() = name.substringAfter(':')
}

class XElement(
    val name: String,
    val start: Int,
    val tagEnd: Int,
    val attrs: List<XAttr>,
) : XNode() {
    val children = mutableListOf<XNode>()
    var closeStart = tagEnd
    var end = tagEnd
    val selfClosing get() = closeStart == tagEnd && end == tagEnd

    fun attr(name: String) = attrs.firstOrNull { it.name == name }?.value

    fun attrLocal(local: String) = attrs.firstOrNull { it.local == local }?.value

    fun elements() = children.filterIsInstance<XElement>()

    fun child(name: String) = elements().firstOrNull { it.name == name }

    fun descendants(): Sequence<XElement> = sequence {
        for (c in elements()) {
            yield(c)
            yieldAll(c.descendants())
        }
    }

    fun text(): String = buildString { appendText(this@XElement) }

    private fun StringBuilder.appendText(e: XElement) {
        for (c in e.children) {
            when (c) {
                is XText -> append(c.text)
                is XElement -> appendText(c)
            }
        }
    }
}

class XmlDoc(val raw: String) {

    val root: XElement = parse()

    private fun parse(): XElement {
        val top = XElement("#document", 0, 0, emptyList())
        val stack = ArrayDeque<XElement>().apply { addLast(top) }
        var i = 0
        val n = raw.length
        while (i < n) {
            if (raw[i] != '<') {
                val next = raw.indexOf('<', i).let { if (it < 0) n else it }
                stack.last().children += XText(i, next, Entities.decode(raw, i, next))
                i = next
                continue
            }
            when {
                raw.startsWith("<!--", i) -> i = skipPast(i, "-->")
                raw.startsWith("<![CDATA[", i) -> {
                    val close = raw.indexOf("]]>", i).let { if (it < 0) n else it }
                    stack.last().children += XText(i, close + 3, raw.substring(i + 9, close))
                    i = close + 3
                }
                raw.startsWith("<?", i) -> i = skipPast(i, "?>")
                raw.startsWith("<!", i) -> i = skipPast(i, ">")
                raw.startsWith("</", i) -> {
                    val close = raw.indexOf('>', i).let { if (it < 0) n - 1 else it }
                    val name = raw.substring(i + 2, close).trim()
                    val idx = stack.indexOfLast { it.name == name }
                    if (idx > 0) {
                        while (stack.size - 1 > idx) {
                            val dangling = stack.removeLast()
                            dangling.closeStart = i
                            dangling.end = i
                        }
                        val e = stack.removeLast()
                        e.closeStart = i
                        e.end = close + 1
                    }
                    i = close + 1
                }
                else -> i = openTag(i, stack)
            }
        }
        return top.elements().firstOrNull() ?: error("empty xml")
    }

    private fun skipPast(i: Int, marker: String): Int {
        val at = raw.indexOf(marker, i)
        return if (at < 0) raw.length else at + marker.length
    }

    private fun openTag(start: Int, stack: ArrayDeque<XElement>): Int {
        var i = start + 1
        val n = raw.length
        while (i < n && !raw[i].isWhitespace() && raw[i] != '>' && raw[i] != '/') i++
        val name = raw.substring(start + 1, i)
        val attrs = mutableListOf<XAttr>()
        while (i < n) {
            val c = raw[i]
            when {
                c.isWhitespace() -> i++
                c == '>' -> {
                    val e = XElement(name, start, i + 1, attrs)
                    stack.last().children += e
                    stack.addLast(e)
                    return i + 1
                }
                c == '/' && i + 1 < n && raw[i + 1] == '>' -> {
                    val e = XElement(name, start, i + 2, attrs)
                    stack.last().children += e
                    return i + 2
                }
                else -> {
                    val nameStart = i
                    while (i < n && raw[i] != '=' && !raw[i].isWhitespace() && raw[i] != '>' && raw[i] != '/') i++
                    val attrName = raw.substring(nameStart, i)
                    while (i < n && raw[i].isWhitespace()) i++
                    if (i < n && raw[i] == '=') {
                        i++
                        while (i < n && raw[i].isWhitespace()) i++
                        val quote = raw[i]
                        val valueStart = i + 1
                        val valueEnd = raw.indexOf(quote, valueStart).let { if (it < 0) n else it }
                        attrs += XAttr(attrName, valueStart, valueEnd, Entities.decode(raw, valueStart, valueEnd))
                        i = valueEnd + 1
                    } else if (attrName.isEmpty()) {
                        i++
                    }
                }
            }
        }
        error("unclosed tag")
    }
}

class XmlEdits(private val doc: XmlDoc) {

    private class Edit(val start: Int, val end: Int, val text: String)

    private val edits = mutableListOf<Edit>()

    val isEmpty get() = edits.isEmpty()

    fun replace(start: Int, end: Int, text: String) {
        edits += Edit(start, end, text)
    }

    fun remove(e: XElement) = replace(e.start, e.end, "")

    fun unwrap(e: XElement) {
        if (e.selfClosing) {
            remove(e)
        } else {
            replace(e.start, e.tagEnd, "")
            replace(e.closeStart, e.end, "")
        }
    }

    fun empty(e: XElement) {
        if (!e.selfClosing) replace(e.tagEnd, e.closeStart, "")
    }

    fun setText(t: XText, text: String) = replace(t.start, t.end, Entities.encode(text))

    fun setAttr(a: XAttr, value: String) = replace(a.start, a.end, Entities.encode(value, attribute = true))

    fun apply(): String {
        if (edits.isEmpty()) return doc.raw
        // Wider spans first at the same start, so a removal swallows the edits inside it.
        val sorted = edits.sortedWith(compareBy<Edit> { it.start }.thenByDescending { it.end })
        val out = StringBuilder(doc.raw.length)
        var pos = 0
        for (e in sorted) {
            if (e.start < pos) continue
            out.append(doc.raw, pos, e.start)
            out.append(e.text)
            pos = e.end
        }
        out.append(doc.raw, pos, doc.raw.length)
        return out.toString()
    }
}

object Entities {

    fun decode(raw: String, start: Int, end: Int): String {
        var amp = start
        while (amp < end && raw[amp] != '&') amp++
        if (amp == end) return raw.substring(start, end)
        val out = StringBuilder(end - start)
        var i = start
        while (i < end) {
            val c = raw[i]
            if (c != '&') {
                out.append(c)
                i++
                continue
            }
            var semi = i + 1
            while (semi < end && semi - i <= 10 && raw[semi] != ';') semi++
            if (semi >= end || raw[semi] != ';') {
                out.append(c)
                i++
                continue
            }
            val name = raw.substring(i + 1, semi)
            val decoded = when {
                name == "amp" -> "&"
                name == "lt" -> "<"
                name == "gt" -> ">"
                name == "quot" -> "\""
                name == "apos" -> "'"
                name.startsWith("#x") || name.startsWith("#X") -> name.substring(2).toIntOrNull(16)?.let { String(Character.toChars(it)) }
                name.startsWith("#") -> name.substring(1).toIntOrNull()?.let { String(Character.toChars(it)) }
                else -> null
            }
            if (decoded == null) {
                out.append(c)
                i++
            } else {
                out.append(decoded)
                i = semi + 1
            }
        }
        return out.toString()
    }

    fun encode(text: String, attribute: Boolean = false): String {
        val out = StringBuilder(text.length + 8)
        for (c in text) {
            when (c) {
                '&' -> out.append("&amp;")
                '<' -> out.append("&lt;")
                '>' -> out.append("&gt;")
                '"' -> if (attribute) out.append("&quot;") else out.append(c)
                else -> out.append(c)
            }
        }
        return out.toString()
    }
}
