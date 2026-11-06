package com.music.spotui.data.api

import android.util.Log
import java.io.File
import java.util.Properties

/**
 * Environment and configuration reader for API keys.
 * Reads publishable/client keys if configured via environment or local properties.
 * Secret keys (like sl_sk_) belong strictly on server backends and must NEVER be used on client devices.
 */
object ApiSecrets {
    private const val TAG = "ApiSecrets"
    private val properties = Properties()

    init {
        runCatching {
            val envFile = File(".env")
            if (envFile.exists() && envFile.isFile) {
                envFile.inputStream().use { properties.load(it) }
            }
        }.onFailure { Log.d(TAG, "Could not load .env file: ${it.message}") }
    }

    private fun getSecret(key: String): String {
        val envVal = System.getenv(key)?.trim()
        if (!envVal.isNullOrEmpty()) return envVal
        val propVal = properties.getProperty(key)?.trim()
        if (!propVal.isNullOrEmpty()) return propVal
        return ""
    }

    /** Publishable client key for Spicy Lyrics (sl_pk_...), if configured. */
    val spicyLyricsPublishableKey: String get() {
        val key = getSecret("SPICY_LYRICS_PUBLISHABLE_KEY")
        if (key.startsWith("sl_sk_")) {
            Log.e(TAG, "Security Error: sl_sk_ secret keys belong on server backends only and must not be used in client applications.")
            return ""
        }
        return key
    }
}
