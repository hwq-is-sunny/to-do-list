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
        if (c.status != CandidateStatus.NEW) return Result.success()
        val due = c.parsedDueAtEpoch ?: return Result.success()

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
        val text = "草稿「$title」临近截止。草稿不会自动提醒，请打开候选箱确认并加入正式待办。"

        val notif = NotificationCompat.Builder(applicationContext, TodoNotificationChannels.channelIdFor(UrgencyLevel.NORMAL))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("候选箱草稿提醒")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(
            NOTIF_BASE_ID + (candidateId % 1_000_000).toInt(),
            notif
        )
        return Result.success()
    }

    companion object {
        const val KEY_CANDIDATE_ID = "candidateId"
        private const val NOTIF_BASE_ID = 8200
    }
}
