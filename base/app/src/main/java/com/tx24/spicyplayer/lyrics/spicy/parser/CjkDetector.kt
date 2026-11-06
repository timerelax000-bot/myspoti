package com.tx24.spicyplayer.lyrics.spicy.parser

object CjkDetector {
    fun isCjk(c: Char): Boolean {
        val code = c.code
        return code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF || code in 0x3000..0x303F || code in 0x3040..0x309F || code in 0x30A0..0x30FF
    }
}
