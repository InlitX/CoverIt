package com.coverit.app.detect.rules

import com.coverit.app.detect.Kind
import com.coverit.app.detect.check.Validators

object Ids {

    private const val LABELS =
        """passport(?:\s+(?:no|number))?|pasaporte|passeport|reisepass|passaporte|""" +
            """id(?:\s+(?:no|number|card|nr))|identity\s+(?:card|number|no)|national\s+id(?:\s+(?:no|number))?|personal\s+id|""" +
            """ssn|social\s+security\s+(?:no|number)|national\s+insurance(?:\s+(?:no|number))?|nino|nhs\s+(?:no|number)|""" +
            """tax\s+(?:id|no|number|code)|tin|vat(?:\s+(?:no|number|id))?|nif|nie|dni|cif|cuit|cuil|rut|rfc|curp|cpf|cnpj|codice\s+fiscale|steuer-?id|bsn|pesel|""" +
            """c[eé]dula(?:\s+de\s+identidad)?|documento(?:\s+de\s+identidad)?|carte\s+d.identit[eé]|personalausweis|""" +
            """n[uú]mero\s+de\s+(?:documento|identificaci[oó]n|pasaporte|afiliaci[oó]n|cliente|cuenta|p[oó]liza|historia|expediente|socio|empleado|colegiado|paciente|seguro)|""" +
            """n[º°o]\.?\s*(?:de\s+)?(?:documento|cliente|cuenta|p[oó]liza|historia|expediente|socio|empleado|colegiado|paciente|afiliaci[oó]n)|""" +
            """historia\s+cl[ií]nica|nhc|tarjeta\s+sanitaria|cip|sip|patient\s+(?:id|no|number)|mrn|medical\s+record(?:\s+(?:no|number))?|""" +
            """insurance\s+(?:id|no|number)|policy\s+(?:no|number)|member\s+(?:id|no|number)|customer\s+(?:id|no|number)|client\s+(?:id|no|number)|""" +
            """account\s+(?:no|number)|employee\s+(?:id|no|number)|staff\s+(?:id|no)|student\s+(?:id|no|number)|licen[cs]e\s+(?:no|number)|""" +
            """n[º°o]\.?\s*(?:de\s+(?:la\s+)?)?seguridad\s+social|(?:n[uú]mero\s+de\s+(?:afiliaci[oó]n\s+a\s+la\s+)?)?seguridad\s+social|""" +
            """(?:n[uú]mero\s+de\s+)?s[ée]curit[ée]\s+sociale|sozialversicherungs-?(?:nummer|nr\.?)|""" +
            """colegiad[oa](?:\s+n[º°o]\.?)?|driver.?s\s+licen[cs]e(?:\s+(?:no|number))?|permiso\s+de\s+conducir|carn[eé]t?\s+de\s+conducir|matr[ií]cula\s+del\s+alumno|""" +
            """vin|n[uú]mero\s+de\s+bastidor|chassis\s+(?:no|number)"""

    private fun standalone(pattern: String) = Regex("""(?<![A-Za-z0-9])(?:$pattern)(?![A-Za-z0-9])""")

    val spain = listOf(
        Rule(Kind.NSS, Regex("""(?<!\d)\d{2}[ /-]?\d{8}[ /-]?\d{2}(?!\d)"""), accept = { m, _ -> Validators.isValidNss(m.value) }),
        Rule(Kind.NIE, standalone("""[XYZxyz][ .-]?\d{7}[ .-]?[A-Za-z]"""), accept = { m, _ -> Validators.isValidNie(m.value) }),
        Rule(Kind.DNI, standalone("""\d{2}\.?\d{3}\.?\d{3}[ .-]?[A-Za-z]"""), accept = { m, _ -> Validators.isValidDni(m.value) }),
    )

    val world = listOf(
        Rule(Kind.ID, Regex("""(?m)^[ \t]*([A-Z0-9<]{28,46})[ \t]*$"""), group = 1, accept = { m, _ -> m.groupValues[1].count { it == '<' } >= 3 }),
        Rule(Kind.ID, standalone("""[A-Z]{6}[0-9LMNPQRSTUV]{2}[A-EHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]"""), accept = { m, _ -> Validators.isValidCodiceFiscale(m.value) }),
        Rule(Kind.ID, standalone("""\d{3}\.\d{3}\.\d{3}-\d{2}"""), accept = { m, _ -> Validators.isValidCpf(m.value) }),
        Rule(Kind.ID, standalone("""\d{1,2}\.\d{3}\.\d{3}-[\dkK]"""), accept = { m, _ -> Validators.isValidRut(m.value) }),
        Rule(Kind.ID, standalone("""[A-Z]{2} ?\d{2} ?\d{2} ?\d{2} ?[A-D]"""), accept = { m, _ -> Validators.isValidNino(m.value) }),
        Rule(Kind.ID, Regex("""(?<!\d)[12] ?\d{2} ?\d{2} ?(?:\d{2}|2[AB]) ?\d{3} ?\d{3} ?\d{2}(?!\d)"""), accept = { m, _ -> Validators.isValidNir(m.value) }),
        Rule(Kind.ID, standalone("""[A-Z]{4}\d{6}[HMX][A-Z]{5}[A-Z0-9]\d"""), accept = { m, _ -> Validators.isValidCompactDate(m.value.substring(4, 10)) }),
        Rule(Kind.ID, standalone("""[A-ZÑ&]{3,4}\d{6}[A-Z0-9]{3}"""), accept = { m, _ -> Validators.isValidCompactDate(m.value.dropLast(3).takeLast(6)) && m.value.any { it.isLetter() } }),
        Rule(
            Kind.ID,
            Regex(
                """(?<![\p{L}\d])(?i:$LABELS)\s*(?:[:#]|n[º°o]\.?|no\.)?\s*:?\s*((?:[A-Z]{1,5}[-/.])?[A-Z]*\d[A-Z0-9]*(?:[ ./-][A-Z]*\d[A-Z0-9]*){0,6})(?![\p{L}\d])""",
            ),
            group = 1,
            accept = { m, _ -> m.groupValues[1].count { it.isDigit() } >= 3 },
        ),
        Rule(
            Kind.ID,
            Regex("""(?<![\d-])(\d{3})-(\d{2})-(\d{4})(?![\d-])"""),
            accept = { m, _ ->
                val (area, group, serial) = m.destructured
                area != "000" && area != "666" && area[0] != '9' && group != "00" && serial != "0000"
            },
        ),
    )
}
