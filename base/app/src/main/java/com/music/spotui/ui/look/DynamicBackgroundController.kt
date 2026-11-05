package com.music.spotui.ui.look

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.util.concurrent.ConcurrentHashMap

enum class BackgroundMode {
    ARTWORK,
    VIDEO,
}

enum class ImmersiveDisplayState {
    PORTRAIT,
    LANDSCAPE,
    TV,
}

@Immutable
data class DynamicPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val darkAccent: Color,
    val mode: BackgroundMode = BackgroundMode.ARTWORK,
    val displayState: ImmersiveDisplayState = ImmersiveDisplayState.PORTRAIT,
) {
    fun toMeshPalette(): MeshPalette = MeshPalette(listOf(primary, secondary, tertiary, darkAccent))
}

val FallbackDynamicPalette = DynamicPalette(
    primary = Color(0xFF3A1C71),
    secondary = Color(0xFFD76D77),
    tertiary = Color(0xFF2B5876),
    darkAccent = Color(0xFF101014),
    mode = BackgroundMode.ARTWORK,
    displayState = ImmersiveDisplayState.PORTRAIT,
)

/**
 * Single, unified controller managing dynamic interface backgrounds across all three
 * Immersive player states:
 * - Immersive Portrait: Fullscreen dynamic artwork/video background.
 * - Immersive Landscape: Canvas + Lyrics with shared dynamic background.
 * - Immersive TV: Widescreen TV modes.
 */
@Stable
object DynamicBackgroundController {
    private val paletteCache = ConcurrentHashMap<String, DynamicPalette>()

    fun resolvePalette(
        coverUrl: String?,
        videoUrl: String?,
        videoActive: Boolean,
        displayState: ImmersiveDisplayState = ImmersiveDisplayState.PORTRAIT,
        baseMeshColors: List<Color> = emptyList(),
    ): DynamicPalette {
        val mode = if (videoActive && !videoUrl.isNullOrBlank()) BackgroundMode.VIDEO else BackgroundMode.ARTWORK
        val key = "$coverUrl|$videoUrl|$mode|$displayState"
        paletteCache[key]?.let { return it }

        val mesh = baseMeshColors.ifEmpty { listOf(FallbackDynamicPalette.primary) }
        val rawPrimary = mesh.firstOrNull() ?: FallbackDynamicPalette.primary
        val rawSecondary = mesh.getOrNull(1) ?: FallbackDynamicPalette.secondary
        val rawTertiary = mesh.getOrNull(2) ?: FallbackDynamicPalette.tertiary

        val primary: Color
        val secondary: Color
        val tertiary: Color
        val darkAccent: Color

        if (displayState == ImmersiveDisplayState.TV) {
            // TV Cinematic Green/Teal/Cyan base palette blended with artwork/video colors
            val tvBasePrimary = Color(0xFF052A1C)
            val tvBaseSecondary = Color(0xFF004D31)
            val tvBaseTertiary = Color(0xFF0A7E70)
            primary = androidx.compose.ui.graphics.lerp(tvBasePrimary, rawPrimary, 0.40f)
            secondary = androidx.compose.ui.graphics.lerp(tvBaseSecondary, rawSecondary, 0.45f)
            tertiary = androidx.compose.ui.graphics.lerp(tvBaseTertiary, rawTertiary, 0.45f)
            darkAccent = Color(0xFF061E15)
        } else {
            primary = rawPrimary
            secondary = rawSecondary
            tertiary = rawTertiary
            darkAccent = Color(0xFF0F0F13)
        }

        val resolved = DynamicPalette(
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            darkAccent = darkAccent,
            mode = mode,
            displayState = displayState,
        )

        if (key.isNotBlank()) {
            paletteCache[key] = resolved
        }
        return resolved
    }

    @Composable
    fun rememberPalette(
        coverUrl: String?,
        videoUrl: String?,
        videoActive: Boolean,
        displayState: ImmersiveDisplayState = ImmersiveDisplayState.PORTRAIT,
        trackKey: Any? = null,
    ): DynamicPalette {
        val artworkPalette = rememberArtworkColors(coverUrl)
        var mode by remember { mutableStateOf(if (videoActive && !videoUrl.isNullOrBlank()) BackgroundMode.VIDEO else BackgroundMode.ARTWORK) }

        LaunchedEffect(videoActive, videoUrl) {
            mode = if (videoActive && !videoUrl.isNullOrBlank()) BackgroundMode.VIDEO else BackgroundMode.ARTWORK
        }

        return resolvePalette(
            coverUrl = coverUrl,
            videoUrl = videoUrl,
            videoActive = videoActive,
            displayState = displayState,
            baseMeshColors = artworkPalette.colors,
        )
    }

    fun clearCache() {
        paletteCache.clear()
    }
}
