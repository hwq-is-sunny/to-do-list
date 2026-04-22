package com.campus.todo.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "candidates")
data class CandidateItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawText: String,
    val sourceKind: SourceKind,
    val parsedTitle: String?,
    val parsedDescription: String? = null,
    val parsedCourseHint: String?,
    val parsedLocation: String? = null,
    val parsedDueAtEpoch: Long?,
    val parsedStartAtEpoch: Long? = null,
    val parsedEndAtEpoch: Long? = null,
    val parsedCategory: String? = null,
    /** 通知结构化类型：deadline / task / exam / meeting / course / activity */
    val parsedNoticeType: String? = null,
    val parsedDateEpochDay: Long? = null,
    val parsedStartMinuteOfDay: Int? = null,
    val parsedEndMinuteOfDay: Int? = null,
    /** low / normal / high / urgent */
    val parsedPriorityTag: String? = null,
    val suggestedTaskType: TaskType?,
    val suggestedUrgency: UrgencyLevel?,
    val confidenceScore: Float = 0.5f,
    val duplicateScore: Float = 0f,
    val mergeSuggestion: String? = null,
    val mergedGroupId: String? = null,
    val originalSourceId: String? = null,
    val relatedEventId: Long? = null,
    val status: CandidateStatus = CandidateStatus.PENDING,
    val createdAtEpoch: Long,
    val linkedTaskId: Long? = null,
    val linkedCourseId: Long? = null,
    /** LOW / MED / HIGH for UI hint */
    val confidenceNote: String? = null
)
