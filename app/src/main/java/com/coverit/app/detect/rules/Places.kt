package com.coverit.app.detect.rules

import com.coverit.app.detect.Kind

object Places {

    private const val DMS = """\d{1,3}\s?°\s?\d{1,2}\s?['′]\s?(?:\d{1,2}(?:[.,]\d+)?\s?(?:["″]|''))?\s?"""

    private const val STREET_ES =
        """c/|calle|avda\.?|avenida|av\.|plaza|pza\.?|paseo|p[º°]|camino|carretera|ctra\.?|ronda|traves[ií]a|urb\.|urbanizaci[oó]n|glorieta|pasaje|r[uú]a|carrer|passeig|pla[cç]a"""

    private const val STREET_EU = """Via|Viale|Piazza|Corso|Rue|Avenue|Boulevard|Bd|Allée|Chemin|Impasse|Quai|Rua|Travessa|Largo|Praça|Calçada|Estrada"""

    private const val STREET_FR = """rue|avenue|boulevard|bd|all[ée]e|chemin|impasse|quai|place|cours"""

    private const val STREET_EN =
        """Street|St\.?|Avenue|Ave\.?|Road|Rd\.?|Lane|Ln\.?|Drive|Dr\.?|Boulevard|Blvd\.?|Way|Court|Ct\.?|Place|Pl\.?|Terrace|Close|Crescent|Square|Sq\.?|Highway|Hwy\.?|Parkway|Pkwy\.?"""

    private const val STREET_DE = """stra(?:ß|ss)e|str\.|weg|gasse|platz|allee|damm"""

    private const val NUMBER = """\d{1,4}[a-zA-Z]?(?:\s*,?\s*\d{1,2}\s?[º°ª]\s?[a-z]?(?![\p{L}]))?"""

    private const val POSTCODE_LABELS =
        """postal\s+code|post\s?code|zip(?:\s+code)?|c[oó]digo\s+postal|c\.\s?p\.?|\bcp|plz|postleitzahl|code\s+postal|cep|cap\.?\s*:"""

    private const val POSTCODE_VALUE =
        """\d{4,6}(?:-\d{3,4})?|\d{2}-\d{3}|\d{4}\s?[A-Z]{2}(?![a-z])|[A-Z]{1,2}\d[A-Z\d]?\s?\d[A-Z]{2}|[A-Z]\d[A-Z]\s?\d[A-Z]\d"""

    val all = listOf(
        Rule(Kind.ADDRESS, Regex("""(?i)(?<![\p{L}])(?:$STREET_ES)\s+[^\n,;:]{2,40}?,?\s*(?:n[º°o]\.?\s*|n[uú]m\.?\s*|n[uú]mero\s+)?$NUMBER""")),
        Rule(Kind.ADDRESS, Regex("""(?<![\p{L}])(?:$STREET_EU)\s+[^\n,;:]{2,40}?,?\s*(?:n[º°o]\.?\s*)?$NUMBER""")),
        Rule(Kind.ADDRESS, Regex("""(?<![\p{L}\d])\d{1,5}(?:\s?(?:bis|ter))?,?\s+(?i:$STREET_FR)\s+(?:[\p{L}'’.-]+\s+){0,4}[\p{Lu}][\p{L}'’.-]+""")),
        Rule(Kind.ADDRESS, Regex("""(?<![\p{L}\d])\d{1,5}[A-Za-z]?\s+(?:[\p{Lu}][\p{L}'’.-]*\s+){1,4}(?:$STREET_EN)(?![\p{L}])""")),
        Rule(Kind.ADDRESS, Regex("""(?<!\p{L})[\p{Lu}][\p{L}-]*(?:$STREET_DE)\s+\d{1,4}\s?[a-z]?(?![\p{L}\d])""")),
        Rule(
            Kind.ADDRESS,
            Regex("""(?im)(?<!\p{L})(?:domicilio|direcci[oó]n(?:\s+postal|\s+particular)?|home\s+address|address|street|adre[cç]a|adresse|anschrift|indirizzo|endere[cç]o|morada)\s*:\s*((?:(?!\s\p{L}+(?:\s\p{L}+)?\s*:)[^\n]){3,80})"""),
            group = 1,
            accept = { m, _ -> m.groupValues[1].any { it.isDigit() } && '@' !in m.groupValues[1] },
        ),
        Rule(
            Kind.ADDRESS,
            Regex("""(?<![\d.\w-])(-?\d{1,2}\.\d{4,8})\s*,\s*(-?\d{1,3}\.\d{4,8})(?![\d.])"""),
            accept = { m, _ -> m.groupValues[1].toDouble() in -90.0..90.0 && m.groupValues[2].toDouble() in -180.0..180.0 },
        ),
        Rule(Kind.ADDRESS, Regex("""(?i)(?<![\d\w])$DMS[NS][,;\s]+$DMS[EWO](?![\p{L}])""")),
        Rule(Kind.POSTCODE, Regex("""(?i:$POSTCODE_LABELS)\s*[:.]?\s*($POSTCODE_VALUE)(?![\p{L}\d])"""), group = 1),
    )

    val spain = listOf(
        Rule(
            Kind.PLATE,
            Regex("""(?<![A-Za-z0-9])\d{4}[ -]?[BCDFGHJKLMNPRSTVWXYZ]{3}(?![A-Za-z0-9])|(?<![A-Za-z0-9])[A-Z]{1,2}[ -]\d{4}[ -][A-Z]{1,2}(?![A-Za-z0-9])"""),
        ),
    )
}
