package com.music.spotui.video

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicVideoSessionTest {

    @Test
    fun testMusicVideoStateReady() {
        val segment = AudioAlign.Segment(0L, 5000L, 100L)
        val timeMap = AudioAlign.TimeMap(listOf(segment), 0.8)
        val state = MusicVideoController.State.Ready("track123", "https://youtube.com/watch?v=test", timeMap)

        assertEquals("track123", state.trackId)
        assertEquals("https://youtube.com/watch?v=test", state.videoUrl)
        assertEquals(0.8, state.map.score, 0.001)
    }

    @Test
    fun testAudioHandoffStateEnumValues() {
        val states = MusicVideoController.AudioHandoffState.values()
        assertEquals(5, states.size)
        assertEquals(MusicVideoController.AudioHandoffState.SPOTIFY_AUDIO, states[0])
        assertEquals(MusicVideoController.AudioHandoffState.VIDEO_MUTED, states[1])
        assertEquals(MusicVideoController.AudioHandoffState.VIDEO_AUDIO, states[2])
    }
}
