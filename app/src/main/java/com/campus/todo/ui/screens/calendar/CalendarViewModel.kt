package com.campus.todo.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TimetableSlot
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class DayCell(
    val date: LocalDate,
    val slots: List<TimetableSlot>,
    val tasks: List<Task>
)

data class CalendarUiState(
    val weekStart: LocalDate,
    val days: List<DayCell>,
    val coursesById: Map<Long, Course>,
    val selected: LocalDate
)

class CalendarViewModel(
    repo: TodoRepository
) : ViewModel() {

    private val selected = MutableStateFlow(LocalDate.now())

    val state = combine(
        selected,
        repo.observeTimetable(),
        repo.observeCourses(),
        repo.observePendingTasks()
    ) { sel, slots, courses, tasks ->
        val start = sel.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val map = courses.associateBy { it.id }
        val days = (0L until 7L).map { offset ->
            val d = start.plusDays(offset)
            val dow = d.dayOfWeek.value
            val daySlots = slots.filter { it.dayOfWeek == dow }.sortedBy { it.startMinuteOfDay }
            val ds = TimeUtils.startOfDayEpoch(d)
            val de = TimeUtils.endOfDayEpoch(d)
            val dayTasks = tasks.filter { t ->
                val due = t.dueAtEpoch
                due != null && due in ds..de
            }.sortedBy { it.dueAtEpoch }
            DayCell(d, daySlots, dayTasks)
        }
        CalendarUiState(weekStart = start, days = days, coursesById = map, selected = sel)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CalendarUiState(LocalDate.now(), emptyList(), emptyMap(), LocalDate.now())
    )

    fun selectDate(d: LocalDate) {
        selected.update { d }
    }

    fun shiftWeek(delta: Long) {
        selected.update { it.plusWeeks(delta) }
    }
}
