package com.music.spotui.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

object ImmersiveSettingsPref {
    private const val PREF_NAME = "immersive_settings_v2"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Appearance & Background
    val dynamicBackgrounds = MutableStateFlow(true)
    val backgroundSource = MutableStateFlow("auto") // "auto", "artwork", "video"
    val backgroundIntensity = MutableStateFlow(0.85f)
    val colorSaturation = MutableStateFlow(0.9f)
    val animationSpeed = MutableStateFlow(1.0f)
    val blurRadiusDp = MutableStateFlow(64)
    val reducedMotion = MutableStateFlow(false)

    // Canvas & Performance Saver
    val autoCanvasSaverMobileDataEnabled = MutableStateFlow(true)
    val canvasQualityLimit = MutableStateFlow("auto") // "auto", "480p", "artwork_only"

    // Animated Artwork, Gestures & Visualizer
    val animatedArtworkEnabled = MutableStateFlow(true)
    val gestureControlsEnabled = MutableStateFlow(true)
    val visualizerEnabled = MutableStateFlow(true)
    val visualizerIntensity = MutableStateFlow("low") // "low", "medium", "high"

    // Lyrics
    val autoResolverEnabled = MutableStateFlow(true)
    val progressiveUpgrades = MutableStateFlow(true)
    val preferWordSynced = MutableStateFlow(true)
    val lyricsFontSizeSp = MutableStateFlow(20)
    val activeLineHighlight = MutableStateFlow(true)
    val manualScrollTimeoutMs = MutableStateFlow(5000L)

    // Music Video & Audio Handoff
    val smartVideoMatching = MutableStateFlow(true)
    val preferOfficialVideo = MutableStateFlow(true)
    val strictConfidence = MutableStateFlow(true)
    val excludeLiveRemix = MutableStateFlow(true)
    val keepVideoOnLyrics = MutableStateFlow(true)
    val smartAudioHandoff = MutableStateFlow(true)
    val longPressDurationMs = MutableStateFlow(2000L)
    val hapticFeedbackEnabled = MutableStateFlow(true)
    val restoreVideoVisualOnExit = MutableStateFlow(true)

    fun init(context: Context) {
        val p = prefs(context)
        dynamicBackgrounds.value = p.getBoolean("dynamic_backgrounds", true)
        backgroundSource.value = p.getString("background_source", "auto") ?: "auto"
        backgroundIntensity.value = p.getFloat("background_intensity", 0.85f)
        colorSaturation.value = p.getFloat("color_saturation", 0.9f)
        animationSpeed.value = p.getFloat("animation_speed", 1.0f)
        blurRadiusDp.value = p.getInt("blur_radius_dp", 64)
        reducedMotion.value = p.getBoolean("reduced_motion", false)

        autoCanvasSaverMobileDataEnabled.value = p.getBoolean("auto_canvas_saver_mobile_data", true)
        canvasQualityLimit.value = p.getString("canvas_quality_limit", "auto") ?: "auto"

        animatedArtworkEnabled.value = p.getBoolean("animated_artwork_enabled", true)
        gestureControlsEnabled.value = p.getBoolean("gesture_controls_enabled", true)
        visualizerEnabled.value = p.getBoolean("visualizer_enabled", true)
        visualizerIntensity.value = p.getString("visualizer_intensity", "low") ?: "low"

        autoResolverEnabled.value = p.getBoolean("auto_resolver_enabled", true)
        progressiveUpgrades.value = p.getBoolean("progressive_upgrades", true)
        preferWordSynced.value = p.getBoolean("prefer_word_synced", true)
        lyricsFontSizeSp.value = p.getInt("lyrics_font_size_sp", 20)
        activeLineHighlight.value = p.getBoolean("active_line_highlight", true)
        manualScrollTimeoutMs.value = p.getLong("manual_scroll_timeout_ms", 5000L)

        smartVideoMatching.value = p.getBoolean("smart_video_matching", true)
        preferOfficialVideo.value = p.getBoolean("prefer_official_video", true)
        strictConfidence.value = p.getBoolean("strict_confidence", true)
        excludeLiveRemix.value = p.getBoolean("exclude_live_remix", true)
        keepVideoOnLyrics.value = p.getBoolean("keep_video_on_lyrics", true)
        smartAudioHandoff.value = p.getBoolean("smart_audio_handoff", true)
        longPressDurationMs.value = p.getLong("long_press_duration_ms", 2000L)
        hapticFeedbackEnabled.value = p.getBoolean("haptic_feedback_enabled", true)
        restoreVideoVisualOnExit.value = p.getBoolean("restore_video_visual_on_exit", true)
    }

    fun setAutoCanvasSaverMobileData(context: Context, enabled: Boolean) {
        autoCanvasSaverMobileDataEnabled.value = enabled
        prefs(context).edit().putBoolean("auto_canvas_saver_mobile_data", enabled).apply()
        com.music.spotui.data.CanvasNetworkPolicy.updateSaverStatus()
    }

    fun setCanvasQualityLimit(context: Context, limit: String) {
        canvasQualityLimit.value = limit
        prefs(context).edit().putString("canvas_quality_limit", limit).apply()
        com.music.spotui.data.CanvasNetworkPolicy.updateSaverStatus()
    }

    fun setDynamicBackgrounds(context: Context, enabled: Boolean) {
        dynamicBackgrounds.value = enabled
        prefs(context).edit().putBoolean("dynamic_backgrounds", enabled).apply()
    }

    fun setAnimatedArtwork(context: Context, enabled: Boolean) {
        animatedArtworkEnabled.value = enabled
        prefs(context).edit().putBoolean("animated_artwork_enabled", enabled).apply()
    }

    fun setGestureControls(context: Context, enabled: Boolean) {
        gestureControlsEnabled.value = enabled
        prefs(context).edit().putBoolean("gesture_controls_enabled", enabled).apply()
    }

    fun setVisualizer(context: Context, enabled: Boolean) {
        visualizerEnabled.value = enabled
        prefs(context).edit().putBoolean("visualizer_enabled", enabled).apply()
    }

    fun setVisualizerIntensity(context: Context, intensity: String) {
        visualizerIntensity.value = intensity
        prefs(context).edit().putString("visualizer_intensity", intensity).apply()
    }

    fun setLongPressDurationMs(context: Context, durationMs: Long) {
        longPressDurationMs.value = durationMs
        prefs(context).edit().putLong("long_press_duration_ms", durationMs).apply()
    }
}
