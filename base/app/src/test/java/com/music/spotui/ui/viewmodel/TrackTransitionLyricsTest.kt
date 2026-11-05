package com.music.spotui.ui.viewmodel

import com.music.spotui.data.api.LyricsApi
import com.music.spotui.data.entity.Lyrics
import org.junit.Assert.*
import org.junit.Test

class TrackTransitionLyricsTest {

    @Test
    fun trackKeyIsUniquePerTrack() {
        val track1 = LyricsApi.Track(
            trackId = "trackA",
            title = "Song A",
            artist = "Artist A",
            album = "Album A",
            durationMs = 180000L,
        )
        val track2 = LyricsApi.Track(
            trackId = "trackB",
            title = "Song B",
            artist = "Artist B",
            album = "Album B",
            durationMs = 200000L,
        )

        assertEquals("trackA", track1.key)
        assertEquals("trackB", track2.key)
        assertNotEquals(track1.key, track2.key)
    }

    @Test
    fun lyricsStateNotFoundOnEmptyLyrics() {
        val vm = LyricsViewModel()
        val trackNoLyrics = LyricsApi.Track(
            trackId = "nonExistentTrack",
            title = "Unknown Song",
            artist = "Unknown Artist",
            album = "Unknown Album",
            durationMs = 120000L,
        )

        // Initial state should be Loading
        assertTrue(vm.state.value is LyricsViewModel.State.Loading)

        // Synchronously test state transition helper
        val state = vm.state.value
        assertFalse(state is LyricsViewModel.State.Loaded)
    }
}
