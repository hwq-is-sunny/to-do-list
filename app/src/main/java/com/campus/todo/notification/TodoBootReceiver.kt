package com.campus.todo.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.campus.todo.CampusTodoApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodoBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val app = context.applicationContext as CampusTodoApp
                val repo = app.repository
                val sched = app.reminderScheduler
                repo.getAllPendingWithDue().forEach { sched.rescheduleTask(it) }
                sched.syncTomatoFocus()
            } finally {
                pending.finish()
            }
        }
    }
}
