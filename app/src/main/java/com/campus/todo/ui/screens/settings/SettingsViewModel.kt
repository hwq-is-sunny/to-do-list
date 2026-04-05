package com.campus.todo.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.session.SessionStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val sessionStore: SessionStore
) : ViewModel() {

    val username = sessionStore.usernameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            sessionStore.logout()
            onDone()
        }
    }
}
