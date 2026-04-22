package com.campus.todo.domain

import com.campus.todo.data.db.entity.SourceKind
import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.UrgencyLevel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

data class ParsedCandidateDraft(
    val title: String,
    val description: String? = null,
    val courseHint: String?,
    val dueDateText: String? = null,
    val dueAtEpoch: Long?,
    val startAtEpoch: Long? = null,
    val endAtEpoch: Long? = null,
    val location: String? = null,
    val category: String = TaskType.OTHER.name,
    val taskType: TaskType,
    val urgency: UrgencyLevel,
    val confidenceNote: String,
    val confidenceScore: Float = 0.45f
)

object SimpleCandidateParser {
    private val dateYmd = Pattern.compile("(\\d{4})[./年\\-](\\d{1,2})[./月\\-](\\d{1,2})")
    private val dateMd = Pattern.compile("(\\d{1,2})[./月](\\d{1,2})")
    private val isoDate = Regex("(\\d{4}-\\d{2}-\\d{2})")
    private val timeHm = Pattern.compile("(\\d{1,2})[:：时](\\d{1,2})")
    private val classPeriod = Pattern.compile("第\\s*(\\d{1,2})\\s*[-到至]\\s*(\\d{1,2})\\s*节")

    fun parse(raw: String, @Suppress("UNUSED_PARAMETER") source: SourceKind): ParsedCandidateDraft {
        val text = raw.trim()
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        var title = firstLine.ifEmpty { "待确认事项" }
        var courseHint: String? = null
        var taskType = TaskType.OTHER
        var urgency = UrgencyLevel.NORMAL
        var location: String? = null

        when {
            text.contains("考试") || text.contains("测验") || text.contains("期末") -> {
                taskType = TaskType.EXAM
                urgency = UrgencyLevel.IMPORTANT
            }
            text.contains("作业") || text.contains("报告") || text.contains("论文") || text.contains("实验") -> {
                taskType = TaskType.HOMEWORK
            }
            text.contains("签到") -> taskType = TaskType.SIGN_IN
            text.contains("上课") || text.contains("教室") || text.contains("第") && text.contains("节") -> taskType = TaskType.CLASS
            text.contains("通知") || text.contains("公告") -> taskType = TaskType.ANNOUNCEMENT
        }
        if (text.contains("紧急") || text.contains("尽快") || text.contains("ddl", ignoreCase = true)) {
            urgency = UrgencyLevel.URGENT
        }

        val courseRegex = Pattern.compile("《([^》]+)》|【([^】]+)】|课程[:：]\\s*([^\\s\\n]+)")
        val cm = courseRegex.matcher(text)
        if (cm.find()) {
            courseHint = cm.group(1) ?: cm.group(2) ?: cm.group(3)
        }
        val roomRegex = Pattern.compile("(教室|地点|实验室|room|Room)[:：\\s]*([^，,。\\n]+)")
        val rm = roomRegex.matcher(text)
        if (rm.find()) {
            location = rm.group(2)?.trim()
        }

        val due = extractDueDate(text)
        val day = due?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
        val startEnd = extractStartEnd(text, day)
        val confidence = buildString {
            append("规则解析")
            if (due != null) append("·含日期")
            if (courseHint != null) append("·含课程线索")
            if (location != null) append("·含地点")
        }

        if (title.length > 80) title = title.take(80) + "…"
        return ParsedCandidateDraft(
            title = title,
            description = text.take(180),
            courseHint = courseHint,
            dueDateText = day?.toString(),
            dueAtEpoch = due,
            startAtEpoch = startEnd?.first,
            endAtEpoch = startEnd?.second,
            location = location,
            category = taskType.name,
            taskType = taskType,
            urgency = urgency,
            confidenceNote = confidence,
            confidenceScore = if (due != null && courseHint != null) 0.78f else 0.55f
        )
    }

    private fun extractDueDate(text: String): Long? {
        val now = LocalDate.now()
        val relative = when {
            text.contains("后天") -> now.plusDays(2)
            text.contains("明天") -> now.plusDays(1)
            text.contains("今天") -> now
            else -> null
        }
        if (relative != null) {
            return toEpochEndOfDayOrWithTime(text, relative.year, relative.monthValue, relative.dayOfMonth)
        }
        parseWeekdayDate(text)?.let {
            return toEpochEndOfDayOrWithTime(text, it.year, it.monthValue, it.dayOfMonth)
        }
        val ymd = dateYmd.matcher(text)
        if (ymd.find()) {
            val year = ymd.group(1)?.toIntOrNull() ?: return null
            val month = ymd.group(2)?.toIntOrNull() ?: return null
            val day = ymd.group(3)?.toIntOrNull() ?: return null
            return toEpochEndOfDayOrWithTime(
                text,
                year,
                month,
                day
            )
        }
        val md = dateMd.matcher(text)
        if (md.find()) {
            val month = md.group(1)?.toIntOrNull() ?: return null
            val day = md.group(2)?.toIntOrNull() ?: return null
            var year = now.year
            return try {
                var date = LocalDate.of(year, month, day)
                if (date.isBefore(now)) year += 1
                date = LocalDate.of(year, month, day)
                toEpochEndOfDayOrWithTime(text, date.year, date.monthValue, date.dayOfMonth)
            } catch (_: Exception) {
                null
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

    private fun parseWeekdayDate(text: String): LocalDate? {
        val now = LocalDate.now()
        val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
        val idx = weekdays.indexOfFirst { text.contains("周$it") || text.contains("星期$it") }
        if (idx < 0) return null
        val target = if (idx == 6) 7 else idx + 1
        val current = now.dayOfWeek.value
        val baseShift = if (text.contains("下周")) 7 else 0
        val delta = (target - current + 7) % 7
        return now.plusDays((baseShift + delta).toLong())
    }

    private fun extractStartEnd(text: String, day: LocalDate?): Pair<Long, Long>? {
        val actualDay = day ?: LocalDate.now()
        val period = classPeriod.matcher(text)
        if (period.find()) {
            val startSection = period.group(1).toIntOrNull() ?: return null
            val endSection = period.group(2).toIntOrNull() ?: return null
            val startMinute = 8 * 60 + (startSection - 1) * 50
            val endMinute = 8 * 60 + endSection * 50
            val z = ZoneId.systemDefault()
            val start = actualDay.atTime(startMinute / 60, startMinute % 60).atZone(z).toInstant().toEpochMilli()
            val end = actualDay.atTime(endMinute / 60, endMinute % 60).atZone(z).toInstant().toEpochMilli()
            return start to end
        }
        val tm = timeHm.matcher(text)
        if (tm.find()) {
            val hour = tm.group(1)?.toIntOrNull()?.coerceIn(0, 23) ?: return null
            val minute = tm.group(2)?.toIntOrNull()?.coerceIn(0, 59) ?: return null
            val z = ZoneId.systemDefault()
            val start = actualDay.atTime(hour, minute).atZone(z).toInstant().toEpochMilli()
            val end = actualDay.atTime(hour, minute).plusMinutes(90).atZone(z).toInstant().toEpochMilli()
            return start to end
        }
        return null
    }

    private fun toEpochEndOfDayOrWithTime(text: String, y: Int, mo: Int, d: Int): Long? {
        return try {
            val tm = timeHm.matcher(text)
            val localDateTime = if (tm.find()) {
                val hour = tm.group(1)?.toIntOrNull()?.coerceIn(0, 23) ?: return null
                val minute = tm.group(2)?.toIntOrNull()?.coerceIn(0, 59) ?: return null
                LocalDateTime.of(y, mo, d, hour, minute)
            } else {
                LocalDateTime.of(y, mo, d, 23, 59)
            }
            localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
