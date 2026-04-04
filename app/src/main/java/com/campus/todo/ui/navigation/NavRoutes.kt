package com.campus.todo.ui.navigation

object NavRoutes {
    const val TODAY = "today"
    const val CALENDAR = "calendar"
    const val COURSES = "courses"
    const val INBOX = "inbox"
    const val SETTINGS = "settings"
    const val ADD_CANDIDATE = "addCandidate"
    const val CANDIDATE = "candidate/{candidateId}"
    const val COURSE = "course/{courseId}"

    fun candidate(id: Long) = "candidate/$id"
    fun course(id: Long) = "course/$id"
}
