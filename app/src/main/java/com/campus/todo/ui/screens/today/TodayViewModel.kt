package com.campus.todo.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TimetableSlot
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.util.TimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class TodayUiState(
    val dayOfWeek: Int,
    val timetableSlots: List<TimetableSlot>,
    val coursesById: Map<Long, Course>,
    val tasksDueToday: List<Task>,
    val upcomingTasks: List<Task>,
    val allPending: List<Task>
)

class TodayViewModel(
    repo: TodoRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val dow = today.dayOfWeek.value

    val state = combine(
        repo.observeTimetable(),
        repo.observeCourses(),
        repo.observePendingTasks()
    ) { slots, courses, tasks ->
        val map = courses.associateBy { it.id }
        val daySlots = slots.filter { it.dayOfWeek == dow }.sortedBy { it.startMinuteOfDay }
        val start = TimeUtils.startOfDayEpoch(today)
        val end = TimeUtils.endOfDayEpoch(today)
        val dueToday = tasks.filter { t ->
            val d = t.dueAtEpoch
            d != null && d in start..end
        }
        val now = System.currentTimeMillis()
        val weekEnd = now + 7L * 24 * 60 * 60 * 1000
        val upcoming = tasks.filter { t ->
            val d = t.dueAtEpoch
            d != null && d in (now + 1)..weekEnd && d !in start..end
        }.sortedBy { it.dueAtEpoch }
        TodayUiState(
            dayOfWeek = dow,
            timetableSlots = daySlots,
            coursesById = map,
            tasksDueToday = dueToday.sortedBy { it.dueAtEpoch },
            upcomingTasks = upcoming,
            allPending = tasks
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState(
        dow, emptyList(), emptyMap(), emptyList(), emptyList(), emptyList()
    ))
}
