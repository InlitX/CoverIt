package com.coverit.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.coverit.app.detect.CustomRule
import com.coverit.app.detect.Kind
import com.coverit.app.redact.Style
import com.coverit.app.ui.AppLanguage
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("coverit", Context.MODE_PRIVATE)

    var theme by mutableStateOf(enumOr(sp.getString("theme", null), ThemeMode.SYSTEM))
        private set
    var style by mutableStateOf(enumOr(sp.getString("style", null), Style.BLACK))
        private set
    var keepOriginal by mutableStateOf(sp.getBoolean("keepOriginal", true))
        private set
    var space by mutableStateOf(Space(sp.getString("space", null) ?: Space.PERSONAL.id))
        private set
    var onboarded by mutableStateOf(sp.getBoolean("onboarded", false))
        private set
    var disabled by mutableStateOf(
        if (sp.contains("disabled")) {
            readSet("disabled").mapNotNull { n -> Kind.entries.firstOrNull { it.name == n } }.toSet()
        } else {
            if (AppLanguage.system.country.equals("ES", ignoreCase = true)) emptySet() else Kind.spain
        },
    )
        private set

    val rules = mutableStateListOf<CustomRule>().apply { addAll(readRules()) }

    val enabledKinds get() = Kind.detectable.toSet() - disabled

    fun updateTheme(value: ThemeMode) {
        theme = value
        sp.edit().putString("theme", value.name).apply()
    }

    fun updateStyle(value: Style) {
        style = value
        sp.edit().putString("style", value.name).apply()
    }

    fun updateKeepOriginal(value: Boolean) {
        keepOriginal = value
        sp.edit().putBoolean("keepOriginal", value).apply()
    }

    fun updateOnboarded(value: Boolean) {
        onboarded = value
        sp.edit().putBoolean("onboarded", value).apply()
    }

    fun toggleKind(kind: Kind, on: Boolean) {
        disabled = if (on) disabled - kind else disabled + kind
        sp.edit().putStringSet("disabled", disabled.map { it.name }.toSet()).apply()
    }

    fun addRule(label: String, pattern: String): Boolean {
        if (rules.any { it.pattern == pattern }) return false
        rules.add(0, CustomRule(UUID.randomUUID().toString(), label.trim(), pattern))
        writeRules()
        return true
    }

    fun removeRule(rule: CustomRule) {
        rules.remove(rule)
        writeRules()
    }

    fun updateSpace(value: Space) {
        space = value
        sp.edit().putString("space", value.id).apply()
    }

    private fun readRules(): List<CustomRule> {
        val raw = sp.getString("rules", null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { array.getJSONObject(it) }.map { CustomRule(it.getString("id"), it.getString("label"), it.getString("pattern")) }
    }

    private fun writeRules() {
        val array = JSONArray(rules.map { JSONObject().put("id", it.id).put("label", it.label).put("pattern", it.pattern) })
        sp.edit().putString("rules", array.toString()).apply()
    }

    private fun readSet(key: String): Set<String> = sp.getStringSet(key, emptySet()).orEmpty()

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
