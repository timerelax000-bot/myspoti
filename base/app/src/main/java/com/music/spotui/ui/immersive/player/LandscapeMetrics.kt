/*
 * Adapted from Spicy Player / Spicy Lyrics (https://github.com/spicylyrics/mobile), AGPL-3.0.
 * Copyright (C) Spicy Lyrics Authors.
 * Modified for Myspoti, 2026.
 */
package com.music.spotui.ui.immersive.player

import kotlin.math.min

/**
 * Geometry of the landscape lyrics page, resolved for one page size: the now-playing panel
 * (square cover with the song text centred under it) on one side, the lyrics on the other.
 */
data class LandscapeMetrics(
    val pageWidthPx: Float,
    val pageHeightPx: Float,
    val density: Float,
    val topPx: Float = 0f,
    val timelineOutside: Boolean = false,
) {
    /** The cover size: proportional to screen dimensions. */
    val coverSizePx = min(pageWidthPx * 0.30f, pageHeightPx * 0.52f) * 1.1f

    private val innerSpacingPx = coverSizePx / 4f
    private val edgeSpacingPx = (pageWidthPx * 0.5f - coverSizePx - innerSpacingPx) * 0.7f

    val coverTopPx = topPx + pageHeightPx * 0.5f - coverSizePx * if (timelineOutside) 0.625f else 0.6f

    val lyricFontSizeSp = maxOf(coverSizePx / density * (DESKTOP_LYRICS_PX / DESKTOP_COVER_PX), MIN_LYRICS_SP)

    private val textBoost = lyricFontSizeSp / (coverSizePx / density * (DESKTOP_LYRICS_PX / DESKTOP_COVER_PX))

    val baseFontSizeSp = coverSizePx / density * 0.03f * textBoost

    fun coverLeftPx(side: Float): Float {
        val left = edgeSpacingPx
        val right = pageWidthPx - coverSizePx - edgeSpacingPx
        return left + (right - left) * side
    }

    val panel = header(edgeSpacingPx)
    val centred = header((pageWidthPx - coverSizePx) / 2f)

    private fun header(left: Float): ExpandedHeaderMetrics {
        val base = baseFontSizeSp
        val titleSize = base * 2.25f
        val artistsSize = base * 1.275f
        return ExpandedHeaderMetrics(
            artLeftPx = left,
            artTopPx = coverTopPx,
            artSizePx = coverSizePx,
            textHeightPx = (titleSize * 1.12f + artistsSize * 1.317f) * density,
            titleSizeSp = titleSize,
            titleLineHeightSp = titleSize * 1.12f,
            artistsSizeSp = artistsSize,
            artistsLineHeightSp = artistsSize * 1.317f,
        )
    }

    val stageFeatherPx = coverSizePx * 0.45f
    val stageSolidPx = edgeSpacingPx + coverSizePx
    val stageWidthPx = stageSolidPx + stageFeatherPx
    val dockInsetPx = coverSizePx * 0.01f

    fun lyricsBox(withPanel: Boolean, panelOnRight: Boolean): LyricsBoxMetrics {
        val textStart: Float
        val textEnd: Float
        if (withPanel) {
            textStart = (pageWidthPx * 0.5f - innerSpacingPx * 0.3f) * 0.9f
            textEnd = pageWidthPx - edgeSpacingPx * 0.35f
        } else {
            textStart = pageWidthPx * 0.18f
            textEnd = pageWidthPx * (1f - 0.155f)
        }
        val sideInset = 0.05f
        val width = (textEnd - textStart) / (1f - 2f * sideInset)
        val left = textStart - width * sideInset
        return LyricsBoxMetrics(if (panelOnRight) pageWidthPx - left - width else left, width)
    }

    companion object {
        const val DESKTOP_LYRICS_PX = 56f
        const val DESKTOP_COVER_PX = 741f
        const val MIN_LYRICS_SP = 29.6f
    }
}

data class ExpandedHeaderMetrics(
    val artLeftPx: Float,
    val artTopPx: Float,
    val artSizePx: Float,
    val textHeightPx: Float,
    val titleSizeSp: Float,
    val titleLineHeightSp: Float,
    val artistsSizeSp: Float,
    val artistsLineHeightSp: Float,
)

data class LyricsBoxMetrics(val leftPx: Float, val widthPx: Float)
