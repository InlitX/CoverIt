package com.coverit.app.detect.names

import com.coverit.app.detect.Detector
import com.coverit.app.detect.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NameFinderTest {

    private fun names(text: String) = NameFinder.find(text).map { text.substring(it.first, it.last + 1) }

    @Test
    fun labelledNames() {
        assertEquals(listOf("Ana María Pérez García"), names("Paciente: Ana María Pérez García\nEdad: 54"))
        assertEquals(listOf("Ruiz"), names("Atendido por la Dra. Ruiz en urgencias"))
        assertEquals(listOf("Zxqwy Blorft"), names("Nombre y apellidos: Zxqwy Blorft"))
        assertEquals(listOf("Laura Martín"), names("Firmado: Dra. Laura Martín, colegiada"))
    }

    @Test
    fun unlabelledNamesNeedAKnownGivenNameOrTwoSurnames() {
        assertEquals(listOf("Lucía de la Fuente"), names("La muestra de Lucía de la Fuente llegó tarde."))
        assertEquals(listOf("Ana Pérez"), names("Informe de Ana Pérez sobre la consulta."))
        assertEquals(listOf("PÉREZ GARCÍA, ANA"), names("PÉREZ GARCÍA, ANA"))
        assertEquals(listOf("John Smith"), names("Signed by John Smith yesterday"))
    }

    @Test
    fun placesAndInstitutionsAreNotNames() {
        assertTrue(names("Hospital Universitario San Juan de Dios").isEmpty())
        assertTrue(names("Santiago de Compostela, 12 de mayo").isEmpty())
        assertTrue(names("INFORME MÉDICO DE ALTA").isEmpty())
        assertTrue(names("Resultados del Análisis Clínico").isEmpty())
        assertTrue(names("Victoria").isEmpty())
    }

    @Test
    fun detectorUsesNameFinder() {
        val found = Detector.find("Paciente: Marta Gómez Ruiz, DNI 12345678Z").map { it.kind to it.value }
        assertTrue(found.contains(Kind.NAME to "Marta Gómez Ruiz"))
        assertTrue(found.contains(Kind.DNI to "12345678Z"))
    }

    @Test
    fun emailsStayWholeWhenANameMatchesInside() {
        val names = NameMatcher(listOf("Ana Pérez García"), includeGiven = false)
        val found = Detector.find("Escribir a ana.perez@correo.es", names).map { it.kind to it.value }
        assertEquals(listOf(Kind.EMAIL to "ana.perez@correo.es"), found)
    }
}
