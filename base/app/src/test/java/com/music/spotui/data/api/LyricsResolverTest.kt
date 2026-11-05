package com.music.spotui.data.api

import com.music.spotui.data.entity.LyricLine
import com.music.spotui.data.entity.LyricWord
import com.music.spotui.data.entity.Lyrics
import com.music.spotui.extensions.LyricsMerge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsResolverTest {

    @Test
    fun testQualityRanking() {
        val plain = Lyrics(listOf(LyricLine(0, "Plain text line")), synced = false)
        val lineSynced = Lyrics(listOf(LyricLine(1000, "Synced line")), synced = true)
        val wordSynced = Lyrics(
            listOf(LyricLine(1000, "Word line", listOf(LyricWord(1000, 1500, "Word")))),
            synced = true
        )

        assertEquals(1, LyricsMerge.rank(plain))
        assertEquals(2, LyricsMerge.rank(lineSynced))
        assertEquals(3, LyricsMerge.rank(wordSynced))
    }

    @Test
    fun testLyricsMergeChoosesHigherRanked() {
        val plain = Lyrics(listOf(LyricLine(0, "Plain text")), synced = false, provider = "PlainProvider")
        val synced = Lyrics(listOf(LyricLine(1000, "Line synced")), synced = true, provider = "SyncedProvider")
        val wordSynced = Lyrics(
            listOf(LyricLine(1000, "Word synced", listOf(LyricWord(1000, 1500, "Word")))),
            synced = true,
            provider = "WordProvider"
        )

        // Line-synced beats plain
        val chosen1 = LyricsMerge.choose(plain, synced)
        assertNotNull(chosen1)
        assertEquals("SyncedProvider", chosen1?.provider)

        // Word-synced beats line-synced
        val chosen2 = LyricsMerge.choose(synced, wordSynced)
        assertNotNull(chosen2)
        assertEquals("WordProvider", chosen2?.provider)
    }

    @Test
    fun testTrackKeyNormalization() {
        val track1 = LyricsApi.Track("123", "  Test Title - Remix ", "Artist A, Artist B", "Album", 180000)
        val track2 = LyricsApi.Track("123", "test title", "artist a", "album", 180000)

        assertEquals(track1.trackId, track2.trackId)
        assertEquals(track1.key, track2.key)
    }
}
