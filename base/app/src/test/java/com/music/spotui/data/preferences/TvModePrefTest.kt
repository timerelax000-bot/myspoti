package com.music.spotui.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvModePrefTest {
    @Test
    fun defaultPresetIsBalanced() {
        assertEquals("balanced", TvModePref.LAYOUT_BALANCED)
        assertEquals("cinema", TvModePref.LAYOUT_CINEMA)
        assertEquals("lyrics", TvModePref.LAYOUT_LYRICS)
        assertEquals("minimal", TvModePref.LAYOUT_MINIMAL)
    }

    @Test
    fun presetsAreKnown() {
        val validPresets = setOf(
            TvModePref.LAYOUT_BALANCED,
            TvModePref.LAYOUT_CINEMA,
            TvModePref.LAYOUT_LYRICS,
            TvModePref.LAYOUT_MINIMAL,
        )
        assertTrue(TvModePref.layoutPreset in validPresets)
    }

    @Test
    fun timeoutsHaveSensibleDefaults() {
        assertTrue(TvModePref.autoHideSeconds in listOf(0, 2, 5, 8))
        assertTrue(TvModePref.ambientSeconds in listOf(0, 15, 30, 60))
        assertTrue(TvModePref.upNextSeconds in 5..30)
    }
}
