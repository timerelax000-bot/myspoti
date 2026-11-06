package com.tx24.spicyplayer.lyrics.spicy.parser

import com.tx24.spicyplayer.lyrics.spicy.RenderConfig
import com.tx24.spicyplayer.lyrics.spicy.SyllableMerge
import com.tx24.spicyplayer.lyrics.spicy.models.Letter
import com.tx24.spicyplayer.lyrics.spicy.models.Line
import com.tx24.spicyplayer.lyrics.spicy.models.Word

object GraphemeSegmenter {
    fun segment(text: String): List<String> {
        val list = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            val charCount = Character.charCount(codePoint)
            list.add(text.substring(i, i + charCount))
            i += charCount
        }
        return list
    }
}

object RtlDetector {
    fun isRtl(text: String): Boolean {
        for (ch in text) {
            val dir = Character.getDirectionality(ch)
            if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return true
            }
        }
        return false
    }
}

/**
 * Merges a word's syllables when [RenderConfig.syllableMerge] asks for it, then splits
 * sufficiently long syllables into per-letter timings for the "held word"
 * letter-by-letter emphasis.
 */
object LetterSynthesizer {

    fun apply(lines: List<Line>, config: RenderConfig, romanized: Boolean): List<Line> {
        return lines.map { line ->
            if (line.isInterlude || line.isSongwriter || line.translationReplaces || line.lineTimed) {
                line
            } else {
                line.copy(words = mergeSyllables(line.words, config, romanized).map { synthesize(it, config, romanized) })
            }
        }
    }

    internal fun mergeSyllables(words: List<Word>, config: RenderConfig, romanized: Boolean): List<Word> {
        if (config.syllableMerge == SyllableMerge.Off) return words
        val runs = mutableListOf<MutableList<Word>>()
        for (word in words) {
            if (word.isPartOfWord && runs.isNotEmpty() && !startsNewWord(runs.last().last(), word)) {
                runs.last() += word
            } else {
                runs += mutableListOf(word)
            }
        }
        return runs.flatMap { run ->
            if (run.size == 1 || config.syllableMerge == SyllableMerge.Steady && !joinsEvenly(run)) return@flatMap run
            val first = run.first()
            val merged = first.copy(
                text = run.joinToString("") { it.text },
                endMs = run.maxOf { it.endMs },
                isLetterGroup = false,
                letters = emptyList(),
                romanizedText = if (run.any { it.romanizedText != null }) run.joinToString("") { it.romanizedText ?: it.text } else null,
                syllables = run,
            )
            if (config.syllableMerge == SyllableMerge.Held && !isLetterCapable(merged, config, romanized)) run else listOf(merged)
        }
    }

    private fun startsNewWord(prev: Word, next: Word): Boolean {
        val before = prev.text.trimEnd()
        val after = next.text.trimStart()
        if (before.isEmpty() || after.isEmpty()) return true
        val last = before.codePointBefore(before.length)
        val first = after.codePointAt(0)
        return last in WORD_JOINERS || first in WORD_JOINERS || isUnspaced(last) || isUnspaced(first)
    }

    private const val MAX_JOIN_GAP_MS = 100L
    private const val MAX_PACE_RATIO = 3.0

    private fun joinsEvenly(run: List<Word>): Boolean {
        if (run.any { part ->
                val last = part === run.last()
                fun spaced(text: String?) = text != null && (if (last) text.trimEnd() else text).any(Char::isWhitespace)
                spaced(part.text) || spaced(part.romanizedText)
            }) return false
        if (run.dropLast(1).any { it.text.isNotEmpty() && it.text.last() in DASHES }) return false
        for (i in 1 until run.size) if (run[i].startMs - run[i - 1].endMs > MAX_JOIN_GAP_MS) return false
        val total = (run.maxOf { it.endMs } - run.first().startMs).toDouble()
        val chars = run.sumOf { it.text.trimEnd().length }
        if (total <= 0 || chars == 0) return false
        return run.all { part ->
            val timeShare = part.duration / total
            val charShare = part.text.trimEnd().length.toDouble() / chars
            timeShare >= charShare / MAX_PACE_RATIO && timeShare <= charShare * MAX_PACE_RATIO
        }
    }

    private val DASHES = "-‐‑‒–—―".toSet()
    private val WORD_JOINERS = "-‐‑‒–—―/".map(Char::code).toSet()

    private val UNSPACED_RANGES = listOf(
        0x0E00..0x0EFF, 0x1000..0x109F, 0x1780..0x17FF,
        0x2E80..0x2FDF, 0x3005..0x3007, 0x3021..0x3029, 0x3038..0x303B,
        0x3040..0x30FF, 0x31F0..0x31FF, 0x3400..0x4DBF, 0x4E00..0x9FFF,
        0xF900..0xFAFF, 0xFF66..0xFF9F, 0x20000..0x323AF,
    )

    private fun isUnspaced(codePoint: Int) = UNSPACED_RANGES.any { codePoint in it }

    private fun display(word: Word, romanized: Boolean) = if (romanized) (word.romanizedText ?: word.text).trim() else word.text

    private fun isLetterCapable(word: Word, config: RenderConfig, romanized: Boolean): Boolean {
        val display = display(word, romanized)
        if (config.isAppleMusic) {
            return config.lettersEnabled && com.tx24.spicyplayer.lyrics.spicy.animation.AppleMusicMotion.emphasizes(
                display, GraphemeSegmenter.segment(display).size, word.duration,
            )
        }
        return config.lettersEnabled &&
            word.duration >= (if (config.isRefreshedSimple) 1000L else config.letterDurationThresholdMs) &&
            GraphemeSegmenter.segment(display).size in (if (config.isRefreshedSimple) 2..minOf(7, config.letterMaxLength) else 1..config.letterMaxLength) &&
            !RtlDetector.isRtl(display)
    }

    private fun synthesize(word: Word, config: RenderConfig, romanized: Boolean): Word {
        val display = display(word, romanized)
        val graphemes = GraphemeSegmenter.segment(display)
        if (!isLetterCapable(word, config, romanized)) {
            return if (word.isLetterGroup) word.copy(isLetterGroup = false, letters = emptyList()) else word
        }

        val letters = bySyllable(word, graphemes, word.startMs, word.endMs, config, romanized)
            ?: evenly(graphemes, word.startMs, word.endMs)
        return word.copy(isLetterGroup = true, letters = letters)
    }

    private fun evenly(graphemes: List<String>, from: Long, to: Long): List<Letter> {
        val span = (to - from).toFloat() / graphemes.size
        return graphemes.mapIndexed { i, grapheme ->
            Letter(char = grapheme, startMs = from + (i * span).toLong(), endMs = from + ((i + 1) * span).toLong())
        }
    }

    private fun bySyllable(word: Word, graphemes: List<String>, windowStart: Long, windowEnd: Long, config: RenderConfig, romanized: Boolean): List<Letter>? {
        if (!config.syllablePace) return null
        val parts = word.syllables.takeIf { it.size > 1 } ?: return null
        val texts = parts.mapIndexed { i, part ->
            val text = if (romanized) part.romanizedText ?: part.text else part.text
            when {
                !romanized -> text
                i == 0 -> text.trimStart()
                i == parts.lastIndex -> text.trimEnd()
                else -> text
            }
        }
        val counts = texts.map { GraphemeSegmenter.segment(it).size }
        if (counts.sum() != graphemes.size) return null
        val scale = (windowEnd - windowStart).toDouble() / (word.endMs - word.startMs).coerceAtLeast(1L)
        fun placed(ms: Long) = windowStart + ((ms - word.startMs) * scale).toLong()
        val letters = ArrayList<Letter>(graphemes.size)
        var cursor = windowStart
        parts.forEachIndexed { i, _ ->
            val end = if (i == parts.lastIndex) windowEnd else placed(parts[i + 1].startMs).coerceIn(cursor, windowEnd)
            if (counts[i] > 0) letters += evenly(graphemes.subList(letters.size, letters.size + counts[i]), cursor, end)
            cursor = end
        }
        return letters
    }
}
