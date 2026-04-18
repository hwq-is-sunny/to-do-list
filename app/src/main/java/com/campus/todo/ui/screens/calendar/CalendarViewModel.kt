package com.campus.todo.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TimetableSlot
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.data.settings.SettingsStore
import com.campus.todo.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
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

    private val selected = MutableStateFlow(LocalDate.now())

    val state = combine(
        selected,
        settingsStore.settingsFlow,
        repo.observeTimetable(),
        repo.observeCourses(),
        repo.observePendingTasks()
    ) { sel, settings, slots, courses, tasks ->
        val today = LocalDate.now()
        val strip = (0L until 7L).map { today.plusDays(it) }
        val active = if (sel.isBefore(today)) today else sel
        val normalized = strip.find { it == active } ?: today
        val map = courses.associateBy { it.id }
        val locale = Locale.getDefault()
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)

        val selectedSlots = slots
            .filter { it.dayOfWeek == normalized.dayOfWeek.value }
            .sortedBy { it.startMinuteOfDay }
        val dayStart = TimeUtils.startOfDayEpoch(normalized)
        val dayEnd = TimeUtils.endOfDayEpoch(normalized)
        val selectedTasks = tasks
            .filter { dueTask ->
                val due = dueTask.dueAtEpoch
                due != null && due in dayStart..dayEnd
            }
            .sortedBy { it.dueAtEpoch }

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
        CalendarUiState(
            (0L until 7L).map { LocalDate.now().plusDays(it) },
            emptyList(),
            emptyList(),
            emptyMap(),
            LocalDate.now(),
            LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
            true
        )
    )

    fun selectDate(d: LocalDate) {
        val today = LocalDate.now()
        val latest = today.plusDays(6)
        if (d in today..latest) {
            selected.update { d }
        }
    }
}
