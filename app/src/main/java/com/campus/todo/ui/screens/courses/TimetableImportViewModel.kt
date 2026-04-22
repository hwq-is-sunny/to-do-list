package com.campus.todo.ui.screens.courses

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.domain.timetable.TimetableOcrTextParser
import com.campus.todo.util.MinuteParse
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.launch
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

    fun parseTextToDrafts(raw: String): List<TimetableDraft> =
        TimetableOcrTextParser.parse(raw)

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

    private fun parseTimeRange(startRaw: String, endRaw: String): Pair<Int, Int>? {
        val start = MinuteParse.parseMinuteOfDay(startRaw) ?: return null
        val end = MinuteParse.parseMinuteOfDay(endRaw) ?: return null
        if (start >= end) return null
        return start to end
    }

    private fun parsePeriodRange(startRaw: String?, endRaw: String?): Pair<Int, Int>? {
        val start = startRaw?.trim()?.toIntOrNull() ?: return null
        val end = endRaw?.trim()?.toIntOrNull() ?: return null
        return periodRangeToMinute(start, end)
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
            11 to 21 * 60,
            12 to 21 * 60 + 55
        )
        val startMinute = periodStartMinute[startPeriod] ?: return null
        val endMinute = (periodStartMinute[endPeriod]?.plus(45)) ?: return null
        if (startMinute >= endMinute) return null
        return startMinute to endMinute
    }

}
