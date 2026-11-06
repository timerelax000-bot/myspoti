package com.tx24.spicyplayer.translation

import com.tx24.spicyplayer.lyrics.spicy.models.Line

enum class TranslationMode {
    Replace,
    UnderLine,
    Off
}

data class TranslationPresentation(
    val lines: List<Line> = emptyList(),
    val displayLines: List<Line> = lines,
    val mode: TranslationMode = TranslationMode.Replace,
) {
    val texts: List<Line> get() = displayLines

    companion object {
        fun forTimeline(
            originalLines: List<Line>,
            rendererLines: List<Line>,
            result: Any?,
            mode: TranslationMode
        ): TranslationPresentation? = null
    }
}
