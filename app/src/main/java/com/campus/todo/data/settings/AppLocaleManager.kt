package com.campus.todo.data.settings

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object AppLocaleManager {

    fun wrapContext(base: Context, languageTag: String): Context {
        val locale = resolveLocale(languageTag) ?: return base
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        }
        return base.createConfigurationContext(config)
    }

    fun applyToApplication(application: Application, languageTag: String) {
        applyToContextResources(application, languageTag)
    }

    fun applyToActivity(activity: Activity, languageTag: String, recreate: Boolean = true) {
        applyToContextResources(activity.applicationContext, languageTag)
        applyToContextResources(activity, languageTag)
        if (recreate) {
            activity.recreate()
        }
    }

    private fun applyToContextResources(context: Context, languageTag: String) {
        val locale = resolveLocale(languageTag) ?: return
        Locale.setDefault(locale)
        val resources = context.resources
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        }
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun resolveLocale(languageTag: String): Locale? {
        val normalized = languageTag.trim()
        if (normalized.isBlank()) return null
        return Locale.forLanguageTag(normalized)
    }
}
