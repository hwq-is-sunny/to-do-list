package com.campus.todo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.campus.todo.ui.navigation.CampusTodoNavHost
import com.campus.todo.ui.theme.CampusTodoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CampusTodoApp
        val openCandidate = intent.getLongExtra(EXTRA_OPEN_CANDIDATE, -1L).takeIf { it >= 0 }
        setContent {
            val context = LocalContext.current
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (!granted) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.notification_permission_denied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
                val settings = app.settingsStore.currentSettings()
                if (!settings.taskReminderEnabled || !settings.notificationsEnabled) return@LaunchedEffect
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    return@LaunchedEffect
                }
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            CampusTodoTheme {
                CampusTodoNavHost(app = app, openCandidateId = openCandidate)
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_CANDIDATE = "com.campus.todo.OPEN_CANDIDATE_ID"
    }
}
