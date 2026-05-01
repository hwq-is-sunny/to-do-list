package com.campus.todo.ui

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.campus.todo.CampusTodoApp
import com.campus.todo.ui.screens.auth.LoginViewModel
import com.campus.todo.ui.screens.calendar.CalendarViewModel
import com.campus.todo.ui.screens.courses.CourseDetailViewModel
import com.campus.todo.ui.screens.courses.CourseListViewModel
import com.campus.todo.ui.screens.courses.TimetableImportViewModel
import com.campus.todo.ui.screens.inbox.AddCandidateViewModel
import com.campus.todo.ui.screens.inbox.CandidateDetailViewModel
import com.campus.todo.ui.screens.inbox.InboxViewModel
import com.campus.todo.ui.screens.settings.SettingsViewModel
import com.campus.todo.ui.screens.tasks.QuickTaskViewModel
import com.campus.todo.ui.screens.tasks.TaskDetailViewModel
import com.campus.todo.ui.screens.today.TodayViewModel

class AppViewModelFactory(
    owner: SavedStateRegistryOwner,
    defaultArgs: android.os.Bundle?,
    private val app: CampusTodoApp
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        val repo = app.repository
        val sched = app.reminderScheduler
        return when {
            modelClass.isAssignableFrom(TodayViewModel::class.java) ->
                TodayViewModel(repo, app.settingsStore, app.sessionStore, sched) as T
            modelClass.isAssignableFrom(CalendarViewModel::class.java) ->
                CalendarViewModel(repo, app.settingsStore) as T
            modelClass.isAssignableFrom(CourseListViewModel::class.java) ->
                CourseListViewModel(repo) as T
            modelClass.isAssignableFrom(TimetableImportViewModel::class.java) ->
                TimetableImportViewModel(repo) as T
            modelClass.isAssignableFrom(CourseDetailViewModel::class.java) ->
                CourseDetailViewModel(repo, sched, handle) as T
            modelClass.isAssignableFrom(InboxViewModel::class.java) ->
                InboxViewModel(repo) as T
            modelClass.isAssignableFrom(CandidateDetailViewModel::class.java) ->
                CandidateDetailViewModel(repo, sched, handle) as T
            modelClass.isAssignableFrom(AddCandidateViewModel::class.java) ->
                AddCandidateViewModel(repo) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(repo, sched, app.sessionStore, app.settingsStore) as T
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(app.accountStore, app.sessionStore) as T
            modelClass.isAssignableFrom(TaskDetailViewModel::class.java) ->
                TaskDetailViewModel(repo, sched, handle) as T
            modelClass.isAssignableFrom(QuickTaskViewModel::class.java) ->
                QuickTaskViewModel(repo, sched) as T
            else -> throw IllegalArgumentException("Unknown VM ${modelClass.name}")
        }
    }
}
