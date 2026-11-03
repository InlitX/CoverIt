package com.coverit.app.detect

class CustomRule(val id: String, val label: String, val pattern: String) {

    val regex: Regex? by lazy { runCatching { Regex(pattern) }.getOrNull() }

    companion object {

        private const val BEFORE = """(?<![\p{L}\p{N}])"""
        private const val AFTER = """(?![\p{L}\p{N}])"""

        fun exact(sample: String): String = "(?i)" + BEFORE + sample.trim().split(Regex("\\s+")).joinToString("""\s+""") { Regex.escape(it) } + AFTER

        fun hasFormat(sample: String) = sample.any { it.isDigit() } && sample.trim().length >= 3

        fun format(sample: String): String {
            val body = StringBuilder()
            runs(sample.trim()) { kind, length, text ->
                body.append(
                    when (kind) {
                        'A' -> """\p{Lu}{$length}"""
                        'a' -> """\p{Ll}{$length}"""
                        '0' -> """\d{$length}"""
                        ' ' -> """\s+"""
                        else -> Regex.escape(text)
                    },
                )
            }
            return BEFORE + body + AFTER
        }

        fun describe(sample: String): String = buildString {
            runs(sample.trim()) { kind, length, text ->
                append(if (kind == '?') text else if (kind == ' ') " " else kind.toString().repeat(length))
            }
        }

        private fun classOf(c: Char) = when {
            c.isDigit() -> '0'
            c.isUpperCase() -> 'A'
            c.isLowerCase() -> 'a'
            c.isWhitespace() -> ' '
            else -> '?'
        }

        private inline fun runs(s: String, each: (Char, Int, String) -> Unit) {
            var i = 0
            while (i < s.length) {
                val kind = classOf(s[i])
                var j = i + 1
                while (j < s.length && classOf(s[j]) == kind && (kind != '?' || s[j] == s[i])) j++
                each(kind, j - i, s.substring(i, j))
                i = j
            }
        }
    }
}
