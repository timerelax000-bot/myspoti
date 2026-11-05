// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.ui.immersive.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.music.spotui.ui.look.MeshPalette

/**
 * Subtle Immersive Visualizer:
 * Minimalist, elegant fluid waveform and radial pulse rings around the album artwork.
 * Driven by playback state, track timeline, and current artwork color palette.
 * Automatically muted when Canvas video is active to avoid visual noise.
 */
@Composable
fun ImmersiveVisualizer(
    enabled: Boolean,
    playing: Boolean,
    intensity: String,
    palette: MeshPalette,
    reducedMotion: Boolean,
    canvasActive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (!enabled || reducedMotion) return

    val scaleFactor = when (intensity.lowercase()) {
        "low" -> 0.45f
        "high" -> 1.15f
        else -> 0.8f // medium
    }

    val playFactor by animateFloatAsState(
        targetValue = if (playing) 1.0f else 0.15f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "visPlayFactor",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "VisualizerWaves")

    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase1",
    )

    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase2",
    )

    val canvasMuteFactor = if (canvasActive) 0.35f else 1.0f
    val cList = palette.colors
    val primaryColor = (cList.getOrNull(0) ?: Color(0xFF3A1C71)).copy(alpha = 0.28f * playFactor * canvasMuteFactor)
    val secondaryColor = (cList.getOrNull(1) ?: Color(0xFFD76D77)).copy(alpha = 0.22f * playFactor * canvasMuteFactor)
    val accentColor = (cList.getOrNull(2) ?: Color(0xFF2B5876)).copy(alpha = 0.18f * playFactor * canvasMuteFactor)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f
        val baseRadius = minOf(width, height) * 0.42f * scaleFactor

        for (i in 0 until 3) {
            val phase = if (i % 2 == 0) wavePhase1 + i * 0.8f else wavePhase2 + i * 0.5f
            val ringColor = when (i) {
                0 -> primaryColor
                1 -> secondaryColor
                else -> accentColor
            }
            val path = Path()
            val points = 60
            for (p in 0..points) {
                val angle = (p.toFloat() / points) * 2f * Math.PI.toFloat()
                val waveOffset = (kotlin.math.sin(angle * (3 + i) + phase) * 12.dp.toPx() +
                        kotlin.math.cos(angle * 2 - phase) * 8.dp.toPx()) * playFactor * scaleFactor
                val r = baseRadius + (i * 18.dp.toPx() * scaleFactor) + waveOffset
                val x = centerX + r * kotlin.math.cos(angle)
                val y = centerY + r * kotlin.math.sin(angle)
                if (p == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(
                path = path,
                color = ringColor,
                style = Stroke(width = (2.5f + i * 0.8f).dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}
