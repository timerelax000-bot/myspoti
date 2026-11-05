package com.music.spotui.data

import app.myspoti.api.FriendActivity
import app.myspoti.api.Track
import app.myspoti.api.UserRef
import org.junit.Assert.*
import org.junit.Test

class FriendActivityTest {

    @Test
    fun isNowPlayingBoundaryClassification() {
        val now = 1_000_000_000L
        val user = UserRef("spotify:user:1", "Friend 1", null)
        val track = Track("spotify:track:1", "Song Title", 180_000, emptyList(), null)

        // 5 minutes ago -> Currently listening
        val recent = FriendActivity(user, track, null, now - 5 * 60_000L)
        assertTrue(recent.isNowPlaying(now))

        // Exactly 14 minutes 59 seconds ago -> Currently listening
        val boundaryIn = FriendActivity(user, track, null, now - 14 * 60_000L - 59_000L)
        assertTrue(boundaryIn.isNowPlaying(now))

        // Exactly 15 minutes ago -> Not listening now (Recently Active)
        val boundaryOut = FriendActivity(user, track, null, now - 15 * 60_000L)
        assertFalse(boundaryOut.isNowPlaying(now))

        // 1 hour ago -> Not listening now
        val old = FriendActivity(user, track, null, now - 60 * 60_000L)
        assertFalse(old.isNowPlaying(now))
    }
}
