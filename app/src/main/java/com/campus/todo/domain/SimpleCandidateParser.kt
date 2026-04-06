package com.campus.todo.domain

import com.campus.todo.data.db.entity.SourceKind
import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.UrgencyLevel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
    // Date patterns
    private val dateYmd = Pattern.compile("(\\d{4})[./年\\-](\\d{1,2})[./月\\-](\\d{1,2})")
    private val dateMd = Pattern.compile("(\\d{1,2})[./月](\\d{1,2})")
    private val isoDate = Regex("(\\d{4}-\\d{2}-\\d{2})")
    private val timeHm = Pattern.compile("(\\d{1,2})[:：时](\\d{1,2})")

    // Course patterns
    private val courseRegex = Pattern.compile("《([^》]+)》|【([^】]+)】|课程[:：]\\s*([^\\s\\n]+)")

    // Time-related keywords for smart date parsing
    private val relativeDatePatterns = listOf(
        "今天" to 0L,
        "明天" to 1L,
        "后天" to 2L,
        "大后天" to 3L,
        "下周" to 7L,
        "下周一" to 7L,
        "下周二" to 8L,
        "下周三" to 9L,
        "下周四" to 10L,
        "下周五" to 11L,
        "下周六" to 12L,
        "下周日" to 13L,
        "周一" to nextWeekday(1),
        "周二" to nextWeekday(2),
        "周三" to nextWeekday(3),
        "周四" to nextWeekday(4),
        "周五" to nextWeekday(5),
        "周六" to nextWeekday(6),
        "周日" to nextWeekday(7)
    )

    private fun nextWeekday(target: Int): Long {
        val today = LocalDate.now()
        val currentDow = today.dayOfWeek.value
        val daysUntil = if (target <= currentDow) target + 7 - currentDow else target - currentDow
        return daysUntil.toLong()
    }

    fun parse(raw: String, @Suppress("UNUSED_PARAMETER") source: SourceKind): ParsedCandidateDraft {
        val text = raw.trim()
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        var title = firstLine.ifEmpty { "待确认事项" }
        var courseHint: String? = null
        var type = TaskType.OTHER
        var urgency = UrgencyLevel.NORMAL

        // Task type detection with priority
        when {
            text.contains("考试") || text.contains("测验") || text.contains("期末") ||
            text.contains("midterm", ignoreCase = true) || text.contains("final", ignoreCase = true) -> {
                type = TaskType.EXAM
                urgency = UrgencyLevel.IMPORTANT
            }
            text.contains("作业") || text.contains("报告") || text.contains("论文") ||
            text.contains("essay", ignoreCase = true) || text.contains("assignment", ignoreCase = true) -> {
                type = TaskType.HOMEWORK
                urgency = if (text.contains("紧急") || text.contains("今晚") ||
                             text.contains("urgent", ignoreCase = true)) UrgencyLevel.URGENT else UrgencyLevel.NORMAL
            }
            text.contains("签到") || text.contains("打卡") -> type = TaskType.SIGN_IN
            text.contains("上课") || text.contains("教室") || text.contains("课程") -> type = TaskType.CLASS
            text.contains("通知") || text.contains("公告") || text.contains("消息") -> type = TaskType.ANNOUNCEMENT
        }

        // Urgency detection
        if (text.contains("紧急") || text.contains("尽快") || text.contains("ddl", ignoreCase = true) ||
            text.contains("deadline", ignoreCase = true) || text.contains("asap", ignoreCase = true)) {
            urgency = UrgencyLevel.URGENT
        }

        // Course detection
        val cm = courseRegex.matcher(text)
        if (cm.find()) {
            courseHint = cm.group(1) ?: cm.group(2) ?: cm.group(3)
        }

        // Date extraction with smart relative date handling
        val due = extractDueDate(text)
        val confidence = buildString {
            append("规则解析")
            if (due != null) append("·含日期")
            if (courseHint != null) append("·含课程线索")
            if (type != TaskType.OTHER) append("·已识别类型")
        }

        // Title truncation
        if (title.length > 80) title = title.take(80) + "…"
        return ParsedCandidateDraft(title, courseHint, due, type, urgency, confidence)
    }

    private fun extractDueDate(text: String): Long? {
        val now = LocalDate.now()

        // Try relative date patterns first (e.g., "今天", "明天", "下周一")
        for ((keyword, daysOffset) in relativeDatePatterns) {
            if (text.contains(keyword)) {
                var targetDate = now.plusDays(daysOffset)
                // Extract time if present
                val tm = timeHm.matcher(text)
                return if (tm.find()) {
                    val hh = tm.group(1)!!.toInt().coerceIn(0, 23)
                    val mm = tm.group(2)!!.toInt().coerceIn(0, 59)
                    LocalDateTime.of(targetDate.year, targetDate.month, targetDate.dayOfMonth, hh, mm)
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } else {
                    // Default to end of day
                    LocalDateTime.of(targetDate.year, targetDate.month, targetDate.dayOfMonth, 23, 59)
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            }
        }

        // Try explicit date formats
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
            var year = now.year
            try {
                var date = LocalDate.of(year, mo, d)
                if (date.isBefore(now)) year += 1
                date = LocalDate.of(year, mo, d)
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
