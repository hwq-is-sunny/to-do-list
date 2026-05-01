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

enum class TaskDeadlineNotifyPhase {
    AT_24H,
    AT_2H,
    AT_30M
}

object TodoNotificationChannels {
    private const val NORMAL_BASE = "campus_todo_normal"
    private const val IMPORTANT_BASE = "campus_todo_important"
    private const val URGENT_BASE = "campus_todo_urgent"
    private const val FOCUS_BASE = "campus_todo_focus"
    private const val DEADLINE_24H_ID = "campus_todo_deadline_24h"
    private const val DEADLINE_2H_ID = "campus_todo_deadline_2h"
    private const val DEADLINE_30M_ID = "campus_todo_deadline_30m"

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
                ReminderMethod.SOUND_AND_VIBRATE -> {
                    setSound(ringtoneUri, audioAttributes)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0L, 180L, 140L, 180L)
                }
                ReminderMethod.SILENT -> {
                    setSound(null, null)
                    enableVibration(false)
                    vibrationPattern = longArrayOf(0L)
                }
            }
        }

        fun deadline24hConfigure(ch: NotificationChannel) {
            when (settings.reminderMethod) {
                ReminderMethod.SOUND, ReminderMethod.SOUND_AND_VIBRATE -> {
                    ch.setSound(ringtoneUri, audioAttributes)
                    ch.enableVibration(false)
                    ch.vibrationPattern = longArrayOf(0L)
                }
                ReminderMethod.VIBRATE, ReminderMethod.SILENT -> {
                    ch.setSound(null, null)
                    ch.enableVibration(false)
                    ch.vibrationPattern = longArrayOf(0L)
                }
            }
        }

        fun deadline2hConfigure(ch: NotificationChannel) {
            val wantVib = settings.deadlineVibrateEnabled
            when (settings.reminderMethod) {
                ReminderMethod.SOUND -> {
                    ch.setSound(ringtoneUri, audioAttributes)
                    ch.enableVibration(wantVib)
                    ch.vibrationPattern =
                        if (wantVib) longArrayOf(0L, 400L, 200L, 400L) else longArrayOf(0L)
                }
                ReminderMethod.SOUND_AND_VIBRATE -> {
                    ch.setSound(ringtoneUri, audioAttributes)
                    ch.enableVibration(wantVib)
                    ch.vibrationPattern =
                        if (wantVib) longArrayOf(0L, 400L, 200L, 400L) else longArrayOf(0L)
                }
                ReminderMethod.VIBRATE -> {
                    ch.setSound(null, null)
                    ch.enableVibration(wantVib)
                    ch.vibrationPattern =
                        if (wantVib) longArrayOf(0L, 400L, 200L, 400L) else longArrayOf(0L)
                }
                ReminderMethod.SILENT -> {
                    ch.setSound(null, null)
                    ch.enableVibration(wantVib)
                    ch.vibrationPattern =
                        if (wantVib) longArrayOf(0L, 400L, 200L, 400L) else longArrayOf(0L)
                }
            }
        }

        fun deadline30mConfigure(ch: NotificationChannel) {
            deadline2hConfigure(ch)
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
        nm.createNotificationChannel(
            NotificationChannel(
                DEADLINE_24H_ID,
                context.getString(R.string.notification_channel_deadline_24h_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_deadline_24h_description)
                deadline24hConfigure(this)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                DEADLINE_2H_ID,
                context.getString(R.string.notification_channel_deadline_2h_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_deadline_2h_description)
                deadline2hConfigure(this)
            }
        )
        val importance30m = if (settings.deadlineStrongReminderEnabled) {
            NotificationManager.IMPORTANCE_HIGH
        } else {
            NotificationManager.IMPORTANCE_DEFAULT
        }
        nm.createNotificationChannel(
            NotificationChannel(
                DEADLINE_30M_ID,
                context.getString(R.string.notification_channel_deadline_30m_name),
                importance30m
            ).apply {
                description = context.getString(R.string.notification_channel_deadline_30m_description)
                deadline30mConfigure(this)
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

    fun deadlineChannelIdFor(phase: TaskDeadlineNotifyPhase): String = when (phase) {
        TaskDeadlineNotifyPhase.AT_24H -> DEADLINE_24H_ID
        TaskDeadlineNotifyPhase.AT_2H -> DEADLINE_2H_ID
        TaskDeadlineNotifyPhase.AT_30M -> DEADLINE_30M_ID
    }

    private fun channelId(base: String, method: ReminderMethod): String =
        "${base}_${method.name.lowercase()}"

    private fun allKnownChannelIds(): List<String> = listOf(
        channelId(NORMAL_BASE, ReminderMethod.SOUND),
        channelId(NORMAL_BASE, ReminderMethod.VIBRATE),
        channelId(NORMAL_BASE, ReminderMethod.SOUND_AND_VIBRATE),
        channelId(NORMAL_BASE, ReminderMethod.SILENT),
        channelId(IMPORTANT_BASE, ReminderMethod.SOUND),
        channelId(IMPORTANT_BASE, ReminderMethod.VIBRATE),
        channelId(IMPORTANT_BASE, ReminderMethod.SOUND_AND_VIBRATE),
        channelId(IMPORTANT_BASE, ReminderMethod.SILENT),
        channelId(URGENT_BASE, ReminderMethod.SOUND),
        channelId(URGENT_BASE, ReminderMethod.VIBRATE),
        channelId(URGENT_BASE, ReminderMethod.SOUND_AND_VIBRATE),
        channelId(URGENT_BASE, ReminderMethod.SILENT),
        channelId(FOCUS_BASE, ReminderMethod.SOUND),
        channelId(FOCUS_BASE, ReminderMethod.VIBRATE),
        channelId(FOCUS_BASE, ReminderMethod.SOUND_AND_VIBRATE),
        channelId(FOCUS_BASE, ReminderMethod.SILENT),
        DEADLINE_24H_ID,
        DEADLINE_2H_ID,
        DEADLINE_30M_ID
    )
}
