package com.campus.todo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.campus.todo.ui.navigation.CampusTodoNavHost
import com.campus.todo.ui.theme.CampusTodoTheme

class MainActivity : AppCompatActivity() {

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
