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
        val leadMs = getLeadTimeMillis(task.urgency)
        var fireAt = due - leadMs
        val now = System.currentTimeMillis()
        if (fireAt <= now) {
            fireAt = now + MIN_DELAY_MS
        }
        val delay = fireAt - now
        scheduleReminder(task.id, delay)
    }

    /**
     * Reschedule multiple tasks at once for batch operations
     */
    fun rescheduleTasks(tasks: List<Task>) {
        tasks.forEach { task ->
            rescheduleTask(task)
        }
    }

    /**
     * Schedule a reminder for a task with given delay
     */
    private fun scheduleReminder(taskId: Long, delayMs: Long) {
        val data = Data.Builder().putLong(TaskReminderWorker.KEY_TASK_ID, taskId).build()
        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(Duration.ofMillis(delayMs))
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName(taskId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelTask(taskId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(taskId))
    }

    /**
     * Cancel multiple tasks at once
     */
    fun cancelTasks(taskIds: List<Long>) {
        taskIds.forEach { taskId ->
            WorkManager.getInstance(context).cancelUniqueWork(uniqueName(taskId))
        }
    }

    /**
     * Cancel all scheduled reminders
     */
    fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG_REMINDER)
    }

    private fun getLeadTimeMillis(urgency: UrgencyLevel): Long {
        return when (urgency) {
            UrgencyLevel.URGENT -> Duration.ofHours(1).toMillis()
            UrgencyLevel.IMPORTANT -> Duration.ofHours(6).toMillis()
            UrgencyLevel.NORMAL -> Duration.ofHours(24).toMillis()
        }
    }

    companion object {
        private const val MIN_DELAY_MS = 15_000L
        private const val TAG_REMINDER = "task_reminder"
        fun uniqueName(taskId: Long) = "task_reminder_$taskId"
    }
}
