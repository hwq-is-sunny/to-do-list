package com.campus.todo.ui.screens.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.SourceKind
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.network.AiParseSuggestion
import com.campus.todo.network.DeepSeekAssist
import kotlinx.coroutines.launch

class AddCandidateViewModel(
    private val repo: TodoRepository,
    private val deepSeek: DeepSeekAssist
) : ViewModel() {

    fun submit(raw: String, source: SourceKind, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repo.ingestRawText(raw, source)
            onCreated(id)
        }
    }

    fun aiAssist(
        raw: String,
        onOk: (AiParseSuggestion) -> Unit,
        onErr: (String) -> Unit
    ) {
        viewModelScope.launch {
            deepSeek.parseNotification(raw).fold(
                onSuccess = onOk,
                onFailure = { e -> onErr(e.message ?: "解析失败") }
            )
        }
    }
}
