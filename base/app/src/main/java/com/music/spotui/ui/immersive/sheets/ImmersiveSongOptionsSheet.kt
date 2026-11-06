package com.music.spotui.ui.immersive.sheets

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowCircleDown
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.ThumbDownOffAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.myspoti.engine.R
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.di.Haptics
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.AddToPlaylistSheet
import com.music.spotui.ui.components.CreditsSheet
import com.music.spotui.ui.components.SavedInSheet
import com.music.spotui.ui.components.ShareTarget
import com.music.spotui.ui.immersive.sheets.ImmersiveSleepTimerSheet as SleepTimerSheet
import com.music.spotui.ui.components.SpeedSheet
import com.music.spotui.ui.components.SpotifyCode
import com.music.spotui.ui.immersive.Immersive
import com.music.spotui.ui.navigation.albumRoute
import kotlinx.coroutines.launch

/**
 * Streamlined track options sheet for the Immersive player:
 * Removes redundant navigation & status rows in nowPlaying mode, grouping
 * recommendation controls under "Manage recommendations".
 */
@Composable
fun ImmersiveSongOptionsSheet(
    song: SongsModel,
    navController: NavController,
    context: Context,
    onDismiss: () -> Unit,
    nowPlaying: Boolean = false,
    onOpenQueue: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    currentPlaylistId: String? = null,
    lyricsOpen: Boolean = false,
) {
    val playerViewModel: com.music.spotui.ui.viewmodel.PlayerViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val liked = com.music.spotui.di.rememberTrackSaved(song.spotifyTrackId)
    var showSavedIn by remember { mutableStateOf(false) }
    var showAddTo by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showCredits by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var showMeaning by remember { mutableStateOf(false) }
    var showSources by remember { mutableStateOf(false) }
    var showNerd by remember { mutableStateOf(false) }
    var showQuality by remember { mutableStateOf(false) }
    var showDevices by remember { mutableStateOf(false) }
    var showRecommendations by remember { mutableStateOf(false) }

    if (showRecommendations) {
        RecommendationsSheet(song = song, context = context, onDismiss = { showRecommendations = false; onDismiss() })
        return
    }
    if (showQuality) { com.music.spotui.ui.components.QualitySheet(onDismiss = { showQuality = false; onDismiss() }); return }
    if (showDevices) { com.music.spotui.ui.components.DevicesSheet(onDismiss = { showDevices = false; onDismiss() }); return }
    if (showNerd) { com.music.spotui.ui.components.StatsForNerdsSheet(onDismiss = { showNerd = false; onDismiss() }); return }
    if (showSources) { com.music.spotui.ui.components.LyricsSourceSheet(onDismiss = { showSources = false; onDismiss() }); return }
    if (showCredits) { CreditsSheet(song = song, onDismiss = { showCredits = false; onDismiss() }); return }
    if (showMeaning) {
        ImmersiveMeaningSheet(song.spotifyTrackId, song.title, song.singer, null, coverUrl = song.coverUri) { showMeaning = false; onDismiss() }
        return
    }
    if (showSpeed) { SpeedSheet(onDismiss = { showSpeed = false; onDismiss() }); return }
    if (showSavedIn) { SavedInSheet(song = song, context = context, onDismiss = { showSavedIn = false; onDismiss() }); return }
    if (showAddTo) {
        AddToPlaylistSheet(song = song, context = context, onDismiss = { showAddTo = false; onDismiss() }, excludePlaylistId = currentPlaylistId)
        return
    }
    if (showSleep) { SleepTimerSheet(onDismiss = { showSleep = false; onDismiss() }); return }

    val hasId = song.spotifyTrackId.isNotBlank()
    val isRadio = nowPlaying && com.music.spotui.radio.RadioPlayer.current() != null
    val episodeKey = com.music.spotui.data.offline.episodeKeyOf(song)

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    ImmersiveSheet(onDismiss = onDismiss, imageUrl = song.coverUri) {
        ImmersiveSheetHeader(title = song.title, subtitle = song.singer, coverUrl = song.coverUri)
        ImmersiveSheetDivider()

        if (!nowPlaying) {
            ImmersiveSheetRow(
                icon = if (liked) null else Icons.Rounded.AddCircleOutline,
                label = stringResource(if (liked) R.string.collection_liked_songs_title else R.string.aligned_curation_context_menu_item_add_to_liked_songs),
                enabled = hasId,
                leading = if (liked) ({ LikedTick() }) else null,
            ) {
                SongPlayer.setTrackSaved(song.spotifyTrackId, !liked)
                toast(context.getString(if (liked) R.string.snackbar_removed_from_liked_songs else R.string.snackbar_added_to_liked_songs))
                onDismiss()
            }
        }

        val videoOn = com.music.spotui.video.MusicVideoController.on.value
        val clipSound = com.music.spotui.video.MusicVideoController.clipSound.value

        if (videoOn) {
            ImmersiveSheetRow(
                icon = if (clipSound) Icons.Default.VolumeUp else Icons.Default.MusicNote,
                label = stringResource(if (clipSound) R.string.clip_sound_clip else R.string.clip_sound_song),
            ) {
                com.music.spotui.video.MusicVideoController.setClipSound(!clipSound)
                val hint = context.getString(if (clipSound) R.string.clip_sound_off_hint else R.string.clip_sound_on_hint)
                toast(hint)
                onDismiss()
            }
        }

        if (onRemoveFromPlaylist != null) {
            ImmersiveSheetRow(Icons.Rounded.DeleteOutline, stringResource(R.string.context_menu_remove_from_playlist)) {
                onRemoveFromPlaylist()
                onDismiss()
            }
        }

        if (!nowPlaying) {
            ImmersiveSheetRow(Icons.AutoMirrored.Rounded.PlaylistAdd, stringResource(R.string.context_menu_add_to_playlist), enabled = hasId) { showSavedIn = true }
        }

        if (nowPlaying) {
            ImmersiveSheetRow(Icons.AutoMirrored.Rounded.PlaylistAdd, stringResource(R.string.context_menu_add_to_playlist), enabled = hasId) { showSavedIn = true }

        } else {
            ImmersiveSheetRow(Icons.AutoMirrored.Rounded.PlaylistPlay, stringResource(R.string.context_menu_add_to_queue), enabled = hasId) {
                SongPlayer.addToQueue(song.spotifyTrackId)
                toast(context.getString(R.string.snackbar_added_to_queue))
                onDismiss()
            }
        }



        if (nowPlaying && hasId && !song.url.startsWith("episode:")) {
            ImmersiveSheetRow(Icons.Rounded.Lightbulb, stringResource(R.string.lyrics_meaning_title)) { showMeaning = true }
        }

        if (nowPlaying && lyricsOpen) {
            val lyricsVm: com.music.spotui.ui.viewmodel.LyricsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val lyricsState = lyricsVm.state.collectAsState().value
            val lyrics = lyricsState as? com.music.spotui.ui.viewmodel.LyricsViewModel.State.Loaded
            val target = com.music.spotui.data.preferences.getLyricTranslateLang(context)
                .ifBlank { java.util.Locale.getDefault().language.ifBlank { "en" } }
            val sameLanguage = lyrics?.sourceLanguage?.substringBefore('-')
                ?.equals(target.substringBefore('-'), ignoreCase = true) == true

            if (lyrics != null) {
                if (!sameLanguage) {
                    val translateLang = com.music.spotui.data.preferences.getLyricTranslateLang(context)
                    val on = translateLang.isNotEmpty()
                    ImmersiveSheetRow(
                        Icons.Rounded.Translate, stringResource(R.string.lyrics_translation_title),
                        tint = if (on) LocalImmersiveSheetTint.current.accent else null,
                    ) {
                        if (on) com.music.spotui.data.preferences.setLyricTranslateLang(context, "")
                        else com.music.spotui.data.preferences.setLyricTranslateLang(context, target)
                        onDismiss()
                    }
                }
                if (lyrics.lyrics.hasRomanization) {
                    val on = com.music.spotui.data.preferences.LyricsRomanizationPref.on
                    ImmersiveSheetRow(
                        Icons.Rounded.SortByAlpha, stringResource(R.string.lyrics_romanization),
                        tint = if (on) LocalImmersiveSheetTint.current.accent else null,
                    ) {
                        com.music.spotui.data.preferences.LyricsRomanizationPref.set(context, !on)
                        onDismiss()
                    }
                }
            }
        }

        // Manage recommendations (Hide song, Exclude from taste, Don't play artist)
        if (hasId && episodeKey == null) {
            ImmersiveSheetRow(Icons.Rounded.ThumbDownOffAlt, stringResource(R.string.manage_recommendations)) {
                showRecommendations = true
            }
        }

        if (nowPlaying && song.url.startsWith("episode:")) {
            ImmersiveSheetRow(Icons.Rounded.Speed, stringResource(R.string.playback_speed)) { showSpeed = true }
        }
        // The app's own playback (extensions, downloads, files): its equalizer, speed and silences.
        if (nowPlaying && com.music.spotui.di.LocalPlayer.active.value) {
            ImmersiveSheetRow(Icons.Rounded.GraphicEq, stringResource(R.string.equalizer_title)) {
                onDismiss()
                navController.navigate(com.music.spotui.ui.navigation.Routes.Equalizer.route)
            }
        }
        ImmersiveSheetRow(Icons.Rounded.Share, stringResource(R.string.context_menu_share), enabled = hasId || isRadio) {
            if (isRadio) com.music.spotui.radio.RadioPlayer.share(context)
            else SpotifyCode.show(ShareTarget("spotify:track:${song.spotifyTrackId}", song.title, song.singer, song.coverUri))
            onDismiss()
        }
        if (nowPlaying) {
            ImmersiveSheetRow(
                Icons.Rounded.Bedtime,
                stringResource(R.string.context_menu_sleep_timer),
                tint = if (SongPlayer.sleepEndsAt.value != null) LocalImmersiveSheetTint.current.accent else null,
            ) { showSleep = true }

            ImmersiveSheetRow(
                Icons.Rounded.DirectionsCar,
                "Car Mode",
            ) {
                onDismiss()
                com.music.spotui.car.CarModeManager.enterCarMode(context)
                navController.navigate(com.music.spotui.ui.navigation.Routes.CarMode.route)
            }


            if (!com.music.spotui.data.preferences.PlayerSourceLinePref.shown) {
                ImmersiveSheetRow(Icons.Rounded.GraphicEq, stringResource(R.string.quality_sheet_title)) { showQuality = true }
            }
            if (!com.music.spotui.data.preferences.PlayerCapsulePref.shown) {
                ImmersiveSheetRow(Icons.Rounded.Devices, stringResource(R.string.listen_on)) { showDevices = true }
            }

            val keepScreenOn by com.music.spotui.data.preferences.ImmersiveSettingsPref.keepScreenOn.collectAsState()
            ImmersiveSheetRow(
                icon = Icons.Rounded.Tv,
                label = "Keep Screen On",
                leading = if (keepScreenOn) ({ LikedTick() }) else null,
            ) {
                com.music.spotui.data.preferences.ImmersiveSettingsPref.setKeepScreenOn(context, !keepScreenOn)
                toast(if (!keepScreenOn) "Keep Screen On enabled" else "Keep Screen On disabled")
                onDismiss()
            }

            val canvasEnabled by com.music.spotui.data.preferences.ImmersiveSettingsPref.canvasEnabled.collectAsState()
            ImmersiveSheetRow(
                icon = Icons.Rounded.OndemandVideo,
                label = "Canvas Video",
                leading = if (canvasEnabled) ({ LikedTick() }) else null,
            ) {
                com.music.spotui.data.preferences.ImmersiveSettingsPref.setCanvasEnabled(context, !canvasEnabled)
                toast(if (!canvasEnabled) "Canvas enabled" else "Canvas disabled")
                onDismiss()
            }

        }

        // Extension menu entries
        if (hasId) com.music.spotui.extensions.ExtensionMenuEntries(
            app.myspoti.extensions.Surface.TRACK_MENU, "spotify:track:${song.spotifyTrackId}", song.title, song.singer, onDone = onDismiss,
        )
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * Compact recommendations sheet grouping "Not interested", "Exclude from taste", and "Don't play artist".
 */
@Composable
private fun RecommendationsSheet(
    song: SongsModel,
    context: Context,
    onDismiss: () -> Unit,
) {
    val hasId = song.spotifyTrackId.isNotBlank()
    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    val hidden = if (hasId) com.music.spotui.data.preferences.HiddenSongs.isHidden(song.spotifyTrackId) else false

    ImmersiveSheet(onDismiss = onDismiss, imageUrl = song.coverUri) {
        ImmersiveSheetHeader(title = stringResource(R.string.manage_recommendations), subtitle = song.title, coverUrl = song.coverUri)
        ImmersiveSheetDivider()

        if (hasId) {
            ImmersiveSheetRow(
                icon = Icons.Rounded.ThumbDownOffAlt,
                label = stringResource(if (hidden) R.string.show_song_again else R.string.hide_song),
            ) {
                if (hidden) com.music.spotui.data.preferences.HiddenSongs.show(context, song.spotifyTrackId)
                else SongPlayer.hideSong(song)
                Haptics.confirm()
                onDismiss()
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** The filled tick of a liked song, in the sheet's accent. */
@Composable
private fun LikedTick() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(24.dp).background(LocalImmersiveSheetTint.current.accent, CircleShape),
    ) {
        androidx.compose.material3.Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
    }
}
