// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.ui.immersive.player

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.spotui.data.preferences.ImmersiveSettingsPref

/**
 * Gesture Control Overlay for Immersive Player Artwork:
 * - Swipe Left: Next track
 * - Swipe Right: Previous track
 * - Double Tap: Like / Unlike current track
 * - Long Press: Open song options sheet
 * - Single Tap: Close active panel (lyrics/queue)
 * Includes subtle haptic feedback when enabled.
 */
@Composable
fun ArtworkGestureOverlay(
    enabled: Boolean,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    onSingleTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!enabled) {
        Box(modifier = modifier)
        return
    }

    val view = LocalView.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { 50.dp.toPx() }

    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    var gestureHandled by remember { mutableStateOf(false) }

    val hapticsEnabled by ImmersiveSettingsPref.hapticFeedbackEnabled.collectAsStateWithLifecycle()

    fun triggerHaptic(type: Int) {
        if (hapticsEnabled) {
            try { view.performHapticFeedback(type) } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onDoubleTap = {
                        triggerHaptic(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        onDoubleTap()
                    },
                    onLongPress = {
                        triggerHaptic(android.view.HapticFeedbackConstants.LONG_PRESS)
                        onLongPress()
                    },
                    onTap = {
                        onSingleTap()
                    },
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragAccumulator = 0f
                        gestureHandled = false
                    },
                    onDragEnd = {
                        if (!gestureHandled && kotlin.math.abs(dragAccumulator) > thresholdPx) {
                            gestureHandled = true
                            if (dragAccumulator < 0) {
                                triggerHaptic(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                onSwipeNext()
                            } else {
                                triggerHaptic(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                onSwipePrevious()
                            }
                        }
                        dragAccumulator = 0f
                    },
                    onDragCancel = {
                        dragAccumulator = 0f
                        gestureHandled = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount
                        if (!gestureHandled && kotlin.math.abs(dragAccumulator) > thresholdPx * 1.5f) {
                            gestureHandled = true
                            if (dragAccumulator < 0) {
                                triggerHaptic(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                onSwipeNext()
                            } else {
                                triggerHaptic(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                onSwipePrevious()
                            }
                        }
                    },
                )
            },
    )
}
