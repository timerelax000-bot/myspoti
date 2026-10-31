package com.music.spotui.ui.immersive.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * Verifies the stability and predictability of the Cinema auto-hide state machine
 * and pointer event filter across repeated cycles.
 */
class CinemaAutoHideTest {

    class AutoHideStateMachine(val autoHideMillis: Long = 5_000L) {
        var controlsVisible: Boolean = true
            private set
        var lastInteractionTime: Long = 0L
            private set
        var interactionKey: Long = 0L
            private set

        private var lastPointerX: Float? = null
        private var lastPointerY: Float? = null

        fun onPointerMove(x: Float, y: Float, currentTime: Long): Boolean {
            val prevX = lastPointerX
            val prevY = lastPointerY
            lastPointerX = x
            lastPointerY = y

            if (prevX != null && prevY != null) {
                val dx = x - prevX
                val dy = y - prevY
                // Filter out sub-pixel jitter and synthetic movements triggered by layout reflows (<= 4px)
                if (hypot(dx, dy) <= 4f) {
                    return false
                }
            }
            registerInteraction(currentTime)
            return true
        }

        fun onPointerPress(x: Float, y: Float, currentTime: Long) {
            lastPointerX = x
            lastPointerY = y
            registerInteraction(currentTime)
        }

        fun onPointerEnterOrExit() {
            // Enter and exit events caused by layout shifts or child boundary movements are intentionally ignored
        }

        fun onKeyEvent(currentTime: Long) {
            registerInteraction(currentTime)
        }

        fun registerInteraction(currentTime: Long) {
            controlsVisible = true
            lastInteractionTime = currentTime
            interactionKey++
        }

        fun tick(currentTime: Long) {
            if (controlsVisible && autoHideMillis > 0) {
                if (currentTime - lastInteractionTime >= autoHideMillis) {
                    controlsVisible = false
                }
            }
        }
    }

    @Test
    fun testSubpixelJitterDoesNotWakeControlsOrResetTimer() {
        val machine = AutoHideStateMachine(autoHideMillis = 5_000L)
        var currentTime = 1_000L
        machine.registerInteraction(currentTime)
        assertTrue(machine.controlsVisible)

        // Advance time past autoHide timeout
        currentTime += 5_100L
        machine.tick(currentTime)
        assertFalse("Controls must be hidden after timeout", machine.controlsVisible)

        // Simulate synthetic layout reflow emitting subpixel pointer moves (<= 4px)
        val woken1 = machine.onPointerMove(100f, 100f, currentTime)
        assertTrue(woken1) // First move records baseline position and registers
        machine.tick(currentTime)
        assertTrue(machine.controlsVisible)

        currentTime += 5_100L
        machine.tick(currentTime)
        assertFalse("Controls must hide after timeout", machine.controlsVisible)

        // Micro-jitter: 2px movement caused by animated scale/bounds shift
        val woken2 = machine.onPointerMove(101f, 102f, currentTime)
        assertFalse("Subpixel move must be filtered out", woken2)
        assertFalse("Controls must remain hidden", machine.controlsVisible)

        // Stationary move event (same coordinates)
        val woken3 = machine.onPointerMove(101f, 102f, currentTime)
        assertFalse("Stationary move must be filtered out", woken3)
        assertFalse("Controls must remain hidden", machine.controlsVisible)
    }

    @Test
    fun testSyntheticEnterAndExitIgnored() {
        val machine = AutoHideStateMachine(autoHideMillis = 5_000L)
        var currentTime = 1_000L
        machine.registerInteraction(currentTime)
        currentTime += 5_100L
        machine.tick(currentTime)
        assertFalse(machine.controlsVisible)

        // Synthetic enter/exit events during animated hide/show transitions
        machine.onPointerEnterOrExit()
        assertFalse("Enter/exit events must not reveal controls", machine.controlsVisible)
    }

    @Test
    fun testCinemaAutoHideAcross30Cycles() {
        val machine = AutoHideStateMachine(autoHideMillis = 4_000L)
        var currentTime = 10_000L

        for (cycle in 1..30) {
            // Step 1: User interacts (either key or real mouse movement > 4px)
            if (cycle % 2 == 0) {
                machine.onKeyEvent(currentTime)
            } else {
                machine.onPointerMove(cycle * 20f, cycle * 20f + 10f, currentTime)
            }

            assertTrue("Cycle $cycle: Controls must be visible immediately upon real user interaction", machine.controlsVisible)
            val interactionKeyAtWake = machine.interactionKey

            // Step 2: During active time, synthetic layout jitter does not cause bouncing
            currentTime += 1_000L
            machine.onPointerEnterOrExit()
            machine.tick(currentTime)
            assertTrue("Cycle $cycle: Controls must stay visible during interaction window", machine.controlsVisible)

            // Step 3: Advance time beyond the auto-hide threshold
            currentTime += 4_100L
            machine.tick(currentTime)
            assertFalse("Cycle $cycle: Controls must cleanly hide when inactivity period elapses", machine.controlsVisible)

            // Step 4: Verify controls STAY hidden without spontaneously reappearing
            for (t in 1..5) {
                currentTime += 1_000L
                machine.tick(currentTime)
                assertFalse("Cycle $cycle: Controls must NEVER spontaneously reappear at t=$t", machine.controlsVisible)
            }

            // Step 5: Verify interaction key was not modified during hidden state
            assertEquals(
                "Cycle $cycle: Interaction key should not be incremented while idle and hidden",
                interactionKeyAtWake,
                machine.interactionKey
            )
        }
    }
}
