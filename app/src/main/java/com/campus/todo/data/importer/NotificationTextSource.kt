package com.campus.todo.data.importer

import com.campus.todo.data.db.entity.SourceKind

data class ImportedTextPayload(
    val sourceKind: SourceKind,
    val sourceId: String,
    val rawText: String
)

interface PlatformTextImporter {
    val sourceKind: SourceKind
    suspend fun importRawTexts(): List<ImportedTextPayload>
}

interface TimetableImporter {
    suspend fun importTimetableRaw(rawText: String): Boolean
}
