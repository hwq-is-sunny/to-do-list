package com.campus.todo.domain.llm

import com.campus.todo.domain.ParsedCandidateDraft
import com.campus.todo.network.DeepSeekAssist

class RemoteLlmParserAdapter(
    private val deepSeekAssist: DeepSeekAssist
) : LlmParser {
    override suspend fun parse(rawText: String): ParsedCandidateDraft? {
        val suggestion = deepSeekAssist.parseNotification(rawText).getOrNull() ?: return null
        val title = suggestion.title?.ifBlank { null } ?: return null
        return ParsedCandidateDraft(
            title = title,
            description = suggestion.description,
            courseHint = suggestion.courseHint,
            dueAtEpoch = suggestion.dueAtEpoch,
            startAtEpoch = suggestion.startAtEpoch,
            endAtEpoch = suggestion.endAtEpoch,
            location = suggestion.location,
            taskType = suggestion.taskType ?: com.campus.todo.data.db.entity.TaskType.OTHER,
            urgency = suggestion.urgency ?: com.campus.todo.data.db.entity.UrgencyLevel.NORMAL,
            confidenceNote = "llm",
            confidenceScore = suggestion.confidenceScore
        )
    }
}
