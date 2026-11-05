// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.ui.immersive.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import app.myspoti.engine.R
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.music.spotui.ui.components.SongBlend
import com.music.spotui.ui.components.SongBlendCover

private const val COVER_DECODE_PX = 640

/**
 * Animated Album Artwork component for Immersive Player:
 * - Parallax depth, floating translation and subtle 3D-like rotation.
 * - Ambient gloss/shimmer overlay reacting smoothly to playback state.
 * - Smoothly calms on pause (800ms easing).
 * - Muted magnitude when Canvas video is active to avoid visual clash.
 * - Seamless crossfade transitions on song changes via [SongBlendCover].
 * - Automatically respects System Reduce Motion & user settings.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AnimatedArtworkContainer(
    state: ImmersivePlayerState,
    blend: SongBlend?,
    isPlaying: Boolean,
    animatedArtworkEnabled: Boolean,
    reducedMotion: Boolean,
    canvasActive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val reduce = reducedMotion || !animatedArtworkEnabled

    val targetFactor = when {
        reduce -> 0f
        !isPlaying -> 0.10f
        canvasActive -> 0.35f
        else -> 1.0f
    }

    val playFactor by animateFloatAsState(
        targetValue = targetFactor,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "artPlayFactor",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ArtworkMotion")

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floatOffset",
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(5800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rotationAngle",
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )

    val currentFloatY = floatOffset * playFactor
    val currentRotation = rotationAngle * playFactor

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = currentFloatY
                rotationZ = currentRotation
            },
    ) {
        GlideImage(
            model = state.coverUrl.takeIf { it.isNotBlank() },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            transition = CrossFade,
            loading = placeholder(R.drawable.placeholder),
            failure = placeholder(R.drawable.placeholder),
            modifier = Modifier.fillMaxSize(),
            requestBuilderTransform = { it.override(COVER_DECODE_PX) },
        )

        SongBlendCover(blend, Modifier.fillMaxSize()) { it.override(COVER_DECODE_PX) }

        if (!reduce && playFactor > 0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        val width = size.width
                        val height = size.height
                        val startX = width * shimmerOffset
                        val endX = startX + width * 0.45f
                        val sheenBrush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.05f * playFactor),
                                Color.White.copy(alpha = 0.12f * playFactor),
                                Color.White.copy(alpha = 0.05f * playFactor),
                                Color.Transparent,
                            ),
                            start = Offset(startX, 0f),
                            end = Offset(endX, height),
                        )
                        drawRect(brush = sheenBrush)
                    },
            )
        }
    }
}
