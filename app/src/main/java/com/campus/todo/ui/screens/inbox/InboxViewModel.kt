package com.campus.todo.ui.screens.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.CandidateStatus
import com.campus.todo.data.db.entity.CandidateItem
import com.campus.todo.data.repo.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class InboxFilter { ALL, PENDING, POSSIBLE_DUPLICATE, MERGED, DISMISSED }
enum class InboxSort { LATEST, DUE_DATE, PRIORITY }

data class InboxUiState(
    val filter: InboxFilter = InboxFilter.ALL,
    val sort: InboxSort = InboxSort.LATEST,
    val selectedIds: Set<Long> = emptySet(),
    val items: List<CandidateItem> = emptyList()
)

class InboxViewModel(private val repo: TodoRepository) : ViewModel() {
    private val uiControl = MutableStateFlow(InboxUiState())

    val state = combine(repo.observeCandidates(), uiControl) { all, control ->
        var list = when (control.filter) {
            InboxFilter.ALL -> all
            InboxFilter.PENDING -> all.filter { it.status == CandidateStatus.PENDING }
            InboxFilter.POSSIBLE_DUPLICATE -> all.filter { it.duplicateScore >= 0.64f && it.status == CandidateStatus.PENDING }
            InboxFilter.MERGED -> all.filter { it.status == CandidateStatus.MERGED }
            InboxFilter.DISMISSED -> all.filter { it.status == CandidateStatus.DISMISSED }
        }
        list = when (control.sort) {
            InboxSort.LATEST -> list.sortedByDescending { it.createdAtEpoch }
            InboxSort.DUE_DATE -> list.sortedBy { it.parsedDueAtEpoch ?: Long.MAX_VALUE }
            InboxSort.PRIORITY -> list.sortedByDescending { it.suggestedUrgency?.ordinal ?: 0 }
        }
        control.copy(items = list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState())

    fun setFilter(filter: InboxFilter) = uiControl.update { it.copy(filter = filter) }
    fun setSort(sort: InboxSort) = uiControl.update { it.copy(sort = sort) }

    fun toggleSelection(id: Long) {
        uiControl.update { state ->
            val next = state.selectedIds.toMutableSet()
            if (!next.add(id)) next.remove(id)
            state.copy(selectedIds = next)
        }
    }

    fun clearSelection() = uiControl.update { it.copy(selectedIds = emptySet()) }

    fun batchConfirm() {
        val ids = uiControl.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repo.confirmCandidates(ids)
            clearSelection()
        }
    }

    fun batchDismiss() {
        val ids = uiControl.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repo.dismissCandidates(ids)
            clearSelection()
        }
    }

    fun batchMerge() {
        val ids = uiControl.value.selectedIds.toList()
        if (ids.size < 2) return
        viewModelScope.launch {
            repo.mergeCandidates(ids)
            clearSelection()
        }
    }
}
