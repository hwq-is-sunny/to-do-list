package com.campus.todo.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "campus_todo_settings")

enum class AppLanguage(val tag: String) {
    CHINESE("zh-CN"),
    ENGLISH("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: CHINESE
    }
}

enum class ReminderMethod {
    SOUND,
    VIBRATE
}

data class AppSettings(
    val languageTag: String = "",
    val displayWeekCount: Boolean = true,
    val taskReminderEnabled: Boolean = true,
    val tomatoFocusEnabled: Boolean = false,
    val tomatoFocusIntervalMinutes: Int = 30,
    val reminderMethod: ReminderMethod = ReminderMethod.SOUND,
    val ringtoneUri: String = "",
    val aiBaseUrl: String = DEFAULT_AI_BASE_URL,
    val aiApiKey: String = "",
    val aiModel: String = DEFAULT_AI_MODEL
) {
    fun selectedLanguage(systemLanguage: AppLanguage): AppLanguage =
        if (languageTag.isBlank()) systemLanguage else AppLanguage.fromTag(languageTag)

    val normalizedAiBaseUrl: String
        get() = aiBaseUrl.trim().trimEnd('/').ifBlank { DEFAULT_AI_BASE_URL }

    val normalizedAiModel: String
        get() = aiModel.trim().ifBlank { DEFAULT_AI_MODEL }

    companion object {
        const val DEFAULT_AI_BASE_URL = "https://api.deepseek.com"
        const val DEFAULT_AI_MODEL = "deepseek-chat"
    }
}

class SettingsStore(private val context: Context) {

    private val keyLanguageTag = stringPreferencesKey("language_tag")
    private val keyDisplayWeekCount = booleanPreferencesKey("display_week_count")
    private val keyTaskReminderEnabled = booleanPreferencesKey("task_reminder_enabled")
    private val keyTomatoFocusEnabled = booleanPreferencesKey("tomato_focus_enabled")
    private val keyTomatoFocusInterval = intPreferencesKey("tomato_focus_interval_minutes")
    private val keyReminderMethod = stringPreferencesKey("reminder_method")
    private val keyRingtoneUri = stringPreferencesKey("ringtone_uri")
    private val keyAiBaseUrl = stringPreferencesKey("ai_base_url")
    private val keyAiApiKey = stringPreferencesKey("ai_api_key")
    private val keyAiModel = stringPreferencesKey("ai_model")

    val settingsFlow = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            languageTag = prefs[keyLanguageTag].orEmpty(),
            displayWeekCount = prefs[keyDisplayWeekCount] ?: true,
            taskReminderEnabled = prefs[keyTaskReminderEnabled] ?: true,
            tomatoFocusEnabled = prefs[keyTomatoFocusEnabled] ?: false,
            tomatoFocusIntervalMinutes = sanitizeInterval(prefs[keyTomatoFocusInterval] ?: 30),
            reminderMethod = prefs[keyReminderMethod]
                ?.let { raw -> ReminderMethod.entries.firstOrNull { it.name == raw } }
                ?: ReminderMethod.SOUND,
            ringtoneUri = prefs[keyRingtoneUri].orEmpty(),
            aiBaseUrl = prefs[keyAiBaseUrl]?.trim().orEmpty().ifBlank { AppSettings.DEFAULT_AI_BASE_URL },
            aiApiKey = prefs[keyAiApiKey].orEmpty(),
            aiModel = prefs[keyAiModel]?.trim().orEmpty().ifBlank { AppSettings.DEFAULT_AI_MODEL }
        )
    }

    suspend fun currentSettings(): AppSettings = settingsFlow.first()

    suspend fun currentLanguageTag(): String =
        context.settingsDataStore.data.map { it[keyLanguageTag].orEmpty() }.first()

    suspend fun setLanguageTag(languageTag: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyLanguageTag] = languageTag.trim()
        }
    }

    suspend fun setDisplayWeekCount(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyDisplayWeekCount] = enabled
        }
    }

    suspend fun setTaskReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyTaskReminderEnabled] = enabled
        }
    }

    suspend fun setTomatoFocusEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyTomatoFocusEnabled] = enabled
        }
    }

    suspend fun setTomatoFocusIntervalMinutes(minutes: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyTomatoFocusInterval] = sanitizeInterval(minutes)
        }
    }

    suspend fun setReminderMethod(method: ReminderMethod) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyReminderMethod] = method.name
        }
    }

    suspend fun setRingtoneUri(uri: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyRingtoneUri] = uri.trim()
        }
    }

    suspend fun updateAiConfig(baseUrl: String, apiKey: String, model: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyAiBaseUrl] = baseUrl.trim().ifBlank { AppSettings.DEFAULT_AI_BASE_URL }
            prefs[keyAiApiKey] = apiKey.trim()
            prefs[keyAiModel] = model.trim().ifBlank { AppSettings.DEFAULT_AI_MODEL }
        }
    }

    private fun sanitizeInterval(minutes: Int): Int = minutes.coerceAtLeast(15)
}
