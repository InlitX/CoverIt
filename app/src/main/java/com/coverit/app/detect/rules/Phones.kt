package com.coverit.app.detect.rules

import com.coverit.app.detect.Kind
import com.coverit.app.detect.text.Around
import com.google.i18n.phonenumbers.PhoneNumberUtil

object Phones {

    class Hit(val range: IntRange, val covered: Boolean)

    private val util by lazy { PhoneNumberUtil.getInstance() }

    private val label = Regex("""(?i)(t[eé]l|tlf|phone|m[oó]vil|mobile|cell|fax|portable|handy|telefono|telefone|telemóvel|whatsapp|contact|☎|📞|📱)""")

    private fun labelled(text: String, at: Int) = label.containsMatchIn(Around.sentenceBefore(text, at, 30))

    private fun isSpanish(raw: String): Boolean {
        val digits = raw.filter { it.isDigit() }
        val national = when {
            digits.startsWith("0034") -> digits.drop(4)
            digits.length == 11 && digits.startsWith("34") -> digits.drop(2)
            else -> digits
        }
        return national.length == 9
    }

    val all = listOf(
        Rule(
            Kind.PHONE,
            Regex("""(?<![\d+])(?:(?:\+|00)34[ .-]?)?[6789]\d{2}(?:[ .-]?\d{2,3}){2,3}(?!\d)"""),
            accept = { m, _ -> isSpanish(m.value) },
        ),
        Rule(
            Kind.PHONE,
            Regex("""(?<![\d+])(?:\+|00)(?!34)[1-9]\d{0,2}(?:[ .-]?\d{2,4}){2,5}(?!\d)"""),
            accept = { m, _ -> m.value.count { it.isDigit() } in 8..15 },
        ),
        Rule(
            Kind.PHONE,
            Regex("""(?<![\d+])(?:1[ .-]?)?(?:\(\d{3}\)\s?|\d{3}[ .-])\d{3}[ .-]\d{4}(?![\d-])"""),
        ),
        Rule(
            Kind.PHONE,
            Regex("""(?<![\d+./-])0\d{1,4}(?:[ .-]?\d{2,4}){2,4}(?!\d|[./-]\d)"""),
            accept = { m, _ -> m.value.count { it.isDigit() } in 10..11 && m.value.any { !it.isDigit() } },
            cover = { m, text -> labelled(text, m.range.first) },
        ),
    )

    fun byNumberingPlan(text: String, region: String): List<Hit> =
        runCatching { util.findNumbers(text, region, PhoneNumberUtil.Leniency.VALID, Long.MAX_VALUE).toList() }
            .getOrDefault(emptyList())
            .map { m ->
                val raw = m.rawString()
                val international = raw.startsWith("+") || raw.startsWith("00")
                Hit(Around.trim(text, m.start() until m.end()), international || labelled(text, m.start()))
            }
}
