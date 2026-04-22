package com.campus.todo

import android.app.Application
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.campus.todo.data.db.AppDatabase
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.data.session.AccountStore
import com.campus.todo.data.settings.SettingsStore
import com.campus.todo.data.session.SessionStore
import com.campus.todo.network.DeepSeekAssist
import com.campus.todo.notification.TaskReminderScheduler
import com.campus.todo.notification.TodoNotificationChannels
import kotlinx.coroutines.runBlocking
import com.campus.todo.data.settings.AppLanguage
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CampusTodoApp : Application() {

    private val applicationJob = SupervisorJob()
    val applicationScope: CoroutineScope = CoroutineScope(applicationJob + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: TodoRepository by lazy { TodoRepository(database) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    val reminderScheduler: TaskReminderScheduler by lazy { TaskReminderScheduler(this, settingsStore) }
    val sessionStore: SessionStore by lazy { SessionStore(this) }
    val accountStore: AccountStore by lazy { AccountStore(this) }
    val deepSeekAssist: DeepSeekAssist by lazy {
        DeepSeekAssist(this, settingsStore, com.campus.todo.BuildConfig.DEEPSEEK_API_KEY)
    }

    override fun onCreate() {
        super.onCreate()
        val settings = runBlocking {
            val current = settingsStore.currentSettings()
            if (current.languageTag.isBlank() ||
                AppLanguage.fromTag(current.languageTag) == AppLanguage.ENGLISH
            ) {
                settingsStore.setLanguageTag(AppLanguage.CHINESE.tag)
            }
            settingsStore.currentSettings()
        }
        // 产品要求：界面统一简体中文，不因系统语言加载英文资源
        val localeTag = AppLanguage.CHINESE.tag
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))
        val locale = Locale.forLanguageTag(localeTag)
        Locale.setDefault(locale)
        val res = resources
        val config = res.configuration
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        }
        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)
        TodoNotificationChannels.ensureAll(this, settings)
    }
}
