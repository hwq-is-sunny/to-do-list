package com.campus.todo.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.media.RingtoneManager
import com.campus.todo.R
import com.campus.todo.data.db.entity.UrgencyLevel
import com.campus.todo.data.settings.AppSettings
import com.campus.todo.data.settings.ReminderMethod

object TodoNotificationChannels {
    private const val NORMAL_BASE = "campus_todo_normal"
    private const val IMPORTANT_BASE = "campus_todo_important"
    private const val URGENT_BASE = "campus_todo_urgent"
    private const val FOCUS_BASE = "campus_todo_focus"

    fun ensureAll(context: Context, settings: AppSettings) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        allKnownChannelIds().forEach(nm::deleteNotificationChannel)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val ringtoneUri = settings.ringtoneUri.takeIf { it.isNotBlank() }
            ?.let(android.net.Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val configureAlerts: NotificationChannel.() -> Unit = {
            when (settings.reminderMethod) {
                ReminderMethod.SOUND -> {
                    setSound(ringtoneUri, audioAttributes)
                    enableVibration(false)
                    vibrationPattern = longArrayOf(0L)
                }
                ReminderMethod.VIBRATE -> {
                    setSound(null, null)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0L, 250L, 200L, 250L)
                }
            }
        }

        nm.createNotificationChannel(
            NotificationChannel(
                taskChannelIdFor(UrgencyLevel.NORMAL, settings),
                context.getString(R.string.notification_channel_normal_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_normal_description)
                configureAlerts()
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                taskChannelIdFor(UrgencyLevel.IMPORTANT, settings),
                context.getString(R.string.notification_channel_important_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_important_description)
                configureAlerts()
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                taskChannelIdFor(UrgencyLevel.URGENT, settings),
                context.getString(R.string.notification_channel_urgent_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_urgent_description)
                configureAlerts()
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                focusChannelIdFor(settings),
                context.getString(R.string.notification_channel_focus_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_focus_description)
                configureAlerts()
            }
        )
    }

    fun taskChannelIdFor(level: UrgencyLevel, settings: AppSettings): String = when (level) {
        UrgencyLevel.NORMAL -> channelId(NORMAL_BASE, settings.reminderMethod)
        UrgencyLevel.IMPORTANT -> channelId(IMPORTANT_BASE, settings.reminderMethod)
        UrgencyLevel.URGENT -> channelId(URGENT_BASE, settings.reminderMethod)
    }

    fun focusChannelIdFor(settings: AppSettings): String =
        channelId(FOCUS_BASE, settings.reminderMethod)

    private fun channelId(base: String, method: ReminderMethod): String =
        "${base}_${method.name.lowercase()}"

    private fun allKnownChannelIds(): List<String> = listOf(
        channelId(NORMAL_BASE, ReminderMethod.SOUND),
        channelId(NORMAL_BASE, ReminderMethod.VIBRATE),
        channelId(IMPORTANT_BASE, ReminderMethod.SOUND),
        channelId(IMPORTANT_BASE, ReminderMethod.VIBRATE),
        channelId(URGENT_BASE, ReminderMethod.SOUND),
        channelId(URGENT_BASE, ReminderMethod.VIBRATE),
        channelId(FOCUS_BASE, ReminderMethod.SOUND),
        channelId(FOCUS_BASE, ReminderMethod.VIBRATE)
    )
}
