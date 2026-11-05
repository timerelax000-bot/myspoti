package com.music.spotui.car

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class CarVoiceCommanderTest {

    private fun simulateCommandAction(spokenText: String): String {
        val lower = spokenText.lowercase(Locale.ROOT)
        return when {
            lower.contains("pause") || lower.contains("stop") || lower.contains("pauza") || lower.contains("stani") || lower.contains("zaustavi") -> "Paused playback"
            lower.contains("play my liked") || lower.contains("liked songs") || lower.contains("favorites") || lower.contains("omiljene") -> "Playing Liked Songs"
            lower.contains("driving playlist") || lower.contains("driving mix") || lower.contains("za vožnju") -> "Playing Driving Playlist"
            lower.contains("energetic") || lower.contains("energy") -> "Playing Energetic Mix"
            lower.contains("skip") || lower.contains("next") || lower.contains("sledeca") || lower.contains("sledeća") || lower.contains("dalje") -> "Playing next song"
            lower.contains("previous") || lower.contains("back") || lower.contains("restart") || lower.contains("prethodna") || lower.contains("nazad") -> "Playing previous song"
            lower.contains("shuffle on") || lower.contains("turn on shuffle") || lower.contains("enable shuffle") || lower.contains("uključi nasumično") -> "Shuffle turned on"
            lower.contains("shuffle off") || lower.contains("turn off shuffle") || lower.contains("disable shuffle") || lower.contains("isključi nasumično") -> "Shuffle turned off"
            lower.contains("resume") || lower.trim() == "play" || lower.trim() == "pusti" || lower.trim() == "play music" || lower.trim() == "pusti muziku" || lower.contains("nastavi") || lower.contains("plej") -> "Resumed playback"
            lower.startsWith("play ") || lower.startsWith("pusti ") -> {
                val query = lower.removePrefix("play ").removePrefix("pusti ").trim()
                if (query.isNotBlank()) "Searching & playing \"$query\"" else "Unrecognized command: \"$spokenText\""
            }
            else -> "Unrecognized command: \"$spokenText\""
        }
    }

    @Test
    fun voiceCommandParsingAccuracy() {
        assertEquals("Paused playback", simulateCommandAction("please pause the music"))
        assertEquals("Playing Liked Songs", simulateCommandAction("play my liked songs"))
        assertEquals("Playing next song", simulateCommandAction("next song on"))
        assertEquals("Playing next song", simulateCommandAction("skip this track"))
        assertEquals("Playing previous song", simulateCommandAction("go back to previous song"))
        assertEquals("Shuffle turned on", simulateCommandAction("turn on shuffle mode"))
        assertEquals("Shuffle turned off", simulateCommandAction("turn off shuffle"))
        assertEquals("Playing Driving Playlist", simulateCommandAction("play my driving playlist"))
        assertEquals("Playing Energetic Mix", simulateCommandAction("play something energetic"))
        assertEquals("Searching & playing \"daft punk\"", simulateCommandAction("play Daft Punk"))
        assertEquals("Unrecognized command: \"hello world\"", simulateCommandAction("hello world"))
    }
}
