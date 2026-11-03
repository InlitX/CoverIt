package com.coverit.app.office

import com.coverit.app.detect.Kind
import com.coverit.app.doc.DetectionConfig
import com.coverit.app.image.Metadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

class OfficeDocTest {

    private val samples = File("../samples")
    private val out = File("build/office-out").apply { mkdirs() }
    private val config = DetectionConfig(Kind.entries.toSet(), emptyList())

    private fun redact(name: String, format: OfficeFormat): Pair<OfficeDoc, OfficeDoc> {
        val doc = OfficeDoc.open(File(samples, name), format)
        val covered = BooleanArray(doc.text.length)
        val names = config.namesIn(listOf(doc.text))
        val values = mutableListOf<String>()
        for (f in config.find(doc.text, names)) {
            if (!f.covered) continue
            for (i in f.start until f.end) covered[i] = true
            values += f.value
        }
        Regex("EXP-2024/00731").findAll(doc.text).forEach { m -> m.range.forEach { covered[it] = true } }
        val target = File(out, "clean-$name")
        doc.write(covered, values, target) { _, bytes -> Metadata.strip(bytes) }
        val reopened = OfficeDoc.open(target, format)
        File(out, "$name.txt").writeText(reopened.allText())
        return doc to reopened
    }

    private fun assertClean(name: String, format: OfficeFormat, keep: List<String>) {
        val (before, after) = redact(name, format)
        val all = after.allText()
        for (secret in SECRETS) {
            assertFalse("$name still has '$secret'", all.contains(secret, ignoreCase = true))
        }
        assertFalse("$name still has the phone", Regex("""612[ .]?345[ .]?678""").containsMatchIn(all))
        for (k in listOf("penicilina", "Laura", "Luis Martín")) assertFalse("$name still has '$k'", all.contains(k))
        for (k in keep) assertTrue("$name lost '$k'", after.text.contains(k))
        assertEquals("$name leftovers", emptyList<String>(), after.leftovers())
        assertTrue(before.text.contains("12345678Z"))
        if (format != OfficeFormat.TEXT) assertWellFormed(File(out, "clean-$name"))
    }

    private fun assertWellFormed(file: File) {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        ZipFile(file).use { zip ->
            for (e in zip.entries()) {
                if (!(e.name.endsWith(".xml") || e.name.endsWith(".rels"))) continue
                zip.getInputStream(e).use { factory.newDocumentBuilder().parse(it) }
            }
        }
    }

    @Test
    fun docx() = assertClean("informe-paciente.docx", OfficeFormat.DOCX, listOf("Informe clínico", "Resonancia", "acudió"))

    @Test
    fun docxDropsRevisionsCommentsAndAuthor() {
        val (_, after) = redact("informe-paciente.docx", OfficeFormat.DOCX)
        val all = after.allText()
        assertFalse(all.contains("281234567840"))
        assertFalse(all.contains("Luis Martín"))
        assertFalse(all.contains("Laura"))
        assertFalse(all.contains("penicilina"))
        assertTrue(after.text.contains("(móvil)"))
    }

    @Test
    fun embeddedPhotosLoseExif() {
        for ((name, format) in listOf("informe-paciente.docx" to OfficeFormat.DOCX, "informe-paciente.odt" to OfficeFormat.ODT)) {
            redact(name, format)
            ZipFile(File(out, "clean-$name")).use { zip ->
                val photos = zip.entries().toList().filter { it.name.endsWith(".jpg") || it.name.endsWith(".jpeg") }
                assertTrue("$name has no photo", photos.isNotEmpty())
                for (p in photos) {
                    val bytes = zip.getInputStream(p).use { it.readBytes() }
                    assertFalse("$name ${p.name} keeps EXIF", String(bytes, 0, minOf(bytes.size, 4096), Charsets.ISO_8859_1).contains("Exif"))
                    assertEquals(0xFF.toByte(), bytes[0])
                    assertEquals(0xD8.toByte(), bytes[1])
                }
            }
        }
    }

    @Test
    fun xlsx() = assertClean("pacientes.xlsx", OfficeFormat.XLSX, listOf("Nombre", "Revisión", "Citas"))

    @Test
    fun xlsxNumbersBecomeTextCells() {
        val (_, after) = redact("pacientes.xlsx", OfficeFormat.XLSX)
        assertFalse(after.text.contains("698765432"))
        assertTrue(after.text.contains("█████████"))
    }

    @Test
    fun pptx() = assertClean("sesion-clinica.pptx", OfficeFormat.PPTX, listOf("Datos del caso", "Neurología"))

    @Test
    fun odt() = assertClean("informe-paciente.odt", OfficeFormat.ODT, listOf("Informe clínico", "Resonancia"))

    @Test
    fun ods() = assertClean("pacientes.ods", OfficeFormat.ODS, listOf("Nombre", "Revisión"))

    @Test
    fun odp() = assertClean("sesion-clinica.odp", OfficeFormat.ODP, listOf("Datos del caso"))

    @Test
    fun txt() = assertClean("nota.txt", OfficeFormat.TEXT, listOf("Nota de consulta", "mutua"))

    @Test
    fun csv() = assertClean("pacientes.csv", OfficeFormat.TEXT, listOf("nombre,dni"))

    @Test
    fun xmlEditsKeepEverythingElse() {
        val raw = """<?xml version="1.0"?><a x="1"><b>Ana &amp; Luis</b><c/><d>keep</d></a>"""
        val doc = XmlDoc(raw)
        val b = doc.root.child("b")!!
        val edits = XmlEdits(doc)
        edits.setText(b.children.single() as XText, "███ & Luis")
        edits.remove(doc.root.child("c")!!)
        assertEquals("""<?xml version="1.0"?><a x="1"><b>███ &amp; Luis</b><d>keep</d></a>""", edits.apply())
    }

    private companion object {
        val SECRETS = listOf(
            "12345678Z", "ana.perez@correo.es", "Pérez García", "ES91 2100", "2100 0418", "Calle Mayor 12",
            "EXP-2024/00731", "87654321X", "jlopez@mail.com",
        )
    }

    @Test
    fun cvSample() {
        val doc = OfficeDoc.open(File(samples, "cv-ejemplo.docx"), OfficeFormat.DOCX)
        val found = config.find(doc.text, config.namesIn(listOf(doc.text))).filter { it.covered }.map { it.kind to it.value.trim() }
        for (expected in listOf(
            Kind.NAME to "Wanjiru Kamau Otieno",
            Kind.EMAIL to "wanjiru.k@correo.es",
            Kind.PROFILE to "linkedin.com/in/wanjiru-kamau",
            Kind.DATE to "15 de marzo de 1991",
            Kind.DETAIL to "Keniana",
            Kind.DNI to "12345678Z",
            Kind.NAME to "Tomás Ibarra Goikoetxea",
            Kind.PHONE to "699 112 233",
        )) assertTrue("$expected not in $found", expected in found)
        assertTrue(found.none { it.second.contains("Developer") || it.second.contains("Acme Software") })
    }
}
