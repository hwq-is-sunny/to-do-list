package com.campus.todo.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TimetableSlot
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Loading state for UI
 */
sealed class LoadingState {
    object Loading : LoadingState()
    object Success : LoadingState()
    data class Error(val message: String) : LoadingState()
}

data class TodayUiState(
    val loadingState: LoadingState = LoadingState.Loading,
    val dayOfWeek: Int,
    val timetableSlots: List<TimetableSlot>,
    val coursesById: Map<Long, Course>,
    val tasksDueToday: List<Task>,
    val upcomingTasks: List<Task>,
    val allPending: List<Task>
)

class TodayViewModel(
    private val repo: TodoRepository
) : ViewModel() {

    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Loading)

    private val today = LocalDate.now()
    private val dow = today.dayOfWeek.value

    val state: StateFlow<TodayUiState> = combine(
        _loadingState,
        repo.observeTimetable(),
        repo.observeCourses(),
        repo.observePendingTasks()
    ) { loadingState, slots, courses, tasks ->
        try {
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
                loadingState = LoadingState.Success,
                dayOfWeek = dow,
                timetableSlots = daySlots,
                coursesById = map,
                tasksDueToday = dueToday.sortedBy { it.dueAtEpoch },
                upcomingTasks = upcoming,
                allPending = tasks
            )
        } catch (e: Exception) {
            TodayUiState(
                loadingState = LoadingState.Error(e.message ?: "未知错误"),
                dayOfWeek = dow,
                timetableSlots = emptyList(),
                coursesById = emptyMap(),
                tasksDueToday = emptyList(),
                upcomingTasks = emptyList(),
                allPending = emptyList()
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState(
        dayOfWeek = dow, timetableSlots = emptyList(), coursesById = emptyMap(),
        tasksDueToday = emptyList(), upcomingTasks = emptyList(), allPending = emptyList()
    ))

    /**
     * Mark a task as done
     */
    fun markTaskDone(taskId: Long) {
        viewModelScope.launch {
            try {
                repo.markTaskDone(taskId)
            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error(e.message ?: "操作失败")
            }
        }
    }

    /**
     * Mark multiple tasks as done at once
     */
    fun markTasksDone(taskIds: List<Long>) {
        viewModelScope.launch {
            try {
                repo.markTasksDone(taskIds)
            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error(e.message ?: "批量操作失败")
            }
        }
    }

    /**
     * Delete a task
     */
    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            try {
                repo.deleteTask(taskId)
            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error(e.message ?: "删除失败")
            }
        }
    }
}
