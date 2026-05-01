package com.campus.todo.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.campus.todo.CampusTodoApp
import com.campus.todo.MainActivity
import com.campus.todo.R
import com.campus.todo.data.db.entity.TaskStatus

class TaskReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        if (taskId < 0) return Result.success()
        val phase = notifyPhaseFrom(inputData.getString(KEY_PHASE)) ?: return Result.success()

        val app = applicationContext.applicationContext as CampusTodoApp
        val task = app.repository.getTask(taskId) ?: return Result.success()
        if (task.status != TaskStatus.PENDING) return Result.success()
        val settings = app.settingsStore.currentSettings()
        if (!settings.taskReminderEnabled || !settings.notificationsEnabled) return Result.success()
        TodoNotificationChannels.ensureAll(applicationContext, settings)

        val channelId = TodoNotificationChannels.deadlineChannelIdFor(phase)
        val open = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            applicationContext,
            (taskId % Int.MAX_VALUE).toInt() + phase.ordinal * 17,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyExtra = when (phase) {
            TaskDeadlineNotifyPhase.AT_24H -> applicationContext.getString(R.string.task_reminder_24h_body)
            TaskDeadlineNotifyPhase.AT_2H -> applicationContext.getString(R.string.task_reminder_2h_body)
            TaskDeadlineNotifyPhase.AT_30M -> applicationContext.getString(R.string.task_reminder_30m_body)
        }
        val text = buildString {
            append(applicationContext.getString(R.string.task_reminder_prefix))
            append(task.title)
            append('\n')
            append(bodyExtra)
        }

        val priority = when (phase) {
            TaskDeadlineNotifyPhase.AT_24H -> NotificationCompat.PRIORITY_DEFAULT
            TaskDeadlineNotifyPhase.AT_2H -> NotificationCompat.PRIORITY_HIGH
            TaskDeadlineNotifyPhase.AT_30M ->
                if (settings.deadlineStrongReminderEnabled) {
                    NotificationCompat.PRIORITY_MAX
                } else {
                    NotificationCompat.PRIORITY_HIGH
                }
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(applicationContext.getString(R.string.task_reminder_title))
            .setContentText(task.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(priority)
            .setOnlyAlertOnce(false)

        if (phase == TaskDeadlineNotifyPhase.AT_30M && settings.deadlineStrongReminderEnabled) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
        }

        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(
                notificationId(taskId, phase),
                builder.build()
            )
        }
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "taskId"
        const val KEY_PHASE = "phase"
        const val PHASE_24H = "24h"
        const val PHASE_2H = "2h"
        const val PHASE_30M = "30m"

        fun notifyPhaseFrom(raw: String?): TaskDeadlineNotifyPhase? = when (raw) {
            PHASE_24H -> TaskDeadlineNotifyPhase.AT_24H
            PHASE_2H -> TaskDeadlineNotifyPhase.AT_2H
            PHASE_30M -> TaskDeadlineNotifyPhase.AT_30M
            else -> null
        }

        fun notificationId(taskId: Long, phase: TaskDeadlineNotifyPhase): Int {
            val base = NOTIF_BASE_ID + (taskId % 1_000_000).toInt()
            return base + phase.ordinal * 3_000_000
        }

        private const val NOTIF_BASE_ID = 7100
    }
}
