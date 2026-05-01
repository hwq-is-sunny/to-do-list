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
            values().firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: CHINESE
    }
}

enum class ReminderMethod {
    SOUND,
    VIBRATE,
    SOUND_AND_VIBRATE,
    SILENT
}

enum class LlmProvider {
    DEEPSEEK,
    CUSTOM
}

data class LlmConfig(
    val enabled: Boolean,
    val provider: LlmProvider,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val timeoutSeconds: Int
)

data class AppSettings(
    val nickname: String = "",
    val languageTag: String = AppLanguage.CHINESE.tag,
    val notificationsEnabled: Boolean = true,
    val aiParseEnabled: Boolean = true,
    val displayWeekCount: Boolean = true,
    val taskReminderEnabled: Boolean = true,
    val tomatoFocusEnabled: Boolean = false,
    val tomatoFocusIntervalMinutes: Int = 30,
    val reminderMethod: ReminderMethod = ReminderMethod.SOUND,
    val ringtoneUri: String = "",
    val llmProvider: String = "DEEPSEEK",
    val aiBaseUrl: String = DEFAULT_AI_BASE_URL,
    val aiApiKey: String = "",
    val aiModel: String = DEFAULT_AI_MODEL,
    val aiTimeoutSeconds: Int = 45,
    val defaultReminderMinutes: Int = 30,
    val defaultHomeTab: String = "today",
    val showCompletedTasks: Boolean = true,
    /** 是否将微信/QQ/知到等应用通知写入候选箱（需系统「通知使用权」） */
    val notificationImportEnabled: Boolean = false,
    /** 截止前 2 小时 / 30 分钟提醒是否使用震动 */
    val deadlineVibrateEnabled: Boolean = true,
    /** 截止前 30 分钟是否使用强提醒（更高优先级与通道） */
    val deadlineStrongReminderEnabled: Boolean = true
) {
    fun selectedLanguage(systemLanguage: AppLanguage): AppLanguage =
        if (languageTag.isBlank()) systemLanguage else AppLanguage.fromTag(languageTag)

    val normalizedAiBaseUrl: String
        get() = aiBaseUrl.trim().trimEnd('/').ifBlank { DEFAULT_AI_BASE_URL }

    val normalizedAiModel: String
        get() = aiModel.trim().ifBlank { DEFAULT_AI_MODEL }

    val normalizedTimeoutSeconds: Int
        get() = aiTimeoutSeconds.coerceIn(10, 120)

    companion object {
        const val DEFAULT_AI_BASE_URL = "https://api.deepseek.com"
        const val DEFAULT_AI_MODEL = "deepseek-chat"
    }
}

class SettingsStore(private val context: Context) {

    private val keyNickname = stringPreferencesKey("nickname")
    private val keyLanguageTag = stringPreferencesKey("language_tag")
    private val keyNotificationsEnabled = booleanPreferencesKey("notifications_enabled")
    private val keyAiParseEnabled = booleanPreferencesKey("ai_parse_enabled")
    private val keyDisplayWeekCount = booleanPreferencesKey("display_week_count")
    private val keyTaskReminderEnabled = booleanPreferencesKey("task_reminder_enabled")
    private val keyTomatoFocusEnabled = booleanPreferencesKey("tomato_focus_enabled")
    private val keyTomatoFocusInterval = intPreferencesKey("tomato_focus_interval_minutes")
    private val keyReminderMethod = stringPreferencesKey("reminder_method")
    private val keyRingtoneUri = stringPreferencesKey("ringtone_uri")
    private val keyLlmProvider = stringPreferencesKey("llm_provider")
    private val keyAiBaseUrl = stringPreferencesKey("ai_base_url")
    private val keyAiApiKey = stringPreferencesKey("ai_api_key")
    private val keyAiModel = stringPreferencesKey("ai_model")
    private val keyAiTimeoutSeconds = intPreferencesKey("ai_timeout_seconds")
    private val keyDefaultReminderMinutes = intPreferencesKey("default_reminder_minutes")
    private val keyDefaultHomeTab = stringPreferencesKey("default_home_tab")
    private val keyShowCompletedTasks = booleanPreferencesKey("show_completed_tasks")
    private val keyNotificationImportEnabled = booleanPreferencesKey("notification_import_enabled")
    private val keyDeadlineVibrateEnabled = booleanPreferencesKey("deadline_vibrate_enabled")
    private val keyDeadlineStrongReminderEnabled = booleanPreferencesKey("deadline_strong_reminder_enabled")

    val settingsFlow = context.settingsDataStore.data.map { prefs ->
        val rawLang = prefs[keyLanguageTag].orEmpty().ifBlank { AppLanguage.CHINESE.tag }
        val langTag = if (AppLanguage.fromTag(rawLang) == AppLanguage.ENGLISH) {
            AppLanguage.CHINESE.tag
        } else {
            rawLang
        }
        AppSettings(
            nickname = prefs[keyNickname].orEmpty(),
            languageTag = langTag,
            notificationsEnabled = prefs[keyNotificationsEnabled] ?: true,
            aiParseEnabled = prefs[keyAiParseEnabled] ?: true,
            displayWeekCount = prefs[keyDisplayWeekCount] ?: true,
            taskReminderEnabled = prefs[keyTaskReminderEnabled] ?: true,
            tomatoFocusEnabled = prefs[keyTomatoFocusEnabled] ?: false,
            tomatoFocusIntervalMinutes = sanitizeInterval(prefs[keyTomatoFocusInterval] ?: 30),
            reminderMethod = prefs[keyReminderMethod]
                ?.let { raw -> ReminderMethod.values().firstOrNull { it.name == raw } }
                ?: ReminderMethod.SOUND,
            ringtoneUri = prefs[keyRingtoneUri].orEmpty(),
            llmProvider = prefs[keyLlmProvider].orEmpty().ifBlank { LlmProvider.DEEPSEEK.name },
            aiBaseUrl = prefs[keyAiBaseUrl]?.trim().orEmpty().ifBlank { AppSettings.DEFAULT_AI_BASE_URL },
            aiApiKey = prefs[keyAiApiKey].orEmpty(),
            aiModel = prefs[keyAiModel]?.trim().orEmpty().ifBlank { AppSettings.DEFAULT_AI_MODEL },
            aiTimeoutSeconds = (prefs[keyAiTimeoutSeconds] ?: 45).coerceIn(10, 120),
            defaultReminderMinutes = sanitizeInterval(prefs[keyDefaultReminderMinutes] ?: 30),
            defaultHomeTab = prefs[keyDefaultHomeTab].orEmpty().ifBlank { "today" },
            showCompletedTasks = prefs[keyShowCompletedTasks] ?: true,
            notificationImportEnabled = prefs[keyNotificationImportEnabled] ?: false,
            deadlineVibrateEnabled = prefs[keyDeadlineVibrateEnabled] ?: true,
            deadlineStrongReminderEnabled = prefs[keyDeadlineStrongReminderEnabled] ?: true
        )
    }

    suspend fun currentSettings(): AppSettings = settingsFlow.first()

    suspend fun setLanguageTag(languageTag: String) {
        val normalized = languageTag.trim().ifBlank { AppLanguage.CHINESE.tag }
        val coerced = if (AppLanguage.fromTag(normalized) == AppLanguage.ENGLISH) {
            AppLanguage.CHINESE.tag
        } else {
            normalized
        }
        context.settingsDataStore.edit { prefs ->
            prefs[keyLanguageTag] = coerced
        }
    }

    suspend fun setNickname(nickname: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyNickname] = nickname.trim()
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyNotificationsEnabled] = enabled
        }
    }

    suspend fun setAiParseEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyAiParseEnabled] = enabled
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

    suspend fun updateAiConfig(
        enabled: Boolean,
        provider: LlmProvider,
        baseUrl: String,
        apiKey: String,
        model: String,
        timeoutSeconds: Int
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyAiParseEnabled] = enabled
            prefs[keyLlmProvider] = provider.name
            prefs[keyAiBaseUrl] = baseUrl.trim().ifBlank { AppSettings.DEFAULT_AI_BASE_URL }
            prefs[keyAiApiKey] = apiKey.trim()
            prefs[keyAiModel] = model.trim().ifBlank { AppSettings.DEFAULT_AI_MODEL }
            prefs[keyAiTimeoutSeconds] = timeoutSeconds.coerceIn(10, 120)
        }
    }

    suspend fun setLlmProvider(provider: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyLlmProvider] = provider.trim().ifBlank { LlmProvider.DEEPSEEK.name }
        }
    }

    suspend fun setLlmProvider(provider: LlmProvider) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyLlmProvider] = provider.name
        }
    }

    suspend fun setAiTimeoutSeconds(seconds: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyAiTimeoutSeconds] = seconds.coerceIn(10, 120)
        }
    }

    suspend fun currentLlmConfig(): LlmConfig {
        val settings = currentSettings()
        val provider = runCatching { LlmProvider.valueOf(settings.llmProvider.trim().uppercase()) }
            .getOrElse {
                if (settings.llmProvider.equals("deepseek", true) || settings.llmProvider.equals("remote", true)) {
                    LlmProvider.DEEPSEEK
                } else {
                    LlmProvider.CUSTOM
                }
            }
        return LlmConfig(
            enabled = settings.aiParseEnabled,
            provider = provider,
            baseUrl = settings.normalizedAiBaseUrl,
            apiKey = settings.aiApiKey,
            model = settings.normalizedAiModel,
            timeoutSeconds = settings.normalizedTimeoutSeconds
        )
    }

    suspend fun getCurrentAiConfig(): LlmConfig = currentLlmConfig()

    suspend fun setDefaultReminderMinutes(minutes: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyDefaultReminderMinutes] = sanitizeInterval(minutes)
        }
    }

    suspend fun setDefaultHomeTab(tab: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyDefaultHomeTab] = tab.trim().ifBlank { "today" }
        }
    }

    suspend fun setShowCompletedTasks(show: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyShowCompletedTasks] = show
        }
    }

    suspend fun setNotificationImportEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyNotificationImportEnabled] = enabled
        }
    }

    suspend fun setDeadlineVibrateEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyDeadlineVibrateEnabled] = enabled
        }
    }

    suspend fun setDeadlineStrongReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyDeadlineStrongReminderEnabled] = enabled
        }
    }

    private fun sanitizeInterval(minutes: Int): Int = minutes.coerceAtLeast(15)
}
