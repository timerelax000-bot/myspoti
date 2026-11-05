// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.tv

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Detects whether the app is running on an Android TV device.
 *
 * Checks both the Leanback feature (declared by Android TV) and the current UI mode.
 * This drives the TV-specific navigation shell and layouts while keeping the phone
 * experience completely unchanged.
 */
object TvDetect {
    private var cached: Boolean? = null

    fun isTv(context: Context): Boolean {
        cached?.let { return it }
        val pm = context.packageManager
        val hasLeanback = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            pm.hasSystemFeature("android.software.leanback")
        val uiMode = (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)
            ?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val result = hasLeanback || uiMode
        cached = result
        return result
    }

    /** Whether the current configuration is television (may change in multi-display setups). */
    fun isTvMode(config: Configuration): Boolean =
        (config.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

    /** Clears the cache (for testing or configuration changes). */
    fun reset() { cached = null }
}

/** Composable-friendly: true when running on an Android TV device. */
@Composable
fun rememberIsTv(): Boolean {
    val context = LocalContext.current
    return remember { TvDetect.isTv(context) }
}
