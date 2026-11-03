package com.coverit.app.detect.text

import java.text.Normalizer

object Fold {

    fun char(c: Char): Char {
        if (c.code < 128) return c.lowercaseChar()
        val base = Normalizer.normalize(c.toString(), Normalizer.Form.NFD).firstOrNull() ?: c
        return base.lowercaseChar()
    }

    fun text(s: String): String = buildString(s.length) { for (c in s) append(char(c)) }
}
