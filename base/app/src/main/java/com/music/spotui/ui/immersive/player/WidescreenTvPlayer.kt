/*
 * Myspoti — Premium TV / Widescreen Immersive Player for Honor MagicOS Desktop Mode & large displays.
 * GPL-3.0-or-later.
 */
package com.music.spotui.ui.immersive.player

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myspoti.engine.R
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.music.spotui.data.preferences.TvModePref
import com.music.spotui.di.Haptics
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.SongBlendCover
import com.music.spotui.ui.components.desktopClickable
import com.music.spotui.ui.components.handPointer
import com.music.spotui.ui.components.secondaryClick
import com.music.spotui.ui.immersive.Immersive
import com.music.spotui.ui.immersive.lyrics.ImmersiveLyricsPanel
import com.music.spotui.ui.immersive.lyrics.ImmersiveQueuePanel
import com.music.spotui.ui.look.MeshPalette
import com.music.spotui.ui.look.ThinSlider
import com.music.spotui.ui.look.rememberArtworkColors
import com.music.spotui.ui.look.rememberPauseScale
import com.music.spotui.ui.look.rememberReduceMotion
import kotlinx.coroutines.delay

private const val COVER_DECODE_PX = 640

/** Sub-panel mode for the right side of the widescreen display. */
internal object WidescreenRightTab {
    const val LYRICS = 0
    const val QUEUE = 1
}

/**
 * Premium Widescreen / TV Immersive Player.
 *
 * Supports Honor MagicOS Desktop Mode, DeX, and large windows:
 * - Four Presets: Balanced (default), Cinema (media focus), Lyrics (sing-along focus), Minimal (clean art).
 * - Auto-Hide Controls on inactivity (mouse move, clicks, keys, drag restores them).
 * - Ambient Screensaver Mode after extended inactivity while playing.
 * - Up Next preview card announcing the upcoming song before track end.
 * - Complete mouse hover, hit targets (44-48dp), hand pointers, and secondary click support.
 */
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun WidescreenTvPlayer(
    state: ImmersivePlayerState,
    actions: ImmersivePlayerActions,
    tint: Color,
    canvasUrl: String?,
    pauseScale: State<Float>,
    volume: PlayerVolume,
    onOpenDevices: () -> Unit,
    onMore: (() -> Unit)?,
    onSource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var rightTab by remember { mutableIntStateOf(WidescreenRightTab.LYRICS) }
    var canvasReady by remember(canvasUrl) {
        mutableStateOf(canvasUrl != null && com.music.spotui.ui.components.CanvasPlayer.isReady(canvasUrl))
    }

    // Current TV Preset (persisted)
    var currentPreset by remember { mutableStateOf(TvModePref.layoutPreset) }
    fun selectPreset(preset: String) {
        currentPreset = preset
        TvModePref.setLayout(context, preset)
    }

    // Inactivity Tracking for Auto-Hide and Ambient Mode
    val lastInteractionTimestamp = remember { java.util.concurrent.atomic.AtomicLong(SystemClock.elapsedRealtime()) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isAmbientActive by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    fun registerInteraction() {
        lastInteractionTimestamp.set(SystemClock.elapsedRealtime())
        if (!controlsVisible) controlsVisible = true
        if (isAmbientActive) isAmbientActive = false
    }

    // Auto-hide & Ambient timer coroutine
    LaunchedEffect(state.playing, volume.dragging, menuOpen) {
        while (true) {
            delay(500)
            val now = SystemClock.elapsedRealtime()
            val idleMs = now - lastInteractionTimestamp.get()
            val autoHideSec = TvModePref.autoHideSeconds
            val ambientSec = TvModePref.ambientSeconds

            // Auto-hide controls check
            if (!volume.dragging && !menuOpen && autoHideSec > 0) {
                if (idleMs >= autoHideSec * 1000L) {
                    if (controlsVisible) controlsVisible = false
                }
            } else {
                if (!controlsVisible) controlsVisible = true
            }

            // Ambient mode check (only when playing and not dragging/menu open)
            if (state.playing && !volume.dragging && !menuOpen && ambientSec > 0) {
                if (idleMs >= ambientSec * 1000L) {
                    if (!isAmbientActive) isAmbientActive = true
                }
            } else if (!state.playing) {
                if (isAmbientActive) isAmbientActive = false
            }
        }
    }

    val mv = com.music.spotui.video.MusicVideoController
    val readyVideo = mv.state.value as? com.music.spotui.video.MusicVideoController.State.Ready
    val remote = SongPlayer.activeDevice.value != null
    val videoMatchesTrack = state.trackId.isNotBlank() && readyVideo?.trackId == state.trackId
    val videoReady = videoMatchesTrack && !remote && !state.buffering && !com.music.spotui.data.DataSaver.active &&
            com.music.spotui.data.preferences.MusicVideoPref.on
    val videoOn = mv.on.value && videoMatchesTrack && !remote && com.music.spotui.data.preferences.MusicVideoPref.on

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        registerInteraction()
                    }
                }
            },
    ) {
        if (isAmbientActive) {
            // ── AMBIENT SCREENSAVER MODE ──
            WidescreenAmbientMode(
                state = state,
                tint = tint,
                canvasUrl = canvasUrl,
                canvasReady = canvasReady,
                onCanvasFirstFrame = { canvasReady = true },
                videoOn = videoOn,
                pauseScale = pauseScale,
                onWake = { registerInteraction() },
            )
        } else {
            // ── REGULAR TV PLAYER MODES ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                // Top Bar (Animated Visibility for Auto-Hide)
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { -it },
                    exit = fadeOut(tween(260)) + slideOutVertically(tween(260)) { -it },
                ) {
                    WidescreenTopBar(
                        state = state,
                        tint = tint,
                        currentPreset = currentPreset,
                        onSelectPreset = ::selectPreset,
                        videoReady = videoReady,
                        videoOn = videoOn,
                        onToggleVideo = { mv.setOn(!videoOn) },
                        onMore = onMore,
                        onSource = onSource,
                        onClose = actions.close,
                        onMenuStateChange = { menuOpen = it },
                    )
                }

                // Main Presets Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    when (currentPreset) {
                        TvModePref.LAYOUT_CINEMA -> {
                            CinemaPreset(
                                state = state,
                                actions = actions,
                                tint = tint,
                                canvasUrl = canvasUrl,
                                canvasReady = canvasReady,
                                onCanvasFirstFrame = { canvasReady = true },
                                videoOn = videoOn,
                                pauseScale = pauseScale,
                            )
                        }
                        TvModePref.LAYOUT_LYRICS -> {
                            LyricsPreset(
                                state = state,
                                actions = actions,
                                tint = tint,
                                canvasUrl = canvasUrl,
                                canvasReady = canvasReady,
                                onCanvasFirstFrame = { canvasReady = true },
                                videoOn = videoOn,
                                pauseScale = pauseScale,
                            )
                        }
                        TvModePref.LAYOUT_MINIMAL -> {
                            MinimalPreset(
                                state = state,
                                actions = actions,
                                tint = tint,
                                pauseScale = pauseScale,
                            )
                        }
                        else -> {
                            // Balanced Mode (Default)
                            BalancedPreset(
                                state = state,
                                actions = actions,
                                tint = tint,
                                canvasUrl = canvasUrl,
                                canvasReady = canvasReady,
                                onCanvasFirstFrame = { canvasReady = true },
                                videoOn = videoOn,
                                pauseScale = pauseScale,
                                rightTab = rightTab,
                                onSelectTab = { rightTab = it },
                            )
                        }
                    }

                    // Up Next preview card (isolated 1-fps tick to prevent whole-player recomposition)
                    UpNextPreviewHost(
                        state = state,
                        tint = tint,
                        controlsVisible = controlsVisible,
                        onOpenQueue = {
                            rightTab = WidescreenRightTab.QUEUE
                            registerInteraction()
                        },
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                }

                // Bottom Playback Control Bar (Animated Visibility for Auto-Hide)
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it },
                    exit = fadeOut(tween(280)) + slideOutVertically(tween(280)) { it },
                ) {
                    WidescreenBottomBar(
                        state = state,
                        actions = actions,
                        tint = tint,
                        volume = volume,
                        rightTab = rightTab,
                        onToggleLyrics = {
                            rightTab = if (rightTab == WidescreenRightTab.LYRICS) WidescreenRightTab.QUEUE else WidescreenRightTab.LYRICS
                        },
                        onToggleQueue = {
                            rightTab = if (rightTab == WidescreenRightTab.QUEUE) WidescreenRightTab.LYRICS else WidescreenRightTab.QUEUE
                        },
                        onOpenDevices = onOpenDevices,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/** Top bar for widescreen layout with Preset selector and management options. */
@Composable
private fun WidescreenTopBar(
    state: ImmersivePlayerState,
    tint: Color,
    currentPreset: String,
    onSelectPreset: (String) -> Unit,
    videoReady: Boolean,
    videoOn: Boolean,
    onToggleVideo: () -> Unit,
    onMore: (() -> Unit)?,
    onSource: () -> Unit,
    onClose: () -> Unit,
    onMenuStateChange: (Boolean) -> Unit,
) {
    var showPresetMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Context / Playing from
        val contextName = state.contextName.ifEmpty {
            if (state.album.isNotBlank()) state.album else state.title
        }
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.contextName.isNotBlank() || state.album.isNotBlank()) {
                Text(
                    text = "PLAYING FROM",
                    style = Immersive.Caption.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = tint.copy(alpha = 0.45f),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = contextName,
                style = Immersive.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = tint.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Layout Preset Selector Button
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.12f))
                    .desktopClickable(minTargetSize = 38.dp, hoverHighlight = true) {
                        showPresetMenu = true
                        onMenuStateChange(true)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = when (currentPreset) {
                        TvModePref.LAYOUT_CINEMA -> Icons.Rounded.Movie
                        TvModePref.LAYOUT_LYRICS -> Icons.Rounded.Lyrics
                        TvModePref.LAYOUT_MINIMAL -> Icons.Rounded.Tune
                        else -> Icons.Rounded.Dashboard
                    },
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when (currentPreset) {
                        TvModePref.LAYOUT_CINEMA -> "CINEMA"
                        TvModePref.LAYOUT_LYRICS -> "LYRICS"
                        TvModePref.LAYOUT_MINIMAL -> "MINIMAL"
                        else -> "BALANCED"
                    },
                    style = Immersive.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                    color = tint,
                )
            }

            DropdownMenu(
                expanded = showPresetMenu,
                onDismissRequest = {
                    showPresetMenu = false
                    onMenuStateChange(false)
                },
                modifier = Modifier.background(Color(0xFF1E1E22)),
            ) {
                DropdownMenuItem(
                    text = { Text("Balanced Mode (Two-panel)", color = Color.White) },
                    leadingIcon = { Icon(Icons.Rounded.Dashboard, null, tint = tint) },
                    onClick = {
                        onSelectPreset(TvModePref.LAYOUT_BALANCED)
                        showPresetMenu = false
                        onMenuStateChange(false)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Cinema Mode (Full Video)", color = Color.White) },
                    leadingIcon = { Icon(Icons.Rounded.Movie, null, tint = tint) },
                    onClick = {
                        onSelectPreset(TvModePref.LAYOUT_CINEMA)
                        showPresetMenu = false
                        onMenuStateChange(false)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Lyrics Mode (Focus)", color = Color.White) },
                    leadingIcon = { Icon(Icons.Rounded.Lyrics, null, tint = tint) },
                    onClick = {
                        onSelectPreset(TvModePref.LAYOUT_LYRICS)
                        showPresetMenu = false
                        onMenuStateChange(false)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Minimal Mode (Art only)", color = Color.White) },
                    leadingIcon = { Icon(Icons.Rounded.Tune, null, tint = tint) },
                    onClick = {
                        onSelectPreset(TvModePref.LAYOUT_MINIMAL)
                        showPresetMenu = false
                        onMenuStateChange(false)
                    },
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Sound source quality badge
        val sourceLabel = com.music.spotui.ui.components.playingSourceLabel()
        if (sourceLabel.isNotBlank() && !state.adPlaying) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(tint.copy(alpha = 0.12f))
                    .desktopClickable(minTargetSize = 36.dp, hoverHighlight = true, onClick = onSource)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = sourceLabel,
                    style = Immersive.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = tint.copy(alpha = 0.8f),
                )
            }
            Spacer(Modifier.width(10.dp))
        }

        // Music Video toggle button
        if (videoReady || videoOn) {
            CircleGlyph(
                icon = Icons.Default.OndemandVideo,
                contentDescription = stringResource(R.string.av_content_desc),
                onClick = onToggleVideo,
                active = videoOn,
            )
            Spacer(Modifier.width(8.dp))
        }

        // More options menu
        if (onMore != null) {
            CircleGlyph(
                icon = Icons.Rounded.MoreHoriz,
                contentDescription = stringResource(R.string.immersive_player_more),
                onClick = onMore,
            )
            Spacer(Modifier.width(8.dp))
        }

        // Desktop close button
        CircleGlyph(
            icon = Icons.Filled.Close,
            contentDescription = stringResource(R.string.immersive_player_close),
            onClick = onClose,
        )
    }
}

/**
 * PRESET 1: BALANCED (Default Two-Panel layout).
 */
@Composable
private fun BalancedPreset(
    state: ImmersivePlayerState,
    actions: ImmersivePlayerActions,
    tint: Color,
    canvasUrl: String?,
    canvasReady: Boolean,
    onCanvasFirstFrame: () -> Unit,
    videoOn: Boolean,
    pauseScale: State<Float>,
    rightTab: Int,
    onSelectTab: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WidescreenCinematicPanel(
            state = state,
            actions = actions,
            tint = tint,
            canvasUrl = canvasUrl,
            canvasReady = canvasReady,
            onCanvasFirstFrame = onCanvasFirstFrame,
            videoOn = videoOn,
            pauseScale = pauseScale,
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight(),
        )
        WidescreenRightPanel(
            state = state,
            tint = tint,
            currentTab = rightTab,
            onSelectTab = onSelectTab,
            modifier = Modifier
                .weight(0.95f)
                .fillMaxHeight(),
        )
    }
}

/**
 * PRESET 2: CINEMA MODE (Full large 16:9 media area with compact metadata overlay).
 */
@Composable
private fun CinemaPreset(
    state: ImmersivePlayerState,
    actions: ImmersivePlayerActions,
    tint: Color,
    canvasUrl: String?,
    canvasReady: Boolean,
    onCanvasFirstFrame: () -> Unit,
    videoOn: Boolean,
    pauseScale: State<Float>,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        WidescreenCinematicPanel(
            state = state,
            actions = actions,
            tint = tint,
            canvasUrl = canvasUrl,
            canvasReady = canvasReady,
            onCanvasFirstFrame = onCanvasFirstFrame,
            videoOn = videoOn,
            pauseScale = pauseScale,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * PRESET 3: LYRICS MODE (Large lyrics prominence, with small floating artwork card).
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun LyricsPreset(
    state: ImmersivePlayerState,
    actions: ImmersivePlayerActions,
    tint: Color,
    canvasUrl: String?,
    canvasReady: Boolean,
    onCanvasFirstFrame: () -> Unit,
    videoOn: Boolean,
    pauseScale: State<Float>,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Floating compact cover & track identity on the left
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
            ) {
                GlideImage(
                    model = state.coverUrl.takeIf { it.isNotBlank() },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    requestBuilderTransform = { it.override(COVER_DECODE_PX) },
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = state.title,
                style = Immersive.Title.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.artist,
                style = Immersive.Caption.copy(fontSize = 16.sp),
                color = tint.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Full-height expansive live lyrics view
        Box(
            modifier = Modifier
                .weight(1.4f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.28f))
                .border(1.dp, tint.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
        ) {
            ImmersiveLyricsPanel(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}

/**
 * PRESET 4: MINIMAL MODE (Clean, elegant artwork with dynamic background).
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun MinimalPreset(
    state: ImmersivePlayerState,
    actions: ImmersivePlayerActions,
    tint: Color,
    pauseScale: State<Float>,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .graphicsLayer {
                        scaleX = pauseScale.value
                        scaleY = pauseScale.value
                    }
                    .shadow(24.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, tint.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
            ) {
                GlideImage(
                    model = state.coverUrl.takeIf { it.isNotBlank() },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    requestBuilderTransform = { it.override(COVER_DECODE_PX) },
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = state.title,
                style = Immersive.Title.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.artist,
                style = Immersive.Caption.copy(fontSize = 18.sp),
                color = tint.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Ambient Mode Screensaver:
 * Visual television presentation with prominent media, ambient float, and gentle progress.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun WidescreenAmbientMode(
    state: ImmersivePlayerState,
    tint: Color,
    canvasUrl: String?,
    canvasReady: Boolean,
    onCanvasFirstFrame: () -> Unit,
    videoOn: Boolean,
    pauseScale: State<Float>,
    onWake: () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    val infiniteTransition = rememberInfiniteTransition(label = "ambientMovement")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ambientY",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .desktopClickable(minTargetSize = 48.dp, onClick = onWake),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 540.dp, height = 304.dp)
                    .graphicsLayer {
                        if (!reduceMotion) translationY = floatY
                    }
                    .shadow(32.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, tint.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    videoOn -> {
                        com.music.spotui.ui.components.VideoSurface(
                            modifier = Modifier.fillMaxSize(),
                            active = true,
                            onTap = onWake,
                        )
                    }
                    canvasUrl != null -> {
                        com.music.spotui.ui.screens.CanvasVideo(
                            url = canvasUrl,
                            onFirstFrame = onCanvasFirstFrame,
                            playing = state.playing && !state.buffering,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        GlideImage(
                            model = state.coverUrl.takeIf { it.isNotBlank() },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            requestBuilderTransform = { it.override(COVER_DECODE_PX) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = state.title,
                style = Immersive.Title.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.artist,
                style = Immersive.Caption.copy(fontSize = 20.sp),
                color = tint.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Isolated Up Next Preview Host:
 * Evaluates queue and remaining duration on a 1-fps cadence without triggering recomposition
 * across the rest of the widescreen player layout or video/lyrics panels.
 */
@Composable
private fun UpNextPreviewHost(
    state: ImmersivePlayerState,
    tint: Color,
    controlsVisible: Boolean,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!TvModePref.upNextEnabled || state.adPlaying || !state.playing || state.durationMs <= 0L) return

    val frame = com.music.spotui.ui.components.rememberFrameClock(state.playing, fps = 1)
    frame.value

    val currentPos = state.positionMs()
    val duration = state.durationMs
    val timeLeftSec = if (duration > 0 && currentPos > 0) ((duration - currentPos) / 1000L) else 999L

    val queue = SongPlayer.engineQueue.value
    val nextTrack = if (state.repeatMode == ImmersiveRepeat.ONE) {
        state.song
    } else {
        queue.queued.firstOrNull()?.song
            ?: queue.nextUp.firstOrNull()?.song
            ?: SongPlayer.nowPlaying.value.next
    }

    val showUpNext = nextTrack != null && timeLeftSec in 1L..TvModePref.upNextSeconds.toLong()

    androidx.compose.animation.AnimatedVisibility(
        visible = showUpNext,
        enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 2 },
        exit = fadeOut(tween(280)) + slideOutVertically(tween(280)) { it / 2 },
        modifier = modifier
            .padding(end = 36.dp, bottom = if (controlsVisible) 110.dp else 24.dp),
    ) {
        nextTrack?.let { track ->
            UpNextCard(
                song = track,
                tint = tint,
                secondsRemaining = timeLeftSec,
                onClick = onOpenQueue,
            )
        }
    }
}

/**
 * Up Next Preview Card: Glassmorphic popup in bottom right.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun UpNextCard(
    song: com.music.spotui.data.entity.SongsModel,
    tint: Color,
    secondsRemaining: Long,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF18181C).copy(alpha = 0.90f))
            .border(1.dp, tint.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
            .shadow(16.dp, RoundedCornerShape(14.dp))
            .desktopClickable(minTargetSize = 44.dp, hoverHighlight = true, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.2f)),
        ) {
            GlideImage(
                model = song.coverUri.takeIf { it.isNotBlank() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                requestBuilderTransform = { it.override(120) },
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.widthIn(max = 200.dp)) {
            Text(
                text = "UP NEXT IN ${secondsRemaining}S",
                style = Immersive.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp),
                color = tint,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.title,
                style = Immersive.Title.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.singer,
                style = Immersive.Caption.copy(fontSize = 12.sp),
                color = tint.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Left Panel: Large 16:9 cinematic Canvas, Music Video, or Animated Album Artwork.
 */
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
private fun WidescreenCinematicPanel(
    state: ImmersivePlayerState,
    actions: ImmersivePlayerActions,
    tint: Color,
    canvasUrl: String?,
    canvasReady: Boolean,
    onCanvasFirstFrame: () -> Unit,
    videoOn: Boolean,
    pauseScale: State<Float>,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    val infiniteTransition = rememberInfiniteTransition(label = "ambientArtworkFloat")
    val ambientFloat by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ambientOffset",
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val containerWidth = minOf(maxWidth, maxHeight * (16f / 9f))
            val containerHeight = containerWidth * (9f / 16f)

            Box(
                modifier = Modifier
                    .size(width = containerWidth, height = containerHeight)
                    .graphicsLayer {
                        scaleX = pauseScale.value
                        scaleY = pauseScale.value
                        if (!reduceMotion && state.playing && canvasUrl == null && !videoOn) {
                            translationY = ambientFloat
                        }
                    }
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(20.dp),
                        clip = false,
                        ambientColor = tint.copy(alpha = 0.25f),
                        spotColor = Color.Black.copy(alpha = 0.5f),
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.40f))
                    .border(1.dp, tint.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    videoOn -> {
                        Box(Modifier.fillMaxSize()) {
                            com.music.spotui.ui.components.VideoSurface(
                                modifier = Modifier.fillMaxSize(),
                                active = true,
                                onTap = { },
                            )
                            com.music.spotui.ui.components.VideoFullscreenButton(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                            )
                        }
                    }
                    canvasUrl != null -> {
                        Box(Modifier.fillMaxSize()) {
                            GlideImage(
                                model = state.coverUrl.takeIf { it.isNotBlank() },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                transition = CrossFade,
                                loading = placeholder(R.drawable.placeholder),
                                failure = placeholder(R.drawable.placeholder),
                                modifier = Modifier.fillMaxSize(),
                                requestBuilderTransform = { it.override(COVER_DECODE_PX) },
                            )
                            com.music.spotui.ui.screens.CanvasVideo(
                                url = canvasUrl,
                                onFirstFrame = onCanvasFirstFrame,
                                playing = state.playing && !state.buffering,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        alpha = if (canvasReady) 1f else 0f
                                    },
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.25f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.40f),
                                            ),
                                        ),
                                    ),
                            )
                        }
                    }
                    else -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            GlideImage(
                                model = state.coverUrl.takeIf { it.isNotBlank() },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(28.dp)
                                    .graphicsLayer { alpha = 0.45f },
                                requestBuilderTransform = { it.override(240) },
                            )
                            Box(
                                modifier = Modifier
                                    .size(containerHeight * 0.88f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .shadow(12.dp, RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.2f)),
                            ) {
                                GlideImage(
                                    model = state.coverUrl.takeIf { it.isNotBlank() },
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    transition = CrossFade,
                                    loading = placeholder(R.drawable.placeholder),
                                    failure = placeholder(R.drawable.placeholder),
                                    modifier = Modifier.fillMaxSize(),
                                    requestBuilderTransform = { it.override(COVER_DECODE_PX) },
                                )
                                SongBlendCover(state.blend, Modifier.fillMaxSize()) { it.override(COVER_DECODE_PX) }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Track title, artists, and like action below the cinematic screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .secondaryClick { actions.openAlbum() },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (state.adPlaying) stringResource(R.string.immersive_player_ad) else state.title,
                        style = Immersive.Title.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .basicMarquee(iterations = Int.MAX_VALUE)
                            .desktopClickable(minTargetSize = 36.dp, onClick = actions.openAlbum),
                    )
                    if (state.explicit && !state.adPlaying) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(tint.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.explicit_badge),
                                style = Immersive.Caption.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color.Black,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                if (!state.adPlaying) {
                    val credit = com.music.spotui.ui.components.rememberArtistCredit(
                        state.trackId,
                        state.artist,
                        tint.copy(alpha = 0.65f),
                    ) { a -> actions.openArtistRef(a) }
                    Text(
                        text = credit ?: androidx.compose.ui.text.AnnotatedString(state.artist),
                        style = Immersive.Title.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = tint.copy(alpha = 0.70f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .basicMarquee(iterations = Int.MAX_VALUE)
                            .desktopClickable(minTargetSize = 32.dp, onClick = actions.openArtist),
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Desktop-friendly Like Star glyph
            CircleGlyph(
                icon = if (state.liked) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = stringResource(
                    if (state.liked) R.string.immersive_player_unlike else R.string.immersive_player_like,
                ),
                onClick = actions.toggleLike,
                active = state.liked,
            )
        }
    }
}

/**
 * Right Panel: Synchronized live lyrics or playback queue embedded in frosted glass container.
 */
@Composable
private fun WidescreenRightPanel(
    state: ImmersivePlayerState,
    tint: Color,
    currentTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.32f))
            .border(1.dp, tint.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WidescreenTabButton(
                    title = stringResource(R.string.immersive_player_lyrics).uppercase(),
                    selected = currentTab == WidescreenRightTab.LYRICS,
                    tint = tint,
                    onClick = { onSelectTab(WidescreenRightTab.LYRICS) },
                )
                WidescreenTabButton(
                    title = stringResource(R.string.immersive_player_queue).uppercase(),
                    selected = currentTab == WidescreenRightTab.QUEUE,
                    tint = tint,
                    onClick = { onSelectTab(WidescreenRightTab.QUEUE) },
                )
            }

            Crossfade(
                targetState = currentTab,
                animationSpec = tween(220),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "widescreenRightContent",
            ) { tab ->
                when (tab) {
                    WidescreenRightTab.LYRICS -> {
                        ImmersiveLyricsPanel(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                    WidescreenRightTab.QUEUE -> {
                        ImmersiveQueuePanel(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Clickable tab button with underline and hover indicator. */
@Composable
private fun WidescreenTabButton(
    title: String,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Column(
        modifier = Modifier
            .desktopClickable(minTargetSize = 40.dp, hoverHighlight = true, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = title,
            style = Immersive.Caption.copy(
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 1.2.sp,
            ),
            color = if (selected) Color.White else if (isHovered) Color.White.copy(alpha = 0.85f) else tint.copy(alpha = 0.50f),
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(if (selected) tint else Color.Transparent),
        )
    }
}

/**
 * Bottom Playback Bar for Widescreen TV mode.
 */
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
private fun WidescreenBottomBar(
    state: ImmersivePlayerState,
    actions: ImmersivePlayerActions,
    tint: Color,
    volume: PlayerVolume,
    rightTab: Int,
    onToggleLyrics: () -> Unit,
    onToggleQueue: () -> Unit,
    onOpenDevices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.38f))
            .border(1.dp, tint.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        PlayerScrubber(state = state, onSeek = actions.seekTo)

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left segment: Album thumbnail + Track info
            Row(
                modifier = Modifier
                    .weight(1f)
                    .desktopClickable(minTargetSize = 44.dp, onClick = actions.openAlbum),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.2f)),
                ) {
                    GlideImage(
                        model = state.coverUrl.takeIf { it.isNotBlank() },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        requestBuilderTransform = { it.override(120) },
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = if (state.adPlaying) stringResource(R.string.immersive_player_ad) else state.title,
                        style = Immersive.Title.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.artist,
                        style = Immersive.Caption.copy(fontSize = 13.sp),
                        color = tint.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Center segment: Transport Controls
            Row(
                modifier = Modifier.weight(1.4f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DesktopControlIcon(
                    icon = if (state.smart) Icons.Rounded.AutoAwesome else Icons.Rounded.Shuffle,
                    contentDescription = stringResource(
                        when {
                            state.smart -> R.string.smart_shuffle_title
                            state.shuffle -> R.string.immersive_player_shuffle_on
                            else -> R.string.immersive_player_shuffle_off
                        },
                    ),
                    tint = if (state.shuffle || state.smart) tint else tint.copy(alpha = 0.45f),
                    onClick = actions.toggleShuffle,
                )

                Spacer(Modifier.width(14.dp))

                DesktopControlIcon(
                    icon = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.immersive_player_previous),
                    size = 32.dp,
                    onClick = actions.previous,
                )

                Spacer(Modifier.width(14.dp))

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .desktopClickable(minTargetSize = 52.dp) {
                            Haptics.click()
                            actions.playPause()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.buffering) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Icon(
                            imageVector = if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(
                                if (state.playing) R.string.immersive_player_pause else R.string.immersive_player_play,
                            ),
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                DesktopControlIcon(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.immersive_player_next),
                    size = 32.dp,
                    onClick = actions.next,
                )

                Spacer(Modifier.width(14.dp))

                DesktopControlIcon(
                    icon = if (state.repeatMode == ImmersiveRepeat.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = when (state.repeatMode) {
                        ImmersiveRepeat.ONE -> stringResource(R.string.immersive_player_repeat_one)
                        ImmersiveRepeat.ALL -> stringResource(R.string.immersive_player_repeat_all)
                        else -> stringResource(R.string.immersive_player_repeat_off)
                    },
                    tint = if (state.repeatMode != ImmersiveRepeat.OFF) tint else tint.copy(alpha = 0.45f),
                    onClick = actions.cycleRepeat,
                )
            }

            // Right segment: Volume, Devices, Lyrics/Queue toggles
            Row(
                modifier = Modifier.weight(1.1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.VolumeDown,
                    contentDescription = stringResource(R.string.immersive_player_volume_down),
                    tint = tint.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                ThinSlider(
                    value = volume.level.value,
                    onValueChange = volume::drag,
                    onValueChangeFinished = volume::release,
                    idleHeight = 5.dp,
                    activeHeight = 8.dp,
                    modifier = Modifier.width(96.dp),
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = stringResource(R.string.immersive_player_volume_up),
                    tint = tint.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp),
                )

                Spacer(Modifier.width(16.dp))

                DesktopControlIcon(
                    icon = Icons.Rounded.Devices,
                    contentDescription = stringResource(R.string.immersive_player_devices),
                    tint = if (state.deviceName != null) tint else tint.copy(alpha = 0.6f),
                    onClick = onOpenDevices,
                )

                Spacer(Modifier.width(10.dp))

                DesktopControlIcon(
                    icon = if (rightTab == WidescreenRightTab.LYRICS) Icons.Rounded.Lyrics else Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = stringResource(
                        if (rightTab == WidescreenRightTab.LYRICS) R.string.immersive_player_lyrics else R.string.immersive_player_queue,
                    ),
                    tint = tint,
                    onClick = if (rightTab == WidescreenRightTab.LYRICS) onToggleQueue else onToggleLyrics,
                )
            }
        }
    }
}

/** Hover-responsive desktop control glyph with minimum 44dp hit target. */
@Composable
private fun DesktopControlIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .desktopClickable(minTargetSize = 44.dp, hoverHighlight = true, shape = CircleShape) {
                Haptics.click()
                onClick()
            }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size),
        )
    }
}
