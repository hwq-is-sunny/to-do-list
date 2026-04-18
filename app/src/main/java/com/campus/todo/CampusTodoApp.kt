package com.campus.todo

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.campus.todo.data.db.AppDatabase
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.data.settings.SettingsStore
import com.campus.todo.data.session.SessionStore
import com.campus.todo.network.DeepSeekAssist
import com.campus.todo.notification.TaskReminderScheduler
import com.campus.todo.notification.TodoNotificationChannels
import kotlinx.coroutines.runBlocking

class CampusTodoApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: TodoRepository by lazy { TodoRepository(database) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    val reminderScheduler: TaskReminderScheduler by lazy { TaskReminderScheduler(this, settingsStore) }
    val sessionStore: SessionStore by lazy { SessionStore(this) }
    val deepSeekAssist: DeepSeekAssist by lazy {
        DeepSeekAssist(this, settingsStore, com.campus.todo.BuildConfig.DEEPSEEK_API_KEY)
    }

    override fun onCreate() {
        super.onCreate()
        val settings = runBlocking { settingsStore.currentSettings() }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(settings.languageTag))
        TodoNotificationChannels.ensureAll(this, settings)
    }
}
