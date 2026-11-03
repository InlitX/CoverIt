package com.coverit.app.pdf

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coverit.app.detect.Detector
import com.coverit.app.detect.Kind
import com.coverit.app.redact.Style
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RedactorTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var source: File

    @Before
    fun setUp() {
        PDFBoxResourceLoader.init(context)
        source = File(context.cacheDir, "informe-prueba.pdf")
        InstrumentationRegistry.getInstrumentation().context.assets.open("informe-prueba.pdf").use { input ->
            source.outputStream().use { input.copyTo(it) }
        }
    }

    @Test
    fun dniIsRemovedFromTextLayerAndMetadataIsGone() {
        val pages = PDDocument.load(source).use { TextExtractor.extract(it) }
        val page = pages.single()
        val dni = Detector.find(page.text).single { it.kind == Kind.DNI }
        assertEquals("12345678Z", dni.value)

        val boxes = page.boxesFor(dni.start, dni.end).map { it.padded(1.5f) }
        val output = File(context.cacheDir, "informe-limpio.pdf")
        Redactor.redact(source, pages, mapOf(0 to boxes.map { com.coverit.app.redact.Cover(it) }), Style.BLACK, output)

        val text = PDDocument.load(output).use { doc ->
            assertTrue(doc.documentInformation.cosObject.size() == 0)
            val catalog = doc.documentCatalog.cosObject
            assertFalse(catalog.containsKey("OpenAction"))
            assertFalse(catalog.containsKey("Metadata"))
            assertFalse(doc.getPage(0).cosObject.containsKey("Annots"))
            TextExtractor.extract(doc).single().text
        }
        assertFalse(text.contains("12345678Z"))
        assertTrue(text.contains("Paciente"))
        assertTrue(text.contains("X1234567L"))

        val raw = output.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(raw.contains("12345678Z"))
        assertFalse(raw.contains("Juan Lopez"))
        assertFalse(raw.contains("Nota privada"))
        assertFalse(raw.contains("JavaScript"))

        val report = Verifier.check(output, listOf(dni.value)) { Detector.find(it) }
        assertTrue(report.leaked.isEmpty())
        assertTrue(report.leftovers.isEmpty())
    }

    @Test
    fun watermarkRebuildsThePageAndKeepsItsText() {
        val pages = PDDocument.load(source).use { TextExtractor.extract(it) }
        val output = File(context.cacheDir, "informe-marca.pdf")
        Redactor.redact(source, pages, emptyMap(), Style.BLACK, output, watermark = "Solo para la prueba")
        PDDocument.load(output).use { doc ->
            assertEquals(1, doc.numberOfPages)
            assertTrue(doc.documentInformation.cosObject.size() == 0)
            assertTrue(TextExtractor.extract(doc).single().text.contains("Paciente"))
        }
        assertFalse(output.readBytes().toString(Charsets.ISO_8859_1).contains("Nota privada"))
    }
}
