package com.campus.todo.domain.llm

import com.campus.todo.data.db.entity.CandidateItem
import com.campus.todo.data.db.entity.Task
import com.campus.todo.domain.ParsedCandidateDraft

data class DedupDecision(
    val duplicateScore: Float,
    val mergeSuggestion: String,
    val existingTaskId: Long? = null,
    val existingCandidateId: Long? = null
)

interface LlmParser {
    suspend fun parse(rawText: String): ParsedCandidateDraft?
}

interface LlmDedupService {
    suspend fun evaluate(
        draft: ParsedCandidateDraft,
        existingTasks: List<Task>,
        existingCandidates: List<CandidateItem>
    ): DedupDecision
}
