package com.coverit.app.detect.rules

import com.coverit.app.detect.Kind

class Rule(
    val kind: Kind,
    val regex: Regex,
    val group: Int = 0,
    val accept: (MatchResult, String) -> Boolean = { _, _ -> true },
    val cover: (MatchResult, String) -> Boolean = { _, _ -> true },
)
