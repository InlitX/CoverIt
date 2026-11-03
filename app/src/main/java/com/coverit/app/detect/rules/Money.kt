package com.coverit.app.detect.rules

import com.coverit.app.detect.Kind
import com.coverit.app.detect.check.Validators

object Money {

    val all = listOf(
        Rule(
            Kind.IBAN,
            Regex("""(?<![A-Za-z0-9])[A-Z]{2}\d{2}(?:[ -]?[A-Z0-9]{4}){2,7}(?:[ -]?[A-Z0-9]{1,4})?(?![A-Za-z0-9])"""),
            accept = { m, _ -> Validators.isValidIban(m.value) },
        ),
        Rule(
            Kind.CARD,
            Regex("""(?<!\d)\d(?:[ -]?\d){12,18}(?!\d)"""),
            accept = { m, _ -> Validators.isCardNumber(m.value) },
        ),
    )
}
