package com.campus.todo.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "candidates",
    indices = [Index("status"), Index("createdAtEpoch")]
)
data class CandidateItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawText: String,
    val parsedTitle: String?,
    val parsedCourseHint: String?,
    val parsedDueAtEpoch: Long?,
    val suggestedTaskType: TaskType?,
    val suggestedUrgency: UrgencyLevel?,
    val sourceKind: SourceKind,
    val status: CandidateStatus = CandidateStatus.NEW,
    val createdAtEpoch: Long,
    val linkedTaskId: Long? = null,
    /** LOW / MED / HIGH for UI hint */
    val confidenceNote: String? = null
)
