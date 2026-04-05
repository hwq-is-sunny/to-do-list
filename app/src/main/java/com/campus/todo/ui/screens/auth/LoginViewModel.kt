package com.campus.todo.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.session.SessionStore
import kotlinx.coroutines.launch

class LoginViewModel(
    private val sessionStore: SessionStore
) : ViewModel() {

    fun login(username: String, onDone: () -> Unit) {
        viewModelScope.launch {
            sessionStore.login(username)
            onDone()
        }
    }
}
