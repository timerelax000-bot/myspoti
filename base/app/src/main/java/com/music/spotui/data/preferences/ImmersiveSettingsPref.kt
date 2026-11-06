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

    // Canvas
    val autoCanvasSaverMobileDataEnabled = MutableStateFlow(false)
    val canvasQualityLimit = MutableStateFlow("auto")
    val canvasEnabled = MutableStateFlow(true)

    // Animated Artwork & Visualizer (Combined toggle)
    val animatedArtworkAndVisualizerEnabled = MutableStateFlow(true)
    val animatedArtworkEnabled = MutableStateFlow(true)
    val visualizerEnabled = MutableStateFlow(true)
    val visualizerIntensity = MutableStateFlow("high")
    val gestureControlsEnabled = MutableStateFlow(true)
    val hapticFeedbackEnabled = MutableStateFlow(true)
    val keepScreenOn = MutableStateFlow(true)

    // Lyrics
    val autoResolverEnabled = MutableStateFlow(true)
    val lyricsFontSizeSp = MutableStateFlow(20)

    fun init(context: Context) {
        val p = prefs(context)
        dynamicBackgrounds.value = p.getBoolean("dynamic_backgrounds", true)
        canvasQualityLimit.value = p.getString("canvas_quality_limit", "auto") ?: "auto"
        canvasEnabled.value = p.getBoolean("immersive_canvas_enabled", true)
        animatedArtworkAndVisualizerEnabled.value = p.getBoolean("animated_artwork_and_visualizer_enabled", true)
        animatedArtworkEnabled.value = animatedArtworkAndVisualizerEnabled.value
        visualizerEnabled.value = animatedArtworkAndVisualizerEnabled.value
        gestureControlsEnabled.value = p.getBoolean("gesture_controls_enabled", true)
        hapticFeedbackEnabled.value = p.getBoolean("haptic_feedback_enabled", true)
        keepScreenOn.value = p.getBoolean("keep_screen_on", true)

        autoResolverEnabled.value = p.getBoolean("auto_resolver_enabled", true)
        lyricsFontSizeSp.value = p.getInt("lyrics_font_size_sp", 20)
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

    fun setCanvasEnabled(context: Context, enabled: Boolean) {
        canvasEnabled.value = enabled
        prefs(context).edit().putBoolean("immersive_canvas_enabled", enabled).apply()
    }

    fun setGestureControls(context: Context, enabled: Boolean) {
        gestureControlsEnabled.value = enabled
        prefs(context).edit().putBoolean("gesture_controls_enabled", enabled).apply()
    }

    fun setKeepScreenOn(context: Context, enabled: Boolean) {
        keepScreenOn.value = enabled
        prefs(context).edit().putBoolean("keep_screen_on", enabled).apply()
    }
}
