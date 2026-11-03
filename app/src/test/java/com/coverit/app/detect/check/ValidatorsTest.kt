package com.coverit.app.detect.check

import com.coverit.app.detect.Detector
import com.coverit.app.detect.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun dni() {
        assertTrue(Validators.isValidDni("12345678Z"))
        assertTrue(Validators.isValidDni("12.345.678-z"))
        assertTrue(Validators.isValidDni("00000000T"))
        assertFalse(Validators.isValidDni("12345678A"))
        assertFalse(Validators.isValidDni("1234567Z"))
    }

    @Test
    fun nie() {
        assertTrue(Validators.isValidNie("X1234567L"))
        assertTrue(Validators.isValidNie("Y-1234567-X"))
        assertTrue(Validators.isValidNie("Z1234567R"))
        assertFalse(Validators.isValidNie("X1234567A"))
        assertFalse(Validators.isValidNie("W1234567L"))
    }

    @Test
    fun iban() {
        assertTrue(Validators.isValidIban("ES91 2100 0418 4502 0005 1332"))
        assertTrue(Validators.isValidIban("GB82WEST12345698765432"))
        assertTrue(Validators.isValidIban("de89 3704 0044 0532 0130 00"))
        assertFalse(Validators.isValidIban("ES91 2100 0418 4502 0005 1333"))
        assertFalse(Validators.isValidIban("ES91 2100 0418 4502 0005 13"))
    }

    @Test
    fun luhn() {
        assertTrue(Validators.isValidLuhn("4111 1111 1111 1111"))
        assertTrue(Validators.isValidLuhn("5500-0000-0000-0004"))
        assertTrue(Validators.isValidLuhn("378282246310005"))
        assertFalse(Validators.isValidLuhn("4111 1111 1111 1112"))
        assertFalse(Validators.isValidLuhn("0000 0000 0000 0000"))
        assertFalse(Validators.isValidLuhn("411111111111"))
    }

    @Test
    fun detectorFindsEachKind() {
        val text = """
            Paciente con DNI 12345678Z y NIE X1234567L.
            Cuenta ES91 2100 0418 4502 0005 1332, tarjeta 4111 1111 1111 1111.
            Contacto ana.perez@correo.es, tel. 612 345 678 o +34 912 34 56 78, Londres +44 20 7946 0958.
            No son datos: 12345678A, 4111 1111 1111 1112, 2026.
        """.trimIndent()
        val found = Detector.find(text).map { it.kind to it.value }
        assertEquals(
            listOf(
                Kind.DNI to "12345678Z",
                Kind.NIE to "X1234567L",
                Kind.IBAN to "ES91 2100 0418 4502 0005 1332",
                Kind.CARD to "4111 1111 1111 1111",
                Kind.EMAIL to "ana.perez@correo.es",
                Kind.PHONE to "612 345 678",
                Kind.PHONE to "+34 912 34 56 78",
                Kind.PHONE to "+44 20 7946 0958",
            ),
            found,
        )
    }
}
