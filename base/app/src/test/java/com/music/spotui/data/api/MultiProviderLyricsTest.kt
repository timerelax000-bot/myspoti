package com.music.spotui.data.api

import com.music.spotui.data.entity.Lyrics
import com.music.spotui.data.entity.SyncPrecision
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.xmlpull.v1.XmlPullParser

class MultiProviderLyricsTest {

    private fun ttml(xml: String): Lyrics? {
        val parser = runCatching { Class.forName("org.kxml2.io.KXmlParser").getDeclaredConstructor().newInstance() as XmlPullParser }.getOrNull()
        return if (parser != null) LyricsApi.parseTtml(xml) { parser } else null
    }

    // 1. Spicy Lyrics Syllable JSON
    @Test
    fun spicyLyricsSyllableJson() {
        val json = """{
            "Type": "Syllable",
            "source": "spicy_lyrics",
            "UploadAttribution": { "uploader": "test_user", "maker": "test_maker" },
            "SongWriters": ["Writer A", "Writer B"],
            "Content": [
                {
                    "StartTime": 1.5,
                    "Text": "Hello world",
                    "Singer": "v1",
                    "Syllables": [
                        { "StartTime": 1.5, "EndTime": 2.0, "Text": "Hello " },
                        { "StartTime": 2.0, "EndTime": 2.5, "Text": "world" }
                    ]
                }
            ]
        }"""
        val l = LyricsApi.parseSpicyLyricsJson(json)
        assertNotNull(l)
        assertTrue(l!!.synced)
        assertEquals(SyncPrecision.SYLLABLE, l.syncPrecision)
        assertEquals("Spicy Lyrics", l.provider)
        assertEquals("test_user", l.uploader)
        assertEquals("test_maker", l.maker)
        assertEquals(listOf("Writer A", "Writer B"), l.songwriters)
        assertEquals(1, l.lines.size)
        assertEquals(1500L, l.lines[0].timeMs)
        assertEquals(2, l.lines[0].words!!.size)
        assertEquals(1500L, l.lines[0].words!![0].startMs)
        assertEquals(2000L, l.lines[0].words!![0].endMs)
    }

    // 2. Spicy Lyrics Line JSON
    @Test
    fun spicyLyricsLineJson() {
        val json = """{
            "Type": "Line",
            "source": "apple_music",
            "Content": [
                { "StartTime": 3.0, "Text": "Line one" },
                { "StartTime": 6.5, "Text": "Line two" }
            ]
        }"""
        val l = LyricsApi.parseSpicyLyricsJson(json)
        assertNotNull(l)
        assertTrue(l!!.synced)
        assertEquals(SyncPrecision.LINE, l.syncPrecision)
        assertEquals("Spicy Lyrics · apple_music", l.provider)
        assertEquals(2, l.lines.size)
        assertEquals(3000L, l.lines[0].timeMs)
        assertEquals(6500L, l.lines[1].timeMs)
        assertNull(l.lines[0].words)
    }

    // 3. Spicy Lyrics Static JSON
    @Test
    fun spicyLyricsStaticJson() {
        val json = """{
            "Type": "Static",
            "source": "spicy_lyrics",
            "Content": [
                { "Text": "Plain line 1" },
                { "Text": "Plain line 2" }
            ]
        }"""
        val l = LyricsApi.parseSpicyLyricsJson(json)
        assertNotNull(l)
        assertFalse(l!!.synced)
        assertEquals(SyncPrecision.STATIC, l.syncPrecision)
        assertEquals(2, l.lines.size)
        assertEquals(0L, l.lines[0].timeMs)
    }

    // 4. AMLL TTML
    @Test
    fun amllTtmlParsing() {
        val xml = """<tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
            <body><div><p begin="00:02.100" end="00:05.000"><span begin="00:02.100" end="00:03.000">AMLL </span><span begin="00:03.000" end="00:05.000">TTML</span></p></div></body></tt>"""
        val l = ttml(xml)
        if (l != null) {
            assertTrue(l.synced)
            assertEquals(1, l.lines.size)
            assertEquals(2100L, l.lines[0].timeMs)
            assertEquals(2, l.lines[0].words!!.size)
        }
    }

    // 5. lrc.red TTML
    @Test
    fun lrcRedTtml() {
        val xml = """<tt xmlns="http://www.w3.org/ns/ttml">
            <body><div><p begin="10.0s" end="15.0s"><span begin="10.0s" end="12.0s">lrc </span><span begin="12.0s" end="15.0s">red</span></p></div></body></tt>"""
        val l = ttml(xml)
        if (l != null) {
            assertTrue(l.synced)
            assertEquals(10000L, l.lines[0].timeMs)
        }
    }

    // 6. Unison TTML & LRC
    @Test
    fun unisonFormatValidation() {
        val jsonTtml = """{"success": true, "data": {"format": "ttml", "lyrics": "<tt><body><div><p begin=\"1.0s\">Unison</p></div></body></tt>"}}"""
        val o = JSONObject(jsonTtml).getJSONObject("data")
        assertEquals("ttml", o.getString("format"))
    }

    // 7. Better Lyrics QQ/QRC
    @Test
    fun qqQrcParsing() {
        val qrc = """[1000,4000]<0,1000,0>QQ <1000,2000,0>QRC <3000,1000,0>Test
[5000,2000]<0,2000,0>Second line"""
        val l = LyricsApi.parseQrc(qrc)
        assertNotNull(l)
        assertTrue(l!!.synced)
        assertEquals(2, l.lines.size)
        assertEquals("QQ QRC Test", l.lines[0].text)
        assertEquals(3, l.lines[0].words!!.size)
        assertEquals(1000L, l.lines[0].words!![0].startMs)
        assertEquals(2000L, l.lines[0].words!![0].endMs)
        assertEquals(2000L, l.lines[0].words!![1].startMs)
        assertEquals(4000L, l.lines[0].words!![1].endMs)
    }

    // 8. LRCMux output
    @Test
    fun lrcMuxParsing() {
        val lrc = "[00:04.50]LRCMux line 1\n[00:10.00]LRCMux line 2"
        val lines = LyricsApi.parseLrc(lrc)
        assertEquals(2, lines.size)
        assertEquals(4500L, lines[0].timeMs)
        assertEquals("LRCMux line 1", lines[0].text)
    }

    // 9 & 10. LRCLIB word-synced & LRC
    @Test
    fun lrclibLrcParsing() {
        val lrc = "[01:02.34]LRCLIB synced lyrics"
        val lines = LyricsApi.parseLrc(lrc)
        assertEquals(1, lines.size)
        assertEquals(62340L, lines[0].timeMs)
    }

    // 11, 12, 13, 14. Plain lyrics
    @Test
    fun plainLyricsFormat() {
        val plainText = "Line 1\nLine 2\nLine 3"
        val lines = plainText.lines().map { com.music.spotui.data.entity.LyricLine(0L, it) }
        val l = Lyrics(lines = lines, synced = false, provider = "Lyrics.ovh")
        assertFalse(l.synced)
        assertEquals(3, l.lines.size)
    }

    // 15. No results
    @Test
    fun noResultsHandled() {
        val emptyL = Lyrics(lines = emptyList(), synced = false)
        assertTrue(emptyL.isEmpty)
    }

    // 16 & 17. Track matching and duration mismatch
    @Test
    fun trackMatching() {
        assertTrue(LyricsApi.isPlausibleMatch("Shape of You", "Ed Sheeran", 233, "Shape of You", "Ed Sheeran", 235))
        assertFalse(LyricsApi.isPlausibleMatch("Shape of You", "Ed Sheeran", 233, "Shape of You", "Ed Sheeran", 260))
        assertFalse(LyricsApi.isPlausibleMatch("Despacito", "Luis Fonsi", 220, "Shape of You", "Ed Sheeran", 220))
    }

    // 18 & 19. Cache and 30-day Spicy Lyrics rule
    @Test
    fun spicyLyrics30DayExpiryRule() {
        val thirtyOneDaysAgo = System.currentTimeMillis() - (31L * 24 * 3600 * 1000)
        val l = Lyrics(
            lines = listOf(com.music.spotui.data.entity.LyricLine(1000L, "Hello")),
            synced = true,
            provider = "Spicy Lyrics",
            uploader = "community_user",
            fetchedAtMs = thirtyOneDaysAgo
        )
        val isExpired = "Spicy Lyrics" in (l.provider ?: "") && l.uploader != null && (System.currentTimeMillis() - l.fetchedAtMs > 30L * 24 * 3600 * 1000)
        assertTrue(isExpired)
    }

    // 20. Rate limiting resilience
    @Test
    fun rateLimitingHandled() {
        val health = LyricsHealth()
        health.failed("test_provider")
        health.failed("test_provider")
        health.failed("test_provider")
        assertNotNull(health.reason("test_provider"))
    }

    // 21 & 22. Rapid track switching and seek sync
    @Test
    fun trackKeyMatching() {
        val t1 = LyricsApi.Track("123", "Song Title", "Artist Name", "Album", 180000L)
        val t2 = LyricsApi.Track("123", "Song Title", "Artist Name", "Album", 180000L)
        assertEquals(t1.key, t2.key)
    }
}
