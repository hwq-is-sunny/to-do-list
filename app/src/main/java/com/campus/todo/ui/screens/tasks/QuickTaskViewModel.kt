package com.campus.todo.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.repo.TodoRepository
import kotlinx.coroutines.launch

class QuickTaskViewModel(
    private val repo: TodoRepository
) : ViewModel() {

    fun create(title: String, dueAtEpoch: Long?, onCreated: (Long) -> Unit) {
        val t = title.trim()
        if (t.isBlank()) return
        viewModelScope.launch {
            val id = repo.createManualTask(title = t, dueAtEpoch = dueAtEpoch)
            onCreated(id)
        }
    }
}
