package com.coverit.app.detect

enum class Kind {
    NAME,
    ID,
    DNI,
    NIE,
    NSS,
    IBAN,
    CARD,
    EMAIL,
    PHONE,
    IP,
    DATE,
    PLATE,
    POSTCODE,
    ADDRESS,
    PROFILE,
    DETAIL,
    CODE,
    FACE,
    CUSTOM,
    MANUAL;

    companion object {
        val detectable = listOf(NAME, FACE, ID, EMAIL, PHONE, ADDRESS, POSTCODE, DATE, IBAN, CARD, PROFILE, DETAIL, IP, CODE, DNI, NIE, NSS, PLATE)

        val spain = setOf(DNI, NIE, NSS, PLATE)
    }
}
