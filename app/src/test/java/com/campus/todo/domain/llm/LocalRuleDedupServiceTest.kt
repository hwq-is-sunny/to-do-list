package com.campus.todo.domain.llm

import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.UrgencyLevel
import com.campus.todo.domain.ParsedCandidateDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalRuleDedupServiceTest {

    private val service = LocalRuleDedupService()

    @Test
    fun exactTaskMatchIsFlaggedAsDuplicate() = runBlocking {
        val dueAt = 1_800_000_000_000L
        val existing = Task(
            id = 7,
            title = "提交数据结构实验报告",
            courseName = "数据结构",
            dueAtEpoch = dueAt,
            createdAtEpoch = 1L,
            updatedAtEpoch = 1L
        )
        val draft = ParsedCandidateDraft(
            title = "提交数据结构实验报告",
            courseHint = "数据结构",
            dueAtEpoch = dueAt,
            taskType = TaskType.HOMEWORK,
            urgency = UrgencyLevel.NORMAL,
            confidenceNote = "test"
        )

        val result = service.evaluate(draft, listOf(existing), emptyList())

        assertEquals("high-duplicate", result.mergeSuggestion)
        assertEquals(7L, result.existingTaskId)
        assertTrue(result.duplicateScore >= 0.86f)
    }

    @Test
    fun noExistingItemsAreClassifiedAsNew() = runBlocking {
        val draft = ParsedCandidateDraft(
            title = "准备课程展示",
            courseHint = null,
            dueAtEpoch = null,
            taskType = TaskType.PERSONAL,
            urgency = UrgencyLevel.NORMAL,
            confidenceNote = "test"
        )

        val result = service.evaluate(draft, emptyList(), emptyList())

        assertEquals("new-item", result.mergeSuggestion)
        assertEquals(0f, result.duplicateScore, 0.001f)
    }
}
