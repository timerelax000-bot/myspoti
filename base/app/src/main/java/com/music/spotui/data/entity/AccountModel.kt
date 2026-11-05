package com.music.spotui.data.entity

import kotlinx.serialization.Serializable

/**
 * A saved Spotify account in Myspoti's multi-account manager.
 *
 * Each account has its own WebView profile, its own offline storage (see OfflineAccounts),
 * and its own session. The [spotifyUserId] is the Spotify user URI's id part
 * (the value after `spotify:user:`).
 */
@Serializable
data class AccountModel(
    /** Local UUID, unique within the Myspoti account list. */
    val id: String = "",
    /** Spotify user id (the part after `spotify:user:`). */
    val spotifyUserId: String = "",
    /** Display name from the Spotify profile. */
    val name: String = "",
    /** E-mail address from the Spotify profile (may be blank if unavailable). */
    val email: String = "",
    /** Profile image URL. */
    val imageUrl: String = "",
    /** Spotify plan (e.g. "premium", "free"). */
    val plan: String = "",
    /** The name of the isolated WebView profile for this account. */
    val webViewProfileName: String = "",
    /** Epoch millis when this account was added to Myspoti. */
    val dateAdded: Long = 0L,
    /** Epoch millis when this account was last actively used. */
    val lastUsed: Long = 0L,
    /** Whether this is the currently active account. */
    val isActive: Boolean = false,
    /** User-assigned custom label (optional rename). */
    val customLabel: String = "",
    /** Whether this account is the default account on app start. */
    val isDefault: Boolean = false,
)
