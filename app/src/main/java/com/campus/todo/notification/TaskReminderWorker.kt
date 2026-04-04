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
import com.campus.todo.data.db.entity.TaskStatus

class TaskReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        if (taskId < 0) return Result.success()

        val app = applicationContext.applicationContext as CampusTodoApp
        val task = app.repository.getTask(taskId) ?: return Result.success()
        if (task.status != TaskStatus.PENDING) return Result.success()

        val channelId = TodoNotificationChannels.channelIdFor(task.urgency)
        val open = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            applicationContext,
            (taskId % Int.MAX_VALUE).toInt(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = buildString {
            append("待办: ")
            append(task.title)
            task.dueAtEpoch?.let {
                append("\n截止临近，记得处理就好。")
            }
        }

        val notif = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("校园待办")
            .setContentText(task.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(
                when (task.urgency) {
                    com.campus.todo.data.db.entity.UrgencyLevel.URGENT ->
                        NotificationCompat.PRIORITY_HIGH
                    com.campus.todo.data.db.entity.UrgencyLevel.IMPORTANT ->
                        NotificationCompat.PRIORITY_HIGH
                    com.campus.todo.data.db.entity.UrgencyLevel.NORMAL ->
                        NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .build()

        NotificationManagerCompat.from(applicationContext).notify(
            NOTIF_BASE_ID + (taskId % 1_000_000).toInt(),
            notif
        )
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "taskId"
        private const val NOTIF_BASE_ID = 7100
    }
}
