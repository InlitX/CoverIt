package com.coverit.app.office

internal object Readers {

    private const val CELL = "  ·  "

    private val W_SKIP = setOf(
        "w:pPr", "w:rPr", "w:sectPr", "w:tblPr", "w:trPr", "w:tcPr", "w:tblGrid", "w:tblPrEx",
        "w:del", "w:moveFrom", "w:delText", "w:instrText", "mc:Fallback",
    )

    fun docx(xml: Map<String, XmlDoc>, view: ViewBuilder) {
        val parts = buildList {
            add("word/document.xml")
            addAll(numbered(xml.keys, "word/header"))
            addAll(numbered(xml.keys, "word/footer"))
            add("word/footnotes.xml")
            add("word/endnotes.xml")
        }
        for (part in parts) {
            val doc = xml[part] ?: continue
            word(part, doc.root, view, 0)
            view.sep("\n")
        }
    }

    private fun word(part: String, e: XElement, view: ViewBuilder, inCell: Int) {
        for (c in e.children) {
            when (c) {
                is XText -> if (e.name == "w:t") view.seg(part, c)
                is XElement -> when (c.name) {
                    in W_SKIP -> Unit
                    "w:tab", "w:ptab" -> view.sep("\t")
                    "w:br", "w:cr" -> view.sep("\n")
                    "w:noBreakHyphen" -> view.sep("-")
                    "w:p" -> {
                        word(part, c, view, inCell)
                        view.sep(if (inCell > 0) " " else "\n")
                    }
                    "w:tc" -> {
                        word(part, c, view, inCell + 1)
                        view.sep(CELL)
                    }
                    "w:tr" -> {
                        word(part, c, view, inCell)
                        view.sep("\n")
                    }
                    else -> word(part, c, view, inCell)
                }
            }
        }
    }

    fun xlsx(xml: Map<String, XmlDoc>, view: ViewBuilder) {
        val shared = xml["xl/sharedStrings.xml"]?.root?.elements()?.filter { it.name == "si" || it.localName() == "si" }.orEmpty()
        val sheets = xlsxSheets(xml)
        for ((name, part) in sheets) {
            val doc = xml[part] ?: continue
            view.sep(name + "\n")
            val data = doc.root.elements().firstOrNull { it.localName() == "sheetData" } ?: continue
            for (row in data.elements()) {
                for (cell in row.elements()) {
                    if (cell.localName() != "c") continue
                    val type = cell.attr("t")
                    val v = cell.elements().firstOrNull { it.localName() == "v" }
                    when (type) {
                        "s" -> {
                            val si = v?.text()?.trim()?.toIntOrNull()?.let { shared.getOrNull(it) } ?: continue
                            sharedText(si) { view.seg("xl/sharedStrings.xml", it) }
                        }
                        "inlineStr" -> cell.elements().firstOrNull { it.localName() == "is" }?.let { sharedText(it) { t -> view.seg(part, t) } }
                        else -> {
                            if (v == null || cell.elements().any { it.localName() == "f" }) continue
                            val t = v.children.filterIsInstance<XText>().singleOrNull() ?: continue
                            when (type) {
                                "b", "e" -> continue
                                "str" -> view.seg(part, t)
                                else -> view.seg(part, t, cell, v)
                            }
                        }
                    }
                    view.sep(CELL)
                }
                view.sep("\n")
            }
            view.sep("\n")
        }
        // Shared strings no cell points to (old values, other sheets' leftovers) are still covered by the blanker.
    }

    private fun sharedText(si: XElement, each: (XText) -> Unit) {
        for (c in si.elements()) {
            when (c.localName()) {
                "t" -> c.children.filterIsInstance<XText>().forEach(each)
                "r" -> c.elements().filter { it.localName() == "t" }.forEach { t -> t.children.filterIsInstance<XText>().forEach(each) }
            }
        }
    }

    private fun xlsxSheets(xml: Map<String, XmlDoc>): List<Pair<String, String>> {
        val workbook = xml["xl/workbook.xml"]?.root
        val rels = rels(xml, "xl/workbook.xml")
        val listed = workbook?.descendants()?.filter { it.localName() == "sheet" }?.mapNotNull { s ->
            val id = s.attrs.firstOrNull { it.local == "id" && it.name != "sheetId" }?.value ?: return@mapNotNull null
            val target = rels[id] ?: return@mapNotNull null
            (s.attr("name") ?: "") to target
        }?.toList().orEmpty()
        if (listed.isNotEmpty()) return listed
        return numbered(xml.keys, "xl/worksheets/sheet").map { it.substringAfterLast('/').removeSuffix(".xml") to it }
    }

    fun pptx(xml: Map<String, XmlDoc>, view: ViewBuilder) {
        val rels = rels(xml, "ppt/presentation.xml")
        val ordered = xml["ppt/presentation.xml"]?.root?.descendants()
            ?.filter { it.localName() == "sldId" }
            ?.mapNotNull { s -> s.attrs.firstOrNull { it.local == "id" && it.name.contains(':') }?.value?.let { rels[it] } }
            ?.toList()
            .orEmpty()
            .ifEmpty { numbered(xml.keys, "ppt/slides/slide") }
        ordered.forEachIndexed { i, slide ->
            val doc = xml[slide] ?: return@forEachIndexed
            view.sep("— ${i + 1} —\n")
            drawing(slide, doc.root, view)
            val notes = rels(xml, slide).values.firstOrNull { it.contains("notesSlide") }
            xml[notes]?.let {
                view.sep("\n")
                drawing(notes!!, it.root, view)
            }
            view.sep("\n")
        }
    }

    private fun drawing(part: String, e: XElement, view: ViewBuilder) {
        for (c in e.children) {
            when (c) {
                is XText -> if (e.name == "a:t") view.seg(part, c)
                is XElement -> when (c.name) {
                    "a:pPr", "a:rPr", "a:endParaRPr", "mc:Fallback" -> Unit
                    "a:br" -> view.sep("\n")
                    "a:p" -> {
                        drawing(part, c, view)
                        view.sep("\n")
                    }
                    "a:tc" -> {
                        drawing(part, c, view)
                        view.sep(CELL)
                    }
                    else -> drawing(part, c, view)
                }
            }
        }
    }

    private val ODF_SKIP = setOf("text:tracked-changes", "office:annotation", "office:annotation-end", "office:forms", "text:sequence-decls", "table:table-columns", "table:table-column")

    fun odf(xml: Map<String, XmlDoc>, view: ViewBuilder) {
        xml["content.xml"]?.let { doc ->
            doc.root.elements().firstOrNull { it.name == "office:body" }?.let { odf("content.xml", it, view, 0, 0) }
        }
        val isText = xml["content.xml"]?.root?.descendants()?.any { it.name == "office:text" } == true
        xml["styles.xml"]?.takeIf { isText }?.let { doc ->
            doc.root.descendants().filter { it.name.startsWith("style:header") || it.name.startsWith("style:footer") }.forEach {
                view.sep("\n")
                odf("styles.xml", it, view, 0, 0)
            }
        }
    }

    private fun odf(part: String, e: XElement, view: ViewBuilder, inPara: Int, inCell: Int, cell: XElement? = null) {
        for (c in e.children) {
            when (c) {
                is XText -> if (inPara > 0) view.seg(part, c, cell)
                is XElement -> when (c.name) {
                    in ODF_SKIP -> Unit
                    "text:s" -> view.sep(" ".repeat((c.attr("text:c")?.toIntOrNull() ?: 1).coerceIn(1, 40)))
                    "text:tab" -> view.sep("\t")
                    "text:line-break" -> view.sep("\n")
                    "text:p", "text:h" -> {
                        odf(part, c, view, inPara + 1, inCell, cell)
                        view.sep(if (inCell > 0) " " else "\n")
                    }
                    "table:table-cell", "table:covered-table-cell" -> {
                        val typed = c.attr("office:value-type")?.let { it != "string" } == true
                        odf(part, c, view, inPara, inCell + 1, if (typed) c else null)
                        if (!view.endsWith('\n')) view.sep(CELL)
                    }
                    "table:table-row", "draw:page" -> {
                        odf(part, c, view, inPara, inCell)
                        view.sep("\n")
                    }
                    "table:table" -> {
                        if (part == "content.xml" && e.name == "office:spreadsheet") view.sep((c.attr("table:name") ?: "") + "\n")
                        odf(part, c, view, inPara, inCell)
                        view.sep("\n")
                    }
                    else -> odf(part, c, view, inPara, inCell, cell)
                }
            }
        }
    }

    fun rels(xml: Map<String, XmlDoc>, part: String): Map<String, String> {
        val dir = part.substringBeforeLast('/', "")
        val relsPart = (if (dir.isEmpty()) "" else "$dir/") + "_rels/" + part.substringAfterLast('/') + ".rels"
        val root = xml[relsPart]?.root ?: return emptyMap()
        return root.elements().filter { it.localName() == "Relationship" && it.attr("TargetMode") != "External" }.mapNotNull { r ->
            val id = r.attr("Id") ?: return@mapNotNull null
            val target = r.attr("Target") ?: return@mapNotNull null
            id to resolve(dir, target)
        }.toMap()
    }

    private fun resolve(dir: String, target: String): String {
        if (target.startsWith("/")) return target.removePrefix("/")
        val parts = (if (dir.isEmpty()) emptyList() else dir.split('/')).toMutableList()
        for (p in target.split('/')) {
            when (p) {
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
                ".", "" -> Unit
                else -> parts += p
            }
        }
        return parts.joinToString("/")
    }

    fun numbered(names: Collection<String>, prefix: String): List<String> =
        names.filter { it.startsWith(prefix) && it.endsWith(".xml") && !it.contains("/_rels/") && it.removePrefix(prefix).removeSuffix(".xml").all(Char::isDigit) }
            .sortedBy { it.removePrefix(prefix).removeSuffix(".xml").toIntOrNull() ?: 0 }
}

internal fun XElement.localName() = name.substringAfter(':')
