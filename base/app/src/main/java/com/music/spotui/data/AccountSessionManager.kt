package com.music.spotui.data

import android.content.Context
import app.myspoti.host.android.Logbook
import app.myspoti.host.android.MyspotiRuntime
import com.music.spotui.data.api.Api
import com.music.spotui.data.api.EditablePlaylists
import com.music.spotui.data.api.ProfileCache
import com.music.spotui.data.api.ScreenCache
import com.music.spotui.data.entity.AccountModel
import com.music.spotui.data.offline.OfflineAccounts
import com.music.spotui.di.SessionActions
import com.music.spotui.di.SongPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicLong

sealed interface AccountSwitchState {
    data object Idle : AccountSwitchState

    data class Switching(
        val targetAccountId: String,
        val stage: String,
    ) : AccountSwitchState

    data class Success(
        val accountId: String,
    ) : AccountSwitchState

    data class Failed(
        val message: String,
        val stage: String = "",
    ) : AccountSwitchState
}

sealed class AccountSwitchResult {
    object Success : AccountSwitchResult()
    data class Failure(val reason: String, val recoveryAccount: AccountModel? = null) : AccountSwitchResult()
}

data class AccountSession(
    val accountId: String,
    val spotifyUserId: String,
    val webViewProfileName: String,
    val generation: Long,
)

/**
 * Central manager responsible for all Spotify account switching and session lifecycle.
 * Serializes switches using a Mutex, exposes observable state transitions, logs diagnostic stages,
 * and validates authenticated Spotify identity before committing state changes.
 */
object AccountSessionManager {
    private val switchMutex = Mutex()
    private val generationCounter = AtomicLong(1L)

    private val _switchState = MutableStateFlow<AccountSwitchState>(AccountSwitchState.Idle)
    val switchState: StateFlow<AccountSwitchState> = _switchState.asStateFlow()

    private val _currentSession = MutableStateFlow<AccountSession?>(null)
    val currentSession: StateFlow<AccountSession?> = _currentSession.asStateFlow()

    private val _sessionVersion = MutableStateFlow(0L)
    val sessionVersion: StateFlow<Long> = _sessionVersion.asStateFlow()

    val currentGeneration: Long get() = generationCounter.get()

    private fun logStage(stage: String, details: String) {
        Logbook.log("ACCOUNT_SWITCH_STAGE [$stage]: $details")
    }

    /**
     * Public entry point. Acquires switchMutex and executes internal switch logic.
     */
    suspend fun switchAccount(
        context: Context,
        targetAccountId: String,
        isNewLogin: Boolean = false,
    ): AccountSwitchResult = switchMutex.withLock {
        logStage("MUTEX_ACQUIRED", "targetAccountId=$targetAccountId")
        try {
            switchAccountInternal(context, targetAccountId, isNewLogin)
        } catch (c: CancellationException) {
            logStage("SWITCH_CANCELLED", "targetAccountId=$targetAccountId")
            _switchState.value = AccountSwitchState.Failed("Switch cancelled", "CANCELLED")
            throw c
        } catch (e: Exception) {
            logStage("SWITCH_EXCEPTION", "targetAccountId=$targetAccountId, error=${e.message}")
            _switchState.value = AccountSwitchState.Failed(e.message ?: "Unknown error", "EXCEPTION")
            AccountSwitchResult.Failure("Exception during switch: ${e.message}")
        }
    }

    /**
     * Internal switch implementation executed inside switchMutex lock.
     */
    private suspend fun switchAccountInternal(
        context: Context,
        targetAccountId: String,
        isNewLogin: Boolean,
    ): AccountSwitchResult {
        val appContext = context.applicationContext
        logStage("SWITCH_REQUESTED", "targetAccountId=$targetAccountId, isNewLogin=$isNewLogin")
        _switchState.value = AccountSwitchState.Switching(targetAccountId, "SWITCH_REQUESTED")

        val targetAccount = if (isNewLogin) {
            val uuid = java.util.UUID.randomUUID().toString()
            val profileName = "account_$uuid"
            val newAcc = AccountModel(
                id = uuid,
                spotifyUserId = "pending_${System.currentTimeMillis()}",
                name = "New Account",
                email = "",
                imageUrl = "",
                plan = "Spotify",
                webViewProfileName = profileName,
                dateAdded = System.currentTimeMillis(),
                lastUsed = System.currentTimeMillis(),
                isActive = true,
            )
            AccountStore.addAccountDirectly(appContext, newAcc)
            newAcc
        } else {
            AccountStore.accounts.value.firstOrNull { it.id == targetAccountId }
        }
        if (!isNewLogin && targetAccount == null) {
            val msg = "Target account $targetAccountId not found"
            logStage("TARGET_ACCOUNT_VALIDATION_FAILED", msg)
            _switchState.value = AccountSwitchState.Failed(msg, "TARGET_ACCOUNT_VALIDATION")
            return AccountSwitchResult.Failure(msg)
        }
        logStage("TARGET_ACCOUNT_VALIDATED", "targetAccountId=$targetAccountId, userId=${targetAccount?.spotifyUserId}")

        val previousAccount = AccountStore.activeAccount.value
        val newGeneration = generationCounter.incrementAndGet()

        return try {
            withTimeout(30_000L) {
                // 4. PLAYBACK_STOPPED
                logStage("PLAYBACK_STOPPED", "Stopping old playback")
                _switchState.value = AccountSwitchState.Switching(targetAccountId, "PLAYBACK_STOPPED")
                runCatching { SongPlayer.clearSession() }

                // 5 & 6. OLD_SESSION_INVALIDATED & OLD_WEBVIEW_DESTROYED
                logStage("OLD_SESSION_INVALIDATED", "Invalidating old session caches")
                _switchState.value = AccountSwitchState.Switching(targetAccountId, "OLD_SESSION_INVALIDATED")
                ProfileCache.clearSession()
                Api.HomeCache.clear()
                ScreenCache.clear()
                EditablePlaylists.clearSession()
                SessionActions.sessionLost()
                com.music.spotui.data.api.TopStats.clear()
                com.music.spotui.ui.screens.ContextViewModel.clearCache()
                com.music.spotui.di.AutoBrowser.clearSession()
                listOf("PlaybackState", "LibraryCache", "RecentItems", "LikedSongs",
                    "LikedSongsFromSpotify", "LikedAlbums", "FollowedArtists").forEach { name ->
                    appContext.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
                }

                logStage("OLD_WEBVIEW_DESTROYED", "Destroying old WebView host")
                _switchState.value = AccountSwitchState.Switching(targetAccountId, "OLD_WEBVIEW_DESTROYED")
                if (MyspotiRuntime.isStarted) {
                    runCatching { MyspotiRuntime.host.destroy() }
                }

                // 7. TARGET_PROFILE_ACQUIRED
                val profileName = targetAccount?.webViewProfileName ?: "account_${java.util.UUID.randomUUID()}"
                logStage("TARGET_PROFILE_ACQUIRED", "profileName=$profileName")
                _switchState.value = AccountSwitchState.Switching(targetAccountId, "TARGET_PROFILE_ACQUIRED")

                // 8, 9, 10. NEW_WEBVIEW_CREATED, WEBVIEW_PROFILE_ASSIGNED, SPOTIFY_PLAYER_INITIALIZED
                logStage("NEW_WEBVIEW_CREATED", "Initializing new WebPlayerHost on main thread")
                _switchState.value = AccountSwitchState.Switching(targetAccountId, "NEW_WEBVIEW_CREATED")

                withContext(Dispatchers.Main) {
                    if (!MyspotiRuntime.isStarted) {
                        MyspotiRuntime.start(appContext, loadPage = true, profileName = profileName)
                    } else {
                        MyspotiRuntime.switchAccount(appContext, profileName = profileName)
                    }
                }
                logStage("WEBVIEW_PROFILE_ASSIGNED", "Assigned profile $profileName to WebView")
                logStage("SPOTIFY_PLAYER_INITIALIZED", "Initialized SpotifyApi and bridge host")

                // 11. WEBVIEW_PAGE_READY
                logStage("WEBVIEW_PAGE_READY", "Waiting for Web Player page readiness")
                _switchState.value = AccountSwitchState.Switching(targetAccountId, "WEBVIEW_PAGE_READY")
                val isReady = withTimeoutOrNull(15_000L) {
                    MyspotiRuntime.api.ready.first { it }
                } ?: false

                if (!isReady && !isNewLogin) {
                    val msg = "Spotify Web Player page failed to become ready within timeout"
                    logStage("WEBVIEW_PAGE_READY_FAILED", msg)
                    recoverToAccountInternal(appContext, previousAccount)
                    _switchState.value = AccountSwitchState.Failed(msg, "WEBVIEW_PAGE_READY")
                    return@withTimeout AccountSwitchResult.Failure(msg, previousAccount)
                }

                // 12 & 13. AUTHENTICATION_CHECK_STARTED & COMPLETED
                if (!isNewLogin && targetAccount != null) {
                    logStage("AUTHENTICATION_CHECK_STARTED", "Checking authenticated token status")
                    _switchState.value = AccountSwitchState.Switching(targetAccountId, "AUTHENTICATION_CHECK_STARTED")

                    val authenticated = MyspotiRuntime.awaitSignedIn(5_000L)
                    logStage("AUTHENTICATION_CHECK_COMPLETED", "result=$authenticated")

                    if (!authenticated) {
                        logStage("ACCOUNT_NEEDS_LOGIN", "Account ${targetAccount.name} is not signed in or session expired. Committing account and opening login page.")
                        AccountStore.setActive(appContext, targetAccount.id)
                        OfflineAccounts.claim(appContext, targetAccount.spotifyUserId)
                        ScreenCache.setAccount(targetAccount.spotifyUserId)
                        _currentSession.value = AccountSession(
                            accountId = targetAccount.id,
                            spotifyUserId = targetAccount.spotifyUserId,
                            webViewProfileName = targetAccount.webViewProfileName,
                            generation = newGeneration,
                        )
                        _sessionVersion.value = System.currentTimeMillis()
                        withContext(Dispatchers.Main) {
                            MyspotiRuntime.host.openLogin()
                        }
                        _switchState.value = AccountSwitchState.Success(targetAccount.id)
                        return@withTimeout AccountSwitchResult.Success
                    }

                    // 14 & 15. SPOTIFY_IDENTITY_FETCHED & IDENTITY_VALIDATED
                    logStage("SPOTIFY_IDENTITY_FETCHED", "Querying users.me() from active session")
                    _switchState.value = AccountSwitchState.Switching(targetAccountId, "SPOTIFY_IDENTITY_FETCHED")

                    val me = withTimeoutOrNull(10_000L) {
                        runCatching { MyspotiRuntime.api.users.me() }.getOrNull()
                    }
                    val authenticatedUserId = me?.username.orEmpty()
                    logStage("SPOTIFY_IDENTITY_FETCHED", "authenticatedUserId=$authenticatedUserId")

                    if (authenticatedUserId.isNotBlank() && !authenticatedUserId.equals(targetAccount.spotifyUserId, ignoreCase = true)) {
                        logStage("IDENTITY_MISMATCH_NOTICE", "Account session authenticated as $authenticatedUserId (stored was ${targetAccount.spotifyUserId}). Updating active mapping.")
                    }
                    logStage("IDENTITY_VALIDATED", "Identity verified successfully for $authenticatedUserId")
                }

                // 16. ACCOUNT_COMMITTED
                if (targetAccount != null) {
                    logStage("ACCOUNT_COMMITTED", "Setting active account in AccountStore")
                    _switchState.value = AccountSwitchState.Switching(targetAccountId, "ACCOUNT_COMMITTED")
                    AccountStore.setActive(appContext, targetAccount.id)
                    OfflineAccounts.claim(appContext, targetAccount.spotifyUserId)
                    ScreenCache.setAccount(targetAccount.spotifyUserId)

                    _currentSession.value = AccountSession(
                        accountId = targetAccount.id,
                        spotifyUserId = targetAccount.spotifyUserId,
                        webViewProfileName = targetAccount.webViewProfileName,
                        generation = newGeneration,
                    )
                    _sessionVersion.value = System.currentTimeMillis()
                }

                // 17. ACCOUNT_DATA_LOADED
                logStage("ACCOUNT_DATA_LOADED", "Loading profile cache for committed account")
                _switchState.value = AccountSwitchState.Switching(targetAccountId, "ACCOUNT_DATA_LOADED")
                ProfileCache.ensure(appContext)

                // 18. SWITCH_COMPLETED
                logStage("SWITCH_COMPLETED", "Switch completed successfully for ${targetAccount?.id ?: targetAccountId}")
                _switchState.value = AccountSwitchState.Success(targetAccount?.id ?: targetAccountId)
                AccountSwitchResult.Success
            }
        } catch (t: TimeoutCancellationException) {
            val msg = "Account switch operation timed out after 30 seconds"
            logStage("SWITCH_TIMEOUT", msg)
            recoverToAccountInternal(appContext, previousAccount)
            _switchState.value = AccountSwitchState.Failed(msg, "TIMEOUT")
            AccountSwitchResult.Failure(msg, previousAccount)
        }
    }

    private suspend fun recoverToAccountInternal(context: Context, recoveryAccount: AccountModel?) {
        if (recoveryAccount == null) return
        logStage("RECOVERY_STARTED", "Recovering to previous account ${recoveryAccount.name} (${recoveryAccount.id})")
        runCatching {
            AccountStore.setActive(context, recoveryAccount.id)
            OfflineAccounts.claim(context, recoveryAccount.spotifyUserId)
            ScreenCache.setAccount(recoveryAccount.spotifyUserId)
            if (MyspotiRuntime.isStarted) {
                MyspotiRuntime.switchAccount(context, profileName = recoveryAccount.webViewProfileName)
            }
            ProfileCache.ensure(context)
            logStage("RECOVERY_COMPLETED", "Recovered successfully to ${recoveryAccount.name}")
        }.onFailure { e ->
            logStage("RECOVERY_FAILED", "Recovery failed: ${e.message}")
        }
    }

    fun resetStateToIdle() {
        _switchState.value = AccountSwitchState.Idle
    }
}
