package com.music.spotui.data

import android.content.Context
import com.music.spotui.data.entity.AccountModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

object AccountStore {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val _accounts = MutableStateFlow<List<AccountModel>>(emptyList())
    val accounts: StateFlow<List<AccountModel>> = _accounts.asStateFlow()

    private val _activeAccount = MutableStateFlow<AccountModel?>(null)
    val activeAccount: StateFlow<AccountModel?> = _activeAccount.asStateFlow()

    private fun file(context: Context) = File(context.filesDir, "myspoti_accounts.json")

    fun initSync(context: Context) {
        val f = file(context)
        if (f.isFile) {
            runCatching {
                val text = f.readText()
                val list = json.decodeFromString<List<AccountModel>>(text)
                _accounts.value = list
                _activeAccount.value = list.firstOrNull { it.isActive } ?: list.firstOrNull()
            }
        }
    }

    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        initSync(context)
    }

    suspend fun save(context: Context) = withContext(Dispatchers.IO) {
        runCatching {
            val text = json.encodeToString(_accounts.value)
            writeTextSafely(file(context), text)
        }
    }

    suspend fun addAccountDirectly(context: Context, account: AccountModel) = withContext(Dispatchers.IO) {
        val list = _accounts.value.toMutableList()
        list.removeAll { it.id == account.id || it.spotifyUserId == account.spotifyUserId }
        val marked = list.map { it.copy(isActive = false) } + account
        _accounts.value = marked
        _activeAccount.value = account
        save(context)
    }

    suspend fun addOrUpdateAccount(
        context: Context,
        spotifyUserId: String,
        name: String,
        email: String,
        imageUrl: String,
        plan: String,
    ): AccountModel = withContext(Dispatchers.IO) {
        val list = _accounts.value.toMutableList()
        val existing = list.firstOrNull { it.spotifyUserId == spotifyUserId }
            ?: list.firstOrNull { it.spotifyUserId.startsWith("pending_") && it.isActive }
        val account = if (existing != null) {
            val updated = existing.copy(
                spotifyUserId = spotifyUserId,
                name = name.ifBlank { existing.name },
                email = email.ifBlank { existing.email },
                imageUrl = imageUrl.ifBlank { existing.imageUrl },
                plan = plan.ifBlank { existing.plan },
                lastUsed = System.currentTimeMillis(),
                isActive = true,
            )
            list.remove(existing)
            updated
        } else {
            val uuid = UUID.randomUUID().toString()
            AccountModel(
                id = uuid,
                spotifyUserId = spotifyUserId,
                name = name,
                email = email,
                imageUrl = imageUrl,
                plan = plan,
                webViewProfileName = "account_$uuid",
                dateAdded = System.currentTimeMillis(),
                lastUsed = System.currentTimeMillis(),
                isActive = true,
            )
        }

        // Mark all others inactive
        val marked = list.map { it.copy(isActive = false) } + account
        _accounts.value = marked
        _activeAccount.value = account
        save(context)
        runCatching { app.myspoti.host.android.MyspotiRuntime.host.flushCookies() }
        account
    }

    suspend fun setActive(context: Context, id: String) = withContext(Dispatchers.IO) {
        val list = _accounts.value
        val target = list.firstOrNull { it.id == id } ?: return@withContext
        val updated = list.map { it.copy(isActive = it.id == id, lastUsed = if (it.id == id) System.currentTimeMillis() else it.lastUsed) }
        _accounts.value = updated
        _activeAccount.value = target.copy(isActive = true, lastUsed = System.currentTimeMillis())
        save(context)
    }

    suspend fun remove(context: Context, id: String) = withContext(Dispatchers.IO) {
        val list = _accounts.value.toMutableList()
        val removed = list.firstOrNull { it.id == id }
        list.removeAll { it.id == id }
        if (removed?.isActive == true && list.isNotEmpty()) {
            val next = list.maxByOrNull { it.lastUsed } ?: list.first()
            val updated = list.map { it.copy(isActive = it.id == next.id) }
            _accounts.value = updated
            _activeAccount.value = next.copy(isActive = true)
        } else {
            _accounts.value = list
            if (list.isEmpty()) {
                _activeAccount.value = null
            }
        }
        save(context)
    }

    suspend fun renameAccount(context: Context, id: String, customLabel: String) = withContext(Dispatchers.IO) {
        val list = _accounts.value.map { acc ->
            if (acc.id == id) acc.copy(customLabel = customLabel.trim()) else acc
        }
        _accounts.value = list
        _activeAccount.value = list.firstOrNull { it.isActive }
        save(context)
    }

    suspend fun setDefaultAccount(context: Context, id: String) = withContext(Dispatchers.IO) {
        val list = _accounts.value.map { acc ->
            acc.copy(isDefault = acc.id == id)
        }
        _accounts.value = list
        save(context)
    }

    suspend fun clearAccountCache(context: Context, id: String) = withContext(Dispatchers.IO) {
        val account = _accounts.value.firstOrNull { it.id == id } ?: return@withContext
        val profileName = account.webViewProfileName
        if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.MULTI_PROFILE)) {
            runCatching {
                val profile = androidx.webkit.ProfileStore.getInstance().getProfile(profileName)
                profile?.webStorage?.deleteAllData()
                profile?.cookieManager?.removeAllCookies(null)
            }
        }
        val key = runCatching {
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(account.spotifyUserId.toByteArray())
                .take(8)
                .joinToString("") { "%02x".format(it) }
        }.getOrDefault("user_${account.spotifyUserId.hashCode()}")
        runCatching { File(context.filesDir, "screen_cache_$key").deleteRecursively() }
    }
}
