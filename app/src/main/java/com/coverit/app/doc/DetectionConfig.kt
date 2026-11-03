package com.coverit.app.doc

import com.coverit.app.detect.CustomRule
import com.coverit.app.detect.Detector
import com.coverit.app.detect.Kind
import com.coverit.app.detect.names.NameFinder
import com.coverit.app.detect.names.NameMatcher

class DetectionConfig(val enabled: Set<Kind>, val custom: List<CustomRule>) {

    val searches get() = enabled.isNotEmpty() || custom.isNotEmpty()

    fun find(text: String, names: NameMatcher? = null) = Detector.find(text, names, enabled, custom)

    fun namesIn(texts: List<String>): NameMatcher? {
        if (Kind.NAME !in enabled) return null
        val found = texts.flatMap { t -> NameFinder.find(t).map { t.substring(it.first, it.last + 1) } }
            .map { it.replace(Regex("""\s+"""), " ").trim() }
            .filter { it.split(' ').size >= 2 }
            .distinct()
        return NameMatcher(found, includeGiven = false).takeUnless { it.isEmpty }
    }

    companion object {
        val NONE = DetectionConfig(emptySet(), emptyList())
    }
}
