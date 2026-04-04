package com.campus.todo.data.db

import androidx.room.TypeConverter
import com.campus.todo.data.db.entity.CandidateStatus
import com.campus.todo.data.db.entity.SourceKind
import com.campus.todo.data.db.entity.TaskStatus
import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.UrgencyLevel

class Converters {
    @TypeConverter fun fromTaskType(v: TaskType): String = v.name
    @TypeConverter fun toTaskType(v: String): TaskType = TaskType.valueOf(v)

    @TypeConverter fun fromUrgency(v: UrgencyLevel): String = v.name
    @TypeConverter fun toUrgency(v: String): UrgencyLevel = UrgencyLevel.valueOf(v)

    @TypeConverter fun fromTaskStatus(v: TaskStatus): String = v.name
    @TypeConverter fun toTaskStatus(v: String): TaskStatus = TaskStatus.valueOf(v)

    @TypeConverter fun fromSourceKind(v: SourceKind): String = v.name
    @TypeConverter fun toSourceKind(v: String): SourceKind = SourceKind.valueOf(v)

    @TypeConverter fun fromCandidateStatus(v: CandidateStatus): String = v.name
    @TypeConverter fun toCandidateStatus(v: String): CandidateStatus = CandidateStatus.valueOf(v)
}
