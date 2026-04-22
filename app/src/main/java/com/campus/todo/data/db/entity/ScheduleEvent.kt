package com.campus.todo.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_events",
    indices = [Index("dateEpoch"), Index("relatedTaskId")]
)
data class ScheduleEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateEpoch: Long,
    val startAtEpoch: Long? = null,
    val endAtEpoch: Long? = null,
    val location: String? = null,
    val teacher: String? = null,
    val courseType: String? = null,
    val isCancelled: Boolean = false,
    val sourceKind: SourceKind = SourceKind.TIMETABLE,
    val relatedTaskId: Long? = null,
    val mergedGroupId: String? = null,
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis()
)
