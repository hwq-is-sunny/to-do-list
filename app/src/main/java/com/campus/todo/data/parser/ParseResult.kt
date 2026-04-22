package com.campus.todo.data.parser

import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.UrgencyLevel
import com.campus.todo.domain.ParsedCandidateDraft
import java.time.LocalDate

/**
 * 本地规则解析通知后的结构化结果（不入库，仅作解析输出）。
 */
data class NoticeParseResult(
    val title: String,
    /** deadline / task / exam / meeting / course / activity */
    val noticeType: String,
    val dateEpochDay: Long?,
    /** 0–1439，与 [dateEpochDay] 同日；可能为空 */
    val startMinute: Int?,
    val endMinute: Int?,
    val location: String?,
    val note: String?,
    /** low / normal / high / urgent */
    val priorityTag: String,
    val dueAtEpoch: Long?,
    val startAtEpoch: Long?,
    val endAtEpoch: Long?,
    /** 课程名线索（类型为 course 时） */
    val courseNameHint: String?,
    val confidenceNote: String
)

fun NoticeParseResult.toParsedCandidateDraft(): ParsedCandidateDraft =
    ParsedCandidateDraft(
        title = title,
        description = note,
        courseHint = courseNameHint,
        dueDateText = dateEpochDay?.let { LocalDate.ofEpochDay(it).toString() },
        dueAtEpoch = dueAtEpoch,
        startAtEpoch = startAtEpoch,
        endAtEpoch = endAtEpoch,
        location = location,
        category = noticeType,
        taskType = mapNoticeTypeToTaskType(noticeType),
        urgency = mapPriorityTagToUrgency(priorityTag),
        confidenceNote = confidenceNote,
        confidenceScore = 0.72f
    )

fun mapNoticeTypeToTaskType(noticeType: String): TaskType = when (noticeType) {
    "exam" -> TaskType.EXAM
    "course" -> TaskType.CLASS
    "meeting" -> TaskType.ANNOUNCEMENT
    "deadline", "task" -> TaskType.HOMEWORK
    "activity" -> TaskType.PERSONAL
    else -> TaskType.OTHER
}

fun mapPriorityTagToUrgency(tag: String): UrgencyLevel = when (tag) {
    "urgent" -> UrgencyLevel.URGENT
    "high" -> UrgencyLevel.IMPORTANT
    else -> UrgencyLevel.NORMAL
}

fun mapUrgencyToPriorityTag(u: UrgencyLevel): String = when (u) {
    UrgencyLevel.URGENT -> "urgent"
    UrgencyLevel.IMPORTANT -> "high"
    UrgencyLevel.NORMAL -> "normal"
}
