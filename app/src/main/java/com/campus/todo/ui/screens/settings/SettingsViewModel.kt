 package com.campus.todo.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.data.settings.AppLanguage
import com.campus.todo.data.settings.AppSettings
import com.campus.todo.data.settings.ReminderMethod
import com.campus.todo.data.settings.SettingsStore
import com.campus.todo.data.session.SessionStore
import com.campus.todo.notification.TaskReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

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

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsStore.setLanguageTag(language.tag)
        }
    }

    fun setDisplayWeekCount(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setDisplayWeekCount(enabled)
        }
    }

    fun setTaskReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setTaskReminderEnabled(enabled)
            scheduler.syncPendingTaskReminders(repo.getAllPendingWithDue())
            scheduler.refreshNotificationChannels()
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

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            sessionStore.logout()
            onDone()
        }
    }

    private fun systemLanguage(): AppLanguage {
        val language = Locale.getDefault().toLanguageTag()
        return if (language.startsWith("zh", ignoreCase = true)) {
            AppLanguage.CHINESE
        } else {
            AppLanguage.ENGLISH
        }
    }
}
