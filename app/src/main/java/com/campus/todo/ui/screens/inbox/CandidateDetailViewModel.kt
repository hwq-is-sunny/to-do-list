package com.campus.todo.ui.screens.inbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.CandidateItem
import com.campus.todo.data.db.entity.CandidateStatus
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.UrgencyLevel
import com.campus.todo.data.repo.TodoRepository
import com.campus.todo.notification.TaskReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CandidateDetailViewModel(
    private val repo: TodoRepository,
    private val scheduler: TaskReminderScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val candidateId: Long = checkNotNull(savedStateHandle["candidateId"])

    private val itemFlow = MutableStateFlow<CandidateItem?>(null)

    init {
        viewModelScope.launch {
            itemFlow.value = repo.getCandidate(candidateId)
        }
    }

    val candidate = itemFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val courses = repo.observeCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Course>())

    fun refresh() {
        viewModelScope.launch {
            itemFlow.value = repo.getCandidate(candidateId)
        }
    }

    fun updateDraft(
        title: String,
        courseHint: String?,
        dueAtEpoch: Long?,
        taskType: TaskType,
        urgency: UrgencyLevel,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val c = itemFlow.value ?: return@launch
            repo.updateCandidateDraft(
                c.copy(
                    parsedTitle = title,
                    parsedCourseHint = courseHint,
                    parsedDueAtEpoch = dueAtEpoch,
                    suggestedTaskType = taskType,
                    suggestedUrgency = urgency
                )
            )
            refresh()
            val updated = itemFlow.value
            val due = updated?.parsedDueAtEpoch
            if (updated != null && updated.status == CandidateStatus.NEW && due != null) {
                scheduler.scheduleDraftReminder(candidateId, due)
            } else {
                scheduler.cancelDraftReminder(candidateId)
            }
            onSaved()
        }
    }

    fun ignore(onDone: () -> Unit) {
        viewModelScope.launch {
            scheduler.cancelDraftReminder(candidateId)
            repo.ignoreCandidate(candidateId)
            onDone()
        }
    }

    fun confirm(
        title: String,
        courseId: Long?,
        dueAtEpoch: Long?,
        taskType: TaskType,
        urgency: UrgencyLevel,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val c = itemFlow.value ?: return@launch
            scheduler.cancelDraftReminder(candidateId)
            val taskId = repo.confirmCandidate(c, title, courseId, dueAtEpoch, taskType, urgency)
            val task = repo.getTask(taskId) ?: return@launch
            scheduler.rescheduleTask(task)
            onDone()
        }
    }
}
