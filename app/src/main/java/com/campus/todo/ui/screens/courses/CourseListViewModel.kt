package com.campus.todo.ui.screens.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.repo.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseListViewModel(
    private val repo: TodoRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isSearching = MutableStateFlow(false)

    val courses = repo.observeCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Course>())

    // Combined search results
    val searchResults = combine(
        _searchQuery,
        repo.observeCourses()
    ) { query, allCourses ->
        if (query.isBlank()) {
            allCourses
        } else {
            allCourses.filter { course ->
                course.name.contains(query, ignoreCase = true) ||
                course.code?.contains(query, ignoreCase = true) == true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isSearching: kotlinx.coroutines.flow.StateFlow<Boolean> = _isSearching
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _isSearching.value = query.isNotBlank()
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _isSearching.value = false
    }

    fun addCourse(name: String, code: String?, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repo.insertCourse(name, code)
            onCreated(id)
        }
    }

    fun deleteCourse(courseId: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repo.deleteCourse(courseId)
            onDeleted()
        }
    }
}
