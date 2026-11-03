package com.coverit.app.detect

import com.coverit.app.detect.check.Validators
import com.coverit.app.detect.names.NameMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectorTest {

    private fun found(text: String, names: NameMatcher? = null) =
        Detector.find(text, names).map { it.kind to it.value }

    @Test
    fun nss() {
        assertTrue(Validators.isValidNss("28 12345678 40"))
        assertTrue(Validators.isValidNss("28/01234567/42"))
        assertFalse(Validators.isValidNss("28 12345678 41"))
        assertFalse(Validators.isValidNss("99 12345678 40"))
    }

    @Test
    fun ipAndDates() {
        assertTrue(Validators.isValidIpv4("192.168.1.20"))
        assertFalse(Validators.isValidIpv4("256.1.1.1"))
        assertFalse(Validators.isValidIpv4("01.2.3.4"))
        assertTrue(Validators.isValidDate(29, 2, 2024))
        assertFalse(Validators.isValidDate(29, 2, 2023))
        assertFalse(Validators.isValidDate(31, 4, 2000))
    }

    @Test
    fun birthDateIsSuggestedOnlyWithContext() {
        val text = "Fecha de nacimiento: 03/05/1988. Cita el 12/10/2026."
        val dates = Detector.find(text).filter { it.kind == Kind.DATE }
        assertEquals(listOf("03/05/1988" to true, "12/10/2026" to false), dates.map { it.value to it.covered })
    }

    @Test
    fun spanishRecords() {
        val text = """
            NSS 28 12345678 40, IP 10.0.0.12, matrícula 1234 BCD y M-1234-AB.
            Domicilio: Calle Mayor 12, 3º B. C.P. 28013 Madrid.
            Otro número 28013 sin contexto.
        """.trimIndent()
        assertEquals(
            listOf(
                Kind.NSS to "28 12345678 40",
                Kind.IP to "10.0.0.12",
                Kind.PLATE to "1234 BCD",
                Kind.PLATE to "M-1234-AB",
                Kind.ADDRESS to "Calle Mayor 12, 3º B",
                Kind.POSTCODE to "28013",
            ),
            found(text),
        )
    }

    @Test
    fun namesWithVariants() {
        val names = NameMatcher(listOf("Ana María Pérez García"), includeGiven = false)
        val text = """
            La paciente ANA MARÍA PEREZ GARCIA acude a consulta.
            Informe de Pérez García. La Sra. Perez llamó.
            Firmado: A. M. Pérez García. Iniciales A. M. P. G.
            Ana vino sola. Su hermano es Juan García López.
        """.trimIndent()
        val values = found(text, names).filter { it.first == Kind.NAME }.map { it.second }
        assertEquals(
            listOf(
                "ANA MARÍA PEREZ GARCIA",
                "Pérez García",
                "Perez",
                "A. M. Pérez García",
                "A. M. P. G.",
                "García",
            ),
            values,
        )
    }

    @Test
    fun namesIncludeGivenWhenAsked() {
        val names = NameMatcher(listOf("Lucía de la Fuente"), includeGiven = true)
        val values = found("Lucía vino. Informe de la Fuente. LUCIA DE LA FUENTE.", names).map { it.second }
        assertEquals(listOf("Lucía", "de la Fuente", "LUCIA DE LA FUENTE"), values)
    }
}
