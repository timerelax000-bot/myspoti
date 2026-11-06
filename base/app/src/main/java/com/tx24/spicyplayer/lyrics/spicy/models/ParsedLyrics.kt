package com.tx24.spicyplayer.lyrics.spicy.models

data class FooterLine(
    val text: String = "",
    val kind: Kind = Kind.WRITERS,
    val name: String? = null,
    val label: String? = null,
    val avatarUrl: String? = null,
    val url: String? = null,
    val profileUrl: String? = null,
    val parts: List<FooterLine> = emptyList(),
) {
    enum class Kind {
        WRITERS,
        PROVIDER,
        CONTRIBUTOR,
        COMMUNITY,
        TEXTS,
        NOTE,
    }
}

/** Non-timed attribution displayed after the vocal timeline. */
data class LyricsFooter(
    val songwriters: List<String> = emptyList(),
    val provenance: LyricsProvenance? = null,
    val lines: List<FooterLine> = emptyList(),
) {
    fun miniCredit(): String? = provenance?.provider ?: songwriters.firstOrNull()
}

data class LyricsProvenance(
    val provider: String,
    val contributor: String? = null,
)

/** Normalized lyric input shared by static, line-synced, and syllable-synced renderers. */
data class LyricsDocument(
    val lines: List<Line>,
    val footer: LyricsFooter = LyricsFooter(),
    /** Synchronization granularity; drives which render path is used. */
    val type: LyricsType = LyricsType.Syllable,
    /** Song URI + source + SHA-256 of the raw lyric payload. */
    val documentId: String = "",
) {
    val songwriters: List<String> get() = footer.songwriters

    /** True if any word carries romanized text (from TTML/API metadata or on-device romanization). */
    val hasTransliteration: Boolean
        get() = lines.any { line -> line.words.any { it.romanizedText != null } }
}

typealias ParsedLyrics = LyricsDocument
