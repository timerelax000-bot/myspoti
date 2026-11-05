// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.data.preferences

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.music.spotui.di.Graph

/**
 * Preferences for Car Mode:
 * - Bluetooth automation (trusted car devices, auto-start, auto-exit)
 * - Display settings (keep screen on, orientation preference)
 * - Playback, voice control, and incoming notifications settings
 */
object CarModePref {
    private const val PREF = "carmode_prefs"

    private const val KEY_AUTO_BLUETOOTH = "car_auto_bluetooth"
    private const val KEY_TRUSTED_DEVICES = "car_trusted_devices"
    private const val KEY_KEEP_SCREEN_ON = "car_keep_screen_on"
    private const val KEY_START_PLAYBACK = "car_start_playback"
    private const val KEY_VOICE_COMMANDS = "car_voice_commands"
    private const val KEY_AUTO_EXIT = "car_auto_exit"
    private const val KEY_ORIENTATION = "car_orientation" // "auto", "portrait", "landscape"
    private const val KEY_NOTIFICATIONS = "car_notifications"

    private fun prefs(c: Context? = null) = (c ?: runCatching { Graph.app }.getOrNull())
        ?.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    var autoBluetoothEnabled by mutableStateOf(
        prefs()?.getBoolean(KEY_AUTO_BLUETOOTH, false) ?: false
    )
        private set

    var trustedDevices by mutableStateOf(
        prefs()?.getStringSet(KEY_TRUSTED_DEVICES, emptySet()) ?: emptySet()
    )
        private set

    var keepScreenOn by mutableStateOf(
        prefs()?.getBoolean(KEY_KEEP_SCREEN_ON, true) ?: true
    )
        private set

    var startPlaybackOnEnter by mutableStateOf(
        prefs()?.getBoolean(KEY_START_PLAYBACK, true) ?: true
    )
        private set

    var voiceCommandsEnabled by mutableStateOf(
        prefs()?.getBoolean(KEY_VOICE_COMMANDS, true) ?: true
    )
        private set

    var autoExitOnDisconnect by mutableStateOf(
        prefs()?.getBoolean(KEY_AUTO_EXIT, true) ?: true
    )
        private set

    var orientationMode by mutableStateOf(
        prefs()?.getString(KEY_ORIENTATION, "auto") ?: "auto"
    )
        private set

    var notificationsEnabled by mutableStateOf(
        prefs()?.getBoolean(KEY_NOTIFICATIONS, true) ?: true
    )
        private set

    fun setAutoBluetooth(c: Context, enabled: Boolean) {
        autoBluetoothEnabled = enabled
        prefs(c)?.edit()?.putBoolean(KEY_AUTO_BLUETOOTH, enabled)?.apply()
    }

    fun setTrustedDevices(c: Context, devices: Set<String>) {
        trustedDevices = devices
        prefs(c)?.edit()?.putStringSet(KEY_TRUSTED_DEVICES, devices)?.apply()
    }

    fun addTrustedDevice(c: Context, deviceAddress: String) {
        val updated = trustedDevices + deviceAddress
        setTrustedDevices(c, updated)
    }

    fun removeTrustedDevice(c: Context, deviceAddress: String) {
        val updated = trustedDevices - deviceAddress
        setTrustedDevices(c, updated)
    }

    fun setKeepScreenOn(c: Context, enabled: Boolean) {
        keepScreenOn = enabled
        prefs(c)?.edit()?.putBoolean(KEY_KEEP_SCREEN_ON, enabled)?.apply()
    }

    fun setStartPlayback(c: Context, enabled: Boolean) {
        startPlaybackOnEnter = enabled
        prefs(c)?.edit()?.putBoolean(KEY_START_PLAYBACK, enabled)?.apply()
    }

    fun setVoiceCommands(c: Context, enabled: Boolean) {
        voiceCommandsEnabled = enabled
        prefs(c)?.edit()?.putBoolean(KEY_VOICE_COMMANDS, enabled)?.apply()
    }

    fun setAutoExit(c: Context, enabled: Boolean) {
        autoExitOnDisconnect = enabled
        prefs(c)?.edit()?.putBoolean(KEY_AUTO_EXIT, enabled)?.apply()
    }

    fun setOrientation(c: Context, mode: String) {
        orientationMode = mode
        prefs(c)?.edit()?.putString(KEY_ORIENTATION, mode)?.apply()
    }

    fun setNotifications(c: Context, enabled: Boolean) {
        notificationsEnabled = enabled
        prefs(c)?.edit()?.putBoolean(KEY_NOTIFICATIONS, enabled)?.apply()
    }
}
