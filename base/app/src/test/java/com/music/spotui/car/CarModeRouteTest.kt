package com.music.spotui.car

import com.music.spotui.ui.navigation.Routes
import org.junit.Assert.*
import org.junit.Test

class CarModeRouteTest {

    @Test
    fun carModeRouteDefinedCorrectly() {
        assertEquals("carmode", Routes.CarMode.route)
        assertEquals("CarMode", Routes.CarMode.label)
        assertEquals("carmode_settings", Routes.CarModeSettings.route)
    }

    @Test
    fun carModeIsFullScreenRoute() {
        val fullScreenRoutes = setOf(
            Routes.Player.route,
            Routes.Queue.route,
            Routes.CarMode.route,
        )

        assertTrue(fullScreenRoutes.contains(Routes.CarMode.route))
        assertTrue(fullScreenRoutes.contains("carmode"))
    }
}
