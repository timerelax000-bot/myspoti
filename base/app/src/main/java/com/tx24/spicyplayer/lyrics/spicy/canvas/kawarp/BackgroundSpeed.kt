package com.tx24.spicyplayer.lyrics.spicy.canvas.kawarp

import kotlin.math.exp

/**
 * How fast the background moves at a point in the song. 1 is the normal speed.
 */
object BackgroundSpeed {
    private const val BASE_TEMPO = 120f
    private const val BEAT_PULSE_MAX = 1.5f
    private const val BEAT_PULSE_DECAY = 5f
    private const val MIN_BEAT_CONFIDENCE = 0.4f

    fun at(seconds: Float, analysis: Any? = null): Float {
        return 1f
    }

    internal fun loudnessFactor(dB: Float): Float = 0.5f + (dB + 40f).coerceAtLeast(0f) / 40f * 0.7f
}
