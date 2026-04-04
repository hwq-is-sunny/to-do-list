package com.campus.todo.ui.screens.courses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TimetableSlot
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.notification.TaskReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CourseDetailUi(
    val course: Course,
    val slots: List<TimetableSlot>,
    val tasks: List<Task>
)

class CourseDetailViewModel(
    private val repo: TodoRepository,
    private val scheduler: TaskReminderScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val courseId: Long = checkNotNull(savedStateHandle["courseId"])

    private val courseFlow = MutableStateFlow<Course?>(null)

    init {
        viewModelScope.launch {
            courseFlow.value = repo.getCourse(courseId)
        }
    }

    val detail = combine(
        courseFlow.filterNotNull(),
        repo.observeTimetableForCourse(courseId),
        repo.observePendingForCourse(courseId)
    ) { c, sl, tk ->
        CourseDetailUi(
            c,
            sl.sortedBy { it.dayOfWeek * 1440 + it.startMinuteOfDay },
            tk
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addSlot(
        dayOfWeek: Int,
        startMin: Int,
        endMin: Int,
        location: String?,
        note: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repo.insertSlot(courseId, dayOfWeek, startMin, endMin, location, note)
            onDone()
        }
    }

    fun deleteSlot(id: Long) {
        viewModelScope.launch { repo.deleteSlot(id) }
    }

    fun markTaskDone(taskId: Long) {
        viewModelScope.launch {
            repo.markTaskDone(taskId)
            scheduler.cancelTask(taskId)
        }
    }
}
