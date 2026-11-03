package com.coverit.app.detect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InternationalTest {

    private val world = Kind.detectable.toSet() - Kind.spain

    private fun found(text: String) = Detector.find(text, enabled = world).map { it.kind to it.value }

    private fun values(text: String, kind: Kind) = found(text).filter { it.first == kind }.map { it.second }

    @Test
    fun nationalIdsWithoutLabel() {
        val text = "RSSMRA85T10A562S, 123.456.789-09, 12.345.678-5, AB 12 34 56 C y GODE561231GR8"
        assertEquals(listOf("RSSMRA85T10A562S", "123.456.789-09", "12.345.678-5", "AB 12 34 56 C", "GODE561231GR8"), values(text, Kind.ID))
    }

    @Test
    fun wrongCheckDigitsStayVisible() {
        assertTrue(values("RSSMRA85T10A562X 123.456.789-00 12.345.678-4", Kind.ID).isEmpty())
    }

    @Test
    fun machineReadableZone() {
        val text = "P<ESPPEREZ<GARCIA<<ANA<MARIA<<<<<<<<<<<<<<<<\nXDA123456<8ESP8503159F3001015<<<<<<<<<<<<<<04"
        assertEquals(2, values(text, Kind.ID).size)
    }

    @Test
    fun networkAddresses() {
        val text = "IPv6 2001:db8::8a2e:370:7334, MAC 00:1A:2B:3C:4D:5E. Hora 12:30:45."
        assertEquals(listOf("2001:db8::8a2e:370:7334", "00:1A:2B:3C:4D:5E"), values(text, Kind.IP))
    }

    @Test
    fun coordinates() {
        val text = "Ubicación 40.416775, -3.703790 o 40°25'00\"N 3°42'13\"W. Precio 1.5, 2.3"
        assertEquals(listOf("40.416775, -3.703790", "40°25'00\"N 3°42'13\"W"), values(text, Kind.ADDRESS))
    }

    @Test
    fun postcodesAfterTheirLabel() {
        val text = "ZIP: 94105. Postcode: SW1A 1AA. PLZ 10115. Sin etiqueta 28013."
        assertEquals(listOf("94105", "SW1A 1AA", "10115"), values(text, Kind.POSTCODE))
    }

    @Test
    fun streetsInOtherLanguages() {
        val text = "Lives at 221B Baker Street. Abita in Via Roma 10. Wohnt in Hauptstraße 5. Spring 2024, via email 2 times."
        assertEquals(listOf("221B Baker Street", "Via Roma 10", "Hauptstraße 5"), values(text, Kind.ADDRESS))
    }

    @Test
    fun handlesNeedASocialLabelToBeCovered() {
        val f = Detector.find("Instagram: @ana_perez. Escríbeme a @hola_mundo", enabled = world).filter { it.kind == Kind.PROFILE }
        assertEquals(listOf("@ana_perez" to true, "@hola_mundo" to false), f.map { it.value to it.covered })
    }

    @Test
    fun birthDatesInMoreLanguages() {
        val text = "F. nac.: 15/03/90. Née le 3 mars 1985. Geboren am 12. Oktober 1979. Factura 15/03/90."
        val f = Detector.find(text, enabled = world).filter { it.kind == Kind.DATE }
        assertEquals(listOf("15/03/90" to true, "3 mars 1985" to true, "12. Oktober 1979" to true), f.map { it.value to it.covered })
    }

    @Test
    fun sensitiveDetails() {
        assertEquals(listOf("A+", "penicilina"), values("Grupo sanguíneo: A+\nAlergias: penicilina", Kind.DETAIL))
    }
}
