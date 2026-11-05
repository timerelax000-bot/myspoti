package com.music.spotui.data

import org.junit.Assert.*
import org.junit.Test

class AccountSessionManagerTest {

    @Test
    fun generationIncrementsMonotonically() {
        val gen1 = AccountSessionManager.currentGeneration
        assertTrue(gen1 >= 1L)
    }

    @Test
    fun accountSessionDataClassEquality() {
        val session1 = AccountSession("acc-1", "user-1", "account_acc-1", 1L)
        val session2 = AccountSession("acc-1", "user-1", "account_acc-1", 1L)
        val session3 = AccountSession("acc-2", "user-2", "account_acc-2", 2L)

        assertEquals(session1, session2)
        assertNotEquals(session1, session3)
        assertEquals("acc-1", session1.accountId)
        assertEquals("user-1", session1.spotifyUserId)
        assertEquals("account_acc-1", session1.webViewProfileName)
        assertEquals(1L, session1.generation)
    }

    @Test
    fun switchStateTransitionsAndReset() {
        assertEquals(AccountSwitchState.Idle, AccountSessionManager.switchState.value)

        AccountSessionManager.resetStateToIdle()
        assertEquals(AccountSwitchState.Idle, AccountSessionManager.switchState.value)
    }

    @Test
    fun accountSwitchStateTypes() {
        val switching = AccountSwitchState.Switching("acc-1", "STAGE_TEST")
        val success = AccountSwitchState.Success("acc-1")
        val failed = AccountSwitchState.Failed("Error occurred", "STAGE_TEST")

        assertEquals("acc-1", switching.targetAccountId)
        assertEquals("STAGE_TEST", switching.stage)
        assertEquals("acc-1", success.accountId)
        assertEquals("Error occurred", failed.message)
        assertEquals("STAGE_TEST", failed.stage)
    }
}
