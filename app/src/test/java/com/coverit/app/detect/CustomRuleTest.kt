package com.coverit.app.detect

import org.junit.Assert.assertEquals
import org.junit.Test

class CustomRuleTest {

    @Test
    fun formatMatchesCodesOfTheSameShape() {
        val regex = Regex(CustomRule.format("EXP-2024/0381"))
        assertEquals("AAA-0000/0000", CustomRule.describe("EXP-2024/0381"))
        val text = "Ver EXP-2024/0381 y EXP-2023/1120, no EX-2024/0381 ni EXPS-2024/0381."
        assertEquals(listOf("EXP-2024/0381", "EXP-2023/1120"), regex.findAll(text).map { it.value }.toList())
    }

    @Test
    fun exactIgnoresCaseAndSpacing() {
        val regex = Regex(CustomRule.exact("Proyecto  Atlas"))
        assertEquals(2, regex.findAll("proyecto atlas, PROYECTO\nATLAS, proyectos atlas").count())
    }
}
