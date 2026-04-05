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

    // ==================== Course Operations ====================

    fun observeCourses(): Flow<List<Course>> = courses.observeAll()
    suspend fun getCourse(id: Long): Course? = courses.getById(id)
    suspend fun getAllCourses(): List<Course> = courses.getAll()
    suspend fun searchCourses(query: String): List<Course> = courses.search(query)
    suspend fun getCourseCount(): Int = courses.count()

    suspend fun insertCourse(name: String, code: String?): Long {
        val id = courses.insert(
            Course(name = name.trim(), code = code?.trim()?.ifEmpty { null })
        )
        return id
    }

    suspend fun updateCourse(course: Course) {
        courses.update(course)
    }

    suspend fun deleteCourse(id: Long) {
        courses.deleteById(id)
    }

    // ==================== Timetable Operations ====================

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

    // ==================== Task Operations ====================

    fun observePendingTasks(): Flow<List<Task>> = tasks.observeAllPending()
    fun observePendingForCourse(courseId: Long): Flow<List<Task>> =
        tasks.observePendingForCourse(courseId)

    suspend fun getTask(id: Long): Task? = tasks.getById(id)
    suspend fun getTasksByIds(ids: List<Long>): List<Task> = tasks.getByIds(ids)

    suspend fun getAllPendingWithDue(): List<Task> = tasks.getAllPendingWithDue()
    suspend fun getTasksDueBetween(start: Long, end: Long): List<Task> =
        tasks.pendingDueBetween(start, end)

    suspend fun getTaskCountByStatus(status: TaskStatus): Int = tasks.countByStatus(status)
    suspend fun getPendingTaskCountByCourse(courseId: Long): Int = tasks.countPendingByCourse(courseId)

    suspend fun insertTask(task: Task): Long = tasks.insert(task)

    suspend fun insertTasks(tasksToInsert: List<Task>): List<Long> = tasks.insertAll(tasksToInsert)

    suspend fun markTaskDone(id: Long) {
        val now = Instant.now().toEpochMilli()
        tasks.updateStatus(id, TaskStatus.DONE, now, now)
    }

    suspend fun markTasksDone(ids: List<Long>) {
        if (ids.isEmpty()) return
        val now = Instant.now().toEpochMilli()
        tasks.updateStatusBatch(ids, TaskStatus.DONE, now, now)
    }

    suspend fun markTaskPending(id: Long) {
        val now = Instant.now().toEpochMilli()
        tasks.updateStatus(id, TaskStatus.PENDING, now, null)
    }

    suspend fun deleteTask(id: Long) {
        tasks.deleteByIds(listOf(id))
    }

    suspend fun deleteTasks(ids: List<Long>) {
        if (ids.isEmpty()) return
        tasks.deleteByIds(ids)
    }

    suspend fun archiveTask(id: Long) {
        val now = Instant.now().toEpochMilli()
        tasks.updateStatus(id, TaskStatus.ARCHIVED, now, now)
    }

    // ==================== Candidate Operations ====================

    fun observeNewCandidates(): Flow<List<CandidateItem>> =
        candidates.observeByStatus(CandidateStatus.NEW)

    suspend fun getCandidate(id: Long): CandidateItem? = candidates.getById(id)

    suspend fun getCandidatesByStatusPaginated(
        status: CandidateStatus,
        limit: Int,
        offset: Int
    ): List<CandidateItem> = candidates.getByStatusPaginated(status, limit, offset)

    suspend fun getCandidateCountByStatus(status: CandidateStatus): Int =
        candidates.countByStatus(status)

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

    suspend fun updateCandidates(items: List<CandidateItem>) {
        candidates.updateAll(items)
    }

    suspend fun ignoreCandidate(id: Long) {
        val c = candidates.getById(id) ?: return
        candidates.update(c.copy(status = CandidateStatus.IGNORED))
    }

    suspend fun ignoreCandidates(ids: List<Long>) {
        val items = ids.mapNotNull { candidates.getById(it) }
        candidates.updateAll(items.map { it.copy(status = CandidateStatus.IGNORED) })
    }

    suspend fun deleteCandidate(id: Long) {
        candidates.deleteById(id)
    }

    suspend fun deleteCandidates(ids: List<Long>) {
        candidates.deleteByIds(ids)
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
