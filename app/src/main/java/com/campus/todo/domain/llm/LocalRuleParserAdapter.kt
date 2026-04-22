package com.campus.todo.domain.llm

import com.campus.todo.data.db.entity.SourceKind
import com.campus.todo.domain.ParsedCandidateDraft
import com.campus.todo.domain.SimpleCandidateParser

class LocalRuleParserAdapter : LlmParser {
    override suspend fun parse(rawText: String): ParsedCandidateDraft? {
        val text = rawText.trim()
        if (text.isBlank()) return null
        return SimpleCandidateParser.parse(text, SourceKind.MANUAL)
    }
}
