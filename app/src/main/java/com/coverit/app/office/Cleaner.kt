package com.coverit.app.office

internal object Cleaner {

    private val W_REMOVE = setOf(
        "w:del", "w:moveFrom", "w:moveFromRangeStart", "w:moveFromRangeEnd", "w:moveToRangeStart", "w:moveToRangeEnd",
        "w:rPrChange", "w:pPrChange", "w:sectPrChange", "w:tblPrChange", "w:tblPrExChange", "w:tblGridChange",
        "w:trPrChange", "w:tcPrChange", "w:numberingChange", "w:cellIns", "w:cellDel", "w:cellMerge",
        "w:customXmlDelRangeStart", "w:customXmlDelRangeEnd", "w:customXmlInsRangeStart", "w:customXmlInsRangeEnd",
        "w:customXmlMoveFromRangeStart", "w:customXmlMoveFromRangeEnd", "w:customXmlMoveToRangeStart", "w:customXmlMoveToRangeEnd",
        "w:commentRangeStart", "w:commentRangeEnd", "w:commentReference",
    )

    private val W_UNWRAP = setOf("w:ins", "w:moveTo")

    private val ODF_REMOVE = setOf("text:tracked-changes", "office:annotation", "office:annotation-end", "text:change", "text:change-start", "text:change-end")

    private val PEOPLE_ATTRS = setOf("author", "initials", "displayName", "userId", "providerId", "creator")

    fun clean(format: OfficeFormat, xml: Map<String, XmlDoc>, edits: Map<String, XmlEdits>, dropped: MutableSet<String>) {
        when (format) {
            OfficeFormat.DOCX, OfficeFormat.XLSX, OfficeFormat.PPTX -> ooxml(format, xml, edits, dropped)
            OfficeFormat.ODT, OfficeFormat.ODS, OfficeFormat.ODP -> odf(xml, edits, dropped)
            OfficeFormat.TEXT -> Unit
        }
    }

    private fun ooxml(format: OfficeFormat, xml: Map<String, XmlDoc>, edits: Map<String, XmlEdits>, dropped: MutableSet<String>) {
        for ((part, doc) in xml) {
            val e = edits.getValue(part)
            when {
                isComments(part) -> scrub(doc.root, e, emptyRoot = format == OfficeFormat.DOCX)
                part == "docProps/core.xml" || part == "docProps/custom.xml" -> e.empty(doc.root)
                part == "docProps/app.xml" -> doc.root.elements().filter { it.localName() in setOf("Company", "Manager", "HyperlinkBase") }.forEach(e::remove)
                format == OfficeFormat.DOCX && part.startsWith("word/") -> {
                    walk(doc.root) { el ->
                        when (el.name) {
                            in W_REMOVE -> {
                                e.remove(el)
                                false
                            }
                            in W_UNWRAP -> {
                                e.unwrap(el)
                                true
                            }
                            else -> true
                        }
                    }
                }
            }
        }
        xml.keys.filter { it.endsWith(".rels") }.forEach { part ->
            val doc = xml.getValue(part)
            doc.root.elements().filter { it.localName() == "Relationship" && it.attr("Target")?.contains("thumbnail", ignoreCase = true) == true }
                .forEach { rel ->
                    edits.getValue(part).remove(rel)
                    rel.attr("Target")?.let { dropped += resolveFromRels(part, it) }
                }
        }
    }

    private fun odf(xml: Map<String, XmlDoc>, edits: Map<String, XmlEdits>, dropped: MutableSet<String>) {
        for ((part, doc) in xml) {
            val e = edits.getValue(part)
            when (part) {
                "content.xml", "styles.xml" -> walk(doc.root) { el ->
                    if (el.name in ODF_REMOVE) {
                        e.remove(el)
                        false
                    } else {
                        true
                    }
                }
                "meta.xml" -> doc.root.elements().filter { it.name == "office:meta" }.forEach(e::empty)
                "META-INF/manifest.xml" -> doc.root.elements()
                    .filter { it.attr("manifest:full-path")?.startsWith("Thumbnails/") == true }
                    .forEach(e::remove)
            }
        }
        dropped += "Thumbnails/thumbnail.png"
    }

    fun leftovers(format: OfficeFormat, xml: Map<String, XmlDoc>, names: List<String>): List<String> = buildList {
        if (format == OfficeFormat.TEXT) return@buildList
        if (names.any { it.contains("thumbnail", ignoreCase = true) }) add("Thumbnail")
        val core = xml["docProps/core.xml"]?.root ?: xml["meta.xml"]?.root?.elements()?.firstOrNull { it.name == "office:meta" }
        if (core != null && core.elements().isNotEmpty()) add("Properties")
        val revisionNames = W_REMOVE.filter { it == "w:del" || it == "w:moveFrom" } + W_UNWRAP + ODF_REMOVE.filter { it == "text:tracked-changes" }
        val commentNames = setOf("office:annotation", "w:comment")
        var revisions = false
        var comments = false
        for ((part, doc) in xml) {
            val all = doc.root.descendants()
            if (all.any { it.name in revisionNames }) revisions = true
            if (all.any { it.name in commentNames }) comments = true
            if (isComments(part) && format != OfficeFormat.DOCX) {
                var text = false
                OfficeDoc.visitText(doc.root) { if (it.text.isNotBlank()) text = true }
                if (text) comments = true
            }
        }
        if (revisions) add("Tracked changes")
        if (comments) add("Comments")
    }

    fun properties(xml: Map<String, XmlDoc>): List<Pair<String, String>> = buildList {
        fun add(e: XElement, name: String = e.name.substringAfter(':')) {
            val value = e.text().trim()
            if (value.isNotEmpty()) add(name to value)
        }
        xml["docProps/core.xml"]?.root?.elements()?.forEach { add(it) }
        xml["docProps/app.xml"]?.root?.elements()?.filter { it.name.substringAfter(':') in APP_PROPS }?.forEach { add(it) }
        xml["docProps/custom.xml"]?.root?.elements()?.forEach { p -> add(p, p.attr("name") ?: "custom") }
        xml["meta.xml"]?.root?.elements()?.firstOrNull { it.name == "office:meta" }?.elements()?.forEach { e ->
            if (e.name == "meta:user-defined") add(e, e.attr("meta:name") ?: "custom") else add(e)
        }
    }

    private val APP_PROPS = setOf("Company", "Manager", "Application", "Template", "HyperlinkBase")

    private fun isComments(part: String): Boolean {
        val name = part.substringAfterLast('/')
        return name.startsWith("comment") || name == "people.xml" || name == "authors.xml" ||
            part.startsWith("xl/threadedComments/") || part.startsWith("xl/persons/") || part.startsWith("ppt/comments/")
    }

    private fun scrub(root: XElement, e: XmlEdits, emptyRoot: Boolean) {
        if (emptyRoot) {
            e.empty(root)
            return
        }
        OfficeDoc.visitText(root) { if (it.text.isNotBlank()) e.setText(it, "") }
        (sequenceOf(root) + root.descendants()).forEach { el ->
            el.attrs.filter { it.local in PEOPLE_ATTRS }.forEach { e.setAttr(it, "") }
        }
    }

    private fun walk(e: XElement, visit: (XElement) -> Boolean) {
        for (c in e.elements()) {
            if (visit(c)) walk(c, visit)
        }
    }

    private fun resolveFromRels(relsPart: String, target: String): String {
        if (target.startsWith("/")) return target.removePrefix("/")
        val base = relsPart.substringBefore("_rels/").trimEnd('/')
        return if (base.isEmpty()) target else "$base/$target"
    }
}
