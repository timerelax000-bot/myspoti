package com.music.spotui.data.preferences

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * TV & Desktop Mode Preferences:
 * Controls auto-hide timeouts, ambient screensaver mode, layout presets, and up-next preview.
 */
object TvModePref {
    private const val PREF = "tv_mode_prefs"

    // Layout presets
    const val LAYOUT_BALANCED = "balanced"
    const val LAYOUT_CINEMA = "cinema"
    const val LAYOUT_LYRICS = "lyrics"
    const val LAYOUT_MINIMAL = "minimal"

    // Keys
    private const val KEY_LAYOUT = "tv_layout_preset"
    private const val KEY_AUTOHIDE_SEC = "tv_autohide_seconds" // 0 = never, 2, 5, 8
    private const val KEY_AMBIENT_SEC = "tv_ambient_seconds"   // 0 = never, 15, 30, 60
    private const val KEY_UP_NEXT_ENABLED = "tv_up_next_enabled"
    private const val KEY_UP_NEXT_SEC = "tv_up_next_seconds"   // seconds before track end, default 12

    private fun prefs(c: Context?) = c?.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        ?: runCatching { com.music.spotui.di.Graph.app.getSharedPreferences(PREF, Context.MODE_PRIVATE) }.getOrNull()

    var layoutPreset by mutableStateOf(
        prefs(null)?.getString(KEY_LAYOUT, LAYOUT_BALANCED) ?: LAYOUT_BALANCED
    )
        private set

    var autoHideSeconds by mutableIntStateOf(
        prefs(null)?.getInt(KEY_AUTOHIDE_SEC, 5) ?: 5
    )
        private set

    var ambientSeconds by mutableIntStateOf(
        prefs(null)?.getInt(KEY_AMBIENT_SEC, 30) ?: 30
    )
        private set

    var upNextEnabled by mutableStateOf(
        prefs(null)?.getBoolean(KEY_UP_NEXT_ENABLED, true) ?: true
    )
        private set

    var upNextSeconds by mutableIntStateOf(
        prefs(null)?.getInt(KEY_UP_NEXT_SEC, 12) ?: 12
    )
        private set

    fun setLayout(c: Context, value: String) {
        layoutPreset = value
        prefs(c)?.edit()?.putString(KEY_LAYOUT, value)?.apply()
    }

    fun setAutoHide(c: Context, seconds: Int) {
        autoHideSeconds = seconds
        prefs(c)?.edit()?.putInt(KEY_AUTOHIDE_SEC, seconds)?.apply()
    }

    fun setAmbient(c: Context, seconds: Int) {
        ambientSeconds = seconds
        prefs(c)?.edit()?.putInt(KEY_AMBIENT_SEC, seconds)?.apply()
    }

    fun setUpNext(c: Context, enabled: Boolean) {
        upNextEnabled = enabled
        prefs(c)?.edit()?.putBoolean(KEY_UP_NEXT_ENABLED, enabled)?.apply()
    }

    fun setUpNextTime(c: Context, seconds: Int) {
        upNextSeconds = seconds
        prefs(c)?.edit()?.putInt(KEY_UP_NEXT_SEC, seconds)?.apply()
    }
}
