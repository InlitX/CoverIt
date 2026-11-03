package com.coverit.app.detect.names

import com.coverit.app.detect.text.Around
import com.coverit.app.detect.text.Fold

object NameFinder {

    private const val TOKEN = """(?:\p{Lu}\p{Ll}+(?:[-'’]\p{Lu}?\p{Ll}+)?|\p{Lu}{2,}(?:[-'’]\p{Lu}{2,})?|\p{Lu}\.)"""
    private const val LINK = """(?:de|del|la|las|los|y|i|da|das|do|dos|van|von|der|di|le|DE|DEL|LA|LAS|LOS|Y)"""
    private val run = Regex("""(?<![\p{L}\p{N}])$TOKEN(?:[ \t]+(?:$LINK[ \t]+)*$TOKEN){0,6}(?![\p{L}\p{N}])""")
    private val token = Regex(TOKEN)

    private val label = Regex(
        """(?i)(?<!\p{L})(?:paciente|nombre(?:\s+y\s+apellidos)?|apellidos(?:\s+y\s+nombre)?|titular|interesad[oa]|cliente|usuari[oa]|""" +
            """emplead[oa]|trabajador[a]?|arrendatari[oa]|arrendador[a]?|inquilin[oa]|propietari[oa]|pasajer[oa]|beneficiari[oa]|m[eé]dic[oa]|tenant|landlord|passenger|beneficiary|account\s+holder|holder|physician|doctor|requested\s+by|alumn[oa]|tutor[a]?|padre|madre|c[oó]nyuge|contacto|solicitante|firmado|fdo|""" +
            """atentamente|sr|sra|srta|d|d[ñn]a|don|do[ñn]a|dr|dra|mr|mrs|ms|miss|patient|name|full\s+name|surname|""" +
            """client|employee|attn|signed|sincerely|regards|first\s+name|last\s+name|given\s+name|family\s+name|nom|pr[eé]nom|vorname|nachname|nome|cognome|naam|m|mme|mlle|herr|frau|sig|sig\.ra)\s*[.:]?\s*$""",
    )

    fun find(text: String): List<IntRange> {
        val out = mutableListOf<IntRange>()
        for (m in run.findAll(text)) {
            val words = token.findAll(m.value).map { it.value.trimEnd('.') }.filter { it.isNotEmpty() }.toList()
            val folded = words.map { Fold.text(it) }
            val labelled = label.containsMatchIn(Around.lineBefore(text, m.range.first))
            val place = folded.any { it in NameData.stop && it !in NameData.given && it !in NameData.surnames }
            if (title.matches(m.value + ". ")) continue
            if (labelled) {
                if (!place) out += withoutTitle(text, m.range)
                continue
            }
            if (place) continue
            val trimmed = trimLead(text, m.range, folded) ?: continue
            val rest = folded.takeLast(trimmed.second)
            if (rest.size < 2) {
                attachGiven(text, m.range, rest, out)
                continue
            }
            val given = rest.count { it in NameData.given }
            val surnames = rest.count { it in NameData.surnames }
            if (given >= 1 || surnames >= 2) out += trimmed.first
        }
        heading(text)?.let { h -> if (out.none { it.first <= h.last && h.first <= it.last }) out += h }
        return out.sortedBy { it.first }
    }

    private val links = setOf("de", "del", "la", "las", "los", "y", "i", "da", "das", "do", "dos", "van", "von", "der", "di", "le", "and", "of", "the")
    private val contact = Regex("""[\w.+-]+@[\w-]+\.\w{2,}|(?<!\d)\+?\d[\d .-]{7,}\d(?!\d)""")
    private val wholeLine = Regex("""^[ \t]*($TOKEN(?:[ \t]+(?:$LINK[ \t]+)*$TOKEN){1,4})[ \t]*$""", RegexOption.MULTILINE)

    private fun heading(text: String): IntRange? {
        if (!contact.containsMatchIn(text)) return null
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.take(3).toList()
        for (line in lines) {
            val words = Fold.text(line).split(Regex("""[^\p{L}]+""")).filter { it.isNotEmpty() && it !in links }
            if (words.isEmpty() || words.all { it in NameData.heading }) continue
            if (words.any { it in NameData.heading || (it in NameData.stop && it !in NameData.given && it !in NameData.surnames) }) return null
            val m = wholeLine.find(text, text.indexOf(line).coerceAtLeast(0)) ?: return null
            val g = m.groups[1]!!.range
            if (text.substring(g.first, g.last + 1) != line) return null
            if (token.findAll(line).any { it.value.length < 2 }) return null
            return g
        }
        return null
    }

    private fun trimLead(text: String, range: IntRange, folded: List<String>): Pair<IntRange, Int>? {
        val first = folded.indexOfFirst { it in NameData.given || it in NameData.surnames || it.length == 1 }
        if (first < 0) return null
        if (first == 0) return range to folded.size
        var start = range.first
        var seen = 0
        for (t in token.findAll(text.substring(range.first, range.last + 1))) {
            if (t.value.trimEnd('.').isEmpty()) continue
            if (seen == first) {
                start = range.first + t.range.first
                break
            }
            seen++
        }
        return start..range.last to folded.size - first
    }

    private fun attachGiven(text: String, range: IntRange, rest: List<String>, out: MutableList<IntRange>) {
        if (rest.size != 1 || rest[0] !in NameData.given) return
        val previous = out.lastOrNull() ?: return
        val between = text.substring(previous.last + 1, range.first)
        if (between.matches(Regex("""\s*,\s*"""))) out[out.lastIndex] = previous.first..range.last
    }

    private val title = Regex("""^(?i)(?:sr|sra|srta|dr|dra|d|d[ñn]a|don|do[ñn]a|mr|mrs|ms|miss)\.?\s+""")

    private fun withoutTitle(text: String, range: IntRange): IntRange {
        val m = title.find(text.substring(range.first, range.last + 1)) ?: return range
        return if (m.range.last < range.last - range.first) range.first + m.range.last + 1..range.last else range
    }
}
