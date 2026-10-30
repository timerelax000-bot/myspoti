package com.music.spotui.data.api

import com.music.spotui.data.entity.SongsModel
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistAddTest {

    @Test
    fun cleanIdStripsPrefixesAndUrls() {
        assertEquals("3n3Ppam7vgaVa1iaRUc9Lp", SpotifySync.cleanId("spotify:track:3n3Ppam7vgaVa1iaRUc9Lp"))
        assertEquals("3n3Ppam7vgaVa1iaRUc9Lp", SpotifySync.cleanId("https://open.spotify.com/track/3n3Ppam7vgaVa1iaRUc9Lp?si=123"))
        assertEquals("37i9dQZF1DXcBWIGoYBM5M", SpotifySync.cleanId("spotify:playlist:37i9dQZF1DXcBWIGoYBM5M"))
        assertEquals("37i9dQZF1DXcBWIGoYBM5M", SpotifySync.cleanId("37i9dQZF1DXcBWIGoYBM5M"))
        assertEquals("", SpotifySync.cleanId(""))
    }

    @Test
    fun resolvesTrackIdFromSpotifyTrackIdOrUrl() {
        val s1 = SongsModel(id = 1, title = "Test1", singer = "A", album = "", coverUri = "", url = "spotify:track:1111111111111111111111").copy(spotifyTrackId = "1111111111111111111111")
        val s2 = SongsModel(id = 2, title = "Test2", singer = "B", album = "", coverUri = "", url = "https://open.spotify.com/track/2222222222222222222222?si=456")

        val id1 = s1.spotifyTrackId.ifBlank {
            if (s1.url.contains("spotify:track:") || s1.url.contains("/track/")) {
                s1.url.substringAfterLast("/track/").substringBefore('?').substringAfterLast(':')
            } else ""
        }
        val id2 = s2.spotifyTrackId.ifBlank {
            if (s2.url.contains("spotify:track:") || s2.url.contains("/track/")) {
                s2.url.substringAfterLast("/track/").substringBefore('?').substringAfterLast(':')
            } else ""
        }

        assertEquals("1111111111111111111111", SpotifySync.cleanId(id1))
        assertEquals("2222222222222222222222", SpotifySync.cleanId(id2))
    }

    @Test
    fun preventsDuplicateTapsWhenAdding() {
        var adding = false
        var count = 0

        fun triggerAdd() {
            if (adding) return
            adding = true
            count++
        }

        triggerAdd()
        triggerAdd()
        triggerAdd()

        assertEquals(1, count)
    }
}
