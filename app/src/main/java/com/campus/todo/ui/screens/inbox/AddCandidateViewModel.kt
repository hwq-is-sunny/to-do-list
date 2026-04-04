package com.campus.todo.ui.screens.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.SourceKind
import com.campus.todo.data.repo.TodoRepository
import kotlinx.coroutines.launch

class AddCandidateViewModel(
    private val repo: TodoRepository
) : ViewModel() {

    fun submit(raw: String, source: SourceKind, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repo.ingestRawText(raw, source)
            onCreated(id)
        }
    }
}
