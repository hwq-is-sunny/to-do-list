package com.campus.todo.notification

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.SuppressLint
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Sends notifications only when the current app notification permission allows it.
 * The permission can change between the check and notify(), so SecurityException
 * is also handled as a final race-condition safeguard.
 */
internal object SafeNotificationManager {

    @SuppressLint("MissingPermission")
    fun notifyIfAllowed(context: Context, notificationId: Int, notification: Notification) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        try {
            manager.notify(notificationId, notification)
        } catch (exception: SecurityException) {
            Log.w(TAG, "Notification permission changed before delivery", exception)
        }
    }

    private const val TAG = "SafeNotificationManager"
}
