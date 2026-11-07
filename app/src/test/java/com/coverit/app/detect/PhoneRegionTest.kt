package com.coverit.app.detect

import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneRegionTest {
    @Test
    fun referenceNumbersAreNotPhonesInAnyRegion() {
        val text = "Total 1.234.567,89 EUR. Fecha 12/03/2024. Pedido 2024-000123. Cuenta 4.500 m2."
        val before = Detector.region
        try {
            for (r in listOf("ES", "US", "GB", "DE", "FR", "MX")) {
                Detector.region = r
                val p = Detector.find(text, enabled = Kind.detectable.toSet() - Kind.spain).filter { it.kind == Kind.PHONE }
                assertTrue("$r $p", p.isEmpty())
            }
            Detector.region = "US"
            val real = Detector.find("Call me at (415) 555-2671 or 2024-000123", enabled = Kind.detectable.toSet()).filter { it.kind == Kind.PHONE }.map { it.value }
            assertTrue(real.toString(), real.any { it.contains("555-2671") } && real.none { it.contains("000123") })
        } finally {
            Detector.region = before
        }
    }
}
