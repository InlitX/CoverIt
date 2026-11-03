package com.coverit.app.detect.rules

import com.coverit.app.detect.Kind
import com.coverit.app.detect.check.Validators
import com.coverit.app.detect.text.Around
import com.coverit.app.detect.text.Fold

object Dates {

    private const val MONTHS_ES = "enero|febrero|marzo|abril|mayo|junio|julio|agosto|sep?tiembre|octubre|noviembre|diciembre"
    private const val MONTHS_EN = "january|february|march|april|may|june|july|august|september|october|november|december|" +
        "jan|feb|mar|apr|jun|jul|aug|sept?|oct|nov|dec"
    private const val MONTHS_FR = "janvier|f[ée]vrier|mars|avril|mai|juin|juillet|ao[uû]t|septembre|octobre|novembre|d[ée]cembre"
    private const val MONTHS_PT = "janeiro|fevereiro|mar[cç]o|maio|junho|julho|setembro|outubro|novembro|dezembro"
    private const val MONTHS_IT = "gennaio|febbraio|aprile|maggio|giugno|luglio|settembre|ottobre|dicembre"
    private const val MONTHS_DE = "januar|februar|m[aä]rz|juni|juli|oktober|dezember"
    private const val MONTHS = "$MONTHS_ES|$MONTHS_EN|$MONTHS_FR|$MONTHS_PT|$MONTHS_IT|$MONTHS_DE"

    private val names = listOf(
        listOf("enero", "january", "janvier", "janeiro", "gennaio", "januar", "jan"),
        listOf("febrero", "february", "fevrier", "fevereiro", "febbraio", "februar", "feb"),
        listOf("marzo", "march", "mars", "marco", "marz", "maerz", "mar"),
        listOf("abril", "april", "avril", "aprile", "apr"),
        listOf("mayo", "may", "mai", "maio", "maggio"),
        listOf("junio", "june", "juin", "junho", "giugno", "juni", "jun"),
        listOf("julio", "july", "juillet", "julho", "luglio", "juli", "jul"),
        listOf("agosto", "august", "aout", "aug"),
        listOf("septiembre", "setiembre", "september", "septembre", "setembro", "settembre", "sep", "sept"),
        listOf("octubre", "october", "octobre", "outubro", "ottobre", "oktober", "oct"),
        listOf("noviembre", "november", "novembre", "novembro", "nov"),
        listOf("diciembre", "december", "decembre", "dezembro", "dicembre", "dezember", "dec"),
    )

    private val birth = Regex("""(?i)(nac|naci|birth|born|f\.\s?n\.|d\.o\.b|dob|geboren|geburt|n[ée]e?\s+le|naissance|nascimento|nascita|nato|nata)""")

    private fun month(name: String): Int {
        val n = Fold.text(name.trimEnd('.'))
        return names.indexOfFirst { n in it } + 1
    }

    private fun isBirth(m: MatchResult, text: String) = birth.containsMatchIn(Around.sentenceBefore(text, m.range.first, 40))

    private fun valid(day: String, month: Int, year: String) = Validators.isValidDate(day.toInt(), month, year.toInt())

    val all = listOf(
        Rule(
            Kind.DATE,
            Regex("""(?<!\d)(\d{1,2})[/.-](\d{1,2})[/.-](\d{4})(?!\d)"""),
            accept = { m, _ ->
                val (a, b, year) = m.destructured
                valid(a, b.toInt(), year) || valid(b, a.toInt(), year)
            },
            cover = ::isBirth,
        ),
        Rule(
            Kind.DATE,
            Regex("""(?<![\d/.-])(\d{1,2})[/.-](\d{1,2})[/.-](\d{2})(?![\d/-]|\.\d)"""),
            accept = { m, text ->
                val (a, b, yy) = m.destructured
                isBirth(m, text) && (valid(a, b.toInt(), "19$yy") || valid(b, a.toInt(), "19$yy"))
            },
        ),
        Rule(
            Kind.DATE,
            Regex("""(?<!\d)(\d{4})-(\d{2})-(\d{2})(?!\d)"""),
            accept = { m, _ -> valid(m.groupValues[3], m.groupValues[2].toInt(), m.groupValues[1]) },
            cover = ::isBirth,
        ),
        Rule(
            Kind.DATE,
            Regex("""(?i)(?<![\p{L}\d])(\d{1,2})(?:º|st|nd|rd|th|er|\.)?(?:\s+de\s+|\s+|-)($MONTHS)\.?(?:\s+de\s+|,?\s+|-)(\d{4})(?!\d)"""),
            accept = { m, _ -> valid(m.groupValues[1], month(m.groupValues[2]), m.groupValues[3]) },
            cover = ::isBirth,
        ),
        Rule(
            Kind.DATE,
            Regex("""(?i)(?<![\p{L}\d])($MONTHS_EN)\.?\s+(\d{1,2})(?:st|nd|rd|th)?,?\s+(\d{4})(?!\d)"""),
            accept = { m, _ -> valid(m.groupValues[2], month(m.groupValues[1]), m.groupValues[3]) },
            cover = ::isBirth,
        ),
    )
}
