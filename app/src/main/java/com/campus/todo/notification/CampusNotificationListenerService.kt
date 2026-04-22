package com.campus.todo.notification

import android.app.Notification
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.campus.todo.CampusTodoApp
import com.campus.todo.data.db.entity.SourceKind
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * 读取已授权应用的通知并写入候选箱（需在系统设置中授予「通知使用权」）。
 * 支持包名：微信、QQ、知到/智慧树、学习通等（见 [resolveSource]）。
 */
class CampusNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val app = applicationContext as? CampusTodoApp ?: return

        app.applicationScope.launch {
            runCatching {
                if (!app.settingsStore.currentSettings().notificationImportEnabled) return@launch
                if (!app.sessionStore.isLoggedIn()) return@launch

                val pkg = notification.packageName ?: return@launch
                val source = resolveSource(pkg) ?: return@launch

                val extras = notification.notification.extras ?: return@launch
                val raw = extractNotificationText(extras)
                if (raw.isBlank()) return@launch

                val stableKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notification.key
                } else {
                    "${notification.packageName}_${notification.id}_${notification.postTime}"
                }
                val dedupKey = "$stableKey|${raw.hashCode()}"
                synchronized(RecentDedup) {
                    if (!RecentDedup.add(dedupKey)) return@launch
                    if (RecentDedup.size > MAX_DEDUP_ENTRIES) RecentDedup.clear()
                }

                app.repository.ingestRawText(raw, source)
            }
        }
    }

    companion object {
        private const val MAX_DEDUP_ENTRIES = 500
        private val RecentDedup: MutableSet<String> =
            Collections.synchronizedSet(mutableSetOf())

        fun isListenerEnabled(context: android.content.Context): Boolean {
            val cn = ComponentName(context, CampusNotificationListenerService::class.java)
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val flattened = cn.flattenToString()
            return flat.split(":")
                .map { it.trim() }
                .any { it == flattened }
        }

        internal fun resolveSource(packageName: String): SourceKind? = when (packageName) {
            "com.tencent.mm" -> SourceKind.WECHAT
            "com.tencent.mobileqq",
            "com.tencent.tim" -> SourceKind.QQ
            "com.chaoxing.mobile" -> SourceKind.CHAOXING
            "com.wisdomtree.wisdomtree",
            "com.able.wisdomtree",
            "com.zhihuishu.zhs",
            "com.zhihuishu.android" -> SourceKind.ZHIHUISHU
            else -> null
        }

        fun extractNotificationText(extras: Bundle): String {
            fun charSeq(key: String): String =
                extras.getCharSequence(key)?.toString()?.trim().orEmpty()

            val title = charSeq(Notification.EXTRA_TITLE)
                .ifBlank { extras.getString("android.title")?.trim().orEmpty() }
            val text = charSeq(Notification.EXTRA_TEXT)
                .ifBlank { charSeq("android.text") }
            val big = charSeq(Notification.EXTRA_BIG_TEXT)
            @Suppress("DEPRECATION")
            val sub = charSeq(Notification.EXTRA_SUB_TEXT)
            val summary = charSeq("android.summaryText")
            return listOf(title, text, big, sub, summary)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString("\n")
        }
    }
}
