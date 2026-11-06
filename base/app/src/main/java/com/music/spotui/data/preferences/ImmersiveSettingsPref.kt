package com.music.spotui.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

object ImmersiveSettingsPref {
    private const val PREF_NAME = "immersive_settings_v3"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Appearance & Background
    val dynamicBackgrounds = MutableStateFlow(true)
    val backgroundSource = MutableStateFlow("auto")
    val backgroundIntensity = MutableStateFlow(0.85f)
    val colorSaturation = MutableStateFlow(0.9f)
    val animationSpeed = MutableStateFlow(1.0f)
    val blurRadiusDp = MutableStateFlow(64)
    val reducedMotion = MutableStateFlow(false)

    // Canvas
    val autoCanvasSaverMobileDataEnabled = MutableStateFlow(false)
    val canvasQualityLimit = MutableStateFlow("auto")

    // Animated Artwork & Visualizer (Combined toggle)
    val animatedArtworkAndVisualizerEnabled = MutableStateFlow(true)
    val animatedArtworkEnabled = MutableStateFlow(true)
    val visualizerEnabled = MutableStateFlow(true)
    val visualizerIntensity = MutableStateFlow("high")
    val gestureControlsEnabled = MutableStateFlow(true)
    val hapticFeedbackEnabled = MutableStateFlow(true)

    // Lyrics
    val autoResolverEnabled = MutableStateFlow(true)
    val progressiveUpgrades = MutableStateFlow(true)
    val preferWordSynced = MutableStateFlow(true)
    val lyricsFontSizeSp = MutableStateFlow(20)
    val activeLineHighlight = MutableStateFlow(true)
    val manualScrollTimeoutMs = MutableStateFlow(5000L)

    fun init(context: Context) {
        val p = prefs(context)
        dynamicBackgrounds.value = p.getBoolean("dynamic_backgrounds", true)
        backgroundSource.value = p.getString("background_source", "auto") ?: "auto"
        backgroundIntensity.value = p.getFloat("background_intensity", 0.85f)
        colorSaturation.value = p.getFloat("color_saturation", 0.9f)
        animationSpeed.value = p.getFloat("animation_speed", 1.0f)
        blurRadiusDp.value = p.getInt("blur_radius_dp", 64)

        canvasQualityLimit.value = p.getString("canvas_quality_limit", "auto") ?: "auto"
        animatedArtworkAndVisualizerEnabled.value = p.getBoolean("animated_artwork_and_visualizer_enabled", true)
        animatedArtworkEnabled.value = animatedArtworkAndVisualizerEnabled.value
        visualizerEnabled.value = animatedArtworkAndVisualizerEnabled.value
        gestureControlsEnabled.value = p.getBoolean("gesture_controls_enabled", true)
        hapticFeedbackEnabled.value = p.getBoolean("haptic_feedback_enabled", true)

        autoResolverEnabled.value = p.getBoolean("auto_resolver_enabled", true)
        progressiveUpgrades.value = p.getBoolean("progressive_upgrades", true)
        preferWordSynced.value = p.getBoolean("prefer_word_synced", true)
        lyricsFontSizeSp.value = p.getInt("lyrics_font_size_sp", 20)
        activeLineHighlight.value = p.getBoolean("active_line_highlight", true)
        manualScrollTimeoutMs.value = p.getLong("manual_scroll_timeout_ms", 5000L)
    }

    fun setDynamicBackgrounds(context: Context, enabled: Boolean) {
        dynamicBackgrounds.value = enabled
        prefs(context).edit().putBoolean("dynamic_backgrounds", enabled).apply()
    }

    fun setAnimatedArtworkAndVisualizer(context: Context, enabled: Boolean) {
        animatedArtworkAndVisualizerEnabled.value = enabled
        animatedArtworkEnabled.value = enabled
        visualizerEnabled.value = enabled
        prefs(context).edit().putBoolean("animated_artwork_and_visualizer_enabled", enabled).apply()
    }

    fun setAnimatedArtwork(context: Context, enabled: Boolean) {
        setAnimatedArtworkAndVisualizer(context, enabled)
    }

    fun setVisualizer(context: Context, enabled: Boolean) {
        setAnimatedArtworkAndVisualizer(context, enabled)
    }

    fun setVisualizerIntensity(context: Context, intensity: String) {
        visualizerIntensity.value = intensity
    }

    fun setAutoCanvasSaverMobileData(context: Context, enabled: Boolean) {
        autoCanvasSaverMobileDataEnabled.value = enabled
    }

    fun setCanvasQualityLimit(context: Context, limit: String) {
        canvasQualityLimit.value = limit
        prefs(context).edit().putString("canvas_quality_limit", limit).apply()
    }

    fun setGestureControls(context: Context, enabled: Boolean) {
        gestureControlsEnabled.value = enabled
        prefs(context).edit().putBoolean("gesture_controls_enabled", enabled).apply()
    }
}
