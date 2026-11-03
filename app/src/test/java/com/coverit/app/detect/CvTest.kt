package com.coverit.app.detect

import com.coverit.app.detect.names.NameFinder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CvTest {

    private val cv = """
        Curriculum Vitae
        Wanjiru Kamau Otieno
        Senior Frontend Developer
        wanjiru.k@correo.es · +34 612 345 678 · linkedin.com/in/wanjiru-kamau · https://github.com/wkamau
        Domicilio: Av. Ruiseñor 14, 2ºA, Getafe
        Fecha de nacimiento: 15 de marzo de 1991
        Nacionalidad: Keniana
        Estado civil: Casada
        Experiencia
        Desarrolladora en Acme Software (2019-2024). Proyecto con fecha de entrega 1 de junio de 2023.
    """.trimIndent()

    private fun found(text: String) = Detector.find(text).map { Triple(it.kind, it.value, it.covered) }

    @Test
    fun cvWithAnUnknownName() {
        val f = found(cv)
        assertEquals(
            listOf(
                Kind.NAME to "Wanjiru Kamau Otieno",
                Kind.EMAIL to "wanjiru.k@correo.es",
                Kind.PHONE to "+34 612 345 678",
                Kind.PROFILE to "linkedin.com/in/wanjiru-kamau",
                Kind.PROFILE to "https://github.com/wkamau",
                Kind.ADDRESS to "Av. Ruiseñor 14, 2ºA",
                Kind.DATE to "15 de marzo de 1991",
                Kind.DETAIL to "Keniana",
                Kind.DETAIL to "Casada",
                Kind.DATE to "1 de junio de 2023",
            ),
            f.map { it.first to it.second },
        )
        assertEquals(false, f.last().third)
        assertEquals(true, f.first { it.second == "15 de marzo de 1991" }.third)
    }

    @Test
    fun writtenAndIsoDates() {
        val f = found("Born March 15, 1990. DOB: 1990-03-15. Born 3rd Sept 1988. Meeting 2024-13-01.")
        assertEquals(
            listOf("March 15, 1990" to true, "1990-03-15" to true, "3rd Sept 1988" to true),
            f.filter { it.first == Kind.DATE }.map { it.second to it.third },
        )
    }

    @Test
    fun headingNeedsContactAndIsNotATitle() {
        assertTrue(NameFinder.find("Wanjiru Kamau\nSome text without contact").isEmpty())
        assertTrue(NameFinder.find("Informe Anual\nventas@acme.com").isEmpty())
        assertTrue(NameFinder.find("Frontend Developer\nhola@acme.com").isEmpty())
        val t = "CURRICULUM VITAE\nTHANDIWE NKOSI\nthandi@mail.com"
        assertEquals(listOf("THANDIWE NKOSI"), NameFinder.find(t).map { t.substring(it.first, it.last + 1) })
    }

    @Test
    fun companyLinksAndLabelsWithoutNumbersStayVisible() {
        val f = found("Web: https://acme.com/empresa. Dirección: Recursos Humanos. Página: 3. Nacionalidad: Española Estado civil: Soltera")
        assertTrue(f.toString(), f.none { it.first == Kind.PROFILE || it.first == Kind.ADDRESS })
        assertEquals(listOf("Española", "Soltera"), f.filter { it.first == Kind.DETAIL }.map { it.second })
    }
}
