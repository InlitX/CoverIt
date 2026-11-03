package com.coverit.app.detect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldTest {

    private val world = Kind.detectable.toSet() - Kind.spain

    private fun found(text: String, enabled: Set<Kind> = world) =
        Detector.find(text, enabled = enabled).map { Triple(it.kind, it.value, it.covered) }

    @Test
    fun labelledIdsFromAnyCountry() {
        val text = """
            Passport No: X1234567
            SSN 123-45-6789
            NHS number: 943 476 5919
            MRN: 00482913. Policy number: POL-2024-88123
            Codice fiscale: RSSMRA85T10A562S
            DNI: 12345678Z
        """.trimIndent()
        assertEquals(
            listOf("X1234567", "123-45-6789", "943 476 5919", "00482913", "POL-2024-88123", "RSSMRA85T10A562S", "12345678Z"),
            found(text).filter { it.first == Kind.ID }.map { it.second },
        )
    }

    @Test
    fun idValueStopsBeforeWords() {
        assertEquals(listOf(Kind.ID to "123456"), found("NHC: 123456 Paciente ingresado").map { it.first to it.second })
        assertTrue(found("Customer number: none yet").toString(), found("Customer number: none yet").isEmpty())
        val bad = found("Ref 000-12-3456 and 666-12-3456")
        assertTrue(bad.toString(), bad.none { it.first == Kind.ID })
    }

    @Test
    fun phonesFromOtherCountries() {
        val f = found("Call (415) 555-0132 or 415-555-0199. Tél : 06 12 34 56 78. Invoice 0123 4567 890.")
        assertEquals(
            listOf("(415) 555-0132" to true, "415-555-0199" to true, "06 12 34 56 78" to true, "0123 4567 890" to false),
            f.filter { it.first == Kind.PHONE }.map { it.second to it.third },
        )
    }

    @Test
    fun usDatesAndOtherLanguagesLabels() {
        val f = found("Date of birth: 03/15/1990\nAdresse: 12 rue de la Paix, 75002 Paris\nNom: Dupont")
        assertTrue(f.toString(), Triple(Kind.DATE, "03/15/1990", true) in f)
        assertTrue(f.toString(), f.any { it.first == Kind.ADDRESS && it.second.startsWith("12 rue de la Paix") })
        assertTrue(f.toString(), f.any { it.first == Kind.NAME && it.second == "Dupont" })
    }

    @Test
    fun spanishFormatsStayOffElsewhere() {
        val text = "Ref 1234 BCD, NIE X1234567L sin etiqueta"
        assertTrue(found(text).none { it.first in Kind.spain })
        assertTrue(found(text, Kind.detectable.toSet()).any { it.first == Kind.PLATE })
    }

    @Test
    fun phonesByNumberingPlan() {
        val before = Detector.region
        try {
            Detector.region = "DE"
            val de = found("Rufen Sie an: +49 30 901820 oder Praxis 030 901820.").filter { it.first == Kind.PHONE }
            assertTrue(de.toString(), de.any { it.second == "+49 30 901820" && it.third })
            assertTrue(de.toString(), de.any { it.second == "030 901820" })
            Detector.region = "BR"
            val br = found("Celular: (11) 98765-4321").filter { it.first == Kind.PHONE }
            assertTrue(br.toString(), br.any { it.second.contains("98765-4321") && it.third })
        } finally {
            Detector.region = before
        }
    }

    @Test
    fun noPhonesInAmountsOrDates() {
        val text = "Total 1.234.567,89 EUR. Fecha 12/03/2024. Pedido 2024-000123. Cuenta 4.500 m2."
        assertTrue(found(text).toString(), found(text).none { it.first == Kind.PHONE })
    }

    @Test
    fun professionalNumbers() {
        val ids = found("Firmado: Dra. Lucía Gómez, colegiada M-12345").filter { it.first == Kind.ID }.map { it.second }
        assertEquals(listOf("M-12345"), ids)
    }

    @Test
    fun socialSecurityByLabel() {
        val ids = found("Nº Seguridad Social 28 12345678 40\nNuméro de sécurité sociale: 1 85 05 78 006 084 36").filter { it.first == Kind.ID }.map { it.second }
        assertEquals(listOf("28 12345678 40", "1 85 05 78 006 084 36"), ids)
    }
}
