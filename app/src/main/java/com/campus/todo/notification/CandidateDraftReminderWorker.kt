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
import com.campus.todo.data.db.entity.CandidateStatus
import com.campus.todo.data.db.entity.UrgencyLevel

class CandidateDraftReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val candidateId = inputData.getLong(KEY_CANDIDATE_ID, -1L)
        if (candidateId < 0) return Result.success()

        val app = applicationContext.applicationContext as CampusTodoApp
        val c = app.repository.getCandidate(candidateId) ?: return Result.success()
        if (c.status != CandidateStatus.PENDING) return Result.success()
        val due = c.parsedDueAtEpoch ?: return Result.success()
        val settings = app.settingsStore.currentSettings()
        if (!settings.taskReminderEnabled || !settings.notificationsEnabled) return Result.success()
        TodoNotificationChannels.ensureAll(applicationContext, settings)

        val open = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_CANDIDATE, candidateId)
        }
        val pi = PendingIntent.getActivity(
            applicationContext,
            (candidateId % Int.MAX_VALUE).toInt(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = c.parsedTitle ?: c.rawText.take(40)
        val text = applicationContext.getString(R.string.draft_reminder_body, title)

        val notif = NotificationCompat.Builder(
            applicationContext,
            TodoNotificationChannels.taskChannelIdFor(UrgencyLevel.NORMAL, settings)
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(applicationContext.getString(R.string.draft_reminder_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(
                NOTIF_BASE_ID + (candidateId % 1_000_000).toInt(),
                notif
            )
        }
        return Result.success()
    }

    companion object {
        const val KEY_CANDIDATE_ID = "candidateId"
        private const val NOTIF_BASE_ID = 8200
    }
}
