package com.music.spotui.ui.immersive.lyrics.spicy

import com.music.spotui.data.entity.Lyrics
import com.music.spotui.data.entity.LyricLine
import com.music.spotui.data.entity.LyricWord
import com.music.spotui.data.entity.SyncPrecision
import com.tx24.spicyplayer.lyrics.spicy.models.Line
import com.tx24.spicyplayer.lyrics.spicy.models.LineRole
import com.tx24.spicyplayer.lyrics.spicy.models.Word
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsType
import com.tx24.spicyplayer.lyrics.spicy.models.ParsedLyrics
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsFooter
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsProvenance
import com.tx24.spicyplayer.lyrics.spicy.models.FooterLine

object SpicyLyricsMapper {

    fun mapToSpicy(lyrics: Lyrics, trackUri: String = ""): ParsedLyrics {
        val type = when (lyrics.syncPrecision) {
            SyncPrecision.SYLLABLE -> LyricsType.Syllable
            SyncPrecision.LINE -> LyricsType.Line
            SyncPrecision.STATIC -> LyricsType.Static
        }

        val lines = mutableListOf<Line>()
        lyrics.lines.forEachIndexed { index, lyricLine ->
            val isGuest = lyricLine.voice != null && lyricLine.voice > 1
            val words = if (lyricLine.words != null && lyricLine.words.isNotEmpty()) {
                lyricLine.words.map { word ->
                    Word(
                        text = word.text,
                        startMs = word.startMs,
                        endMs = if (word.endMs > 0) word.endMs else word.startMs + 500L,
                        romanizedText = lyricLine.romanization,
                        translatedText = lyricLine.translation,
                    )
                }
            } else {
                val nextStartMs = lyrics.lines.getOrNull(index + 1)?.timeMs ?: (lyricLine.timeMs + 3000L)
                val lineEndMs = maxOf(lyricLine.timeMs + 1000L, nextStartMs)
                listOf(
                    Word(
                        text = lyricLine.text,
                        startMs = lyricLine.timeMs,
                        endMs = lineEndMs,
                        romanizedText = lyricLine.romanization,
                        translatedText = lyricLine.translation,
                    )
                )
            }

            val nextTimeMs = lyrics.lines.getOrNull(index + 1)?.timeMs ?: (lyricLine.timeMs + 3000L)
            val lineEndMs = words.lastOrNull()?.endMs ?: maxOf(lyricLine.timeMs + 1000L, nextTimeMs)

            val leadLine = Line(
                words = words,
                startMs = lyricLine.timeMs,
                endMs = lineEndMs,
                oppositeAligned = isGuest,
                lineTimed = lyricLine.words.isNullOrEmpty() && lyrics.synced,
                untimed = !lyrics.synced,
            )
            lines.add(leadLine)

            if (!lyricLine.background.isNullOrBlank()) {
                val bgWords = if (!lyricLine.backgroundWords.isNullOrEmpty()) {
                    lyricLine.backgroundWords.map { bgWord ->
                        Word(
                            text = bgWord.text,
                            startMs = bgWord.startMs,
                            endMs = if (bgWord.endMs > 0) bgWord.endMs else bgWord.startMs + 400L,
                        )
                    }
                } else {
                    listOf(
                        Word(
                            text = lyricLine.background,
                            startMs = lyricLine.timeMs,
                            endMs = lineEndMs,
                        )
                    )
                }
                lines.add(
                    Line(
                        words = bgWords,
                        startMs = lyricLine.timeMs,
                        endMs = lineEndMs,
                        role = LineRole.BACKGROUND,
                        oppositeAligned = !isGuest,
                        lineTimed = lyricLine.backgroundWords.isNullOrEmpty() && lyrics.synced,
                        untimed = !lyrics.synced,
                    )
                )
            }
        }

        val footerLines = mutableListOf<FooterLine>()
        lyrics.provider?.let {
            footerLines.add(FooterLine(text = "Provided by $it", kind = FooterLine.Kind.PROVIDER))
        }
        lyrics.songwriters?.let { writers ->
            if (writers.isNotEmpty()) {
                footerLines.add(FooterLine(text = "Written by ${writers.joinToString(", ")}", kind = FooterLine.Kind.WRITERS))
            }
        }

        val footer = LyricsFooter(
            songwriters = lyrics.songwriters.orEmpty(),
            provenance = lyrics.provider?.let { LyricsProvenance(provider = it) },
            lines = footerLines,
        )

        return ParsedLyrics(
            lines = lines,
            footer = footer,
            type = type,
            documentId = "$trackUri-${lyrics.fetchedAtMs}",
        )
    }
}
