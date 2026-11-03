package com.coverit.app.detect.rules

import com.coverit.app.detect.Kind

object Details {

    private const val LABELS =
        """nacionalidad|nationality|nationalit[ée]|staatsangeh[oö]rigkeit|nacionalidade|nazionalit[àa]|""" +
            """estado\s+civil|marital\s+status|situation\s+familiale|familienstand|stato\s+civile|""" +
            """lugar\s+de\s+nacimiento|place\s+of\s+birth|lieu\s+de\s+naissance|geburtsort|luogo\s+di\s+nascita|""" +
            """sexo|g[eé]nero|gender|sex|geschlecht|sesso|edad|age|alter|et[àa]|""" +
            """religi[oó]n|religion|confesi[oó]n|n[uú]mero\s+de\s+hijos|hijos|children|""" +
            """discapacidad|disability|grado\s+de\s+discapacidad|grupo\s+sangu[ií]neo|blood\s+(?:type|group)|""" +
            """orientaci[oó]n\s+sexual|sexual\s+orientation|etnia|ethnicity|origen\s+[eé]tnico|ethnic\s+origin|""" +
            """afiliaci[oó]n\s+(?:sindical|pol[ií]tica)|union\s+membership|political\s+(?:party|affiliation)|""" +
            """diagn[oó]stico|diagnosis|alergias?|allerg(?:y|ies)|medicaci[oó]n|medication"""

    val all = listOf(
        Rule(
            Kind.DETAIL,
            Regex("""(?im)(?<!\p{L})(?:$LABELS)\s*:\s*((?:(?!\s\p{L}+(?:\s\p{L}+)?\s*:)[^\n,;|·]){1,40})"""),
            group = 1,
            accept = { m, _ -> m.groupValues[1].isNotBlank() },
        ),
    )
}
