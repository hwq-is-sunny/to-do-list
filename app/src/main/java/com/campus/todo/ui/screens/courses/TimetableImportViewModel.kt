package com.campus.todo.ui.screens.courses

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.util.MinuteParse
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

enum class WeekMode {
    EVERY,
    ODD,
    EVEN
}

data class TimetableDraft(
    val courseName: String,
    val teacher: String,
    val location: String,
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val startWeek: Int,
    val endWeek: Int,
    val weekMode: WeekMode,
    val courseType: String
)

class TimetableImportViewModel(
    private val repo: TodoRepository
) : ViewModel() {

    fun buildManualDraft(
        courseName: String,
        teacher: String,
        location: String,
        dayOfWeek: Int,
        startPeriodText: String,
        endPeriodText: String,
        startTimeText: String,
        endTimeText: String,
        startWeek: String,
        endWeek: String,
        weekMode: WeekMode,
        courseType: String
    ): Result<TimetableDraft> {
        val title = courseName.trim()
        if (title.isBlank()) return Result.failure(IllegalArgumentException("课程名称不能为空"))
        val locationText = location.trim()
        if (locationText.isBlank()) return Result.failure(IllegalArgumentException("上课地点不能为空"))
        val startWeekValue = startWeek.trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
        val endWeekValue = endWeek.trim().toIntOrNull()?.coerceAtLeast(startWeekValue) ?: startWeekValue
        val periodRange = parsePeriodRange(startPeriodText, endPeriodText)
        val minuteRange = periodRange ?: parseTimeRange(startTimeText.trim(), endTimeText.trim())
        if (minuteRange == null || minuteRange.first >= minuteRange.second) {
            return Result.failure(IllegalArgumentException("请填写有效时间（节次或 时:分，如 08:00）"))
        }
        return Result.success(
            TimetableDraft(
                courseName = title,
                teacher = teacher.trim(),
                location = locationText,
                dayOfWeek = dayOfWeek.coerceIn(1, 7),
                startMinute = minuteRange.first,
                endMinute = minuteRange.second,
                startWeek = startWeekValue,
                endWeek = endWeekValue,
                weekMode = weekMode,
                courseType = courseType.trim().ifBlank { "必修" }
            )
        )
    }

    fun parseTextToDrafts(raw: String): List<TimetableDraft> {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        return lines.mapNotNull { line ->
            parseLine(line)?.let { row ->
                TimetableDraft(
                    courseName = row.courseName,
                    teacher = row.teacher.orEmpty(),
                    location = row.location.orEmpty(),
                    dayOfWeek = row.dayOfWeek,
                    startMinute = row.startMinute,
                    endMinute = row.endMinute,
                    startWeek = row.startWeek ?: 1,
                    endWeek = row.endWeek ?: 20,
                    weekMode = row.weekMode,
                    courseType = row.courseType ?: "必修"
                )
            }
        }
    }

    fun importDrafts(
        drafts: List<TimetableDraft>,
        onResult: (Boolean, String) -> Unit
    ) {
        if (drafts.isEmpty()) {
            onResult(false, "请先添加待导入课程")
            return
        }
        viewModelScope.launch {
            runCatching {
                drafts.forEach { draft ->
                    val courseId = repo.insertCourse(draft.courseName, null)
                    val note = buildString {
                        if (draft.teacher.isNotBlank()) append("教师：${draft.teacher}；")
                        append("周次：${draft.startWeek}-${draft.endWeek}；")
                        append(
                            when (draft.weekMode) {
                                WeekMode.EVERY -> "每周；"
                                WeekMode.ODD -> "单周；"
                                WeekMode.EVEN -> "双周；"
                            }
                        )
                        if (draft.courseType.isNotBlank()) append("类型：${draft.courseType}")
                    }.trim().ifBlank { null }
                    repo.insertSlot(
                        courseId = courseId,
                        dayOfWeek = draft.dayOfWeek,
                        startMinute = draft.startMinute,
                        endMinute = draft.endMinute,
                        location = draft.location.ifBlank { null },
                        note = note
                    )
                }
            }.onSuccess {
                onResult(true, "已导入 ${drafts.size} 门课程")
            }.onFailure { e ->
                onResult(false, e.message ?: "导入失败")
            }
        }
    }

    suspend fun recognizeTextFromImage(context: Context, uri: Uri): Result<String> {
        return runCatching {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(
                ChineseTextRecognizerOptions.Builder().build()
            )
            try {
                suspendCancellableCoroutine { continuation ->
                    recognizer.process(image)
                        .addOnSuccessListener { continuation.resume(it.text) }
                        .addOnFailureListener { continuation.resumeWithException(it) }
                }
            } finally {
                recognizer.close()
            }
        }
    }

    private data class ParsedLine(
        val courseName: String,
        val teacher: String?,
        val dayOfWeek: Int,
        val startMinute: Int,
        val endMinute: Int,
        val location: String?,
        val startWeek: Int?,
        val endWeek: Int?,
        val weekMode: WeekMode,
        val courseType: String?
    )

    private fun parseLine(line: String): ParsedLine? {
        val normalized = line.replace('：', ':')
        val parts = normalized.split("|", ",").map { it.trim() }.filter { it.isNotBlank() }
        if (parts.size >= 3) {
            val courseName = parts[0]
            val dayOfWeek = parseDay(parts[1]) ?: return null
            val time = parseTimeRange(parts[2]) ?: parsePeriodRange(parts.getOrNull(2), parts.getOrNull(2))
            val (start, end) = time ?: return null
            val location = parts.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() && it != "-" }
            val teacher = parts.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() && it != "-" }
            return ParsedLine(courseName, teacher, dayOfWeek, start, end, location, null, null, WeekMode.EVERY, null)
        }
        val day = parseDay(normalized) ?: return null
        val timeByText = extractTimeRange(normalized) ?: extractPeriodRange(normalized) ?: return null
        val weekRange = extractWeekRange(normalized)
        val weekMode = when {
            normalized.contains("单周") -> WeekMode.ODD
            normalized.contains("双周") -> WeekMode.EVEN
            else -> WeekMode.EVERY
        }
        val teacher = extractTeacher(normalized)
        val location = extractLocation(normalized)
        val courseName = extractCourseName(normalized)
        if (courseName.isBlank()) return null
        return ParsedLine(
            courseName = courseName,
            teacher = teacher,
            dayOfWeek = day,
            startMinute = timeByText.first,
            endMinute = timeByText.second,
            location = location,
            startWeek = weekRange?.first,
            endWeek = weekRange?.second,
            weekMode = weekMode,
            courseType = extractCourseType(normalized)
        )
    }

    private fun parseTimeRange(startRaw: String, endRaw: String): Pair<Int, Int>? {
        val start = MinuteParse.parseMinuteOfDay(startRaw) ?: return null
        val end = MinuteParse.parseMinuteOfDay(endRaw) ?: return null
        if (start >= end) return null
        return start to end
    }

    private fun extractTimeRange(raw: String): Pair<Int, Int>? {
        val regex = Regex("(\\d{1,2}[:：]\\d{2})\\s*[-~—至]\\s*(\\d{1,2}[:：]\\d{2})")
        val match = regex.find(raw) ?: return null
        val startRaw = match.groupValues[1].replace('：', ':')
        val endRaw = match.groupValues[2].replace('：', ':')
        return parseTimeRange(startRaw, endRaw)
    }

    private fun parsePeriodRange(startRaw: String?, endRaw: String?): Pair<Int, Int>? {
        val start = startRaw?.trim()?.toIntOrNull() ?: return null
        val end = endRaw?.trim()?.toIntOrNull() ?: return null
        return periodRangeToMinute(start, end)
    }

    private fun extractPeriodRange(raw: String): Pair<Int, Int>? {
        val regex = Regex("(\\d{1,2})\\s*[-~—至]\\s*(\\d{1,2})\\s*节")
        val match = regex.find(raw) ?: return null
        return periodRangeToMinute(
            match.groupValues[1].toIntOrNull() ?: return null,
            match.groupValues[2].toIntOrNull() ?: return null
        )
    }

    private fun periodRangeToMinute(startPeriod: Int, endPeriod: Int): Pair<Int, Int>? {
        if (startPeriod <= 0 || endPeriod < startPeriod) return null
        val periodStartMinute = mapOf(
            1 to 8 * 60,
            2 to 8 * 60 + 55,
            3 to 10 * 60 + 10,
            4 to 11 * 60 + 5,
            5 to 14 * 60,
            6 to 14 * 60 + 55,
            7 to 16 * 60 + 10,
            8 to 17 * 60 + 5,
            9 to 19 * 60,
            10 to 19 * 60 + 55,
            11 to 21 * 60
        )
        val startMinute = periodStartMinute[startPeriod] ?: return null
        val endMinute = (periodStartMinute[endPeriod]?.plus(45)) ?: return null
        if (startMinute >= endMinute) return null
        return startMinute to endMinute
    }

    private fun extractWeekRange(raw: String): Pair<Int, Int>? {
        val regex = Regex("(\\d{1,2})\\s*[-~—至到]\\s*(\\d{1,2})\\s*周")
        val match = regex.find(raw) ?: return null
        val start = match.groupValues[1].toIntOrNull() ?: return null
        val end = match.groupValues[2].toIntOrNull() ?: return null
        if (start <= 0 || end < start) return null
        return start to end
    }

    private fun extractTeacher(raw: String): String? {
        val regex = Regex("(?:教师|老师|Teacher)[:：]?\\s*([\\p{L}A-Za-z· ]{2,20})")
        return regex.find(raw)?.groupValues?.getOrNull(1)?.trim()?.ifBlank { null }
    }

    private fun extractLocation(raw: String): String? {
        val regex = Regex("([A-Za-z0-9\\-]*\\s*(?:楼|教室|室|馆|实验室|讲堂)[A-Za-z0-9\\- ]*)")
        return regex.find(raw)?.value?.trim()?.ifBlank { null }
    }

    private fun extractCourseType(raw: String): String? = when {
        raw.contains("必修") -> "必修"
        raw.contains("选修") -> "选修"
        else -> null
    }

    private fun extractCourseName(raw: String): String {
        var text = raw
        text = text.replace(Regex("周[一二三四五六日天]|星期[一二三四五六日天]|Mon|Tue|Wed|Thu|Fri|Sat|Sun", RegexOption.IGNORE_CASE), " ")
        text = text.replace(Regex("\\d{1,2}[:：]\\d{2}\\s*[-~—至]\\s*\\d{1,2}[:：]\\d{2}"), " ")
        text = text.replace(Regex("\\d{1,2}\\s*[-~—至]\\s*\\d{1,2}\\s*节"), " ")
        text = text.replace(Regex("\\d{1,2}\\s*[-~—至到]\\s*\\d{1,2}\\s*周"), " ")
        text = text.replace(Regex("(单周|双周|每周|必修|选修|教师[:：]?|老师[:：]?|Teacher[:：]?)"), " ")
        text = text.replace(Regex("[,，|]"), " ")
        text = text.replace(Regex("\\s+"), " ").trim()
        return text.take(30)
    }

    private fun parseTimeRange(raw: String): Pair<Int, Int>? {
        val normalized = raw.replace("~", "-").replace("—", "-").replace("至", "-")
        val tokens = normalized.split("-").map { it.trim() }
        if (tokens.size != 2) return null
        val start = MinuteParse.parseMinuteOfDay(tokens[0]) ?: return null
        val end = MinuteParse.parseMinuteOfDay(tokens[1]) ?: return null
        if (start >= end) return null
        return start to end
    }

    private fun parseDay(raw: String): Int? {
        val token = raw.trim().lowercase(Locale.getDefault())
        return when {
            token.contains("周一") || token.contains("星期一") -> 1
            token.contains("周二") || token.contains("星期二") -> 2
            token.contains("周三") || token.contains("星期三") -> 3
            token.contains("周四") || token.contains("星期四") -> 4
            token.contains("周五") || token.contains("星期五") -> 5
            token.contains("周六") || token.contains("星期六") -> 6
            token.contains("周日") || token.contains("周天") || token.contains("星期日") || token.contains("星期天") -> 7
            token in setOf("1", "mon", "monday", "周一", "星期一") -> 1
            token in setOf("2", "tue", "tuesday", "周二", "星期二") -> 2
            token in setOf("3", "wed", "wednesday", "周三", "星期三") -> 3
            token in setOf("4", "thu", "thursday", "周四", "星期四") -> 4
            token in setOf("5", "fri", "friday", "周五", "星期五") -> 5
            token in setOf("6", "sat", "saturday", "周六", "星期六") -> 6
            token in setOf("7", "sun", "sunday", "周日", "周天", "星期日", "星期天") -> 7
            else -> null
        }
    }
}
