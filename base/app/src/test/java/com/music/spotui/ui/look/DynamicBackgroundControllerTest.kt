package com.music.spotui.ui.look

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DynamicBackgroundControllerTest {

    @Test
    fun testDynamicPaletteConversion() {
        val palette = DynamicPalette(
            primary = Color.Red,
            secondary = Color.Green,
            tertiary = Color.Blue,
            darkAccent = Color.Black,
            mode = BackgroundMode.ARTWORK,
            displayState = ImmersiveDisplayState.PORTRAIT,
        )

        val mesh = palette.toMeshPalette()
        assertEquals(4, mesh.colors.size)
        assertEquals(Color.Red, mesh.colors[0])
        assertEquals(Color.Green, mesh.colors[1])
        assertEquals(Color.Blue, mesh.colors[2])
        assertEquals(Color.Black, mesh.colors[3])
    }

    @Test
    fun testPortraitArtworkPaletteResolution() {
        DynamicBackgroundController.clearCache()
        val palette = DynamicBackgroundController.resolvePalette(
            coverUrl = "https://example.com/cover.jpg",
            videoUrl = null,
            videoActive = false,
            displayState = ImmersiveDisplayState.PORTRAIT,
        )

        assertEquals(BackgroundMode.ARTWORK, palette.mode)
        assertEquals(ImmersiveDisplayState.PORTRAIT, palette.displayState)
    }

    @Test
    fun testPortraitVideoPaletteResolution() {
        DynamicBackgroundController.clearCache()
        val palette = DynamicBackgroundController.resolvePalette(
            coverUrl = "https://example.com/cover.jpg",
            videoUrl = "https://youtube.com/watch?v=123",
            videoActive = true,
            displayState = ImmersiveDisplayState.PORTRAIT,
        )

        assertEquals(BackgroundMode.VIDEO, palette.mode)
        assertEquals(ImmersiveDisplayState.PORTRAIT, palette.displayState)
    }

    @Test
    fun testVideoToArtworkTransition() {
        DynamicBackgroundController.clearCache()

        val videoPalette = DynamicBackgroundController.resolvePalette(
            coverUrl = "https://example.com/cover.jpg",
            videoUrl = "https://youtube.com/watch?v=123",
            videoActive = true,
            displayState = ImmersiveDisplayState.PORTRAIT,
        )
        assertEquals(BackgroundMode.VIDEO, videoPalette.mode)

        val artworkPalette = DynamicBackgroundController.resolvePalette(
            coverUrl = "https://example.com/cover.jpg",
            videoUrl = "https://youtube.com/watch?v=123",
            videoActive = false,
            displayState = ImmersiveDisplayState.PORTRAIT,
        )
        assertEquals(BackgroundMode.ARTWORK, artworkPalette.mode)
    }

    @Test
    fun testDisplayStateTransitions() {
        DynamicBackgroundController.clearCache()

        val portrait = DynamicBackgroundController.resolvePalette(
            coverUrl = "https://example.com/cover.jpg",
            videoUrl = null,
            videoActive = false,
            displayState = ImmersiveDisplayState.PORTRAIT,
        )
        val landscape = DynamicBackgroundController.resolvePalette(
            coverUrl = "https://example.com/cover.jpg",
            videoUrl = null,
            videoActive = false,
            displayState = ImmersiveDisplayState.LANDSCAPE,
        )
        val tv = DynamicBackgroundController.resolvePalette(
            coverUrl = "https://example.com/cover.jpg",
            videoUrl = null,
            videoActive = false,
            displayState = ImmersiveDisplayState.TV,
        )

        assertEquals(ImmersiveDisplayState.PORTRAIT, portrait.displayState)
        assertEquals(ImmersiveDisplayState.LANDSCAPE, landscape.displayState)
        assertEquals(ImmersiveDisplayState.TV, tv.displayState)
    }
}
