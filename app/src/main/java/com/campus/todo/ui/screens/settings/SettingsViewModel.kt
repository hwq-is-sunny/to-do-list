 package com.campus.todo.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.data.settings.AppLanguage
import com.campus.todo.data.settings.AppSettings
import com.campus.todo.data.settings.LlmProvider
import com.campus.todo.data.settings.ReminderMethod
import com.campus.todo.data.settings.SettingsStore
import com.campus.todo.data.session.SessionStore
import com.campus.todo.notification.TaskReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val username: String = "",
    val settings: AppSettings = AppSettings(),
    val currentLanguage: AppLanguage = AppLanguage.CHINESE
)

class SettingsViewModel(
    private val repo: TodoRepository,
    private val scheduler: TaskReminderScheduler,
    private val sessionStore: SessionStore,
    private val settingsStore: SettingsStore
) : ViewModel() {

    val state = combine(sessionStore.usernameFlow, settingsStore.settingsFlow) { username, settings ->
        SettingsUiState(
            username = username,
            settings = settings,
            currentLanguage = settings.selectedLanguage(systemLanguage())
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState()
    )

    fun setLanguage(language: AppLanguage, onSaved: (() -> Unit)? = null) {
        viewModelScope.launch {
            settingsStore.setLanguageTag(language.tag)
            onSaved?.invoke()
        }
    }

    fun setDisplayWeekCount(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setDisplayWeekCount(enabled)
        }
    }

    fun setNickname(nickname: String) {
        viewModelScope.launch {
            settingsStore.setNickname(nickname)
        }
    }

    fun setTaskReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setNotificationsEnabled(enabled)
            settingsStore.setTaskReminderEnabled(enabled)
            scheduler.syncPendingTaskReminders(repo.getAllPendingWithDue())
            scheduler.refreshNotificationChannels()
            scheduler.syncTomatoFocus()
        }
    }

    fun setTomatoFocusEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setTomatoFocusEnabled(enabled)
            scheduler.syncTomatoFocus()
        }
    }

    fun setTomatoFocusInterval(minutes: Int) {
        viewModelScope.launch {
            settingsStore.setTomatoFocusIntervalMinutes(minutes)
            scheduler.syncTomatoFocus()
        }
    }

    fun setReminderMethod(method: ReminderMethod) {
        viewModelScope.launch {
            settingsStore.setReminderMethod(method)
            scheduler.refreshNotificationChannels()
        }
    }

    fun setRingtoneUri(uri: String) {
        viewModelScope.launch {
            settingsStore.setRingtoneUri(uri)
            scheduler.refreshNotificationChannels()
        }
    }

    fun saveAiConfig(baseUrl: String, apiKey: String, model: String) {
        viewModelScope.launch {
            settingsStore.updateAiConfig(baseUrl, apiKey, model)
        }
    }

    fun saveAiConfig(
        enabled: Boolean,
        provider: LlmProvider,
        baseUrl: String,
        apiKey: String,
        model: String,
        timeoutSeconds: Int
    ) {
        viewModelScope.launch {
            settingsStore.updateAiConfig(enabled, provider, baseUrl, apiKey, model, timeoutSeconds)
        }
    }

    fun triggerTomatoFocusTest() {
        viewModelScope.launch {
            scheduler.fireTomatoFocusTest()
        }
    }

    fun setNotificationImportEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setNotificationImportEnabled(enabled)
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            sessionStore.logout()
            onDone()
        }
    }

    private fun systemLanguage(): AppLanguage = AppLanguage.CHINESE
}
