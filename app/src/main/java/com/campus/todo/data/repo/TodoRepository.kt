package com.campus.todo.data.repo

import com.campus.todo.data.db.AppDatabase
import com.campus.todo.data.db.entity.CandidateItem
import com.campus.todo.data.db.entity.CandidateStatus
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.SourceKind
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TaskStatus
import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.TimetableSlot
import com.campus.todo.data.db.entity.UrgencyLevel
import com.campus.todo.domain.SimpleCandidateParser
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class TodoRepository(private val db: AppDatabase) {
    private val courses = db.courseDao()
    private val slots = db.timetableDao()
    private val tasks = db.taskDao()
    private val candidates = db.candidateDao()

    fun observeCourses(): Flow<List<Course>> = courses.observeAll()
    suspend fun getCourse(id: Long): Course? = courses.getById(id)
    suspend fun insertCourse(name: String, code: String?): Long {
        val id = courses.insert(
            Course(name = name.trim(), code = code?.trim()?.ifEmpty { null })
        )
        return id
    }

    fun observeTimetable(): Flow<List<TimetableSlot>> = slots.observeAll()
    fun observeTimetableForCourse(courseId: Long): Flow<List<TimetableSlot>> =
        slots.observeForCourse(courseId)

    suspend fun slotsForDay(dayOfWeek: Int): List<TimetableSlot> = slots.slotsForDay(dayOfWeek)

    suspend fun insertSlot(
        courseId: Long,
        dayOfWeek: Int,
        startMinute: Int,
        endMinute: Int,
        location: String?,
        note: String?
    ): Long = slots.insert(
        TimetableSlot(
            courseId = courseId,
            dayOfWeek = dayOfWeek,
            startMinuteOfDay = startMinute,
            endMinuteOfDay = endMinute,
            location = location?.ifBlank { null },
            note = note?.ifBlank { null }
        )
    )

    suspend fun deleteSlot(id: Long) = slots.deleteById(id)

    fun observePendingTasks(): Flow<List<Task>> = tasks.observeAllPending()
    fun observePendingForCourse(courseId: Long): Flow<List<Task>> =
        tasks.observePendingForCourse(courseId)

    suspend fun getTask(id: Long): Task? = tasks.getById(id)

    suspend fun getAllPendingWithDue(): List<Task> = tasks.getAllPendingWithDue()

    suspend fun markTaskDone(id: Long) {
        val now = Instant.now().toEpochMilli()
        tasks.updateStatus(id, TaskStatus.DONE, now, now)
    }

    fun observeNewCandidates(): Flow<List<CandidateItem>> =
        candidates.observeByStatus(CandidateStatus.NEW)

    suspend fun getCandidate(id: Long): CandidateItem? = candidates.getById(id)

    suspend fun ingestRawText(raw: String, source: SourceKind): Long {
        val parsed = SimpleCandidateParser.parse(raw, source)
        val now = Instant.now().toEpochMilli()
        val item = CandidateItem(
            rawText = raw,
            parsedTitle = parsed.title,
            parsedCourseHint = parsed.courseHint,
            parsedDueAtEpoch = parsed.dueAtEpoch,
            suggestedTaskType = parsed.taskType,
            suggestedUrgency = parsed.urgency,
            sourceKind = source,
            status = CandidateStatus.NEW,
            createdAtEpoch = now,
            confidenceNote = parsed.confidenceNote
        )
        return candidates.insert(item)
    }

    suspend fun updateCandidateDraft(item: CandidateItem) {
        candidates.update(item)
    }

    suspend fun ignoreCandidate(id: Long) {
        val c = candidates.getById(id) ?: return
        candidates.update(c.copy(status = CandidateStatus.IGNORED))
    }

    suspend fun confirmCandidate(
        candidate: CandidateItem,
        title: String,
        courseId: Long?,
        dueAtEpoch: Long?,
        taskType: TaskType,
        urgency: UrgencyLevel
    ): Long {
        val now = Instant.now().toEpochMilli()
        val task = Task(
            title = title.trim(),
            courseId = courseId,
            taskType = taskType,
            urgency = urgency,
            status = TaskStatus.PENDING,
            dueAtEpoch = dueAtEpoch,
            sourceKind = candidate.sourceKind,
            rawSnippet = candidate.rawText.take(500),
            createdAtEpoch = now,
            updatedAtEpoch = now
        )
        val taskId = tasks.insert(task)
        candidates.update(
            candidate.copy(
                status = CandidateStatus.CONFIRMED,
                linkedTaskId = taskId,
                parsedTitle = title,
                parsedDueAtEpoch = dueAtEpoch
            )
        )
        return taskId
    }
}
