package com.music.spotui.ui.components

import com.music.spotui.data.CanvasNetworkPolicy
import com.music.spotui.data.preferences.ImmersiveSettingsPref
import com.music.spotui.di.SongPlayer

/**
 * One Canvas player for the whole app: closing and reopening the player screen keeps the
 * clip loaded (and its last frame), instead of building a new player that starts from nothing.
 * Centralized policy enforcement for mobile data saving, resolution limits, and media caching.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object CanvasPlayer {
    private var exo: androidx.media3.exoplayer.ExoPlayer? = null
    private var url: String? = null
    private var framed: String? = null
    private val firstFrame = mutableListOf<() -> Unit>()

    /** Checks if [url] is cached locally in storage. */
    fun isCached(context: android.content.Context, url: String): Boolean =
        CanvasNetworkPolicy.isCached(context, url)

    /**
     * [url] is loaded and ready to draw: its first frame shows as soon as a view is attached,
     * so the player screen opens on the clip instead of the cover.
     */
    fun isReady(url: String) = this.url == url && (framed == url || exo?.playbackState == androidx.media3.common.Player.STATE_READY)

    /**
     * Preloads the clip of the song now playing before the player screen opens, respecting
     * current network policy and Canvas Saver settings.
     */
    fun preload(context: android.content.Context, url: String) {
        if (CanvasNetworkPolicy.isPreloadAllowed(context, url)) {
            get(context, url)
        }
    }

    /**
     * Downloads the clip [url] to the media cache on storage, without a player and without
     * keeping anything in memory: the next song's Canvas.
     * When Canvas Saver is active on mobile data, warming is skipped if the video is not cached.
     */
    suspend fun warm(context: android.content.Context, url: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (!CanvasNetworkPolicy.isPreloadAllowed(context, url)) {
            return@withContext
        }
        runCatching {
            val source = androidx.media3.datasource.cache.CacheDataSource.Factory()
                .setCache(SongPlayer.mediaCache(context.applicationContext))
                .setUpstreamDataSourceFactory(androidx.media3.datasource.DefaultHttpDataSource.Factory())
                .createDataSource()
            androidx.media3.datasource.cache.CacheWriter(source, androidx.media3.datasource.DataSpec(android.net.Uri.parse(url)), null, null).cache()
        }
    }

    fun get(context: android.content.Context, url: String): androidx.media3.exoplayer.ExoPlayer {
        val app = context.applicationContext
        val isSaver = CanvasNetworkPolicy.isCanvasSaverActive
        val limit = ImmersiveSettingsPref.canvasQualityLimit.value

        val player = exo ?: run {
            val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(app).apply {
                if (isSaver || limit == "480p") {
                    parameters = buildUponParameters()
                        .setMaxVideoSize(854, 480)
                        .setMaxVideoBitrate(800_000)
                        .build()
                }
            }

            val cached = androidx.media3.datasource.cache.CacheDataSource.Factory()
                .setCache(SongPlayer.mediaCache(app))
                .setUpstreamDataSourceFactory(
                    if (isSaver && !isCached(app, url)) null
                    else androidx.media3.datasource.DefaultHttpDataSource.Factory()
                )

            androidx.media3.exoplayer.ExoPlayer.Builder(app)
                .setTrackSelector(trackSelector)
                .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(cached))
                .build().apply {
                    repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
                    volume = 0f
                    playWhenReady = false
                    addListener(object : androidx.media3.common.Player.Listener {
                        override fun onRenderedFirstFrame() {
                            framed = this@CanvasPlayer.url
                            firstFrame.toList().forEach { it() }
                        }
                    })
                }.also { exo = it }
        }

        // Dynamically update track constraints if quality/saver preferences changed
        (player.trackSelector as? androidx.media3.exoplayer.trackselection.DefaultTrackSelector)?.let { ts ->
            if (isSaver || limit == "480p") {
                ts.parameters = ts.buildUponParameters()
                    .setMaxVideoSize(854, 480)
                    .setMaxVideoBitrate(800_000)
                    .build()
            } else {
                ts.parameters = ts.buildUponParameters()
                    .clearVideoSizeConstraints()
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                    .build()
            }
        }

        if (url != this.url) {
            this.url = url
            framed = null
            player.setMediaItem(androidx.media3.common.MediaItem.fromUri(url))
            player.prepare()
        }
        return player
    }

    fun onFirstFrame(listener: () -> Unit): () -> Unit {
        firstFrame += listener
        return { firstFrame -= listener }
    }
}
