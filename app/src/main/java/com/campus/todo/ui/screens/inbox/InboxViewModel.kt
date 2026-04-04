package com.campus.todo.ui.screens.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.CandidateItem
import com.campus.todo.data.repo.TodoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class InboxViewModel(repo: TodoRepository) : ViewModel() {
    val candidates = repo.observeNewCandidates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<CandidateItem>())
}
