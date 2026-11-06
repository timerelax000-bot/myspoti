package com.music.spotui.data.api

import android.util.Log
import com.music.spotui.data.entity.LyricLine
import com.music.spotui.data.entity.LyricWord
import com.music.spotui.data.entity.Lyrics
import com.music.spotui.data.entity.LyricsColors
import com.music.spotui.data.entity.SyncPrecision
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Multi-provider lyrics engine resolving lyrics in priority order:
 * 1. Spicy Lyrics API
 * 2. Priority Tier 1 (TTML): AMLL, lrc.red, Unison TTML
 * 3. Priority Tier 2 (Synchronized): Better Lyrics QQ QRC, LRCMux, LRCLIB, Unison LRC
 * 4. Priority Tier 3 (Unsynchronized): LRCLIB plain, Lyrics.ovh, Spotify plain/NetEase/Genius, Spicy Static
 */
object LyricsApi {
    private const val LRCLIB = "https://lrclib.net/api"
    private const val UA = "Myspoti (https://github.com/)"

    /** What the resolver needs to know about a track. */
    data class Track(
        val trackId: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val coverUrl: String = "",
    ) {
        val key: String get() = trackId.ifBlank { "${title.lowercase()}|${artist.substringBefore(",").trim().lowercase()}" }
    }

    private class Miss(val at: Long = System.currentTimeMillis())
    private const val MISS_RETRY_MS = 120_000L
    private class Partial(val lyrics: Lyrics, val at: Long = System.currentTimeMillis())
    private const val MEMORY_MAX = 100
    private val cache: MutableMap<String, Any> = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, Any>(128, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Any>) = size > MEMORY_MAX
        }
    )
    private val inFlight = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Deferred<Lyrics?>>()
    private val health = LyricsHealth()
    private val bypass = ThreadLocal<Boolean>()
    private val scope =
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    @Volatile var extraSources: Boolean = false
    @Volatile var extraSourcesOff: Boolean = false
    @Volatile var wordSync: Boolean = true

    private const val WORD_WAIT_MS = 4_000L

    val early = kotlinx.coroutines.flow.MutableStateFlow<Pair<String, Lyrics>?>(null)
    private val earlyByKey = java.util.concurrent.ConcurrentHashMap<String, Lyrics>()

    fun earlyFor(track: Track): Lyrics? = earlyByKey[track.key]

    data class Romanized(val key: String, val before: Lyrics, val after: Lyrics)

    val romanized = kotlinx.coroutines.flow.MutableStateFlow<Romanized?>(null)

    private fun showEarly(track: Track, lyrics: Lyrics) {
        earlyByKey[track.key] = lyrics
        early.value = track.key to lyrics
    }

    fun cached(track: Track): Lyrics? = when (val c = cache[track.key]) {
        is Lyrics -> c
        is Partial -> c.lyrics
        else -> null
    }

    fun settled(track: Track): Boolean = cache[track.key] is Lyrics

    private fun wanted(track: Track): Boolean {
        if (track.title.isBlank() && track.trackId.isBlank()) return false
        return when (val c = cache[track.key]) {
            is Lyrics -> false
            is Miss -> System.currentTimeMillis() - c.at >= MISS_RETRY_MS
            is Partial -> System.currentTimeMillis() - c.at >= MISS_RETRY_MS
            else -> true
        }
    }

    private val after = java.util.concurrent.ConcurrentHashMap<String, Track>()

    fun prefetch(track: Track, then: Track? = null) {
        if (!wanted(track)) { after.remove(track.key); then?.let { prefetch(it) }; return }
        if (then != null) after[track.key] = then
        if (inFlight.containsKey(track.key)) return
        scope.launch { runCatching { fetch(track) }; after.remove(track.key)?.let { prefetch(it) } }
    }

    fun forget(track: Track) { cache.remove(track.key); health.forgetTrack(track.key); scope.launch { LyricsDisk.drop(track.key) } }

    fun forgetAll() { cache.clear(); health.forgetAll(); scope.launch { LyricsDisk.clear() } }

    val sourcesTick = kotlinx.coroutines.flow.MutableStateFlow(0L)

    fun sourcesChanged(keepFound: Boolean) {
        if (keepFound) synchronized(cache) { cache.values.removeAll { it is Miss || it is Partial } }
        else { cache.clear(); LyricsDisk.clear() }
        health.forgetAll()
        sourcesTick.value++
    }

    fun known(track: Track): Lyrics? {
        val found = when (val c = cache[track.key]) {
            is Lyrics -> c
            is Partial -> null
            is Miss -> null
            else -> LyricsDisk.read(track.key)
        }
        return found?.takeIf { it.synced && it.uncapped && !it.isEmpty }
    }

    suspend fun fetch(track: Track): Lyrics? {
        when (val c = cache[track.key]) {
            is Lyrics -> return c
            is Partial -> if (System.currentTimeMillis() - c.at < MISS_RETRY_MS) return c.lyrics
            is Miss -> if (System.currentTimeMillis() - c.at < MISS_RETRY_MS) return null
        }
        val job = synchronized(inFlight) {
            inFlight[track.key] ?: scope.async(start = kotlinx.coroutines.CoroutineStart.LAZY) {
                (LyricsDisk.read(track.key)?.also { cache[track.key] = it }
                    ?: (if (online()) null else kept(track))
                    ?: resolve(track)
                    ?: kept(track))?.also { romanizeLater(track, it) }
            }.also { created ->
                inFlight[track.key] = created
                created.invokeOnCompletion {
                    inFlight.remove(track.key, created)
                    after.remove(track.key)?.let { prefetch(it) }
                }
                created.start()
            }
        }
        return job.await()
    }

    data class Source(val id: String, val name: String, val words: Boolean = false, val setAside: String? = null)

    private fun allSetAside(vararg ids: String): String? = ids.map { health.reason(it) }.takeIf { r -> r.all { it != null } }?.first()

    private val WORD_SOURCES = setOf("spicy", "amll", "unison", "qq", "lrcmux", "lyricsplus", "lrcred", "kugou")

    fun sources(track: Track): List<Source> = buildList {
        if (track.trackId.isNotBlank()) add(Source("spicy", "Spicy Lyrics", words = true, setAside = allSetAside("spicy")))
        if (track.trackId.isNotBlank()) add(Source("spotify", "Spotify", words = true))
        add(Source("amll", "AMLL (TTML)", words = true, setAside = allSetAside("amll")))
        add(Source("lrcred", "lrc.red", words = true, setAside = allSetAside("lrcred")))
        add(Source("unison", "Unison", words = true, setAside = allSetAside("unison")))
        add(Source("qq", "QQ Music (QRC)", words = true, setAside = allSetAside("qq")))
        add(Source("lrcmux", "LRCMux", words = true, setAside = allSetAside("lrcmux")))
        add(Source("lyricsplus", "LyricsPlus", words = true, setAside = allSetAside(*LYRICS_PLUS.toTypedArray())))
        add(Source("kugou", "Kugou", words = true, setAside = allSetAside("kugou")))
        add(Source("lrclib", "LRCLIB", words = false, setAside = allSetAside("lrclib")))
        add(Source("netease", "NetEase", words = false, setAside = allSetAside("netease")))
        add(Source("genius", "Genius", words = false, setAside = allSetAside("genius")))
        add(Source("lyricsovh", "Lyrics.ovh", words = false, setAside = allSetAside("lyricsovh")))
        runCatching { com.music.spotui.extensions.Extensions.lyricsSources() }.getOrDefault(emptyList())
            .forEach { (id, name) -> add(Source("ext:$id", name)) }
    }

    fun sourceOf(lyrics: Lyrics): String? {
        val p = lyrics.provider.orEmpty().lowercase()
        return when {
            "spicy" in p -> "spicy"
            "amll" in p -> "amll"
            "lrc.red" in p || "lrcred" in p -> "lrcred"
            "unison" in p -> "unison"
            "qq" in p || "qrc" in p -> "qq"
            "lrcmux" in p -> "lrcmux"
            "lyricsplus" in p -> "lyricsplus"
            "kugou" in p -> "kugou"
            "lrclib" in p -> "lrclib"
            "netease" in p -> "netease"
            "genius" in p -> "genius"
            "lyrics.ovh" in p -> "lyricsovh"
            lyrics.colors != null -> "spotify"
            else -> null
        }
    }

    suspend fun fetchFrom(track: Track, sourceId: String): Lyrics? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val asked = track.title.trim()
        val title = cleanTitle(track.title)
        val artist = track.artist.substringBefore(",").trim()
        val durationMs = track.durationMs
        val durationSec = (durationMs / 1000).toInt()
        fun <T> byHand(block: () -> T): T { bypass.set(true); try { return block() } finally { bypass.remove() } }
        val spotify = if (sourceId in WORD_SOURCES) runCatching { fromSpotify(track) }.getOrNull() else null
        val found = try {
            when {
                sourceId == "spicy" -> byHand { fromSpicyLyrics(track) }
                sourceId == "spotify" -> fromSpotify(track)
                sourceId == "amll" -> byHand { fromAmll(track) }
                sourceId == "unison" -> byHand { fromUnison(track) }
                sourceId == "qq" -> byHand { fromBetterLyricsQQ(track) }
                sourceId == "lrcmux" -> byHand { fromLrcMux(track) }
                sourceId.startsWith("ext:") -> com.music.spotui.extensions.Extensions.lyricsFromExtension(
                    sourceId.removePrefix("ext:"), track.trackId, track.title, track.artist, track.album, durationMs, track.coverUrl)
                sourceId == "lyricsplus" -> byHand { fromLyricsPlus(asked, title, artist, durationMs) }
                sourceId == "lrcred" -> byHand { fromLrcRed(asked, title, artist, durationMs) }
                sourceId == "kugou" -> byHand { fromKugou(title, artist, durationMs) }
                sourceId == "lrclib" -> byHand {
                    lrclibGet(title, artist, track.album, durationSec)
                        ?: lrclibSearch("$LRCLIB/search?track_name=${enc(title)}&artist_name=${enc(artist)}", title, artist, durationSec)
                        ?: lrclibSearch("$LRCLIB/search?track_name=${enc(title)}", title, artist, durationSec)
                }
                sourceId == "netease" -> byHand { fromNetEase(title, artist, durationSec) }
                sourceId == "genius" -> byHand { fromGenius(title, artist) }
                sourceId == "lyricsovh" -> byHand { fromLyricsOvh(track) }
                else -> null
            }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { Log.d("LyricsApi", "$sourceId by hand: ${e.javaClass.simpleName}"); null }
        val lyrics = found?.takeIf { !it.isEmpty } ?: return@withContext null
        val chosen = if (lyrics.wordSynced && spotify != null) (alignTo(lyrics, spotify) ?: lyrics).copy(colors = spotify.colors) else lyrics
        cache[track.key] = chosen
        LyricsDisk.write(track.key, chosen)
        Log.d("LyricsApi", "${track.title}: ${chosen.lines.size} lines from ${chosen.provider} (chosen: $sourceId)")
        romanizeLater(track, chosen)
        chosen
    }

    private fun romanizeLater(track: Track, lyrics: Lyrics) {
        val wanted = LyricsRomanize.wanted(lyrics)
        if (wanted.isEmpty() || !runCatching { com.music.spotui.extensions.Extensions.hasRomanizer() }.getOrDefault(false)) return
        scope.launch {
            val found = runCatching {
                com.music.spotui.extensions.Extensions.romanizeFor(
                    track.trackId, track.title, track.artist, track.album, track.durationMs, track.coverUrl, lyrics.language, wanted)
            }.getOrDefault(emptyMap())
            val updated = LyricsRomanize.apply(lyrics, found)
            if (updated === lyrics) return@launch
            if (!cache.replace(track.key, lyrics, updated)) return@launch
            LyricsDisk.write(track.key, updated)
            romanized.value = Romanized(track.key, lyrics, updated)
        }
    }

    private suspend fun resolve(track: Track): Lyrics? = try { coroutineScope {
        val ext = if (extensionsAsked()) async {
            runCatching {
                com.music.spotui.extensions.Extensions.lyricsFor(track.trackId, track.title, track.artist, track.album, track.durationMs, track.coverUrl)
            }
        } else null

        // 1. FIRST — Spicy Lyrics API
        var spicyStatic: Lyrics? = null
        val spicy = asking("spicy", track) { fromSpicyLyrics(track) }
        if (spicy != null && !spicy.isEmpty) {
            if (spicy.synced && spicy.syncPrecision != SyncPrecision.STATIC) {
                return@coroutineScope spicy
            } else {
                spicyStatic = spicy
            }
        }

        // 2. PRIORITY TIER 1 — TTML
        val amll = asking("amll", track) { fromAmll(track) }
        if (amll != null && amll.synced) return@coroutineScope amll

        val red = asking("lrcred", track) { fromLrcRed(track.title, cleanTitle(track.title), track.artist, track.durationMs) }
        if (red != null && red.synced) return@coroutineScope red

        val unisonTtml = asking("unison", track) { fromUnison(track, requiredFormat = "ttml") }
        if (unisonTtml != null && unisonTtml.synced) return@coroutineScope unisonTtml

        // 3. PRIORITY TIER 2 — SYNCHRONIZED LYRICS
        val qq = asking("qq", track) { fromBetterLyricsQQ(track) }
        if (qq != null && qq.synced) return@coroutineScope qq

        val lrcmux = asking("lrcmux", track) { fromLrcMux(track) }
        if (lrcmux != null && lrcmux.synced) return@coroutineScope lrcmux

        val title = cleanTitle(track.title)
        val artist = track.artist.substringBefore(",").trim()
        val durationSec = (track.durationMs / 1000).toInt()
        val lrclib = asking("lrclib", track) {
            lrclibGet(title, artist, track.album, durationSec)
                ?: lrclibSearch("$LRCLIB/search?track_name=${enc(title)}&artist_name=${enc(artist)}", title, artist, durationSec)
                ?: lrclibSearch("$LRCLIB/search?track_name=${enc(title)}", title, artist, durationSec)
        }
        if (lrclib != null && lrclib.synced) return@coroutineScope lrclib

        val unisonLrc = asking("unison", track) { fromUnison(track, requiredFormat = "lrc") }
        if (unisonLrc != null && unisonLrc.synced) return@coroutineScope unisonLrc

        val own = if (wordSync) wordFirst(track) else fromSpotify(track) ?: lineFallbacks(track)
        val extAnswer = ext?.await()?.getOrNull()
        var result = com.music.spotui.extensions.LyricsMerge.choose(own, extAnswer)

        if (result != null && result.synced) return@coroutineScope result

        // 4. PRIORITY TIER 3 — UNSYNCHRONIZED LYRICS
        // 1. LRCLIB plainLyrics
        if (lrclib != null && !lrclib.isEmpty) return@coroutineScope lrclib

        // 2. Lyrics.ovh
        val lyricsovh = asking("lyricsovh", track) { fromLyricsOvh(track) }
        if (lyricsovh != null && !lyricsovh.isEmpty) return@coroutineScope lyricsovh

        // 3. Existing supplementary plain-text (Spotify plain, NetEase, Genius)
        if (result != null && !result.isEmpty) return@coroutineScope result

        val netease = if (extraSources) asking("netease", track) { fromNetEase(title, artist, durationSec) } else null
        if (netease != null && !netease.isEmpty) return@coroutineScope netease

        val genius = if (extraSources || !extraSourcesOff) asking("genius", track) { fromGenius(title, artist) } else null
        if (genius != null && !genius.isEmpty) return@coroutineScope genius

        // 4. Spicy Lyrics Static result as final fallback
        if (spicyStatic != null && !spicyStatic.isEmpty) return@coroutineScope spicyStatic

        val pending = ext?.isCompleted == false
        val finalRes = result ?: spicyStatic
        cache[track.key] = when {
            finalRes == null -> Miss()
            pending -> Partial(finalRes)
            else -> finalRes
        }
        if (!pending) finalRes?.let { LyricsDisk.write(track.key, it); keepIfDownloaded(track, it) }
        finalRes
    } } finally {
        earlyByKey.remove(track.key)
    }

    private object LyricsDisk {
        private const val MAX_FILES = 300
        private const val MAX_AGE_MS = 14 * 24 * 3_600_000L
        private val dir: java.io.File? by lazy {
            runCatching {
                val files = com.music.spotui.di.Graph.app.filesDir
                java.io.File(files, "lyrics_cache1").takeIf { it.exists() }?.deleteRecursively()
                java.io.File(files, "lyrics_cache2").also { it.mkdirs() }
            }.getOrNull()
        }
        private var writes = 0

        private fun file(key: String): java.io.File? {
            val sha = java.security.MessageDigest.getInstance("SHA-1").digest(key.toByteArray())
            return dir?.let { java.io.File(it, sha.joinToString("") { b -> "%02x".format(b) } + ".json") }
        }

        fun read(key: String): Lyrics? = runCatching {
            val f = file(key)?.takeIf { it.exists() } ?: return null
            val o = JSONObject(f.readText())
            if (o.getString("key") != key) return null
            if (System.currentTimeMillis() - o.getLong("at") > MAX_AGE_MS) { f.delete(); return null }
            f.setLastModified(System.currentTimeMillis())
            decode(o)
        }.onFailure { Log.w("LyricsApi", "unreadable lyrics file: ${it.javaClass.simpleName}") }.getOrNull()

        fun write(key: String, lyrics: Lyrics) {
            if (!lyrics.synced || !lyrics.uncapped || lyrics.isEmpty) return
            runCatching {
                val f = file(key) ?: return
                val o = encode(lyrics).put("key", key).put("at", System.currentTimeMillis())
                com.music.spotui.data.writeTextSafely(f, o.toString(), sync = false)
                if (++writes % 20 == 0) prune()
            }.onFailure { Log.w("LyricsApi", "could not keep lyrics: ${it.javaClass.simpleName}") }
        }

        fun drop(key: String) { runCatching { file(key)?.delete() } }

        fun clear() { runCatching { dir?.listFiles()?.forEach { it.delete() } } }

        private fun prune() {
            dir?.listFiles { f -> f.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() }?.drop(MAX_FILES)?.forEach { it.delete() }
        }

        fun encode(l: Lyrics) = JSONObject()
            .put("synced", l.synced)
            .put("language", l.language)
            .put("provider", l.provider)
            .put("trl", l.translationLanguage)
            .put("uploader", l.uploader)
            .put("maker", l.maker)
            .put("sourceUrl", l.sourceUrl)
            .put("syncPrecision", l.syncPrecision.name)
            .put("fetchedAtMs", l.fetchedAtMs)
            .put("songwriters", l.songwriters?.let { JSONArray(it) })
            .put("colors", l.colors?.let { JSONArray(listOf(it.background, it.text, it.highlight)) })
            .put("lines", JSONArray().also { arr ->
                l.lines.forEach { line ->
                    arr.put(JSONObject().put("t", line.timeMs).put("x", line.text).also { lo ->
                        line.words?.let { ws -> lo.put("w", wordsJson(ws)) }
                        line.translation?.let { lo.put("tr", it) }
                        line.romanization?.let { lo.put("ro", it) }
                        line.voice?.let { lo.put("v", it) }
                        line.background?.let { lo.put("bg", it) }
                        line.backgroundWords?.let { lo.put("bw", wordsJson(it)) }
                    })
                }
            })

        private fun wordsJson(ws: List<LyricWord>) = JSONArray().also { wa -> ws.forEach { w -> wa.put(JSONArray(listOf(w.startMs, w.endMs, w.text))) } }

        private fun wordsOf(wa: JSONArray?) = wa?.let { List(it.length()) { j -> it.getJSONArray(j).let { w -> LyricWord(w.getLong(0), w.getLong(1), w.getString(2)) } } }

        fun decode(o: JSONObject): Lyrics {
            val colors = o.optJSONArray("colors")?.let { LyricsColors(it.getInt(0), it.getInt(1), it.getInt(2)) }
            val lines = o.getJSONArray("lines").let { arr ->
                List(arr.length()) { i ->
                    val lo = arr.getJSONObject(i)
                    LyricLine(lo.getLong("t"), lo.getString("x"), wordsOf(lo.optJSONArray("w")),
                        translation = lo.optString("tr").ifEmpty { null }, romanization = lo.optString("ro").ifEmpty { null },
                        voice = if (lo.has("v")) lo.optInt("v") else null, background = lo.optString("bg").ifEmpty { null },
                        backgroundWords = wordsOf(lo.optJSONArray("bw")))
                }
            }
            val uploader = o.optString("uploader").takeIf { it.isNotEmpty() && it != "null" }
            val maker = o.optString("maker").takeIf { it.isNotEmpty() && it != "null" }
            val sourceUrl = o.optString("sourceUrl").takeIf { it.isNotEmpty() && it != "null" }
            val songwriters = o.optJSONArray("songwriters")?.let { arr -> List(arr.length()) { j -> arr.getString(j) } }
            val precName = o.optString("syncPrecision").takeIf { it.isNotEmpty() && it != "null" }
            val prec = precName?.let { runCatching { SyncPrecision.valueOf(it) }.getOrNull() }
                ?: if (lines.any { it.words != null }) SyncPrecision.SYLLABLE else if (o.getBoolean("synced")) SyncPrecision.LINE else SyncPrecision.STATIC
            val fetchedAtMs = o.optLong("fetchedAtMs", System.currentTimeMillis())

            if ("Spicy Lyrics" in (o.optString("provider") ?: "") && uploader != null) {
                if (System.currentTimeMillis() - fetchedAtMs > 30L * 24 * 3600 * 1000) {
                    return Lyrics(lines = emptyList(), synced = false)
                }
            }

            return Lyrics(
                lines = lines,
                synced = o.getBoolean("synced"),
                language = o.optString("language").takeIf { it.isNotEmpty() && it != "null" },
                provider = o.optString("provider").takeIf { it.isNotEmpty() && it != "null" },
                colors = colors,
                translationLanguage = o.optString("trl").takeIf { it.isNotEmpty() && it != "null" },
                uploader = uploader,
                maker = maker,
                sourceUrl = sourceUrl,
                songwriters = songwriters,
                syncPrecision = prec,
                fetchedAtMs = fetchedAtMs,
            )
        }
    }

    private fun kept(track: Track): Lyrics? = runCatching {
        val f = com.music.spotui.data.offline.OfflineStore.lyricsFile(track.trackId)?.takeIf { it.isFile } ?: return null
        LyricsDisk.decode(JSONObject(f.readText()))
    }.getOrNull()?.takeUnless { it.isEmpty }?.also {
        cache[track.key] = Partial(it)
        Log.d("LyricsApi", "${track.title}: ${it.lines.size} lines kept with the download")
    }

    internal fun keepable(lyrics: Lyrics): Boolean = !lyrics.isEmpty && lyrics.uncapped

    private fun keep(track: Track, lyrics: Lyrics) {
        if (!keepable(lyrics)) return
        runCatching {
            val f = com.music.spotui.data.offline.OfflineStore.lyricsFile(track.trackId) ?: return
            f.parentFile?.mkdirs()
            com.music.spotui.data.writeTextSafely(f, LyricsDisk.encode(lyrics).toString(), sync = false)
        }.onFailure { Log.w("LyricsApi", "could not keep lyrics with the download: ${it.javaClass.simpleName}") }
    }

    private fun keepIfDownloaded(track: Track, lyrics: Lyrics) {
        if (!runCatching { com.music.spotui.data.preferences.LyricsOfflinePref.on &&
                com.music.spotui.data.offline.OfflineStore.isDownloaded(track.trackId) }.getOrDefault(false)) return
        keep(track, lyrics)
    }

    private val keeping = kotlinx.coroutines.sync.Mutex()

    fun keepForOffline(track: Track) {
        if (track.trackId.isBlank()) return
        scope.launch {
            keeping.lock()
            try {
                val lyrics = runCatching { fetch(track) }.getOrNull() ?: return@launch
                keep(track, lyrics)
            } finally { keeping.unlock() }
        }
    }

    private fun online(): Boolean =
        runCatching { com.music.spotui.data.offline.OfflineStore.isOnline(com.music.spotui.di.Graph.app) }.getOrDefault(true)

    private fun extensionsAsked(): Boolean = runCatching { com.music.spotui.extensions.Extensions.hasLyrics() }.getOrDefault(false)

    private suspend fun <T> within(ms: Long, block: () -> T?): T? =
        kotlinx.coroutines.withTimeoutOrNull(ms) { scope.async { block() }.await() }

    private suspend fun withBacks(to: Lyrics, vararg asked: kotlinx.coroutines.Deferred<Lyrics?>): Lyrics {
        if (LyricsExtras.hasBacks(to)) return to
        for (a in asked) {
            val from = a.await()?.takeIf { LyricsExtras.hasBacks(it) } ?: continue
            return LyricsExtras.carry(to, from)
        }
        return to
    }

    private suspend fun wordFirst(track: Track): Lyrics? = coroutineScope {
        val asked = track.title.trim()
        val title = cleanTitle(track.title)
        val artist = track.artist.substringBefore(",").trim()
        val durationMs = track.durationMs
        val spotify = async { fromSpotify(track) }
        val plus = async { within(WORD_WAIT_MS) { asking("lyricsplus", track) { fromLyricsPlus(asked, title, artist, durationMs) } } }
        val red = async { within(WORD_WAIT_MS) { asking("lrcred", track) { fromLrcRed(asked, title, artist, durationMs) } } }
        val kugou = async { within(WORD_WAIT_MS) { asking("kugou", track) { fromKugou(title, artist, durationMs) } } }
        val sp = spotify.await()
        if (sp != null) showEarly(track, sp)
        if (sp != null && sp.wordSynced && sp.uncapped) {
            kugou.cancel()
            return@coroutineScope withBacks(sp, plus, red)
        }
        val words = (plus.await() ?: red.await() ?: kugou.await())?.takeIf { it.wordSynced }
        when {
            words != null -> alignTo(words, sp)?.copy(colors = sp?.colors)?.let { withBacks(it, plus, red) } ?: sp
            sp != null -> sp
            else -> lineFallbacks(track)
        }
    }

    private fun alignTo(words: Lyrics, spotify: Lyrics?): Lyrics? {
        if (spotify == null || !spotify.synced) return words
        fun uniqueKeys(lines: List<LyricLine>) =
            lines.map { matchKey(it.text) }.filter { it.length >= 6 }.groupingBy { it }.eachCount().filterValues { it == 1 }.keys
        val theirs = spotify.lines.filter { it.timeMs > 0 && matchKey(it.text).length >= 6 }
        val shared = uniqueKeys(theirs) intersect uniqueKeys(words.lines)
        val theirAt = theirs.associateBy { matchKey(it.text) }
        val gaps = words.lines.mapNotNull { l -> theirAt[matchKey(l.text)]?.takeIf { matchKey(l.text) in shared }?.let { it.timeMs - l.timeMs } }
        if (gaps.size < 3) {
            if (gaps.isEmpty()) return words
            Log.d("LyricsApi", "${words.provider}: only ${gaps.size} lines to compare with Spotify's timing, Spotify's lines kept")
            return null
        }
        val shift = gaps.sorted()[gaps.size / 2]
        val agreeing = gaps.count { kotlin.math.abs(it - shift) <= ALIGN_TOLERANCE_MS }
        if (agreeing < maxOf(3, (gaps.size * 0.6).toInt()) || kotlin.math.abs(shift) > MAX_ALIGN_SHIFT_MS) {
            Log.d("LyricsApi", "${words.provider}: timing off Spotify's by $shift ms, $agreeing of ${gaps.size} lines agree: Spotify's lines kept")
            return null
        }
        if (kotlin.math.abs(shift) < 60) return words
        Log.d("LyricsApi", "${words.provider}: moved by $shift ms to Spotify's timing ($agreeing of ${gaps.size} lines)")
        return words.copy(lines = words.lines.map { l ->
            l.copy(timeMs = (l.timeMs + shift).coerceAtLeast(0), words = l.words?.map { w ->
                w.copy(startMs = (w.startMs + shift).coerceAtLeast(0), endMs = if (w.endMs > 0) (w.endMs + shift).coerceAtLeast(0) else 0)
            })
        })
    }

    private const val ALIGN_TOLERANCE_MS = 1_500L
    private const val MAX_ALIGN_SHIFT_MS = 20_000L

    private suspend fun lineFallbacks(track: Track): Lyrics? = coroutineScope {
        val title = cleanTitle(track.title)
        val artist = track.artist.substringBefore(",").trim()
        val durationSec = (track.durationMs / 1000).toInt()
        val netease = if (extraSources) async { asking("netease", track) { fromNetEase(title, artist, durationSec) } } else null
        val genius = if (extraSources) async { asking("genius", track) { fromGenius(title, artist) } } else null
        val lrclib = asking("lrclib", track) {
            val exact = async { lrclibGet(title, artist, track.album, durationSec) }
            val fuzzy = async { lrclibSearch("$LRCLIB/search?track_name=${enc(title)}&artist_name=${enc(artist)}", title, artist, durationSec) }
            exact.await() ?: fuzzy.await()
                ?: lrclibSearch("$LRCLIB/search?track_name=${enc(title)}", title, artist, durationSec)
        }
        val ne = netease?.await()
        when {
            lrclib != null && lrclib.synced -> LyricsExtras.carry(lrclib, ne)
            ne != null && ne.synced -> ne
            else -> asking("kugou", track) { fromKugou(title, artist, track.durationMs) } ?: lrclib ?: ne ?: genius?.await()
        }
    }

    // ── Spicy Lyrics ──
    private fun fromSpicyLyrics(track: Track): Lyrics? {
        val headers = HashMap<String, String>()
        val pk = ApiSecrets.spicyLyricsPublishableKey
        if (pk.isNotBlank()) headers["Authorization"] = "Bearer $pk"

        val queryId = track.trackId.ifBlank { null }
        val body = if (queryId != null) {
            httpGet("https://api.spicylyrics.org/v1/lyrics/${enc(queryId)}", timeoutMs = 6000, provider = "spicy", headers = headers)
        } else {
            httpGet("https://api.spicylyrics.org/v1/lyrics/search?title=${enc(cleanTitle(track.title))}&artist=${enc(track.artist)}", timeoutMs = 6000, provider = "spicy", headers = headers)
        } ?: return null

        return parseSpicyLyricsJson(body)
    }

    internal fun parseSpicyLyricsJson(jsonStr: String): Lyrics? {
        val root = runCatching { JSONObject(jsonStr) }.getOrNull() ?: return null
        val o = root.optJSONObject("Body") ?: root
        val typeStr = o.optString("Type").ifEmpty { o.optString("type") }
        val sourceStr = o.optString("source").ifEmpty { "spicy_lyrics" }
        val uploadAttr = o.optJSONObject("UploadAttribution")
        val uploader = uploadAttr?.optString("uploader")?.ifBlank { null }
        val maker = uploadAttr?.optString("maker")?.ifBlank { null }
        val songwritersArr = o.optJSONArray("SongWriters")
        val songwriters = songwritersArr?.let { List(it.length()) { i -> it.getString(i) } }
        val contentArr = o.optJSONArray("Content") ?: o.optJSONArray("lines") ?: return null

        val isSyllable = typeStr.equals("Syllable", ignoreCase = true)
        val isLine = typeStr.equals("Line", ignoreCase = true)
        val isStatic = typeStr.equals("Static", ignoreCase = true)

        val lines = ArrayList<LyricLine>()
        for (i in 0 until contentArr.length()) {
            val item = contentArr.optJSONObject(i) ?: continue
            val secStart = item.optDouble("StartTime", item.optDouble("Time", item.optDouble("start", 0.0)))
            val timeMs = (secStart * 1000).toLong()

            val text = item.optString("Text").ifEmpty { item.optString("text").ifEmpty { item.optString("Content") } }.trim()
            val tr = item.optString("Translation").ifEmpty { item.optString("translation") }.ifBlank { null }
            val ro = item.optString("Romanization").ifEmpty { item.optString("romanization") }.ifBlank { null }
            val singer = item.optString("Singer").ifEmpty { item.optString("singer") }

            val sylArr = item.optJSONArray("Syllables") ?: item.optJSONArray("Words")
            var words: List<LyricWord>? = null
            var bgWords: List<LyricWord>? = null
            var bgText: String? = null

            if (sylArr != null && sylArr.length() > 0) {
                val mainW = ArrayList<LyricWord>()
                val bgW = ArrayList<LyricWord>()
                for (j in 0 until sylArr.length()) {
                    val sObj = sylArr.optJSONObject(j) ?: continue
                    val sStart = (sObj.optDouble("StartTime", sObj.optDouble("time", 0.0)) * 1000).toLong()
                    val sEnd = (sObj.optDouble("EndTime", sObj.optDouble("end", 0.0)) * 1000).toLong()
                    val sText = sObj.optString("Text").ifEmpty { sObj.optString("text") }
                    val isBg = sObj.optBoolean("isBackground", false)
                    val lw = LyricWord(sStart, sEnd, sText)
                    if (isBg) bgW.add(lw) else mainW.add(lw)
                }
                if (mainW.isNotEmpty()) words = mainW
                if (bgW.isNotEmpty()) {
                    bgWords = bgW
                    bgText = bgW.joinToString("") { it.text }.trim().removeSurrounding("(", ")")
                }
            }

            if (text.isNotEmpty() || words != null) {
                val lineText = text.ifEmpty { words?.joinToString("") { it.text }?.trim() ?: "" }
                lines.add(
                    LyricLine(
                        timeMs = if (isStatic) 0L else timeMs,
                        text = lineText,
                        words = words,
                        translation = tr,
                        romanization = ro,
                        voice = voiceOf(singer),
                        background = bgText,
                        backgroundWords = bgWords,
                    )
                )
            }
        }

        if (lines.isEmpty()) return null

        val syncPrecision = when {
            isSyllable || lines.any { it.words != null } -> SyncPrecision.SYLLABLE
            isLine || lines.any { it.timeMs > 0 } -> SyncPrecision.LINE
            else -> SyncPrecision.STATIC
        }

        val providerName = if (sourceStr.contains("spicy", ignoreCase = true)) "Spicy Lyrics" else "Spicy Lyrics · $sourceStr"
        val sourceUrl = "https://spicylyrics.org"

        return Lyrics(
            lines = lines,
            synced = syncPrecision != SyncPrecision.STATIC,
            provider = providerName,
            uploader = uploader,
            maker = maker,
            sourceUrl = sourceUrl,
            songwriters = songwriters,
            syncPrecision = syncPrecision,
        )
    }

    // ── AMLL TTML Database ──
    private fun fromAmll(track: Track): Lyrics? {
        val title = cleanTitle(track.title)
        val artist = track.artist.substringBefore(",").trim()
        val searchUrl = "https://api.amll.dev/v1/lyrics/search?q=${enc("$title $artist")}"
        val searchBody = httpGet(searchUrl, timeoutMs = 6000, provider = "amll") ?: return null
        val root = runCatching { JSONObject(searchBody) }.getOrNull() ?: return null
        val items = root.optJSONArray("data") ?: root.optJSONArray("results") ?: return null
        var matchedTtml: String? = null
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val candTitle = item.optString("title").ifEmpty { item.optString("trackName") }
            val candArtist = item.optString("artist").ifEmpty { item.optString("artistName") }
            val durSec = item.optInt("duration", 0)
            if (isPlausibleMatch(candTitle, candArtist, durSec, title, artist, (track.durationMs / 1000).toInt())) {
                matchedTtml = item.optString("ttml").takeIf { it.isNotBlank() }
                if (matchedTtml == null) {
                    val id = item.optString("id")
                    if (id.isNotBlank()) {
                        matchedTtml = httpGet("https://api.amll.dev/v1/lyrics/get?id=${enc(id)}", timeoutMs = 6000, provider = "amll")
                    }
                }
                if (!matchedTtml.isNullOrBlank()) break
            }
        }
        if (matchedTtml.isNullOrBlank()) return null
        return parseTtml(matchedTtml)?.copy(provider = "AMLL (TTML)")
    }

    // ── Unison (TTML or LRC) ──
    private fun fromUnison(track: Track, requiredFormat: String? = null): Lyrics? {
        val title = cleanTitle(track.title)
        val artist = track.artist.substringBefore(",").trim()
        val url = "https://unison.boidu.dev/lyrics?song=${enc(title)}&artist=${enc(artist)}&al=${enc(track.album)}&d=${track.durationMs / 1000}"
        val body = httpGet(url, timeoutMs = 6000, provider = "unison") ?: return null
        val o = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (o.optBoolean("success") == false) return null
        val data = o.optJSONObject("data") ?: return null
        val format = data.optString("format").lowercase()
        if (requiredFormat != null && format != requiredFormat.lowercase()) return null
        val lyricsContent = data.optString("lyrics").takeIf { it.isNotBlank() } ?: return null

        return when (format) {
            "ttml" -> parseTtml(lyricsContent)?.copy(provider = "Unison (TTML)")
            "lrc" -> {
                val lines = parseLrc(lyricsContent)
                if (lines.isNotEmpty()) Lyrics(lines, synced = true, provider = "Unison (LRC)") else null
            }
            else -> {
                val lines = lyricsContent.lines().map { LyricLine(0L, it.trim()) }.filter { it.text.isNotEmpty() }
                if (lines.isNotEmpty()) Lyrics(lines, synced = false, provider = "Unison (Plain)") else null
            }
        }
    }

    // ── Better Lyrics (QQ QRC XML) ──
    private fun fromBetterLyricsQQ(track: Track): Lyrics? {
        val title = cleanTitle(track.title)
        val artist = track.artist.substringBefore(",").trim()
        val url = "https://api.betterlyrics.org/qq/getLyrics?s=${enc(title)}&a=${enc(artist)}&al=${enc(track.album)}&d=${track.durationMs / 1000}"
        val body = httpGet(url, timeoutMs = 6000, provider = "qq") ?: return null
        val o = runCatching { JSONObject(body) }.getOrNull()
        val qrcContent = o?.optString("qrc").takeIf { !it.isNullOrBlank() }
            ?: o?.optString("lyrics").takeIf { !it.isNullOrBlank() }
            ?: body
        return parseQrc(qrcContent)
    }

    private val qrcLinePattern = Regex("""^\[(\d+),(\d+)\](.*)$""")
    private val qrcWordPattern = Regex("""<(\d+),(\d+),\d+>([^<]*)""")

    internal fun parseQrc(qrc: String): Lyrics? {
        val lines = ArrayList<LyricLine>()
        for (raw in qrc.lines()) {
            val trimmed = raw.trim()
            val m = qrcLinePattern.find(trimmed)
            if (m != null) {
                val lineStartMs = m.groupValues[1].toLong()
                val content = m.groupValues[3]
                val wordMatches = qrcWordPattern.findAll(content).toList()
                if (wordMatches.isNotEmpty()) {
                    val words = wordMatches.map { w ->
                        val wStart = lineStartMs + w.groupValues[1].toLong()
                        val wDur = w.groupValues[2].toLong()
                        LyricWord(wStart, wStart + wDur, w.groupValues[3])
                    }
                    val lineText = words.joinToString("") { it.text }.trim()
                    if (lineText.isNotEmpty()) {
                        lines.add(LyricLine(lineStartMs, lineText, words))
                    }
                } else {
                    val lineText = content.replace(Regex("""<[^>]*>"""), "").trim()
                    if (lineText.isNotEmpty()) {
                        lines.add(LyricLine(lineStartMs, lineText))
                    }
                }
            } else if (trimmed.startsWith("[")) {
                val lrcLines = parseLrc(trimmed)
                lines.addAll(lrcLines)
            }
        }
        if (lines.isEmpty()) return null
        return Lyrics(lines = lines.sortedBy { it.timeMs }, synced = true, provider = "QQ Music · QRC", syncPrecision = if (lines.any { it.words != null }) SyncPrecision.SYLLABLE else SyncPrecision.LINE)
    }

    // ── LRCMux ──
    private fun fromLrcMux(track: Track): Lyrics? {
        val title = cleanTitle(track.title)
        val artist = track.artist.substringBefore(",").trim()
        val url = "https://api.lrcmux.dev/v1/lyrics?title=${enc(title)}&artist=${enc(artist)}&duration=${track.durationMs / 1000}"
        val body = httpGet(url, timeoutMs = 6000, provider = "lrcmux") ?: return null
        val o = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val synced = o.optBoolean("synced", false) || o.optString("syncedLyrics").isNotBlank()
        val lrcText = o.optString("syncedLyrics").ifEmpty { o.optString("lyrics") }
        if (lrcText.isBlank()) return null
        val lines = parseLrc(lrcText)
        if (lines.isEmpty()) return null
        return Lyrics(lines = lines, synced = synced, provider = "LRCMux")
    }

    // ── Lyrics.ovh ──
    private fun fromLyricsOvh(track: Track): Lyrics? {
        val title = cleanTitle(track.title)
        val artist = track.artist.substringBefore(",").trim()
        if (title.isBlank() || artist.isBlank()) return null
        val url = "https://api.lyrics.ovh/v1/${enc(artist)}/${enc(title)}"
        val body = httpGet(url, timeoutMs = 6000, provider = "lyricsovh") ?: return null
        val o = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val text = o.optString("lyrics").trim()
        if (text.isBlank()) return null
        val lines = text.lines().map { LyricLine(0L, it.trim()) }.filter { it.text.isNotEmpty() }
        if (lines.isEmpty()) return null
        return Lyrics(lines = lines, synced = false, provider = "Lyrics.ovh")
    }

    // ── Spotify color-lyrics ──
    private suspend fun fromSpotify(track: Track): Lyrics? {
        if (track.trackId.isBlank()) return null
        val sp = runCatching { app.myspoti.host.android.MyspotiRuntime.api.extras.lyrics("spotify:track:${track.trackId}") }
            .onFailure { Log.d("LyricsApi", "spotify lyrics miss for ${track.trackId}: ${it.message}") }
            .getOrNull() ?: return null
        if (sp.lines.isEmpty()) return null
        val bg = sp.background
        val text = sp.text
        val hi = sp.highlight
        return Lyrics(
            lines = sp.lines.map { l -> LyricLine(l.startMs, l.text, l.words?.map { w -> LyricWord(w.startMs, w.startMs, w.text) }) },
            synced = sp.synced,
            language = sp.language,
            provider = sp.provider ?: "Spotify",
            colors = if (bg != null && text != null && hi != null) LyricsColors(bg, text, hi) else null,
        )
    }

    // ── NetEase Cloud Music ──
    private fun fromNetEase(title: String, artist: String, durationSec: Int): Lyrics? {
        val search = httpGet("https://music.163.com/api/search/get?s=${enc("$title $artist")}&type=1&limit=6", timeoutMs = 6000, provider = "netease")
            ?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return null
        val songs = search.optJSONObject("result")?.optJSONArray("songs") ?: return null
        for (i in 0 until songs.length()) {
            val song = songs.optJSONObject(i) ?: continue
            val songArtist = song.optJSONArray("artists")?.optJSONObject(0)?.optString("name").orEmpty()
            if (!isPlausibleMatch(song.optString("name"), songArtist, (song.optLong("duration") / 1000).toInt(), title, artist, durationSec)) continue
            val o = httpGet("https://music.163.com/api/song/lyric?id=${song.optLong("id")}&lv=1&kv=1&tv=-1&rv=-1", timeoutMs = 6000, provider = "netease")
                ?.let { runCatching { JSONObject(it) }.getOrNull() } ?: continue
            val lrc = o.optJSONObject("lrc")?.optString("lyric").orEmpty()
            val plain = parseLrc(lrc).filter { it.text.isNotBlank() && !it.text.contains(" : ") }
            if (plain.isEmpty()) continue
            val lines = LyricsExtras.attach(plain,
                parseLrc(o.optJSONObject("tlyric")?.optString("lyric").orEmpty()),
                parseLrc(o.optJSONObject("romalrc")?.optString("lyric").orEmpty()))
            return Lyrics(lines = lines, synced = lines.any { it.timeMs > 0 }, provider = "NetEase Cloud Music",
                translationLanguage = if (lines.any { it.translation != null }) "zh" else null)
        }
        return null
    }

    // ── Genius ──
    private fun fromGenius(title: String, artist: String): Lyrics? {
        val search = httpGet("https://genius.com/api/search/song?per_page=10&q=${enc("$title $artist")}", timeoutMs = 6000, provider = "genius")
            ?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return null
        val hits = search.optJSONObject("response")?.optJSONArray("sections")?.optJSONObject(0)?.optJSONArray("hits") ?: return null
        for (i in 0 until hits.length()) {
            val r = hits.optJSONObject(i)?.optJSONObject("result") ?: continue
            val primary = r.optJSONObject("primary_artist")?.optString("name").orEmpty()
            if (!geniusMatch(r.optString("title"), r.optString("artist_names"), primary, title, artist)) continue
            val html = httpGet(r.optString("url"), timeoutMs = 8000, provider = "genius") ?: continue
            val text = geniusText(html) ?: continue
            val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() && !(it.startsWith("[") && it.endsWith("]")) }
            if (lines.isEmpty()) continue
            return Lyrics(lines = lines.map { LyricLine(0, it) }, synced = false, provider = "Genius")
        }
        return null
    }

    internal fun geniusMatch(candTitle: String, artistNames: String, primaryArtist: String, title: String, artist: String): Boolean {
        val names = artistNames.replace(geniusAside, "")
        val artists = names.split(',', '&', '/') + names + primaryArtist.replace(geniusAside, "")
        if (matchKey(artist).isEmpty() || artists.none { it.isNotBlank() && LyricsMatch.sameArtist(it.trim(), artist) }) return false
        fun bare(t: String) = cleanTitle(t).let { c -> matchKey(c.replace(lastAside, "")).ifEmpty { matchKey(c) } }
        return overlaps(bare(candTitle), bare(title))
    }

    private val geniusAside = Regex("""\s*\([^)]*\)""")
    private val lastAside = Regex("""\s*\([^)]*\)\s*$""")
    private val geniusContainer = Regex("<div[^>]*data-lyrics-container=\"true\"[^>]*>", RegexOption.IGNORE_CASE)
    private val geniusHeader = Regex("<div[^>]*data-exclude-from-selection=\"true\"[^>]*>", RegexOption.IGNORE_CASE)
    private val spaces = Regex("\\s+")
    private val htmlBreak = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)

    private fun divEnd(html: String, from: Int): Int {
        var depth = 1
        var i = from
        while (i < html.length) {
            when {
                html.startsWith("<div", i, ignoreCase = true) -> depth++
                html.startsWith("</div", i, ignoreCase = true) -> { depth--; if (depth == 0) return i }
            }
            i++
        }
        return -1
    }

    internal fun withoutGeniusHeader(part: String): String {
        var out = part
        while (true) {
            val m = geniusHeader.find(out) ?: return out
            val end = divEnd(out, m.range.last + 1).takeIf { it >= 0 } ?: return out
            val close = out.indexOf('>', end).takeIf { it >= 0 } ?: return out
            out = out.removeRange(m.range.first, close + 1)
        }
    }

    internal fun geniusText(html: String): String? {
        val parts = geniusContainer.findAll(html).mapNotNull { m ->
            val start = m.range.last + 1
            divEnd(html, start).takeIf { it >= 0 }?.let { html.substring(start, it) }
        }.toList()
        if (parts.isEmpty()) return null
        return parts.joinToString("\n") { part ->
            androidx.core.text.HtmlCompat.fromHtml(withoutGeniusHeader(part).replace(htmlBreak, "\n"), androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY)
                .toString()
        }.trim().takeIf { it.isNotEmpty() }
    }

    // ── LRCLIB ──
    private fun lrclibGet(title: String, artist: String, album: String, durationSec: Int): Lyrics? {
        val url = buildString {
            append("$LRCLIB/get?track_name=").append(enc(title))
            append("&artist_name=").append(enc(artist))
            if (album.isNotBlank()) append("&album_name=").append(enc(album))
            if (durationSec > 0) append("&duration=").append(durationSec)
        }
        val o = httpGet(url, provider = "lrclib")?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return null
        if (!isPlausibleMatch(o.optString("trackName"), o.optString("artistName"), o.optInt("duration"), title, artist, durationSec)) return null
        return parseLrclib(o)
    }

    private fun lrclibSearch(url: String, title: String, artist: String, durationSec: Int): Lyrics? {
        val arr = httpGet(url, provider = "lrclib")?.let { runCatching { JSONArray(it) }.getOrNull() } ?: return null
        var best: JSONObject? = null
        var bestScore = Long.MAX_VALUE
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val dur = o.optInt("duration", 0)
            if (!isPlausibleMatch(o.optString("trackName"), o.optString("artistName"), dur, title, artist, durationSec)) continue
            val hasSynced = o.optString("syncedLyrics").isNotBlank() && !o.isNull("syncedLyrics")
            val diff = if (durationSec > 0 && dur > 0) kotlin.math.abs(dur - durationSec).toLong() else 0L
            val score = diff + (if (hasSynced) 0 else 100_000)
            if (score < bestScore) { bestScore = score; best = o }
        }
        return best?.let { parseLrclib(it) }
    }

    private fun parseLrclib(o: JSONObject): Lyrics? {
        val synced = o.optString("syncedLyrics").takeIf { it.isNotBlank() && !o.isNull("syncedLyrics") }
        if (synced != null) {
            val lines = parseLrc(synced)
            if (lines.isNotEmpty()) return Lyrics(lines, synced = true, provider = "LRCLIB")
        }
        val plain = o.optString("plainLyrics").takeIf { it.isNotBlank() && !o.isNull("plainLyrics") } ?: return null
        return Lyrics(plain.split("\n").map { LyricLine(0L, it) }, synced = false, provider = "LRCLIB")
    }

    // ── LyricsPlus ──
    private val LYRICS_PLUS = listOf("https://lyricsplus.binimum.org", "https://lyricsplus.prjktla.my.id")

    private fun fromLyricsPlus(asked: String, title: String, artist: String, durationMs: Long): Lyrics? {
        val q = "title=${enc(asked)}&artist=${enc(artist)}" + if (durationMs > 0) "&duration=${durationMs / 1000}" else ""
        for (base in LYRICS_PLUS) {
            val body = httpGet("$base/v2/lyrics/get?$q", timeoutMs = 5000, provider = base) ?: continue
            val o = runCatching { JSONObject(body) }.getOrNull() ?: continue
            val meta = o.optJSONObject("metadata")
            if (meta != null && !sameSong(meta.optString("title"), meta.optString("artist"), parseClock(meta.optString("totalDuration")), title, artist, durationMs)) return null
            return lyricsPlusLines(o) ?: continue
        }
        return null
    }

    internal fun lyricsPlusLines(o: JSONObject): Lyrics? {
        val meta = o.optJSONObject("metadata")
        val arr = o.optJSONArray("lyrics") ?: return null
        var translationLanguage: String? = null
        val lines = ArrayList<LyricLine>()
        for (i in 0 until arr.length()) {
            val l = arr.optJSONObject(i) ?: continue
            val syl = l.optJSONArray("syllabus")
            val all = syl?.let { s -> (0 until s.length()).mapNotNull { j -> s.optJSONObject(j) } }.orEmpty()
            fun word(w: JSONObject): LyricWord { val t = w.optLong("time"); return LyricWord(t, t + w.optLong("duration"), w.optString("text")) }
            var words = all.filterNot { it.optBoolean("isBackground") }.map(::word).takeIf { it.isNotEmpty() }
            var back = all.filter { it.optBoolean("isBackground") }.map(::word).takeIf { it.isNotEmpty() }
            if (words == null && back != null) { words = back; back = null }
            val text = words?.joinToString("") { it.text }?.trim() ?: l.optString("text").trim()
            if (text.isEmpty()) continue
            val tr = l.opt("translation")
            val translation = textOf(tr)?.takeIf { LyricsMatch.key(it) != LyricsMatch.key(text) }
            if (translation != null && translationLanguage == null) translationLanguage = (tr as? JSONObject)?.optString("lang")?.ifBlank { null }
            val roman = textOf(l.opt("transliteration"))?.takeIf { LyricsMatch.key(it) != LyricsMatch.key(text) }
            lines += LyricLine(l.optLong("time"), text, words, translation, roman,
                voice = voiceOf(l.optJSONObject("element")?.optString("singer")),
                background = back?.joinToString("") { it.text }?.let(::backingText), backgroundWords = back)
        }
        if (lines.isEmpty()) return null
        val source = meta?.optString("source").orEmpty()
        return Lyrics(lines, synced = true, language = meta?.optString("language")?.takeIf { it.isNotBlank() },
            provider = if (source.contains("apple", true)) "Apple Music · LyricsPlus" else "LyricsPlus",
            translationLanguage = translationLanguage.takeIf { lines.any { it.translation != null } })
    }

    private fun textOf(v: Any?): String? =
        when (v) { is String -> v; is JSONObject -> v.optString("text"); else -> null }?.trim()?.ifEmpty { null }

    internal fun voiceOf(agent: String?): Int? =
        agent?.trim()?.removePrefix("v")?.toIntOrNull()?.takeIf { it >= 0 }?.let { if (it >= 1000) 0 else it }

    private fun backingText(s: String): String? = s.replace(spaces, " ").trim().removeSurrounding("(", ")").trim().ifEmpty { null }

    // ── lrc.red ──
    private fun fromLrcRed(asked: String, title: String, artist: String, durationMs: Long): Lyrics? {
        val search = httpGet("https://lrc.red/search.json?q=${enc("$asked $artist")}", timeoutMs = 5000, provider = "lrcred") ?: return null
        val hits = runCatching { JSONObject(search).optJSONArray("hits") }.getOrNull() ?: return null
        val hit = (0 until hits.length()).mapNotNull { hits.optJSONObject(it) }.firstOrNull {
            sameSong(it.optString("title"), it.optString("artist"), (it.optDouble("duration", 0.0) * 1000).toLong(), title, artist, durationMs)
        } ?: return null
        val isrc = hit.optString("isrc").takeIf { it.isNotBlank() } ?: return null
        val ttml = httpGet("https://lrc.red/s/${enc(isrc)}.ttml", timeoutMs = 6000, provider = "lrcred") ?: return null
        return parseTtml(ttml)?.copy(provider = "Apple Music · lrc.red", sourceUrl = "https://lrc.red")
    }

    internal fun parseTtml(xml: String, newParser: () -> org.xmlpull.v1.XmlPullParser = { android.util.Xml.newPullParser() }): Lyrics? = runCatching {
        val p = newParser()
        p.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        p.setInput(java.io.StringReader(xml))
        fun attr(name: String) = p.getAttributeValue(null, name)
        fun lineKey() = (0 until p.attributeCount).firstOrNull { p.getAttributeName(it).let { n -> n == "key" || n.endsWith(":key") } }?.let { p.getAttributeValue(it) }
        fun tidy(sb: CharSequence) = sb.toString().replace(spaces, " ").trim()
        val headTranslation = HashMap<String, String>(); val headRoman = HashMap<String, String>()
        var translationLanguage: String? = null
        var headKind = 0; var headSeenTr = false; var headSeenRo = false
        var headKey: String? = null; var headText = StringBuilder(); var headSkip = 0
        val lines = ArrayList<LyricLine>()
        var lineStart = -1L
        var words: MutableList<LyricWord>? = null
        var plain = StringBuilder()
        var wordStart = -1L; var wordEnd = 0L; var word = StringBuilder()
        var role: String? = null; var roleDepth = 0
        var key: String? = null; var voice: Int? = null
        var bgWords = ArrayList<LyricWord>(); var bgPlain = StringBuilder()
        var bgStart = -1L; var bgEnd = 0L; var bgWord = StringBuilder(); var bgSpan = longArrayOf(-1, 0)
        var inTr = StringBuilder(); var inRo = StringBuilder()
        var inBody = false
        while (true) {
            when (p.next()) {
                org.xmlpull.v1.XmlPullParser.END_DOCUMENT -> break
                org.xmlpull.v1.XmlPullParser.START_TAG -> when {
                    p.name == "body" -> inBody = true
                    !inBody -> when {
                        headKey != null -> if (headSkip > 0) headSkip++ else if (attr("ttm:role")?.startsWith("x-") == true) headSkip = 1
                        p.name == "translation" -> if (!headSeenTr) { headSeenTr = true; headKind = 1; translationLanguage = attr("xml:lang") } else headKind = -1
                        p.name == "transliteration" -> if (!headSeenRo) { headSeenRo = true; headKind = 2 } else headKind = -1
                        p.name == "text" && headKind > 0 -> { headKey = attr("for"); headText = StringBuilder(); headSkip = 0 }
                    }
                    role != null -> {
                        roleDepth++
                        if (role == "x-bg" && p.name == "span" && roleDepth == 2) { bgStart = clock(attr("begin")); bgEnd = clock(attr("end")); bgWord = StringBuilder() }
                    }
                    attr("ttm:role")?.startsWith("x-") == true -> {
                        role = attr("ttm:role"); roleDepth = 1
                        if (role == "x-bg") bgSpan = longArrayOf(clock(attr("begin")), clock(attr("end")))
                        if (role == "x-translation" && translationLanguage == null) translationLanguage = attr("xml:lang")
                    }
                    p.name == "p" -> {
                        lineStart = clock(attr("begin")); words = ArrayList(); plain = StringBuilder()
                        key = lineKey(); voice = voiceOf(attr("ttm:agent"))
                        bgWords = ArrayList(); bgPlain = StringBuilder(); bgStart = -1; inTr = StringBuilder(); inRo = StringBuilder()
                    }
                    p.name == "span" && words != null -> {
                        wordStart = clock(attr("begin")); wordEnd = clock(attr("end")); word = StringBuilder()
                    }
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> {
                    val t = p.text
                    if (!inBody) { if (headKey != null && headSkip == 0) headText.append(t) }
                    else if (words != null) when (role) {
                        null -> {
                            if (wordStart >= 0) word.append(t)
                            else if (words!!.isNotEmpty()) words!![words!!.lastIndex] = words!![words!!.lastIndex].let { it.copy(text = it.text + t) }
                            plain.append(t)
                        }
                        "x-bg" -> {
                            bgPlain.append(t)
                            if (bgStart >= 0) bgWord.append(t)
                            else if (bgWords.isNotEmpty()) bgWords[bgWords.lastIndex] = bgWords[bgWords.lastIndex].let { it.copy(text = it.text + t) }
                        }
                        "x-translation" -> inTr.append(t)
                        "x-roman" -> inRo.append(t)
                    }
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> when {
                    !inBody -> when {
                        headKey != null && headSkip > 0 -> headSkip--
                        headKey != null && p.name == "text" -> {
                            val text = tidy(headText).ifEmpty { null }
                            if (text != null) (if (headKind == 1) headTranslation else headRoman)[headKey!!] = text
                            headKey = null
                        }
                        p.name == "translation" || p.name == "transliteration" -> headKind = 0
                    }
                    role != null -> {
                        if (role == "x-bg" && roleDepth == 2 && p.name == "span" && bgStart >= 0) { bgWords.add(LyricWord(bgStart, bgEnd, bgWord.toString())); bgStart = -1 }
                        roleDepth--
                        if (roleDepth == 0) {
                            if (role == "x-bg" && bgWords.isEmpty() && bgSpan[0] >= 0 && bgPlain.isNotBlank()) bgWords.add(LyricWord(bgSpan[0], bgSpan[1], bgPlain.toString()))
                            role = null
                        }
                    }
                    p.name == "span" && wordStart >= 0 -> { words?.add(LyricWord(wordStart, wordEnd, word.toString())); wordStart = -1 }
                    p.name == "p" && words != null -> {
                        var text = tidy(plain)
                        var foreground: List<LyricWord>? = words!!.takeIf { it.isNotEmpty() }
                        var back: List<LyricWord>? = bgWords.takeIf { it.isNotEmpty() }
                        var backText = backingText(tidy(bgPlain))
                        if (text.isEmpty() && backText != null) { text = backText; foreground = back; back = null; backText = null }
                        if (text.isNotEmpty() && lineStart >= 0) {
                            val tr = tidy(inTr).ifEmpty { null } ?: key?.let(headTranslation::get)
                            val ro = tidy(inRo).ifEmpty { null } ?: key?.let(headRoman::get)
                            lines += LyricLine(lineStart, text, foreground,
                                translation = tr?.takeIf { LyricsMatch.key(it) != LyricsMatch.key(text) },
                                romanization = ro?.takeIf { LyricsMatch.key(it) != LyricsMatch.key(text) },
                                voice = voice, background = backText, backgroundWords = back)
                        }
                        words = null
                    }
                }
            }
        }
        if (lines.isEmpty()) null
        else Lyrics(lines, synced = true, translationLanguage = translationLanguage.takeIf { lines.any { it.translation != null } }, syncPrecision = SyncPrecision.SYLLABLE)
    }.getOrNull()

    private fun clock(v: String?): Long {
        if (v.isNullOrBlank()) return -1
        val parts = v.trim().removeSuffix("s").split(':')
        var ms = 0.0
        for (part in parts) ms = ms * 60 + (part.toDoubleOrNull() ?: return -1)
        return (ms * 1000).toLong()
    }
    private fun parseClock(v: String?): Long = clock(v).coerceAtLeast(0)

    // ── Kugou ──
    private val KRC_KEY = intArrayOf(64, 71, 97, 119, 94, 50, 116, 71, 81, 54, 49, 45, 206, 210, 110, 105)
    private val krcLine = Regex("""^\[(\d+),(\d+)](.*)$""")
    private val krcWord = Regex("""<(\d+),(\d+),\d+>([^<]*)""")

    private fun fromKugou(title: String, artist: String, durationMs: Long): Lyrics? {
        val search = httpGet("https://lyrics.kugou.com/search?ver=1&man=yes&client=pc&keyword=${enc("$artist - $title")}" +
            (if (durationMs > 0) "&duration=$durationMs" else ""), timeoutMs = 5000, provider = "kugou") ?: return null
        val cands = runCatching { JSONObject(search).optJSONArray("candidates") }.getOrNull() ?: return null
        val c = (0 until cands.length()).mapNotNull { cands.optJSONObject(it) }.firstOrNull {
            sameSong(it.optString("song"), it.optString("singer"), it.optLong("duration"), title, artist, durationMs)
        } ?: return null
        val dl = httpGet("https://lyrics.kugou.com/download?ver=1&client=pc&id=${c.optString("id")}&accesskey=${c.optString("accesskey")}&fmt=krc&charset=utf8", timeoutMs = 5000, provider = "kugou") ?: return null
        val content = runCatching { JSONObject(dl).optString("content") }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val krc = runCatching {
            val raw = android.util.Base64.decode(content, android.util.Base64.DEFAULT)
            val x = ByteArray(raw.size - 4) { i -> (raw[i + 4].toInt() xor KRC_KEY[i % KRC_KEY.size]).toByte() }
            java.util.zip.InflaterInputStream(x.inputStream()).readBytes().decodeToString()
        }.getOrNull() ?: return null
        val lines = krc.lines().mapNotNull { raw ->
            val m = krcLine.find(raw.trim()) ?: return@mapNotNull null
            val start = m.groupValues[1].toLong()
            val words = krcWord.findAll(m.groupValues[3]).map { w ->
                val s = start + w.groupValues[1].toLong(); LyricWord(s, s + w.groupValues[2].toLong(), w.groupValues[3])
            }.toList()
            val text = words.joinToString("") { it.text }.trim()
            if (text.isEmpty() || text.contains('：')) null else LyricLine(start, text, words.takeIf { it.isNotEmpty() })
        }
        if (lines.size < 3) return null
        return Lyrics(lines, synced = true, provider = "Kugou", syncPrecision = SyncPrecision.SYLLABLE)
    }

    internal fun sameSong(candTitle: String, candArtist: String, candDurationMs: Long, title: String, artist: String, durationMs: Long): Boolean {
        if (matchKey(artist).isEmpty() || (candArtist.split(',', '&', '、', '/') + candArtist).none { LyricsMatch.sameArtist(it, artist) }) return false
        val t = matchKey(title); val ct = matchKey(cleanTitle(candTitle))
        if (t.isEmpty() || !(ct == t || ct.startsWith(t) || t.startsWith(ct))) return false
        return durationMs <= 0 || candDurationMs <= 0 || kotlin.math.abs(candDurationMs - durationMs) <= 4_000
    }

    private val featTag = Regex("""\s*[(\[][^)\]]*(feat\.?|ft\.?|with )[^)\]]*[)\]]""", RegexOption.IGNORE_CASE)
    private val bracketTag = Regex("""\s*[(\[][^)\]]*(remaster|remastered|live|version|edit|mono|stereo|deluxe|bonus)[^)\]]*[)\]]""", RegexOption.IGNORE_CASE)
    private fun cleanTitle(title: String) =
        title.substringBefore(" - ").replace(featTag, "").replace(bracketTag, "").trim()

    internal fun matchKey(s: String) = LyricsMatch.key(s)
    private fun overlaps(a: String, b: String) = a.isNotEmpty() && a == b

    fun isPlausibleMatch(
        candTitle: String, candArtist: String, candDurationSec: Int,
        title: String, artist: String, durationSec: Int,
    ): Boolean {
        if (!LyricsMatch.sameArtist(candArtist, artist)) return false
        if (!overlaps(matchKey(cleanTitle(candTitle)), matchKey(title))) return false
        return durationSec <= 0 || candDurationSec <= 0 || kotlin.math.abs(candDurationSec - durationSec) <= 3
    }

    private val lrcTag = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
    fun parseLrc(lrc: String): List<LyricLine> {
        val out = ArrayList<LyricLine>()
        for (raw in lrc.split("\n")) {
            val tags = lrcTag.findAll(raw).toList()
            if (tags.isEmpty()) continue
            val text = raw.substring(tags.last().range.last + 1).trim().ifEmpty { "♪" }
            for (m in tags) {
                val frac = m.groupValues[3]
                val ms = when (frac.length) {
                    0 -> 0L
                    1 -> frac.toLong() * 100
                    2 -> frac.toLong() * 10
                    else -> frac.take(3).toLong()
                }
                out.add(LyricLine(m.groupValues[1].toLong() * 60_000 + m.groupValues[2].toLong() * 1_000 + ms, text))
            }
        }
        val sorted = out.sortedBy { it.timeMs }
        return sorted.filterIndexed { i, l -> !(l.text == "♪" && i > 0 && sorted[i - 1].text == "♪") }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun httpGet(
        spec: String,
        timeoutMs: Int = 5000,
        provider: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): String? {
        if (provider != null && bypass.get() != true) health.reason(provider)?.let {
            Log.d("LyricsApi", "$provider set aside ($it)")
            health.skipped()
            return null
        }
        return runCatching {
            com.music.spotui.data.DataSaver.check()
            val conn = (URL(spec).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Accept", "application/json, text/html;q=0.9, */*;q=0.8")
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            try {
                val code = conn.responseCode
                if (code != 200) {
                    Log.d("LyricsApi", "HTTP $code for $spec")
                    if (provider == null) health.skipped()
                    else if (code >= 500 || code == 429 || code == 408) health.failed(provider)
                    else health.answered(provider)
                    return null
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                provider?.let(health::answered)
                body
            } finally {
                conn.disconnect()
            }
        }.getOrElse {
            Log.d("LyricsApi", "request failed: ${it.message}")
            if (provider == null || it is com.music.spotui.data.DataSaver.Blocked) health.skipped() else health.failed(provider)
            null
        }
    }

    private inline fun asking(provider: String, track: Track, block: () -> Lyrics?): Lyrics? {
        if (health.isMissing(track.key, provider)) return null
        val lostBefore = health.lostCount()
        val found = block()
        if (found == null && health.lostCount() == lostBefore) health.noteMissing(track.key, provider)
        return found
    }
}
