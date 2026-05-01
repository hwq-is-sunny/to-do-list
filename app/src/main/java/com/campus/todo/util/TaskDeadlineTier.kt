package com.campus.todo.util

import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TaskStatus

/** 基于截止时间与当前时刻的自动分级（与数据库中的 [com.campus.todo.data.db.entity.UrgencyLevel] 独立）。 */
enum class TaskDeadlineTier {
    NORMAL,
    NEAR_DUE,
    URGENT,
    OVERDUE;

    val sortKey: Int
        get() = when (this) {
            OVERDUE -> 0
            URGENT -> 1
            NEAR_DUE -> 2
            NORMAL -> 3
        }
}

private const val HOUR_MS = 60L * 60 * 1000

fun Task.deadlineTier(nowMs: Long = System.currentTimeMillis()): TaskDeadlineTier {
    if (status != TaskStatus.PENDING) return TaskDeadlineTier.NORMAL
    val due = dueAtEpoch ?: return TaskDeadlineTier.NORMAL
    if (nowMs > due) return TaskDeadlineTier.OVERDUE
    val remaining = due - nowMs
    return when {
        remaining <= 2 * HOUR_MS -> TaskDeadlineTier.URGENT
        remaining <= 24 * HOUR_MS -> TaskDeadlineTier.NEAR_DUE
        else -> TaskDeadlineTier.NORMAL
    }
}

fun comparePendingTasksByDeadlineTier(nowMs: Long = System.currentTimeMillis()): Comparator<Task> =
    compareBy<Task> { it.deadlineTier(nowMs).sortKey }
        .thenBy { it.dueAtEpoch ?: Long.MAX_VALUE }
        .thenBy { it.id }
