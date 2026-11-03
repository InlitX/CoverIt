package com.coverit.app.detect

import com.coverit.app.detect.names.NameFinder
import com.coverit.app.detect.names.NameMatcher
import com.coverit.app.detect.rules.Contact
import com.coverit.app.detect.rules.Dates
import com.coverit.app.detect.rules.Details
import com.coverit.app.detect.rules.Ids
import com.coverit.app.detect.rules.Money
import com.coverit.app.detect.rules.Network
import com.coverit.app.detect.rules.Phones
import com.coverit.app.detect.rules.Places
import com.coverit.app.detect.rules.Rule
import com.coverit.app.detect.text.Around
import java.util.Locale

object Detector {

    @Volatile
    var region: String = Locale.getDefault().country.ifEmpty { "US" }

    // Order matters: the first rule to claim some text keeps it. Emails go before names so
    // "ana.perez@..." stays one email, and labelled IDs before phones.
    private val rules: List<Rule> =
        Money.all + Ids.spain + Network.all + Ids.world + Phones.all + Dates.all + Places.all + Places.spain + Details.all

    fun find(
        text: String,
        names: NameMatcher? = null,
        enabled: Set<Kind> = Kind.detectable.toSet(),
        custom: List<CustomRule> = emptyList(),
    ): List<Finding> {
        val claims = Claims(text)
        for (rule in custom) rule.regex?.findAll(text)?.forEach { claims.take(Kind.CUSTOM, it.range) }
        apply(Contact.early, text, enabled, claims)
        if (Kind.NAME in enabled) {
            names?.find(text)?.forEach { claims.take(Kind.NAME, it) }
            NameFinder.find(text).forEach { claims.take(Kind.NAME, it) }
        }
        apply(rules, text, enabled, claims)
        if (Kind.PHONE in enabled) Phones.byNumberingPlan(text, region).forEach { claims.take(Kind.PHONE, it.range, it.covered) }
        return claims.found.sortedBy { it.start }
    }

    private fun apply(rules: List<Rule>, text: String, enabled: Set<Kind>, claims: Claims) {
        for (rule in rules) {
            if (rule.kind !in enabled) continue
            for (m in rule.regex.findAll(text)) {
                val range = Around.trim(text, m.groups[rule.group]?.range ?: continue)
                if (!claims.free(range) || !rule.accept(m, text)) continue
                claims.take(rule.kind, range, rule.cover(m, text))
            }
        }
    }

    private class Claims(private val text: String) {
        private val taken = BooleanArray(text.length)
        val found = mutableListOf<Finding>()

        fun free(range: IntRange) = !range.isEmpty() && range.none { taken[it] }

        fun take(kind: Kind, range: IntRange, covered: Boolean = true) {
            if (!free(range)) return
            range.forEach { taken[it] = true }
            found += Finding(kind, range.first, range.last + 1, text.substring(range.first, range.last + 1), covered)
        }
    }
}
