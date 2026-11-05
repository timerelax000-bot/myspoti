package com.music.spotui.data

import com.music.spotui.data.entity.AccountModel
import com.music.spotui.data.offline.OfflineAccounts
import org.junit.Assert.*
import org.junit.Test

class AccountStoreTest {

    @Test
    fun accountModelPropertiesDefaults() {
        val model = AccountModel(
            id = "acc-123",
            spotifyUserId = "user-abc",
            name = "Test User",
            email = "user@example.com",
            imageUrl = "https://example.com/avatar.jpg",
            plan = "premium",
            webViewProfileName = "account_acc-123",
            dateAdded = 1000L,
            lastUsed = 1000L,
            isActive = true,
        )

        assertEquals("acc-123", model.id)
        assertEquals("user-abc", model.spotifyUserId)
        assertEquals("Test User", model.name)
        assertEquals("user@example.com", model.email)
        assertEquals("https://example.com/avatar.jpg", model.imageUrl)
        assertEquals("premium", model.plan)
        assertEquals("account_acc-123", model.webViewProfileName)
        assertTrue(model.isActive)
        assertFalse(model.isDefault)
        assertEquals("", model.customLabel)
    }

    @Test
    fun offlineAccountsKeyOfIsDeterministicAndOpaque() {
        val key1 = OfflineAccounts.keyOf("user-abc")
        val key2 = OfflineAccounts.keyOf("user-abc")
        val key3 = OfflineAccounts.keyOf("user-xyz")

        assertEquals(key1, key2)
        assertNotEquals(key1, key3)
        assertEquals(24, key1.length) // 12 bytes = 24 hex chars
    }
}
