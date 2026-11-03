package com.coverit.app.detect.names

import com.coverit.app.detect.text.Fold

class NameMatcher(names: List<String>, includeGiven: Boolean) {

    private class Part(val text: String, val initial: Boolean = false)

    private val patterns: List<Regex> = names
        .flatMap { variants(it, includeGiven) }
        .distinctBy { key(it) }
        .sortedByDescending { v -> v.sumOf { it.text.length } }
        .map { regexOf(it) }

    val isEmpty get() = patterns.isEmpty()

    fun find(text: String): List<IntRange> {
        val folded = Fold.text(text)
        val taken = BooleanArray(text.length)
        val out = mutableListOf<IntRange>()
        for (p in patterns) {
            for (m in p.findAll(folded)) {
                if (m.range.any { taken[it] }) continue
                m.range.forEach { taken[it] = true }
                out += m.range
            }
        }
        return out.sortedBy { it.first }
    }

    companion object {

        private val particles = setOf("de", "del", "la", "las", "los", "y", "da", "das", "do", "dos", "van", "von", "di", "le")

        fun describe(name: String, includeGiven: Boolean): List<String> =
            variants(name, includeGiven).distinctBy { key(it) }.map { v ->
                v.joinToString(" ") { if (it.initial) it.text.uppercase() + "." else it.text }
            }

        private fun key(v: List<Part>) = v.joinToString("|") { (if (it.initial) "*" else "") + Fold.text(it.text) }

        private fun variants(name: String, includeGiven: Boolean): List<List<Part>> {
            val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (words.isEmpty()) return emptyList()
            val full = listOf(words.map { Part(it) })
            val core = words.indices.filter { Fold.text(words[it]) !in particles }
            if (core.size < 2) return full

            val surnameCount = if (core.size >= 3) 2 else 1
            val firstSurname = core[core.size - surnameCount]
            var surnameStart = firstSurname
            while (surnameStart - 1 > core.first() && Fold.text(words[surnameStart - 1]) in particles) surnameStart--
            val given = words.subList(0, surnameStart).filter { Fold.text(it) !in particles }
            val surnameWords = words.subList(surnameStart, words.size)
            val surnameGroups = splitSurnames(surnameWords)

            val out = mutableListOf<List<Part>>()
            out += full
            out += surnameWords.map { Part(it) }
            surnameGroups.filter { g -> g.any { Fold.text(it) !in particles && it.length >= 3 } }
                .forEach { g -> out += g.map { Part(it) } }
            out += listOf(Part(given.first())) + surnameGroups.first().map { Part(it) }
            out += given.map { Part(it.take(1), initial = true) } + surnameWords.map { Part(it) }
            out += given.map { Part(it.take(1), initial = true) } + surnameGroups.first().map { Part(it) }
            out += core.map { Part(words[it].take(1), initial = true) }
            if (includeGiven) given.filter { it.length >= 3 }.forEach { out += listOf(Part(it)) }
            return out
        }

        private fun splitSurnames(words: List<String>): List<List<String>> {
            val groups = mutableListOf<MutableList<String>>()
            var pending = mutableListOf<String>()
            for (w in words) {
                pending += w
                if (Fold.text(w) !in particles) {
                    groups += pending
                    pending = mutableListOf()
                }
            }
            if (pending.isNotEmpty()) groups.lastOrNull()?.addAll(pending) ?: groups.add(pending)
            return groups
        }

        private fun regexOf(parts: List<Part>): Regex {
            val body = StringBuilder()
            parts.forEachIndexed { i, p ->
                if (i > 0) body.append(if (parts[i - 1].initial) """\s*""" else """[\s ]+""")
                body.append(Regex.escape(Fold.text(p.text)))
                if (p.initial) body.append("""\.""")
            }
            return Regex("""(?<![\p{L}\p{N}])$body(?![\p{L}\p{N}])""")
        }
    }
}
