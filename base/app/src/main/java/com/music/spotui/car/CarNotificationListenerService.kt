// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.car

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.myspoti.host.android.Logbook

class CarNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return
        if (!CarModeManager.isCarModeActive.value) return

        val packageName = sbn.packageName ?: return
        if (packageName == applicationContext.packageName) return // Ignore Myspoti's own playback notifications

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()

        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        // Ignore ongoing notifications without explicit user message
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 && text.isNullOrBlank()) return

        val appName = try {
            val pm = applicationContext.packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            packageName
        }

        val item = CarNotificationItem(
            id = sbn.id.toString() + "_" + sbn.postTime,
            packageName = packageName,
            appName = appName,
            title = title ?: appName,
            text = text ?: subText ?: "",
            postTime = sbn.postTime,
        )

        Logbook.log("CarNotificationListenerService: Received notification from $appName")
        CarNotificationManager.postNotification(applicationContext, item)
    }
}
