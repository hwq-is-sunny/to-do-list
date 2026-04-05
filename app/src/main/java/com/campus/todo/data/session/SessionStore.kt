package com.campus.todo.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "campus_todo_session")

class SessionStore(private val context: Context) {

    private val keyLoggedIn = booleanPreferencesKey("logged_in")
    private val keyUsername = stringPreferencesKey("username")

    val loggedInFlow = context.sessionDataStore.data.map { prefs ->
        prefs[keyLoggedIn] == true && !prefs[keyUsername].isNullOrBlank()
    }

    val usernameFlow = context.sessionDataStore.data.map { it[keyUsername].orEmpty() }

    suspend fun isLoggedIn(): Boolean = loggedInFlow.first()

    suspend fun login(username: String) {
        val name = username.trim()
        require(name.isNotEmpty())
        context.sessionDataStore.edit { prefs ->
            prefs[keyLoggedIn] = true
            prefs[keyUsername] = name
        }
    }

    suspend fun logout() {
        context.sessionDataStore.edit { prefs ->
            prefs[keyLoggedIn] = false
            prefs.remove(keyUsername)
        }
    }

    suspend fun username(): String? =
        context.sessionDataStore.data.map { it[keyUsername] }.first()
}
