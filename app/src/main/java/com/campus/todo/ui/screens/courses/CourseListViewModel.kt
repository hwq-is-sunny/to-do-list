package com.campus.todo.ui.screens.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TimetableSlot
import com.campus.todo.data.repo.TodoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseListViewModel(
    private val repo: TodoRepository
) : ViewModel() {
    val courses = repo.observeCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Course>())
    val timetableSlots = repo.observeTimetable()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<TimetableSlot>())
    val pendingTasks = repo.observePendingTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Task>())

    fun addCourse(name: String, code: String?, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repo.insertCourse(name, code)
            onCreated(id)
        }
    }

    fun updateCourse(id: Long, name: String, code: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.updateCourse(id, name, code)
            onDone()
        }
    }

    fun deleteCourse(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteCourse(id)
            onDone()
        }
    }
}
