package com.music.spotui.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSourceStateTest {

    @Test
    fun audioSourceStateTogglesCleanly() {
        var clipSound = false

        fun toggleAudioSource(): Boolean {
            clipSound = !clipSound
            return clipSound
        }

        assertFalse(clipSound)
        assertTrue(toggleAudioSource())
        assertFalse(toggleAudioSource())
    }

    @Test
    fun hushedStateCalculatesCorrectlyForClipSound() {
        fun calculateHushed(
            clipSound: Boolean,
            videoPlaying: Boolean,
            songPlaying: Boolean,
            adPlaying: Boolean
        ): Boolean {
            return clipSound && videoPlaying && songPlaying && !adPlaying
        }

        // When clip sound is off, Spotify song is heard (not hushed)
        assertFalse(calculateHushed(clipSound = false, videoPlaying = true, songPlaying = true, adPlaying = false))

        // When clip sound is on and both are playing, Spotify song is hushed so only clip audio is audible
        assertTrue(calculateHushed(clipSound = true, videoPlaying = true, songPlaying = true, adPlaying = false))

        // When an ad is playing, clip sound is muted and Spotify ad audio is heard
        assertFalse(calculateHushed(clipSound = true, videoPlaying = true, songPlaying = true, adPlaying = true))
    }

    @Test
    fun videoStatePreservedAcrossModeSwitch() {
        var videoOn = false
        var fullscreen = false

        fun enterFullscreen() {
            if (videoOn) {
                fullscreen = true
            }
        }

        fun exitFullscreen() {
            fullscreen = false
        }

        videoOn = true
        enterFullscreen()
        assertTrue(fullscreen)
        assertTrue(videoOn)

        exitFullscreen()
        assertFalse(fullscreen)
        assertTrue(videoOn)
    }
}
