package com.campus.todo.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.campus.todo.data.db.entity.UrgencyLevel

object TodoNotificationChannels {
    const val NORMAL_ID = "campus_todo_normal"
    const val IMPORTANT_ID = "campus_todo_important"
    const val URGENT_ID = "campus_todo_urgent"

    fun ensureAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                NORMAL_ID,
                "普通提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "接近截止时的温和提醒"
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                IMPORTANT_ID,
                "重要提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "需要优先处理的事项"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                URGENT_ID,
                "紧急提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "即将到期或高优先级事项"
            }
        )
    }

    fun channelIdFor(level: UrgencyLevel): String = when (level) {
        UrgencyLevel.NORMAL -> NORMAL_ID
        UrgencyLevel.IMPORTANT -> IMPORTANT_ID
        UrgencyLevel.URGENT -> URGENT_ID
    }
}
