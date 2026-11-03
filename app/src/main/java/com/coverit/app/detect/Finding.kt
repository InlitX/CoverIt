package com.coverit.app.detect

data class Finding(
    val kind: Kind,
    val start: Int,
    val end: Int,
    val value: String,
    val covered: Boolean = true,
)
