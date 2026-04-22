package com.campus.todo.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TaskStatus
import com.campus.todo.data.db.entity.TimetableSlot
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.data.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CalendarUiState(
    val dateStrip: List<LocalDate>,
    val selectedDayTasks: List<Task>,
    val selectedDaySlots: List<TimetableSlot>,
    val coursesById: Map<Long, Course>,
    val selected: LocalDate,
    val monthTitle: String,
    val displayWeekCount: Boolean
)

class CalendarViewModel(
    repo: TodoRepository,
    settingsStore: SettingsStore
) : ViewModel() {

    private val today = LocalDate.now()
    private val selected = MutableStateFlow(today)

    val state = combine(
        selected,
        settingsStore.settingsFlow,
        repo.observeTimetable(),
        repo.observeCourses(),
        repo.observeAllTasks()
    ) { sel, settings, slots, courses, allTasks ->
        val month = YearMonth.from(sel)
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        val strip = (1..month.lengthOfMonth()).map { month.atDay(it) }
        val normalized = if (sel in monthStart..monthEnd) sel else monthStart
        val map = courses.associateBy { it.id }
        val monthFormatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)

        val selectedSlots = slots
            .filter { it.dayOfWeek == normalized.dayOfWeek.value }
            .sortedBy { it.startMinuteOfDay }
        val selectedTasks = allTasks
            .filter { it.status == TaskStatus.PENDING && taskOccursOnDay(it, normalized) }
            .sortedBy { taskSortMinute(it) }

        CalendarUiState(
            dateStrip = strip,
            selectedDayTasks = selectedTasks,
            selectedDaySlots = selectedSlots,
            coursesById = map,
            selected = normalized,
            monthTitle = normalized.format(monthFormatter),
            displayWeekCount = settings.displayWeekCount
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        run {
            val start = YearMonth.from(today)
            val selectedDay = if (YearMonth.from(today) == start) today else start.atDay(1)
            CalendarUiState(
                (1..start.lengthOfMonth()).map { start.atDay(it) },
                emptyList(),
                emptyList(),
                emptyMap(),
                selectedDay,
                selectedDay.format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)),
                true
            )
        }
    )

    fun selectDate(d: LocalDate) {
        val stripStart = YearMonth.from(selected.value).atDay(1)
        val stripEnd = YearMonth.from(selected.value).atEndOfMonth()
        if (d in stripStart..stripEnd) {
            selected.update { d }
        }
    }

    fun previousMonth() {
        selected.update { current ->
            YearMonth.from(current).minusMonths(1).atDay(1)
        }
    }

    fun nextMonth() {
        selected.update { current ->
            YearMonth.from(current).plusMonths(1).atDay(1)
        }
    }

    private fun taskOccursOnDay(task: Task, date: LocalDate): Boolean {
        val z = ZoneId.systemDefault()
        val start = date.atStartOfDay(z).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(z).toInstant().toEpochMilli() - 1
        val due = task.dueAtEpoch
        if (due != null && due in start..end) return true
        val s = task.startAtEpoch
        if (s != null && s in start..end) return true
        val e = task.endAtEpoch
        return e != null && e in start..end
    }

    private fun taskSortMinute(task: Task): Long {
        val z = ZoneId.systemDefault()
        task.startAtEpoch?.let {
            val t = Instant.ofEpochMilli(it).atZone(z).toLocalTime()
            return (t.hour * 60 + t.minute).toLong()
        }
        task.dueAtEpoch?.let {
            val t = Instant.ofEpochMilli(it).atZone(z).toLocalTime()
            return (t.hour * 60 + t.minute).toLong()
        }
        return Long.MAX_VALUE
    }
}
