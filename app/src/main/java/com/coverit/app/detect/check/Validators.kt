package com.coverit.app.detect.check

import java.math.BigInteger

object Validators {

    private const val DNI_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE"

    private val IBAN_LENGTHS = mapOf(
        "AD" to 24, "AE" to 23, "AL" to 28, "AT" to 20, "AZ" to 28, "BA" to 20, "BE" to 16, "BG" to 22,
        "BH" to 22, "BI" to 27, "BR" to 29, "BY" to 28, "CH" to 21, "CR" to 22, "CY" to 28, "CZ" to 24,
        "DE" to 22, "DJ" to 27, "DK" to 18, "DO" to 28, "EE" to 20, "EG" to 29, "ES" to 24, "FI" to 18,
        "FK" to 18, "FO" to 18, "FR" to 27, "GB" to 22, "GE" to 22, "GI" to 23, "GL" to 18, "GR" to 27,
        "GT" to 28, "HR" to 21, "HU" to 28, "IE" to 22, "IL" to 23, "IQ" to 23, "IS" to 26, "IT" to 27,
        "JO" to 30, "KW" to 30, "KZ" to 20, "LB" to 28, "LC" to 32, "LI" to 21, "LT" to 20, "LU" to 20,
        "LV" to 21, "LY" to 25, "MC" to 27, "MD" to 24, "ME" to 22, "MK" to 19, "MN" to 20, "MR" to 27,
        "MT" to 31, "MU" to 30, "NI" to 28, "NL" to 18, "NO" to 15, "OM" to 23, "PK" to 24, "PL" to 28,
        "PS" to 29, "PT" to 25, "QA" to 29, "RO" to 24, "RS" to 22, "RU" to 33, "SA" to 24, "SC" to 31,
        "SD" to 18, "SE" to 24, "SI" to 19, "SK" to 24, "SM" to 27, "SO" to 23, "ST" to 25, "SV" to 28,
        "TL" to 23, "TN" to 24, "TR" to 26, "UA" to 29, "VA" to 22, "VG" to 24, "XK" to 20, "YE" to 30,
    )

    private const val FISCAL_ODD = "BAKPLCQDREVOSFTGUHMINJWZYX"

    fun isValidDni(raw: String): Boolean {
        val s = compact(raw)
        if (!Regex("""\d{8}[A-Z]""").matches(s)) return false
        return DNI_LETTERS[s.take(8).toInt() % 23] == s.last()
    }

    fun isValidNie(raw: String): Boolean {
        val s = compact(raw)
        if (!Regex("""[XYZ]\d{7}[A-Z]""").matches(s)) return false
        val number = ("XYZ".indexOf(s.first()).toString() + s.substring(1, 8)).toInt()
        return DNI_LETTERS[number % 23] == s.last()
    }

    fun isValidIban(raw: String): Boolean {
        val s = compact(raw)
        if (!Regex("""[A-Z]{2}\d{2}[A-Z0-9]{11,30}""").matches(s)) return false
        if (IBAN_LENGTHS[s.take(2)] != s.length) return false
        return mod97(s.drop(4) + s.take(4)) == 1
    }

    fun isValidLuhn(raw: String): Boolean {
        val digits = raw.filter { it.isDigit() }
        if (digits.length !in 13..19) return false
        if (digits.all { it == digits[0] }) return false
        var sum = 0
        for ((i, c) in digits.reversed().withIndex()) {
            var d = c - '0'
            if (i % 2 == 1) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
        }
        return sum % 10 == 0
    }

    fun isCardNumber(raw: String): Boolean = raw.firstOrNull { it.isDigit() } in '2'..'6' && isValidLuhn(raw)

    fun isValidNss(raw: String): Boolean {
        val s = raw.filter { it.isDigit() }
        if (s.length != 12) return false
        val province = s.take(2).toInt()
        if (province !in 1..53 && province != 66) return false
        val middle = s.substring(2, 10).toLong()
        val base = if (middle < 10_000_000) province * 10_000_000L + middle else s.take(10).toLong()
        return base % 97 == s.takeLast(2).toLong()
    }

    fun isValidCodiceFiscale(raw: String): Boolean {
        val s = compact(raw)
        if (!Regex("""[A-Z]{6}[0-9LMNPQRSTUV]{2}[A-EHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]""").matches(s)) return false
        var sum = 0
        for (i in 0 until 15) {
            val c = s[i]
            val v = if (c.isDigit()) c - '0' else c - 'A'
            sum += if (i % 2 == 0) FISCAL_ODD.indexOf('A' + v) else v
        }
        return 'A' + sum % 26 == s[15]
    }

    fun isValidCpf(raw: String): Boolean {
        val d = raw.filter { it.isDigit() }.map { it - '0' }
        if (d.size != 11 || d.all { it == d[0] }) return false
        fun check(n: Int) = (0 until n).sumOf { d[it] * (n + 1 - it) } * 10 % 11 % 10
        return check(9) == d[9] && check(10) == d[10]
    }

    fun isValidNir(raw: String): Boolean {
        val s = compact(raw)
        if (!Regex("""[12]\d{4}(?:\d{2}|2A|2B)\d{6}\d{2}""").matches(s)) return false
        val body = s.take(13).replace("2A", "19").replace("2B", "18").toLong()
        return 97 - body % 97 == s.takeLast(2).toLong()
    }

    fun isValidRut(raw: String): Boolean {
        val s = compact(raw)
        if (s.length < 7) return false
        val body = s.dropLast(1)
        if (!body.all { it.isDigit() }) return false
        var sum = 0
        var factor = 2
        for (c in body.reversed()) {
            sum += (c - '0') * factor
            factor = if (factor == 7) 2 else factor + 1
        }
        val expected = when (val r = 11 - sum % 11) {
            11 -> '0'
            10 -> 'K'
            else -> '0' + r
        }
        return expected == s.last()
    }

    fun isValidNino(raw: String): Boolean {
        val s = compact(raw)
        if (!Regex("""[A-CEGHJ-PR-TW-Z][A-CEGHJ-NPR-TW-Z]\d{6}[A-D]""").matches(s)) return false
        return s.take(2) !in setOf("BG", "GB", "NK", "KN", "TN", "NT", "ZZ")
    }

    fun isValidIpv4(raw: String): Boolean {
        val parts = raw.split('.')
        if (parts.size != 4) return false
        return parts.all { p -> p.isNotEmpty() && p.length <= 3 && p.toInt() <= 255 && (p.length == 1 || p[0] != '0') }
    }

    fun isValidIpv6(raw: String): Boolean {
        if (raw.length < 6) return false
        val halves = raw.split("::")
        if (halves.size > 2) return false
        val groups = halves.flatMap { h -> if (h.isEmpty()) emptyList() else h.split(':') }
        if (groups.any { it.isEmpty() || it.length > 4 || !it.all { c -> c.isDigit() || c.lowercaseChar() in 'a'..'f' } }) return false
        return if (halves.size == 2) groups.size <= 7 else groups.size == 8
    }

    fun isValidDate(day: Int, month: Int, year: Int): Boolean {
        if (year !in 1900..2100 || month !in 1..12 || day < 1) return false
        val days = when (month) {
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return day <= days
    }

    fun isValidCompactDate(yymmdd: String): Boolean {
        if (yymmdd.length != 6 || !yymmdd.all { it.isDigit() }) return false
        val yy = yymmdd.take(2).toInt()
        val month = yymmdd.substring(2, 4).toInt()
        val day = yymmdd.substring(4).toInt()
        return isValidDate(day, month, 1900 + yy) || isValidDate(day, month, 2000 + yy)
    }

    private fun mod97(alnum: String): Int {
        val digits = buildString { for (c in alnum) if (c.isDigit()) append(c) else append(c - 'A' + 10) }
        return BigInteger(digits).mod(BigInteger.valueOf(97)).toInt()
    }

    private fun compact(raw: String) = raw.uppercase().filter { it.isLetterOrDigit() }
}
