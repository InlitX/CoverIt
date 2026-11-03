package com.coverit.app.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    EN("en"),
    ES("es"),
    ;

    val locale: Locale get() = tag?.let { Locale.forLanguageTag(it) } ?: system

    companion object {
        private const val KEY = "language"

        fun read(context: Context): AppLanguage {
            val tag = context.getSharedPreferences("coverit", Context.MODE_PRIVATE).getString(KEY, null)
            return entries.firstOrNull { it.tag == tag } ?: SYSTEM
        }

        fun write(context: Context, language: AppLanguage) {
            context.getSharedPreferences("coverit", Context.MODE_PRIVATE).edit().putString(KEY, language.tag).apply()
        }

        val system: Locale get() = Resources.getSystem().configuration.locales[0]

        fun wrap(base: Context): Context {
            val locale = read(base).locale
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            return base.createConfigurationContext(config)
        }

        fun localized(activity: Context, language: AppLanguage): Context {
            val locale = language.locale
            Locale.setDefault(locale)
            val config = Configuration(activity.resources.configuration)
            config.setLocale(locale)
            val resources = activity.createConfigurationContext(config).resources
            return object : ContextWrapper(activity) {
                override fun getResources(): Resources = resources
            }
        }
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.SYSTEM }
