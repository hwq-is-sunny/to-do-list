package com.campus.todo.ui.screens.inbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.CandidateItem
import com.campus.todo.data.db.entity.CandidateStatus
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.UrgencyLevel
import com.campus.todo.data.parser.mapNoticeTypeToTaskType
import com.campus.todo.data.parser.mapPriorityTagToUrgency
import com.campus.todo.data.parser.mapUrgencyToPriorityTag
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
        noticeType: String,
        location: String?,
        note: String?,
        courseHint: String?,
        dueAtEpoch: Long?,
        startAtEpoch: Long?,
        endAtEpoch: Long?,
        dateEpochDay: Long?,
        startMinute: Int?,
        endMinute: Int?,
        taskType: TaskType,
        urgency: UrgencyLevel,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val c = itemFlow.value ?: return@launch
            repo.updateCandidateDraft(
                c.copy(
                    parsedTitle = title,
                    parsedNoticeType = noticeType,
                    parsedCategory = noticeType,
                    parsedLocation = location?.trim()?.ifBlank { null },
                    parsedDescription = note?.trim()?.ifBlank { null },
                    parsedCourseHint = courseHint?.trim()?.ifBlank { null },
                    parsedDueAtEpoch = dueAtEpoch,
                    parsedStartAtEpoch = startAtEpoch,
                    parsedEndAtEpoch = endAtEpoch,
                    parsedDateEpochDay = dateEpochDay,
                    parsedStartMinuteOfDay = startMinute,
                    parsedEndMinuteOfDay = endMinute,
                    parsedPriorityTag = mapUrgencyToPriorityTag(urgency),
                    suggestedTaskType = taskType,
                    suggestedUrgency = urgency
                )
            )
            refresh()
            val updated = itemFlow.value
            val due = updated?.parsedDueAtEpoch
            if (updated != null && updated.status == CandidateStatus.PENDING && due != null) {
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
            repo.dismissCandidate(candidateId)
            onDone()
        }
    }

    fun confirm(
        title: String,
        noticeType: String,
        courseId: Long?,
        location: String?,
        note: String?,
        dueAtEpoch: Long?,
        startAtEpoch: Long?,
        endAtEpoch: Long?,
        taskType: TaskType,
        urgency: UrgencyLevel,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val c = itemFlow.value ?: return@launch
            scheduler.cancelDraftReminder(candidateId)
            if (noticeType == "course") {
                repo.confirmCandidateAsCourse(
                    candidate = c,
                    courseName = title,
                    location = location,
                    note = note
                )
            } else {
                val mappedType = mapNoticeTypeToTaskType(noticeType)
                val useType = if (mappedType == TaskType.OTHER) taskType else mappedType
                val taskId = repo.confirmCandidate(
                    candidate = c,
                    title = title,
                    courseId = courseId,
                    dueAtEpoch = dueAtEpoch,
                    taskType = useType,
                    urgency = urgency,
                    description = note,
                    location = location,
                    startAtEpoch = startAtEpoch,
                    endAtEpoch = endAtEpoch
                )
                val task = repo.getTask(taskId) ?: return@launch
                scheduler.rescheduleTask(task)
            }
            onDone()
        }
    }

    fun mergeToLinkedTask(onDone: () -> Unit) {
        viewModelScope.launch {
            val c = itemFlow.value ?: return@launch
            val taskId = c.linkedTaskId ?: return@launch
            repo.attachCandidateToTask(c.id, taskId)
            scheduler.cancelDraftReminder(candidateId)
            onDone()
        }
    }
}
