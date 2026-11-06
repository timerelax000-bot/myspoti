package com.tx24.spicyplayer.lyrics.spicy.models

enum class LineRole { LEAD, BACKGROUND, INTERLUDE }

/**
 * Represents a full line of lyrics composed of multiple [Word]s.
 */
data class Line(
    /** The list of words that make up this line. */
    val words: List<Word>,
    /** The official start time of the line in milliseconds. */
    val startMs: Long,
    /** The normalized line lifetime, independent from the final word's timing. */
    val endMs: Long = words.lastOrNull()?.endMs ?: startMs,
    /** Optional identifier for the singer (agent). */
    val agent: String? = null,
    /** Structural role in the normalized lyric timeline. */
    val role: LineRole = LineRole.LEAD,
    /** Stable group shared by a lead line and its background vocals. */
    val groupId: Int? = null,
    /** If true, the line should be aligned to the opposite side (e.g., right-aligned for harmonies). */
    val oppositeAligned: Boolean = false,
    val translationReplaces: Boolean = false,
    val lineTimed: Boolean = false,
    val untimed: Boolean = false,
) {
    val isBackground: Boolean get() = role == LineRole.BACKGROUND
    val isInterlude: Boolean get() = role == LineRole.INTERLUDE
    val isSongwriter: Boolean get() = false

    val duration: Long
        get() = (endMs - startMs).coerceAtLeast(1)
}
