/*
 * Adapted from Spicy Player / Spicy Lyrics (https://github.com/spicylyrics/mobile), AGPL-3.0.
 * Copyright (C) Spicy Lyrics Authors.
 * Modified for Myspoti, 2026.
 */
package com.music.spotui.ui.immersive.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

private const val STAGE_OPACITY = 0.85f
private const val CROP_FROM_TOP = 0.4f
private val CropAlignment = BiasAlignment(0f, CROP_FROM_TOP * 2f - 1f)
private val HalfCosine = Easing { 0.5f - 0.5f * cos(PI.toFloat() * it) }
private val FEATHER_STOPS = listOf(0f to 0f, 0.18f to 0.10f, 0.38f to 0.36f, 0.72f to 0.85f, 1f to 1f)
private const val TOP_FEATHER = 0.18f

/**
 * The landscape cover stage: beside the lyrics, the cover fills the panel side
 * edge-to-edge, feathering softly toward the lyrics and over its top.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CoverStage(
    coverUrl: String,
    metrics: LandscapeMetrics,
    onRight: Boolean,
    startInsetPx: Float,
    endInsetPx: Float,
    controlsShown: () -> Float,
    modifier: Modifier = Modifier,
    videoUrl: String? = null,
    videoActive: Boolean = false,
) {
    val featherPx = metrics.stageFeatherPx
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f
            }
    ) {
        Box(
            Modifier
                .layout { measurable, constraints ->
                    val (left, right) = stageSpan(metrics, onRight, startInsetPx, endInsetPx, constraints.maxWidth.toFloat(), 0f)
                    val width = (right - left).roundToInt().coerceIn(1, constraints.maxWidth)
                    val placeable = measurable.measure(Constraints.fixed(width, constraints.maxHeight))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(left.roundToInt(), 0)
                    }
                }
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val innerEdge = if (onRight) 0f else size.width
                    val toward = if (onRight) 1f else -1f
                    val stops = FEATHER_STOPS.map { (at, a) ->
                        ((innerEdge + toward * at * featherPx) / size.width).coerceIn(0f, 1f) to Color.Black.copy(alpha = a)
                    }.toMutableList()
                    stops.sortBy { it.first }
                    drawRect(Brush.horizontalGradient(*stops.toTypedArray()), blendMode = BlendMode.DstIn)
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            TOP_FEATHER * 0.5f to Color.Black.copy(alpha = 0.28f),
                            TOP_FEATHER to Color.Black,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
        ) {
            Box(Modifier.fillMaxSize().clipToBounds().graphicsLayer { alpha = STAGE_OPACITY }) {
                Crossfade(targetState = coverUrl, animationSpec = tween(800, easing = HalfCosine), label = "stageCover") { url ->
                    if (url.isNotBlank()) {
                        GlideImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alignment = CropAlignment,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            it.placeholder(app.myspoti.engine.R.drawable.placeholder)
                        }
                    } else {
                        Box(Modifier.fillMaxSize().background(Color.DarkGray))
                    }
                }
                if (videoActive && videoUrl != null) {
                    com.music.spotui.ui.screens.CanvasVideo(
                        url = videoUrl,
                        playing = true,
                        onFirstFrame = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                // Black veil dimming when controls show
                Box(
                    Modifier.fillMaxSize().drawBehind {
                        val brightness = 1f - 0.45f * controlsShown()
                        drawRect(Color.Black.copy(alpha = 1f - 0.8f * (0.45f + 0.55f * brightness)))
                    }
                )
            }
            // Bottom gradient for readability
            Box(
                Modifier.fillMaxSize().drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to Color.Transparent,
                            0.60f to Color.Black.copy(alpha = 0.04f),
                            0.72f to Color.Black.copy(alpha = 0.11f),
                            0.84f to Color.Black.copy(alpha = 0.19f),
                            1f to Color.Black.copy(alpha = 0.35f),
                        )
                    )
                }
            )
        }
    }
}

private fun stageSpan(
    metrics: LandscapeMetrics,
    onRight: Boolean,
    startInsetPx: Float,
    endInsetPx: Float,
    screenWidthPx: Float,
    centred: Float,
): Pair<Float, Float> {
    val sideWidth = (if (onRight) endInsetPx else startInsetPx) + metrics.stageWidthPx
    val sideLeft = if (onRight) screenWidthPx - sideWidth else 0f
    val middleHalf = metrics.coverSizePx / 2f + metrics.stageFeatherPx
    val middle = startInsetPx + metrics.centred.artLeftPx + metrics.coverSizePx / 2f
    val left = sideLeft + (middle - middleHalf - sideLeft) * centred
    val right = sideLeft + sideWidth + (middle + middleHalf - sideLeft - sideWidth) * centred
    return left to right
}
