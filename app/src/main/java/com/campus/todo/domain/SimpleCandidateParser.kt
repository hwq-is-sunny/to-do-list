package com.campus.todo.domain

import com.campus.todo.data.db.entity.SourceKind
import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.UrgencyLevel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

data class ParsedCandidateDraft(
    val title: String,
    val courseHint: String?,
    val dueAtEpoch: Long?,
    val taskType: TaskType,
    val urgency: UrgencyLevel,
    val confidenceNote: String
)

object SimpleCandidateParser {
    private val dateYmd = Pattern.compile("(\\d{4})[./年\\-](\\d{1,2})[./月\\-](\\d{1,2})")
    private val dateMd = Pattern.compile("(\\d{1,2})[./月](\\d{1,2})")
    private val isoDate = Regex("(\\d{4}-\\d{2}-\\d{2})")
    private val timeHm = Pattern.compile("(\\d{1,2})[:：时](\\d{1,2})")

    fun parse(raw: String, @Suppress("UNUSED_PARAMETER") source: SourceKind): ParsedCandidateDraft {
        val text = raw.trim()
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        var title = firstLine.ifEmpty { "待确认事项" }
        var courseHint: String? = null
        var type = TaskType.OTHER
        var urgency = UrgencyLevel.NORMAL

        when {
            text.contains("考试") || text.contains("测验") || text.contains("期末") -> {
                type = TaskType.EXAM
                urgency = UrgencyLevel.IMPORTANT
            }
            text.contains("作业") || text.contains("报告") || text.contains("论文") -> {
                type = TaskType.HOMEWORK
                urgency = if (text.contains("紧急") || text.contains("今晚")) UrgencyLevel.URGENT else UrgencyLevel.NORMAL
            }
            text.contains("签到") -> type = TaskType.SIGN_IN
            text.contains("上课") || text.contains("教室") -> type = TaskType.CLASS
            text.contains("通知") || text.contains("公告") -> type = TaskType.ANNOUNCEMENT
        }

        if (text.contains("紧急") || text.contains("尽快") || text.contains("ddl", ignoreCase = true)) {
            urgency = UrgencyLevel.URGENT
        }

        val courseRegex = Pattern.compile("《([^》]+)》|【([^】]+)】|课程[:：]\\s*([^\\s\\n]+)")
        val cm = courseRegex.matcher(text)
        if (cm.find()) {
            courseHint = cm.group(1) ?: cm.group(2) ?: cm.group(3)
        }

        val due = extractDueDate(text)
        val confidence = buildString {
            append("规则解析")
            if (due != null) append("·含日期")
            if (courseHint != null) append("·含课程线索")
        }

        if (title.length > 80) title = title.take(80) + "…"
        return ParsedCandidateDraft(title, courseHint, due, type, urgency, confidence)
    }

    private fun extractDueDate(text: String): Long? {
        val now = LocalDate.now()
        val ymd = dateYmd.matcher(text)
        if (ymd.find()) {
            return toEpochEndOfDayOrWithTime(
                text,
                ymd.group(1)!!.toInt(),
                ymd.group(2)!!.toInt(),
                ymd.group(3)!!.toInt()
            )
        }
        val md = dateMd.matcher(text)
        if (md.find()) {
            val mo = md.group(1)!!.toInt()
            val d = md.group(2)!!.toInt()
            var y = now.year
            try {
                var date = LocalDate.of(y, mo, d)
                if (date.isBefore(now)) y += 1
                date = LocalDate.of(y, mo, d)
                return toEpochEndOfDayOrWithTime(text, date.year, date.monthValue, date.dayOfMonth)
            } catch (_: Exception) {
                return null
            }
        }
        val iso = isoDate.find(text)?.groupValues?.get(1) ?: return null
        return try {
            val date = LocalDate.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE)
            toEpochEndOfDayOrWithTime(text, date.year, date.monthValue, date.dayOfMonth)
        } catch (_: Exception) {
            null
        }
    }

    private fun toEpochEndOfDayOrWithTime(text: String, y: Int, mo: Int, d: Int): Long? {
        return try {
            val tm = timeHm.matcher(text)
            val ldt = if (tm.find()) {
                val hh = tm.group(1)!!.toInt().coerceIn(0, 23)
                val mm = tm.group(2)!!.toInt().coerceIn(0, 59)
                LocalDateTime.of(y, mo, d, hh, mm)
            } else {
                LocalDateTime.of(y, mo, d, 23, 59)
            }
            ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
