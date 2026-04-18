package com.campus.todo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.campus.todo.data.settings.AppLocaleManager
import com.campus.todo.data.settings.SettingsStore
import com.campus.todo.ui.navigation.CampusTodoNavHost
import com.campus.todo.ui.theme.CampusTodoTheme
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val languageTag = runBlocking { SettingsStore(newBase).currentLanguageTag() }
        super.attachBaseContext(AppLocaleManager.wrapContext(newBase, languageTag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CampusTodoApp
        val openCandidate = intent.getLongExtra(EXTRA_OPEN_CANDIDATE, -1L).takeIf { it >= 0 }
        setContent {
            CampusTodoTheme {
                CampusTodoNavHost(app = app, openCandidateId = openCandidate)
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_CANDIDATE = "com.campus.todo.OPEN_CANDIDATE_ID"
    }
}
