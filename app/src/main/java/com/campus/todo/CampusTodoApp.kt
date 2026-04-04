package com.campus.todo

import android.app.Application
import com.campus.todo.data.db.AppDatabase
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.notification.TaskReminderScheduler
import com.campus.todo.notification.TodoNotificationChannels

class CampusTodoApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: TodoRepository by lazy { TodoRepository(database) }
    val reminderScheduler: TaskReminderScheduler by lazy { TaskReminderScheduler(this) }

    override fun onCreate() {
        super.onCreate()
        TodoNotificationChannels.ensureAll(this)
    }
}
