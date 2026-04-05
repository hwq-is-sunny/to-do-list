package com.campus.todo

import android.app.Application
import com.campus.todo.data.db.AppDatabase
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.data.session.SessionStore
import com.campus.todo.network.DeepSeekAssist
import com.campus.todo.notification.TaskReminderScheduler
import com.campus.todo.notification.TodoNotificationChannels

class CampusTodoApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: TodoRepository by lazy { TodoRepository(database) }
    val reminderScheduler: TaskReminderScheduler by lazy { TaskReminderScheduler(this) }
    val sessionStore: SessionStore by lazy { SessionStore(this) }
    val deepSeekAssist: DeepSeekAssist by lazy { DeepSeekAssist(com.campus.todo.BuildConfig.DEEPSEEK_API_KEY) }

    override fun onCreate() {
        super.onCreate()
        TodoNotificationChannels.ensureAll(this)
    }
}
