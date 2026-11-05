// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.car

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import app.myspoti.host.android.Logbook
import com.music.spotui.data.preferences.CarModePref
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object CarNotificationManager {
    private val _currentNotification = MutableStateFlow<CarNotificationItem?>(null)
    val currentNotification: StateFlow<CarNotificationItem?> = _currentNotification.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var dismissJob: Job? = null

    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    fun postNotification(context: Context, item: CarNotificationItem) {
        if (!CarModePref.notificationsEnabled) return

        _currentNotification.value = item
        playSubtleAlertSound(context)

        dismissJob?.cancel()
        dismissJob = scope.launch {
            delay(7000L) // Auto-dismiss after 7 seconds
            if (_currentNotification.value?.id == item.id) {
                _currentNotification.value = null
            }
        }
    }

    fun dismiss() {
        dismissJob?.cancel()
        _currentNotification.value = null
    }

    private fun playSubtleAlertSound(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val ringerMode = audioManager.ringerMode
            if (ringerMode != AudioManager.RINGER_MODE_NORMAL) return // Respect silent / vibrate mode

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null) {
                val filter = notificationManager.currentInterruptionFilter
                if (filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                    return // Respect Do Not Disturb (DND)
                }
            }

            // Play a subtle, non-disruptive chime tone without interrupting music playback
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 40)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            scope.launch {
                delay(300L)
                toneGen.release()
            }
        } catch (e: Exception) {
            Logbook.log("CarNotificationManager alert sound error: ${e.message}")
        }
    }
}
