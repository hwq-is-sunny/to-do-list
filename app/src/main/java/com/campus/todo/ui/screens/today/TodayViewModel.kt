package com.campus.todo.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TaskStatus
import com.campus.todo.data.db.entity.TimetableSlot
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.data.settings.AppSettings
import com.campus.todo.data.settings.SettingsStore
import com.campus.todo.data.session.SessionStore
import com.campus.todo.notification.TaskReminderScheduler
import com.campus.todo.util.TimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayUiState(
    val nickname: String,
    val dayOfWeek: Int,
    val timetableSlots: List<TimetableSlot>,
    val coursesById: Map<Long, Course>,
    val tasksDueToday: List<Task>,
    val upcomingTasks: List<Task>,
    val allPending: List<Task>,
    /** 含已完成，用于统计与归档视图 */
    val allTasks: List<Task>,
    val showCompletedTasks: Boolean
)

class TodayViewModel(
    private val repo: TodoRepository,
    private val settingsStore: SettingsStore,
    private val sessionStore: SessionStore,
    private val reminderScheduler: TaskReminderScheduler
) : ViewModel() {

    private val today = LocalDate.now()
    private val dow = today.dayOfWeek.value

    val state = combine(
        settingsStore.settingsFlow,
        sessionStore.usernameFlow,
        repo.observeTimetable(),
        repo.observeCourses(),
        repo.observeAllTasks()
    ) { settings: AppSettings, username: String, slots: List<TimetableSlot>, courses: List<Course>, all: List<Task> ->
        val pending = all.filter { it.status == TaskStatus.PENDING }
        val map = courses.associateBy { it.id }
        val daySlots = slots.filter { it.dayOfWeek == dow }.sortedBy { it.startMinuteOfDay }
        val start = TimeUtils.startOfDayEpoch(today)
        val end = TimeUtils.endOfDayEpoch(today)
        val dueToday = pending.filter { t ->
            val d = t.dueAtEpoch
            d != null && d in start..end
        }
        val now = System.currentTimeMillis()
        val weekEnd = now + 7L * 24 * 60 * 60 * 1000
        val upcoming = pending.filter { t ->
            val d = t.dueAtEpoch
            d != null && d in (now + 1)..weekEnd && d !in start..end
        }.sortedBy { it.dueAtEpoch }
        TodayUiState(
            nickname = settings.nickname.ifBlank { username },
            dayOfWeek = dow,
            timetableSlots = daySlots,
            coursesById = map,
            tasksDueToday = dueToday.sortedBy { it.dueAtEpoch },
            upcomingTasks = upcoming,
            allPending = pending,
            allTasks = all,
            showCompletedTasks = settings.showCompletedTasks
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TodayUiState(
            nickname = "",
            dayOfWeek = dow,
            timetableSlots = emptyList(),
            coursesById = emptyMap(),
            tasksDueToday = emptyList(),
            upcomingTasks = emptyList(),
            allPending = emptyList(),
            allTasks = emptyList(),
            showCompletedTasks = true
        )
    )

    fun markDone(taskId: Long) {
        viewModelScope.launch {
            repo.markTaskDone(taskId)
            reminderScheduler.cancelTask(taskId)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            reminderScheduler.cancelTask(taskId)
            repo.deleteTask(taskId)
        }
    }

    fun renameTask(taskId: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repo.updateTaskTitle(taskId, title) }
    }
}
