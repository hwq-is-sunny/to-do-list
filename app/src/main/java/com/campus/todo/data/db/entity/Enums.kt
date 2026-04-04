package com.campus.todo.data.db.entity

enum class TaskType {
    HOMEWORK,
    EXAM,
    SIGN_IN,
    CLASS,
    ANNOUNCEMENT,
    PERSONAL,
    OTHER
}

enum class UrgencyLevel {
    NORMAL,
    IMPORTANT,
    URGENT
}

enum class TaskStatus {
    PENDING,
    DONE,
    ARCHIVED
}

enum class SourceKind {
    WECHAT,
    QQ,
    ZHIHUISHU,
    CHAOXING,
    TJU_PORTAL,
    TIMETABLE,
    MANUAL,
    MOCK,
    SHARE_IMPORT_STUB
}

enum class CandidateStatus {
    NEW,
    CONFIRMED,
    IGNORED
}
