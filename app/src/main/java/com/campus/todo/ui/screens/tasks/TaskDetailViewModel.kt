package com.campus.todo.ui.screens.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.repo.TodoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskDetailViewModel(
    private val repo: TodoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: Long = requireNotNull(savedStateHandle.get<Long>("taskId"))

    val task: StateFlow<Task?> = repo.observeTask(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(task: Task, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.updateTaskEditable(task)
            onDone()
        }
    }

    fun markDone(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.markTaskDone(taskId)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteTask(taskId)
            onDone()
        }
    }
}
