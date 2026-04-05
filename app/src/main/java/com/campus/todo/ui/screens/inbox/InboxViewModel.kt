package com.campus.todo.ui.screens.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.CandidateItem
import com.campus.todo.data.db.entity.CandidateStatus
import com.campus.todo.data.repo.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Loading state for UI
 */
sealed class InboxLoadingState {
    object Loading : InboxLoadingState()
    object Success : InboxLoadingState()
    data class Error(val message: String) : InboxLoadingState()
}

data class InboxUiState(
    val loadingState: InboxLoadingState = InboxLoadingState.Loading,
    val candidates: List<CandidateItem> = emptyList(),
    val totalCount: Int = 0
)

class InboxViewModel(private val repo: TodoRepository) : ViewModel() {

    private val _loadingState = MutableStateFlow<InboxLoadingState>(InboxLoadingState.Loading)

    val state: StateFlow<InboxUiState> = combine(
        _loadingState,
        repo.observeNewCandidates()
    ) { loadingState, candidates ->
        InboxUiState(
            loadingState = InboxLoadingState.Success,
            candidates = candidates,
            totalCount = candidates.size
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        InboxUiState()
    )

    /**
     * Ignore multiple candidates at once
     */
    fun ignoreCandidates(ids: List<Long>) {
        viewModelScope.launch {
            try {
                repo.ignoreCandidates(ids)
            } catch (e: Exception) {
                _loadingState.value = InboxLoadingState.Error(e.message ?: "批量忽略失败")
            }
        }
    }

    /**
     * Delete multiple candidates at once
     */
    fun deleteCandidates(ids: List<Long>) {
        viewModelScope.launch {
            try {
                repo.deleteCandidates(ids)
            } catch (e: Exception) {
                _loadingState.value = InboxLoadingState.Error(e.message ?: "批量删除失败")
            }
        }
    }

    /**
     * Get candidate count by status
     */
    suspend fun getCountByStatus(status: CandidateStatus): Int {
        return try {
            repo.getCandidateCountByStatus(status)
        } catch (e: Exception) {
            0
        }
    }
}
