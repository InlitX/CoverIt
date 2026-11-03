package com.coverit.app.pdf

import com.coverit.app.detect.Finding
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File

class PdfCheck(val leaked: List<String>, val remaining: List<Finding>, val leftovers: List<String>)

object Verifier {

    fun check(file: File, covered: List<String>, detect: (String) -> List<Finding>): PdfCheck = PDDocument.load(file).use { doc ->
        val text = TextExtractor.extract(doc).joinToString("\n") { it.text }
        val flat = normalize(text)
        val leaked = covered.distinct().filter { normalize(it).let { n -> n.isNotEmpty() && flat.contains(n) } }
        val leftovers = buildList {
            if (doc.documentInformation.cosObject.size() > 0) add("Info")
            val catalog = doc.documentCatalog.cosObject
            for (key in listOf("Metadata", "AcroForm", "Names", "OpenAction", "AA")) {
                if (catalog.containsKey(key)) add(key)
            }
            doc.pages.forEachIndexed { i, page ->
                if (page.cosObject.containsKey("Annots")) add("Annots ${i + 1}")
            }
        }
        PdfCheck(leaked, detect(text), leftovers)
    }

    private fun normalize(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
}
