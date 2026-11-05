// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.music.spotui.data.preferences.ImmersiveSettingsPref
import com.music.spotui.di.SongPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Canvas Network Policy Manager:
 * Monitors active network connectivity via [ConnectivityManager] and enforces Canvas Saver rules.
 * Automatically activates Canvas Saver when connected to mobile data (CELLULAR) if enabled.
 * Prevents unnecessary video downloads over cellular networks and allows playback of cached videos.
 */
object CanvasNetworkPolicy {
    private var context: Context? = null

    /** Indicates whether the current active network is mobile data (Cellular). */
    var isCellular by mutableStateOf(false)
        private set

    /** Indicates whether Canvas Saver is currently active (Mobile data + Auto Canvas Saver ON, or forced Artwork Only). */
    var isCanvasSaverActive by mutableStateOf(false)
        private set

    private val _policyState = MutableStateFlow(false)
    val policyState: StateFlow<Boolean> = _policyState.asStateFlow()

    fun init(c: Context) {
        if (context != null) return
        val app = c.applicationContext
        context = app
        ImmersiveSettingsPref.init(app)

        val cm = app.getSystemService(ConnectivityManager::class.java)
        if (cm != null) {
            updateNetworkState(cm)
            runCatching {
                cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                    override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                        val cell = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        if (isCellular != cell) {
                            isCellular = cell
                            updateSaverStatus()
                        }
                    }

                    override fun onLost(network: Network) {
                        updateNetworkState(cm)
                    }
                })
            }
        } else {
            updateSaverStatus()
        }
    }

    private fun updateNetworkState(cm: ConnectivityManager) {
        val activeNet = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNet)
        isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        updateSaverStatus()
    }

    fun updateSaverStatus() {
        val autoSaver = ImmersiveSettingsPref.autoCanvasSaverMobileDataEnabled.value
        val qualityLimit = ImmersiveSettingsPref.canvasQualityLimit.value
        val saverActive = (autoSaver && isCellular) || qualityLimit == "artwork_only"

        if (isCanvasSaverActive != saverActive) {
            isCanvasSaverActive = saverActive
            _policyState.value = saverActive
        }
    }

    /**
     * Returns true if Canvas playback is allowed for [url].
     * If Canvas Saver is active and [url] is NOT cached, network fetching is blocked.
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun isCanvasAllowed(context: Context, url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val qualityLimit = ImmersiveSettingsPref.canvasQualityLimit.value
        if (qualityLimit == "artwork_only") return false

        updateSaverStatus()
        if (isCanvasSaverActive) {
            // When Canvas Saver is active on mobile data: only allow if already in cache!
            return isCached(context, url)
        }
        return true
    }

    /**
     * Checks if the given Canvas video [url] is cached in storage.
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun isCached(context: Context, url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return runCatching {
            val cache = SongPlayer.mediaCache(context.applicationContext)
            cache.isCached(url, 0, 1) || cache.keys.contains(url)
        }.getOrDefault(false)
    }

    /**
     * Returns true if pre-downloading/warming Canvas is allowed over the current network.
     */
    fun isPreloadAllowed(context: Context, url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        updateSaverStatus()
        if (isCanvasSaverActive) {
            return isCached(context, url)
        }
        return true
    }
}
