package com.coverit.app.ui

import com.coverit.app.doc.Report

enum class Tab { HOME, LIBRARY, SETTINGS }

sealed interface Route {
    data object Home : Route
    data object Onboarding : Route
    data class Working(val message: Int) : Route
    data object Editor : Route
    data class Result(val recordId: String, val report: Report, val hiddenOnly: Boolean = false) : Route
    data class Cleaned(val items: List<Item>, val failed: Int) : Route {
        class Item(val recordId: String, val removed: Int, val leftovers: List<String>)
    }
    data class Detail(val recordId: String) : Route
    data object Detection : Route
    data object About : Route
    data object Privacy : Route
    data class Failed(val message: Int) : Route
}
