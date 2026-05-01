package com.campus.todo.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.settings.SettingsStore
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.util.concurrent.TimeUnit

class TaskReminderScheduler(
    private val context: Context,
    private val settingsStore: SettingsStore
) {

    fun rescheduleTask(task: Task) {
        cancelTask(task.id)
        if (!remindersGloballyEnabled()) return
        val due = task.dueAtEpoch ?: return
        val now = System.currentTimeMillis()
        schedulePhase(task.id, due, now, TaskReminderWorker.PHASE_24H, Duration.ofHours(24).toMillis())
        schedulePhase(task.id, due, now, TaskReminderWorker.PHASE_2H, Duration.ofHours(2).toMillis())
        schedulePhase(task.id, due, now, TaskReminderWorker.PHASE_30M, Duration.ofMinutes(30).toMillis())
    }

    private fun schedulePhase(taskId: Long, dueAt: Long, now: Long, phase: String, leadMs: Long) {
        var fireAt = dueAt - leadMs
        if (fireAt <= now) return
        if (fireAt >= dueAt) return
        val delay = fireAt - now
        val workDelay = delay.coerceAtLeast(MIN_WORK_DELAY_MS)
        val data = Data.Builder()
            .putLong(TaskReminderWorker.KEY_TASK_ID, taskId)
            .putString(TaskReminderWorker.KEY_PHASE, phase)
            .build()
        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(Duration.ofMillis(workDelay))
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName(taskId, phase),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelTask(taskId: Long) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(uniqueName(taskId, TaskReminderWorker.PHASE_24H))
        wm.cancelUniqueWork(uniqueName(taskId, TaskReminderWorker.PHASE_2H))
        wm.cancelUniqueWork(uniqueName(taskId, TaskReminderWorker.PHASE_30M))
    }

    /** Remind user that draft candidates do not trigger formal task reminders until confirmed. */
    fun scheduleDraftReminder(candidateId: Long, dueEpoch: Long) {
        cancelDraftReminder(candidateId)
        if (!remindersGloballyEnabled()) return
        val leadMs = Duration.ofHours(24).toMillis()
        var fireAt = dueEpoch - leadMs
        val now = System.currentTimeMillis()
        if (fireAt <= now) {
            fireAt = now + MIN_WORK_DELAY_MS
        }
        if (fireAt >= dueEpoch) {
            fireAt = (dueEpoch - MIN_WORK_DELAY_MS).coerceAtLeast(now + MIN_WORK_DELAY_MS)
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

    fun syncPendingTaskReminders(tasks: List<Task>) {
        if (remindersGloballyEnabled()) {
            tasks.forEach(::rescheduleTask)
        } else {
            tasks.forEach { cancelTask(it.id) }
        }
    }

    fun refreshNotificationChannels() {
        val settings = runBlocking { settingsStore.currentSettings() }
        TodoNotificationChannels.ensureAll(context, settings)
    }

    fun syncTomatoFocus() {
        refreshNotificationChannels()
        val settings = runBlocking { settingsStore.currentSettings() }
        val workManager = WorkManager.getInstance(context)
        if (!settings.tomatoFocusEnabled || !settings.notificationsEnabled) {
            workManager.cancelUniqueWork(TOMATO_FOCUS_WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<TomatoFocusReminderWorker>(
            settings.tomatoFocusIntervalMinutes.toLong(),
            TimeUnit.MINUTES
        ).build()
        workManager.enqueueUniquePeriodicWork(
            TOMATO_FOCUS_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun fireTomatoFocusTest() {
        val settings = runBlocking { settingsStore.currentSettings() }
        if (!settings.tomatoFocusEnabled || !settings.notificationsEnabled) return
        val request = OneTimeWorkRequestBuilder<TomatoFocusReminderWorker>()
            .setInitialDelay(Duration.ofSeconds(2))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "tomato_focus_test",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun remindersGloballyEnabled(): Boolean = runBlocking {
        val settings = settingsStore.currentSettings()
        settings.taskReminderEnabled && settings.notificationsEnabled
    }

    companion object {
        private const val MIN_WORK_DELAY_MS = 5_000L
        private const val TOMATO_FOCUS_WORK_NAME = "tomato_focus_reminder"
        fun uniqueName(taskId: Long, phase: String) = "task_reminder_${taskId}_$phase"
        fun draftUniqueName(candidateId: Long) = "candidate_draft_reminder_$candidateId"
    }
}
