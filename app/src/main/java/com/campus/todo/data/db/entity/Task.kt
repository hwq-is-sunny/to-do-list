package com.campus.todo.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("courseId"), Index("dueAtEpoch"), Index("status")]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val courseId: Long? = null,
    val courseName: String? = null,
    val taskType: TaskType = TaskType.OTHER,
    val category: String = TaskType.OTHER.name,
    val urgency: UrgencyLevel = UrgencyLevel.NORMAL,
    val status: TaskStatus = TaskStatus.PENDING,
    val dueAtEpoch: Long? = null,
    val startAtEpoch: Long? = null,
    val endAtEpoch: Long? = null,
    val reminderAtEpoch: Long? = null,
    val location: String? = null,
    val tags: String? = null,
    val isAllDay: Boolean = false,
    val mergedGroupId: String? = null,
    val originalSourceId: String? = null,
    val sourceKind: SourceKind = SourceKind.MANUAL,
    val rawSnippet: String? = null,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long,
    val completedAtEpoch: Long? = null
)
