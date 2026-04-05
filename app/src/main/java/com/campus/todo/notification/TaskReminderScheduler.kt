package com.campus.todo.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.UrgencyLevel
import java.time.Duration

class TaskReminderScheduler(private val context: Context) {

    fun rescheduleTask(task: Task) {
        cancelTask(task.id)
        val due = task.dueAtEpoch ?: return
        val leadMs = when (task.urgency) {
            UrgencyLevel.NORMAL -> Duration.ofHours(24)
            UrgencyLevel.IMPORTANT -> Duration.ofHours(6)
            UrgencyLevel.URGENT -> Duration.ofHours(1)
        }.toMillis()
        var fireAt = due - leadMs
        val now = System.currentTimeMillis()
        if (fireAt <= now) {
            fireAt = now + MIN_DELAY_MS
        }
        val delay = fireAt - now
        val data = Data.Builder().putLong(TaskReminderWorker.KEY_TASK_ID, task.id).build()
        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(Duration.ofMillis(delay))
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName(task.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelTask(taskId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(taskId))
    }

    /** Remind user that draft candidates do not trigger formal task reminders until confirmed. */
    fun scheduleDraftReminder(candidateId: Long, dueEpoch: Long) {
        cancelDraftReminder(candidateId)
        val leadMs = Duration.ofHours(24).toMillis()
        var fireAt = dueEpoch - leadMs
        val now = System.currentTimeMillis()
        if (fireAt <= now) {
            fireAt = now + MIN_DELAY_MS
        }
        if (fireAt >= dueEpoch) {
            fireAt = (dueEpoch - MIN_DELAY_MS).coerceAtLeast(now + MIN_DELAY_MS)
        }
        val delay = fireAt - now
        val data = Data.Builder()
            .putLong(CandidateDraftReminderWorker.KEY_CANDIDATE_ID, candidateId)
            .build()
        val request = OneTimeWorkRequestBuilder<CandidateDraftReminderWorker>()
            .setInitialDelay(Duration.ofMillis(delay))
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            draftUniqueName(candidateId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelDraftReminder(candidateId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(draftUniqueName(candidateId))
    }

    companion object {
        private const val MIN_DELAY_MS = 15_000L
        fun uniqueName(taskId: Long) = "task_reminder_$taskId"
        fun draftUniqueName(candidateId: Long) = "candidate_draft_reminder_$candidateId"
    }
}
