package com.campus.todo.data.repo

import com.campus.todo.data.db.AppDatabase
import com.campus.todo.data.db.entity.CandidateItem
import com.campus.todo.data.db.entity.CandidateStatus
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.ScheduleEvent
import com.campus.todo.data.db.entity.SourceKind
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TaskStatus
import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.TimetableSlot
import com.campus.todo.data.db.entity.UrgencyLevel
import com.campus.todo.data.parser.NoticeParser
import com.campus.todo.data.parser.toParsedCandidateDraft
import com.campus.todo.domain.SimpleCandidateParser
import com.campus.todo.domain.llm.LocalRuleDedupService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class TodoRepository(private val db: AppDatabase) {
    private val courses = db.courseDao()
    private val slots = db.timetableDao()
    private val tasks = db.taskDao()
    private val candidates = db.candidateDao()
    private val scheduleEvents = db.scheduleEventDao()
    private val dedup = LocalRuleDedupService()

    fun observeCourses(): Flow<List<Course>> = courses.observeAll()
    suspend fun getCourse(id: Long): Course? = courses.getById(id)
    suspend fun getAllCourses(): List<Course> = courses.getAll()

    suspend fun insertCourse(name: String, code: String?): Long {
        return courses.insert(
            Course(name = name.trim(), code = code?.trim()?.ifEmpty { null })
        )
    }

    suspend fun updateCourse(id: Long, name: String, code: String?) {
        val c = courses.getById(id) ?: return
        courses.update(
            c.copy(
                name = name.trim(),
                code = code?.trim()?.ifEmpty { null }
            )
        )
    }

    suspend fun deleteCourse(id: Long) {
        courses.deleteById(id)
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

    fun observeAllTasks(): Flow<List<Task>> = tasks.observeAll()
    fun observePendingTasks(): Flow<List<Task>> = tasks.observeAllPending()
    fun observePendingForCourse(courseId: Long): Flow<List<Task>> =
        tasks.observePendingForCourse(courseId)

    fun observeTask(id: Long): Flow<Task?> = tasks.observeById(id)

    suspend fun getTask(id: Long): Task? = tasks.getById(id)
    suspend fun getAllPendingWithDue(): List<Task> = tasks.getAllPendingWithDue()

    suspend fun markTaskDone(id: Long) {
        val now = Instant.now().toEpochMilli()
        tasks.updateStatus(id, TaskStatus.DONE, now, now)
    }

    suspend fun updateTaskTitle(id: Long, newTitle: String) {
        val now = Instant.now().toEpochMilli()
        tasks.updateTitle(id, newTitle.trim(), now)
    }

    suspend fun createManualTask(
        title: String,
        description: String? = null,
        dueAtEpoch: Long? = null,
        location: String? = null,
        taskType: TaskType = TaskType.PERSONAL,
        urgency: UrgencyLevel = UrgencyLevel.NORMAL
    ): Long {
        val now = Instant.now().toEpochMilli()
        return tasks.insert(
            Task(
                title = title.trim(),
                description = description?.trim()?.ifBlank { null },
                courseId = null,
                courseName = null,
                taskType = taskType,
                category = taskType.name,
                urgency = urgency,
                status = TaskStatus.PENDING,
                dueAtEpoch = dueAtEpoch,
                startAtEpoch = null,
                endAtEpoch = null,
                reminderAtEpoch = dueAtEpoch,
                location = location?.trim()?.ifBlank { null },
                tags = null,
                isAllDay = false,
                mergedGroupId = null,
                originalSourceId = null,
                sourceKind = SourceKind.MANUAL,
                rawSnippet = null,
                createdAtEpoch = now,
                updatedAtEpoch = now,
                completedAtEpoch = null
            )
        )
    }

    suspend fun updateTaskEditable(task: Task) {
        val now = Instant.now().toEpochMilli()
        tasks.updateEditableFields(
            id = task.id,
            title = task.title.trim(),
            description = task.description?.ifBlank { null },
            dueAt = task.dueAtEpoch,
            location = task.location?.ifBlank { null },
            taskType = task.taskType,
            category = task.taskType.name,
            urgency = task.urgency,
            reminderAt = task.reminderAtEpoch ?: task.dueAtEpoch,
            updated = now
        )
    }

    suspend fun deleteTask(id: Long) {
        tasks.deleteById(id)
    }

    fun observeCandidates(): Flow<List<CandidateItem>> = candidates.observeAll()
    fun observePendingCandidates(): Flow<List<CandidateItem>> = candidates.observeByStatus(CandidateStatus.PENDING)

    suspend fun getCandidate(id: Long): CandidateItem? = candidates.getById(id)
    suspend fun getCandidates(ids: List<Long>): List<CandidateItem> = if (ids.isEmpty()) emptyList() else candidates.getByIds(ids)

    suspend fun ingestRawText(raw: String, source: SourceKind): Long {
        return ingestRawTextBatch(raw, source).firstOrNull() ?: -1L
    }

    /**
     * 本地规则结构化通知：写入一条候选并返回 id，供直接进入详情页确认。
     */
    suspend fun insertStructuredNoticeCandidate(raw: String, source: SourceKind): Long {
        val normalized = raw.trim()
        if (normalized.isBlank()) return -1L
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val result = NoticeParser.parse(normalized, today, zone)
        val draft = result.toParsedCandidateDraft()
        val existingTasks = getAllPendingWithDue()
        val existingCandidates = candidates.observeByStatus(CandidateStatus.PENDING).first()
        val dedupDecision = dedup.evaluate(draft, existingTasks, existingCandidates)
        val now = Instant.now().toEpochMilli()
        val mergedGroupId = if (dedupDecision.duplicateScore >= 0.64f) {
            dedupDecision.existingTaskId?.let { "task-$it" }
                ?: dedupDecision.existingCandidateId?.let { "candidate-$it" }
                ?: UUID.randomUUID().toString()
        } else {
            null
        }
        val item = CandidateItem(
            rawText = normalized,
            sourceKind = source,
            parsedTitle = result.title,
            parsedDescription = result.note,
            parsedCourseHint = result.courseNameHint,
            parsedLocation = result.location,
            parsedDueAtEpoch = result.dueAtEpoch,
            parsedStartAtEpoch = result.startAtEpoch,
            parsedEndAtEpoch = result.endAtEpoch,
            parsedCategory = result.noticeType,
            parsedNoticeType = result.noticeType,
            parsedDateEpochDay = result.dateEpochDay,
            parsedStartMinuteOfDay = result.startMinute,
            parsedEndMinuteOfDay = result.endMinute,
            parsedPriorityTag = result.priorityTag,
            suggestedTaskType = draft.taskType,
            suggestedUrgency = draft.urgency,
            confidenceScore = draft.confidenceScore,
            duplicateScore = dedupDecision.duplicateScore,
            mergeSuggestion = dedupDecision.mergeSuggestion,
            mergedGroupId = mergedGroupId,
            originalSourceId = "$source-notice-$now",
            status = if (dedupDecision.mergeSuggestion == "high-duplicate") CandidateStatus.MERGED else CandidateStatus.PENDING,
            createdAtEpoch = now,
            linkedTaskId = dedupDecision.existingTaskId,
            linkedCourseId = null,
            confidenceNote = result.confidenceNote
        )
        return candidates.insert(item)
    }

    suspend fun ingestRawTextBatch(raw: String, source: SourceKind): List<Long> {
        val normalized = raw.trim()
        if (normalized.isBlank()) return emptyList()
        val now = Instant.now().toEpochMilli()
        val existingTasks = getAllPendingWithDue()
        val existingCandidates = candidates.observeByStatus(CandidateStatus.PENDING).firstOrNull().orEmpty()
        val chunks = splitRawText(normalized)
        val inserted = mutableListOf<Long>()

        chunks.forEachIndexed { index, chunk ->
            val parsed = SimpleCandidateParser.parse(chunk, source)
            val dedupDecision = dedup.evaluate(parsed, existingTasks, existingCandidates)
            val mergedGroupId = if (dedupDecision.duplicateScore >= 0.64f) {
                dedupDecision.existingTaskId?.let { "task-$it" }
                    ?: dedupDecision.existingCandidateId?.let { "candidate-$it" }
                    ?: UUID.randomUUID().toString()
            } else {
                null
            }
            val candidate = CandidateItem(
                rawText = chunk,
                sourceKind = source,
                parsedTitle = parsed.title,
                parsedDescription = parsed.description,
                parsedCourseHint = parsed.courseHint,
                parsedLocation = parsed.location,
                parsedDueAtEpoch = parsed.dueAtEpoch,
                parsedStartAtEpoch = parsed.startAtEpoch,
                parsedEndAtEpoch = parsed.endAtEpoch,
                parsedCategory = parsed.category,
                parsedNoticeType = null,
                parsedDateEpochDay = null,
                parsedStartMinuteOfDay = null,
                parsedEndMinuteOfDay = null,
                parsedPriorityTag = null,
                suggestedTaskType = parsed.taskType,
                suggestedUrgency = parsed.urgency,
                confidenceScore = parsed.confidenceScore,
                duplicateScore = dedupDecision.duplicateScore,
                mergeSuggestion = dedupDecision.mergeSuggestion,
                mergedGroupId = mergedGroupId,
                originalSourceId = "$source-${now + index}",
                status = if (dedupDecision.mergeSuggestion == "high-duplicate") CandidateStatus.MERGED else CandidateStatus.PENDING,
                createdAtEpoch = now + index,
                linkedTaskId = dedupDecision.existingTaskId,
                linkedCourseId = null,
                confidenceNote = parsed.confidenceNote
            )
            val id = candidates.insert(candidate)
            inserted += id
        }
        return inserted
    }

    suspend fun updateCandidateDraft(item: CandidateItem) {
        candidates.update(item)
    }

    suspend fun dismissCandidate(id: Long) {
        val c = candidates.getById(id) ?: return
        candidates.update(c.copy(status = CandidateStatus.DISMISSED))
    }

    suspend fun dismissCandidates(ids: List<Long>) {
        ids.forEach { dismissCandidate(it) }
    }

    suspend fun confirmCandidates(ids: List<Long>): List<Long> {
        val items = getCandidates(ids)
        val taskIds = mutableListOf<Long>()
        for (item in items) {
            if (item.status == CandidateStatus.DISMISSED) continue
            val taskId = confirmCandidate(
                candidate = item,
                title = item.parsedTitle ?: item.rawText.take(60),
                courseId = resolveCourseId(item.parsedCourseHint),
                dueAtEpoch = item.parsedDueAtEpoch,
                taskType = item.suggestedTaskType ?: TaskType.OTHER,
                urgency = item.suggestedUrgency ?: UrgencyLevel.NORMAL
            )
            taskIds += taskId
        }
        return taskIds
    }

    suspend fun mergeCandidates(ids: List<Long>): Long? {
        val items = getCandidates(ids).filter { it.status == CandidateStatus.PENDING || it.status == CandidateStatus.MERGED }
        if (items.isEmpty()) return null
        val primary = items.first()
        val groupId = primary.mergedGroupId ?: UUID.randomUUID().toString()
        items.forEachIndexed { index, item ->
            val updated = if (index == 0) {
                item.copy(status = CandidateStatus.PENDING, mergedGroupId = groupId)
            } else {
                item.copy(status = CandidateStatus.MERGED, mergedGroupId = groupId, linkedTaskId = primary.linkedTaskId)
            }
            candidates.update(updated)
        }
        return primary.id
    }

    suspend fun confirmCandidate(
        candidate: CandidateItem,
        title: String,
        courseId: Long?,
        dueAtEpoch: Long?,
        taskType: TaskType,
        urgency: UrgencyLevel,
        description: String? = null,
        location: String? = null,
        startAtEpoch: Long? = null,
        endAtEpoch: Long? = null
    ): Long {
        val now = Instant.now().toEpochMilli()
        val courseName = courseId?.let { courses.getById(it)?.name } ?: candidate.parsedCourseHint
        val task = Task(
            title = title.trim(),
            description = description?.trim()?.ifBlank { null } ?: candidate.parsedDescription,
            courseId = courseId,
            courseName = courseName,
            taskType = taskType,
            category = taskType.name,
            urgency = urgency,
            status = TaskStatus.PENDING,
            dueAtEpoch = dueAtEpoch,
            startAtEpoch = startAtEpoch ?: candidate.parsedStartAtEpoch,
            endAtEpoch = endAtEpoch ?: candidate.parsedEndAtEpoch,
            reminderAtEpoch = dueAtEpoch,
            location = location?.trim()?.ifBlank { null } ?: candidate.parsedLocation,
            tags = courseName,
            mergedGroupId = candidate.mergedGroupId,
            originalSourceId = candidate.originalSourceId,
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
                parsedDueAtEpoch = dueAtEpoch,
                parsedDescription = task.description,
                parsedLocation = task.location,
                parsedStartAtEpoch = task.startAtEpoch,
                parsedEndAtEpoch = task.endAtEpoch
            )
        )
        return taskId
    }

    suspend fun confirmCandidateAsCourse(
        candidate: CandidateItem,
        courseName: String,
        location: String?,
        note: String?
    ): Long {
        val zone = ZoneId.systemDefault()
        val name = courseName.trim().ifBlank { candidate.parsedCourseHint ?: "新课程" }
        val courseId = insertCourse(name, null)
        val epochDay = candidate.parsedDateEpochDay
            ?: candidate.parsedDueAtEpoch?.let {
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate().toEpochDay()
            }
            ?: LocalDate.now().toEpochDay()
        val date = LocalDate.ofEpochDay(epochDay)
        val dow = date.dayOfWeek.value
        val periodNote = note ?: candidate.parsedDescription
        val (startMin, endMin) = when {
            candidate.parsedStartMinuteOfDay != null && candidate.parsedEndMinuteOfDay != null ->
                candidate.parsedStartMinuteOfDay to candidate.parsedEndMinuteOfDay
            else -> {
                val m = Regex("第\\s*(\\d{1,2})\\s*[-到至~]\\s*(\\d{1,2})\\s*节").find(candidate.rawText)
                if (m != null) {
                    val a = m.groupValues[1].toInt()
                    val b = m.groupValues[2].toInt()
                    NoticeParser.periodsToMinutes(a, b)
                } else {
                    9 * 60 + 50 to 11 * 60 + 30
                }
            }
        }
        insertSlot(
            courseId = courseId,
            dayOfWeek = dow,
            startMinute = startMin,
            endMinute = endMin,
            location = location?.ifBlank { null } ?: candidate.parsedLocation,
            note = periodNote?.trim()?.ifBlank { null }
        )
        candidates.update(
            candidate.copy(
                status = CandidateStatus.CONFIRMED,
                linkedCourseId = courseId,
                parsedTitle = name,
                parsedLocation = location ?: candidate.parsedLocation,
                parsedDescription = periodNote
            )
        )
        return courseId
    }

    suspend fun attachCandidateToTask(candidateId: Long, taskId: Long) {
        val candidate = candidates.getById(candidateId) ?: return
        val task = tasks.getById(taskId) ?: return
        val now = Instant.now().toEpochMilli()
        val mergedDescription = buildString {
            append(task.description.orEmpty())
            if (isNotBlank()) append('\n')
            append("附加来源：")
            append(candidate.rawText.take(180))
        }.trim()
        tasks.insert(task.copy(description = mergedDescription, updatedAtEpoch = now))
        candidates.update(candidate.copy(status = CandidateStatus.MERGED, linkedTaskId = taskId))
    }

    fun observeScheduleEventsBetween(startEpoch: Long, endEpoch: Long): Flow<List<ScheduleEvent>> =
        scheduleEvents.observeBetween(startEpoch, endEpoch)

    fun observeAllScheduleEvents(): Flow<List<ScheduleEvent>> = scheduleEvents.observeAll()

    suspend fun insertScheduleEvent(event: ScheduleEvent): Long = scheduleEvents.insert(event)

    suspend fun resolveCourseId(courseHint: String?): Long? {
        if (courseHint.isNullOrBlank()) return null
        return courses.getAll().firstOrNull { c ->
            c.name.contains(courseHint, ignoreCase = true) || courseHint.contains(c.name, ignoreCase = true)
        }?.id
    }

    private fun splitRawText(raw: String): List<String> {
        val blocks = raw.split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (blocks.size > 1) return blocks
        return raw.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 8 }
            .ifEmpty { listOf(raw) }
    }
}
