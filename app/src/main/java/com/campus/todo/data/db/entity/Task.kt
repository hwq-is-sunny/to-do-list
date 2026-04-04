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
    val courseId: Long? = null,
    val taskType: TaskType = TaskType.OTHER,
    val urgency: UrgencyLevel = UrgencyLevel.NORMAL,
    val status: TaskStatus = TaskStatus.PENDING,
    val dueAtEpoch: Long? = null,
    val sourceKind: SourceKind = SourceKind.MANUAL,
    val rawSnippet: String? = null,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long,
    val completedAtEpoch: Long? = null
)
