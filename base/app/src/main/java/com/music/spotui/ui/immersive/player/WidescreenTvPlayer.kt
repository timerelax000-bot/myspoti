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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
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
import com.music.spotui.ui.look.MeshGradientBackground
import com.music.spotui.ui.look.rememberArtworkColors
import com.music.spotui.ui.components.rememberFrameClock
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
    var showPresetMenu by remember { mutableStateOf(false) }
    fun selectPreset(preset: String) {
        currentPreset = preset
        TvModePref.setLayout(context, preset)
    }

    // Inactivity Tracking for Auto-Hide and Ambient Mode
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionKey by remember { mutableLongStateOf(0L) }
    var isAmbientActive by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var lastPointerPosition by remember { mutableStateOf<Offset?>(null) }

    fun registerInteraction() {
        interactionKey++
        if (!controlsVisible) controlsVisible = true
        if (isAmbientActive) isAmbientActive = false
    }

    // Auto-hide & Ambient countdown logic (Single authoritative source of truth)
    LaunchedEffect(interactionKey, controlsVisible, volume.dragging, menuOpen, state.playing) {
        if (!controlsVisible) {
            // Controls are hidden and remain hidden.
            // Check for ambient screensaver transition if playing and not interacting
            val ambientSec = TvModePref.ambientSeconds
            if (state.playing && ambientSec > 0 && !isAmbientActive) {
                val autoHideSec = TvModePref.autoHideSeconds
                val remainingAmbientSec = (ambientSec - autoHideSec).coerceAtLeast(1)
                delay(remainingAmbientSec * 1000L)
                if (!volume.dragging && !menuOpen && state.playing) {
                    isAmbientActive = true
                }
            }
            return@LaunchedEffect
        }

        // Controls are visible: countdown to auto-hide
        val autoHideSec = TvModePref.autoHideSeconds
        if (autoHideSec > 0 && !volume.dragging && !menuOpen) {
            delay(autoHideSec * 1000L)
            controlsVisible = false
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
            .onPreviewKeyEvent {
                registerInteraction()
                false
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        when (event.type) {
                            PointerEventType.Press, PointerEventType.Release -> {
                                registerInteraction()
                            }
                            PointerEventType.Scroll -> {
                                registerInteraction()
                            }
                            PointerEventType.Move -> {
                                val currentPos = event.changes.firstOrNull()?.position
                                if (currentPos != null) {
                                    val prev = lastPointerPosition
                                    if (prev == null) {
                                        lastPointerPosition = currentPos
                                    } else {
                                        val dist = (currentPos - prev).getDistance()
                                        // Ignore subpixel jitter or layout changes: only genuine physical movement > 4px
                                        if (dist > 4f) {
                                            lastPointerPosition = currentPos
                                            registerInteraction()
                                        }
                                    }
                                }
                            }
                            else -> {
                                // Ignore Enter, Exit, Unknown events caused by layout shifts or animations!
                            }
                        }
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
            // ── REGULAR TV PLAYER MODES (STABLE OVERLAY LAYOUT) ──
            // Layer 1: Main Presets Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(
                        top = if (currentPreset == TvModePref.LAYOUT_BALANCED) 0.dp else 48.dp,
                        bottom = if (currentPreset == TvModePref.LAYOUT_BALANCED) 0.dp else 96.dp,
                    ),
            ) {
                Crossfade(
                    targetState = currentPreset,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    label = "tvPresetCrossfade",
                ) { preset ->
                    when (preset) {
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
                            // Balanced Mode (Default 1:1 matching reference image)
                            BalancedPreset(
                                state = state,
                                actions = actions,
                                tint = tint,
                                canvasUrl = canvasUrl,
                                canvasReady = canvasReady,
                                onCanvasFirstFrame = { canvasReady = true },
                                videoOn = videoOn,
                                pauseScale = pauseScale,
                                volume = volume,
                                rightTab = rightTab,
                                onSelectTab = { rightTab = it },
                                onOpenPresets = { showPresetMenu = true },
                                onMore = onMore,
                            )
                        }
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

            // Layer 2: Top Bar (Shown for Cinema, Lyrics, Minimal presets when controls are visible)
            if (currentPreset != TvModePref.LAYOUT_BALANCED) {
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(240, easing = FastOutSlowInEasing)) + slideInVertically(tween(240, easing = FastOutSlowInEasing)) { -it },
                    exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) + slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { -it },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding(),
                ) {
                    WidescreenTopBar(
                        state = state,
                        tint = tint,
                        currentPreset = currentPreset,
                        onSelectPreset = ::selectPreset,
                        videoReady = videoReady,
                        videoOn = videoOn,
                        onToggleVideo = { mv.handleShortClick() },
                        rightTab = rightTab,
                        onSelectTab = { rightTab = it },
                        onMore = onMore,
                        onSource = onSource,
                        onClose = actions.close,
                        onMenuStateChange = { menuOpen = it },
                    )
                }
            }

            // Layer 3: Bottom Playback Control Bar (Shown for Cinema, Lyrics, Minimal presets)
            AnimatedVisibility(
                visible = controlsVisible && currentPreset != TvModePref.LAYOUT_BALANCED,
                enter = fadeIn(tween(260, easing = FastOutSlowInEasing)) + slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it },
                exit = fadeOut(tween(220, easing = FastOutSlowInEasing)) + slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
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
                        .padding(horizontal = 28.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/**
 * Focus- and hover-responsive glyph button for TV/Desktop top bar & actions.
 * Enforces minimum 48dp hit targets, clear focus indication, and press feedback.
 */
@Composable
private fun TvCircleGlyph(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    tint: Color = LocalPlayerTint.current,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = rememberReduceMotion()

    val scale by animateFloatAsState(
        targetValue = when {
            reduceMotion -> 1f
            isPressed -> 0.90f
            isHovered || isFocused -> 1.06f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "tvGlyphScale",
    )

    val baseAlpha = if (active) 0.32f else 0.14f
    val bgAlpha = when {
        isFocused -> 0.38f
        isHovered -> 0.24f
        else -> baseAlpha
    }
    val animatedBgAlpha by animateFloatAsState(bgAlpha, tween(160), label = "tvGlyphBg")

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(tint.copy(alpha = animatedBgAlpha))
            .then(
                if (isFocused) Modifier.border(2.dp, tint.copy(alpha = 0.65f), CircleShape) else Modifier
            )
            .desktopClickable(minTargetSize = size, shape = CircleShape) {
                Haptics.click()
                onClick()
            }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = icon,
            animationSpec = tween(160, easing = FastOutSlowInEasing),
            label = "tvGlyphIconCrossfade",
        ) { targetIcon ->
            Icon(
                imageVector = targetIcon,
                contentDescription = null,
                tint = if (active) tint else Color.White,
                modifier = Modifier.size(iconSize),
            )
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
    rightTab: Int = WidescreenRightTab.LYRICS,
    onSelectTab: (Int) -> Unit = {},
    onMore: (() -> Unit)?,
    onSource: () -> Unit,
    onClose: () -> Unit,
    onMenuStateChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var showPresetMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top-Left Row of 6 Translucent Circular Icon Buttons matching reference image
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 1. Queue Toggle Button
            TvCircleGlyph(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = "Queue",
                onClick = { onSelectTab(if (rightTab == WidescreenRightTab.QUEUE) WidescreenRightTab.LYRICS else WidescreenRightTab.QUEUE) },
                active = rightTab == WidescreenRightTab.QUEUE,
                size = 38.dp,
                iconSize = 18.dp,
            )

            // 2. Video Toggle Button
            TvCircleGlyph(
                icon = Icons.Rounded.Videocam,
                contentDescription = "Video",
                onClick = onToggleVideo,
                active = videoOn,
                size = 38.dp,
                iconSize = 18.dp,
            )

            // 3. Preset Selector Button
            Box {
                TvCircleGlyph(
                    icon = Icons.Rounded.Dashboard,
                    contentDescription = "Presets",
                    onClick = {
                        showPresetMenu = true
                        onMenuStateChange(true)
                    },
                    active = currentPreset == TvModePref.LAYOUT_BALANCED,
                    size = 38.dp,
                    iconSize = 18.dp,
                )

                DropdownMenu(
                    expanded = showPresetMenu,
                    onDismissRequest = {
                        showPresetMenu = false
                        onMenuStateChange(false)
                    },
                    modifier = Modifier
                        .background(Color(0xFF1A1A1E))
                        .border(1.dp, tint.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
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

            // 4. Lyrics View Button
            TvCircleGlyph(
                icon = Icons.Rounded.Lyrics,
                contentDescription = "Lyrics",
                onClick = { onSelectTab(WidescreenRightTab.LYRICS) },
                active = rightTab == WidescreenRightTab.LYRICS,
                size = 38.dp,
                iconSize = 18.dp,
            )

            // 5. Settings / More Options Button
            if (onMore != null) {
                TvCircleGlyph(
                    icon = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.immersive_player_more),
                    onClick = onMore,
                    size = 38.dp,
                    iconSize = 18.dp,
                )
            }

            // 6. Close Button
            TvCircleGlyph(
                icon = Icons.Filled.Close,
                contentDescription = stringResource(R.string.immersive_player_close),
                onClick = onClose,
                size = 38.dp,
                iconSize = 18.dp,
            )
        }

        // Top-Right Context / Source Quality Info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val contextName = state.contextName.ifEmpty {
                if (state.album.isNotBlank()) state.album else state.title
            }
            if (contextName.isNotBlank()) {
                Text(
                    text = contextName,
                    style = Immersive.Caption.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val sourceLabel = com.music.spotui.ui.components.playingSourceLabel()
            if (sourceLabel.isNotBlank() && !state.adPlaying) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .desktopClickable(minTargetSize = 40.dp, hoverHighlight = true, shape = RoundedCornerShape(8.dp), onClick = onSource)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = sourceLabel,
                        style = Immersive.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

/**
 * Custom scrubber for Balanced Preset displaying exact elapsed time (left) and total duration (right)
 * without negative signs, matching the reference image specification.
 */
@Composable
private fun BalancedScrubber(
    state: ImmersivePlayerState,
    onSeek: (Long) -> Unit,
) {
    val scrub = rememberPlayerScrub(state.title, state.positionMs, state.durationMs)
    val frame = rememberFrameClock(state.playing && !state.buffering, fps = 15)
    Column(Modifier.fillMaxWidth()) {
        ThinSlider(
            value = { frame.value; scrub.shown(state.positionMs(), state.durationMs) },
            onValueChange = scrub::drag,
            onValueChangeFinished = { scrub.release { onSeek((it * state.durationMs).toLong()) } },
            loading = state.buffering && !scrub.scrubbing,
            activeColor = Color.White,
            inactiveColor = Color.White.copy(alpha = 0.25f),
            idleHeight = 4.dp,
            activeHeight = 7.dp,
        )
        frame.value
        val shown = scrub.shown(state.positionMs(), state.durationMs)
        val duration = state.durationMs.coerceAtLeast(0L)
        val elapsed = (shown * duration).toLong()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-6).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(elapsed),
                style = Immersive.Caption.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = Color.White.copy(alpha = 0.70f),
            )
            Text(
                text = formatTime(duration),
                style = Immersive.Caption.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = Color.White.copy(alpha = 0.70f),
            )
        }
    }
}

/**
 * PRESET 1: BALANCED (Redesigned Cinematic Split-Screen Layout matching reference image).
 * Left (~43%): Media Canvas/Video with Bottom-Left Song Details, Progress Bar & Playback Controls.
 * Right (~57%): Full-Height Synchronized Live Lyrics with bold active line and subtle glow.
 */
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
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
    volume: PlayerVolume,
    rightTab: Int,
    onSelectTab: (Int) -> Unit,
    onOpenPresets: () -> Unit = {},
    onMore: (() -> Unit)? = null,
) {
    val artworkColors = rememberArtworkColors(state.coverUrl)
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Spicy Lyrics Ambient Mesh Gradient Backdrop
        MeshGradientBackground(
            palette = artworkColors,
            trackKey = state.coverUrl,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
        )

        val isCompact = maxHeight < 480.dp
        val titleFont = if (isCompact) 24.sp else 32.sp
        val artistFont = if (isCompact) 15.sp else 20.sp
        val itemGap = if (isCompact) 6.dp else 12.dp
        val btnSize = if (isCompact) 38.dp else 44.dp
        val prevNextSize = if (isCompact) 40.dp else 48.dp
        val playBtnSize = if (isCompact) 44.dp else 52.dp

        val canvasAlpha by animateFloatAsState(
            targetValue = if (canvasReady) 1f else 0f,
            animationSpec = tween(400, easing = FastOutSlowInEasing),
            label = "balancedCanvasFade",
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── LEFT COLUMN (~50% width): Edge-to-Edge Full-Height Media Canvas with Smooth Crossfade & Overlaid UI ──
            Box(
                modifier = Modifier
                    .weight(1.08f)
                    .fillMaxHeight(),
            ) {
                Crossfade(
                    targetState = state.trackId.ifBlank { state.coverUrl },
                    animationSpec = tween(400, easing = FastOutSlowInEasing),
                    modifier = Modifier.fillMaxSize(),
                    label = "balancedMediaCrossfade",
                ) { _ ->
                    Box(Modifier.fillMaxSize()) {
                        when {
                            videoOn -> {
                                Box(Modifier.fillMaxSize()) {
                                    com.music.spotui.ui.components.VideoSurface(
                                        modifier = Modifier.fillMaxSize(),
                                        active = true,
                                        onTap = { },
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
                                        modifier = Modifier.fillMaxSize(),
                                        requestBuilderTransform = { it.override(COVER_DECODE_PX) },
                                    )
                                    com.music.spotui.ui.screens.CanvasVideo(
                                        url = canvasUrl,
                                        onFirstFrame = onCanvasFirstFrame,
                                        playing = state.playing && !state.buffering,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { alpha = canvasAlpha },
                                    )
                                    SongBlendCover(state.blend, Modifier.fillMaxSize()) { it.override(COVER_DECODE_PX) }
                                }
                            }
                            else -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    GlideImage(
                                        model = state.coverUrl.takeIf { it.isNotBlank() },
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        requestBuilderTransform = { it.override(COVER_DECODE_PX) },
                                    )
                                    SongBlendCover(state.blend, Modifier.fillMaxSize()) { it.override(COVER_DECODE_PX) }
                                }
                            }
                        }
                    }
                }

                // 2. Dark Vertical Gradient Scrim over the full-height Canvas/Video so overlaid UI stays crisp
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Black.copy(alpha = 0.90f),
                                ),
                            ),
                        ),
                )

                // 3. Top-Left Floating Action Buttons Row over Video/Canvas
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TvCircleGlyph(
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        onClick = { onSelectTab(if (rightTab == WidescreenRightTab.QUEUE) WidescreenRightTab.LYRICS else WidescreenRightTab.QUEUE) },
                        active = rightTab == WidescreenRightTab.QUEUE,
                        size = 36.dp,
                        iconSize = 18.dp,
                    )
                    TvCircleGlyph(
                        icon = Icons.Rounded.Videocam,
                        contentDescription = "Video",
                        onClick = { com.music.spotui.video.MusicVideoController.handleShortClick() },
                        active = videoOn,
                        size = 36.dp,
                        iconSize = 18.dp,
                    )
                    TvCircleGlyph(
                        icon = Icons.Rounded.Dashboard,
                        contentDescription = "Presets",
                        onClick = onOpenPresets,
                        active = true,
                        size = 36.dp,
                        iconSize = 18.dp,
                    )
                    TvCircleGlyph(
                        icon = Icons.Rounded.Lyrics,
                        contentDescription = "Lyrics",
                        onClick = { onSelectTab(WidescreenRightTab.LYRICS) },
                        active = rightTab == WidescreenRightTab.LYRICS,
                        size = 36.dp,
                        iconSize = 18.dp,
                    )
                    if (onMore != null) {
                        TvCircleGlyph(
                            icon = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.immersive_player_more),
                            onClick = onMore,
                            size = 36.dp,
                            iconSize = 18.dp,
                        )
                    }
                    TvCircleGlyph(
                        icon = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.immersive_player_close),
                        onClick = actions.close,
                        size = 36.dp,
                        iconSize = 18.dp,
                    )
                }

                // 4. Bottom-Left Overlaid Track Title, Artist, Scrubber & Playback Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                ) {
                    // Track Title
                    Text(
                        text = if (state.adPlaying) stringResource(R.string.immersive_player_ad) else state.title,
                        style = Immersive.Title.copy(fontSize = titleFont, fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    )
                    Spacer(Modifier.height(2.dp))
                    // Artist Name
                    Text(
                        text = state.artist,
                        style = Immersive.Caption.copy(fontSize = artistFont, fontWeight = FontWeight.Medium),
                        color = Color.White.copy(alpha = 0.80f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(itemGap))

                    // Scrubber Bar
                    BalancedScrubber(state = state, onSeek = actions.seekTo)

                    Spacer(Modifier.height(if (isCompact) 4.dp else 8.dp))

                    // Playback Controls Row (Heart, Shuffle, Prev, Play/Pause, Next, Repeat, Volume)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Heart / Like Button (REAL Heart Icon)
                        DesktopControlIcon(
                            icon = if (state.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = stringResource(if (state.liked) R.string.immersive_player_unlike else R.string.immersive_player_like),
                            tint = if (state.liked) Color(0xFFFF4D6D) else Color.White.copy(alpha = 0.85f),
                            active = state.liked,
                            activeColor = Color(0xFFFF4D6D),
                            onClick = actions.toggleLike,
                            targetSize = btnSize,
                            size = if (isCompact) 20.dp else 22.dp,
                        )

                        // Shuffle
                        DesktopControlIcon(
                            icon = if (state.smart) Icons.Rounded.AutoAwesome else Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (state.shuffle || state.smart) tint else Color.White.copy(alpha = 0.55f),
                            active = state.shuffle || state.smart,
                            activeColor = tint,
                            targetSize = btnSize,
                            size = if (isCompact) 18.dp else 20.dp,
                            onClick = actions.toggleShuffle,
                        )

                        // Previous
                        DesktopControlIcon(
                            icon = Icons.Rounded.SkipPrevious,
                            contentDescription = stringResource(R.string.immersive_player_previous),
                            tint = Color.White,
                            targetSize = prevNextSize,
                            size = if (isCompact) 22.dp else 24.dp,
                            onClick = actions.previous,
                        )

                        // Play / Pause (Prominent Central Button)
                        DesktopControlIcon(
                            icon = if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(if (state.playing) R.string.immersive_player_pause else R.string.immersive_player_play),
                            tint = Color.Black,
                            active = true,
                            activeColor = Color.White,
                            targetSize = playBtnSize,
                            size = if (isCompact) 24.dp else 28.dp,
                            onClick = actions.playPause,
                        )

                        // Next
                        DesktopControlIcon(
                            icon = Icons.Rounded.SkipNext,
                            contentDescription = stringResource(R.string.immersive_player_next),
                            tint = Color.White,
                            targetSize = prevNextSize,
                            size = if (isCompact) 22.dp else 24.dp,
                            onClick = actions.next,
                        )

                        // Repeat
                        DesktopControlIcon(
                            icon = if (state.repeatMode == ImmersiveRepeat.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (state.repeatMode != ImmersiveRepeat.OFF) tint else Color.White.copy(alpha = 0.55f),
                            active = state.repeatMode != ImmersiveRepeat.OFF,
                            activeColor = tint,
                            targetSize = btnSize,
                            size = if (isCompact) 18.dp else 20.dp,
                            onClick = actions.cycleRepeat,
                        )

                        // Volume
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(if (isCompact) 16.dp else 18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            ThinSlider(
                                value = volume.level.value,
                                onValueChange = volume::drag,
                                onValueChangeFinished = volume::release,
                                idleHeight = 5.dp,
                                activeHeight = 8.dp,
                                modifier = Modifier.width(if (isCompact) 60.dp else 76.dp),
                            )
                        }
                    }
                }
            }

            // ── RIGHT COLUMN (~50% width): Full-Height Synchronized Live Lyrics / Queue ──
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight(),
            ) {
                if (rightTab == WidescreenRightTab.LYRICS) {
                    ImmersiveLyricsPanel(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    )
                } else {
                    ImmersiveQueuePanel(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
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
            .padding(horizontal = 48.dp, vertical = 8.dp),
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
            .padding(horizontal = 36.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Floating compact cover & track identity on the left
        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .shadow(20.dp, RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, tint.copy(alpha = 0.15f), RoundedCornerShape(18.dp)),
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
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = state.title,
                style = Immersive.Title.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.artist,
                style = Immersive.Caption.copy(fontSize = 16.sp),
                color = tint.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Full-height expansive live lyrics view
        Box(
            modifier = Modifier
                .weight(1.45f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black.copy(alpha = 0.32f))
                .border(1.dp, tint.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
        ) {
            ImmersiveLyricsPanel(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
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
            .padding(horizontal = 36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(340.dp)
                    .graphicsLayer {
                        scaleX = pauseScale.value
                        scaleY = pauseScale.value
                    }
                    .shadow(28.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.2.dp, tint.copy(alpha = 0.18f), RoundedCornerShape(24.dp)),
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
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = state.title,
                style = Immersive.Title.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.artist,
                style = Immersive.Caption.copy(fontSize = 18.sp),
                color = tint.copy(alpha = 0.75f),
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
        enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) + slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 2 },
        exit = fadeOut(tween(240, easing = FastOutSlowInEasing)) + slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { it / 2 },
        modifier = modifier
            .padding(end = 36.dp, bottom = if (controlsVisible) 124.dp else 28.dp),
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
 * Sized 30-40% larger for TV distance readability.
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
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141418).copy(alpha = 0.94f))
            .border(1.5.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .shadow(20.dp, RoundedCornerShape(16.dp))
            .desktopClickable(minTargetSize = 56.dp, hoverHighlight = true, shape = RoundedCornerShape(16.dp), onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.3f)),
        ) {
            GlideImage(
                model = song.coverUri.takeIf { it.isNotBlank() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                transition = CrossFade,
                loading = placeholder(R.drawable.placeholder),
                failure = placeholder(R.drawable.placeholder),
                modifier = Modifier.fillMaxSize(),
                requestBuilderTransform = { it.override(200) },
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.widthIn(min = 180.dp, max = 280.dp)) {
            Text(
                text = "UP NEXT IN ${secondsRemaining}S",
                style = Immersive.Caption.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                ),
                color = tint,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = song.title,
                style = Immersive.Title.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.singer,
                style = Immersive.Caption.copy(fontSize = 13.sp),
                color = tint.copy(alpha = 0.70f),
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

            Spacer(Modifier.width(16.dp))

            // Desktop-friendly Like Star glyph (48dp hit target)
            TvCircleGlyph(
                icon = if (state.liked) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = stringResource(
                    if (state.liked) R.string.immersive_player_unlike else R.string.immersive_player_like,
                ),
                onClick = actions.toggleLike,
                active = state.liked,
                tint = if (state.liked) Color(0xFFFFD700) else tint,
                size = 48.dp,
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
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black.copy(alpha = 0.32f))
            .border(1.dp, tint.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
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
                animationSpec = tween(240, easing = FastOutSlowInEasing),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "widescreenRightContent",
            ) { tab ->
                when (tab) {
                    WidescreenRightTab.LYRICS -> {
                        ImmersiveLyricsPanel(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    WidescreenRightTab.QUEUE -> {
                        ImmersiveQueuePanel(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Clickable tab button with underline and hover indicator (48dp minimum hit target). */
@Composable
private fun WidescreenTabButton(
    title: String,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(
        modifier = Modifier
            .desktopClickable(minTargetSize = 48.dp, hoverHighlight = true, shape = RoundedCornerShape(8.dp), onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = title,
            style = Immersive.Caption.copy(
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 1.2.sp,
            ),
            color = if (selected) Color.White else if (isHovered || isFocused) Color.White.copy(alpha = 0.90f) else tint.copy(alpha = 0.50f),
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.5.dp)
                .clip(CircleShape)
                .background(if (selected) tint else Color.Transparent),
        )
    }
}

/**
 * Bottom Playback Bar for Widescreen TV mode.
 * - Symmetrical 3-part layout (left, center, right) keeping transport controls perfectly centered.
 * - Removed duplicate song title & artist info from bottom-left.
 * - Enlarged clickable surfaces (>= 56dp for main transport controls).
 * - Clear hover, focus, and tactile press animations.
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
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF101014).copy(alpha = 0.82f))
            .border(1.dp, tint.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .shadow(16.dp, RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        PlayerScrubber(state = state, onSeek = actions.seekTo)

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left segment: Clean Star / Like action balancing the layout (NO duplicate title/artist)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DesktopControlIcon(
                    icon = if (state.liked) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = stringResource(
                        if (state.liked) R.string.immersive_player_unlike else R.string.immersive_player_like,
                    ),
                    tint = if (state.liked) Color(0xFFFFD700) else tint.copy(alpha = 0.65f),
                    active = state.liked,
                    activeColor = Color(0xFFFFD700),
                    onClick = actions.toggleLike,
                    targetSize = 52.dp,
                )
            }

            // Center segment: Transport Controls (Centered with generous 18dp spacing and >=56dp hit targets)
            Row(
                modifier = Modifier.weight(1.5f),
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
                    active = state.shuffle || state.smart,
                    activeColor = tint,
                    targetSize = 52.dp,
                    size = 22.dp,
                    onClick = actions.toggleShuffle,
                )

                Spacer(Modifier.width(18.dp))

                DesktopControlIcon(
                    icon = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.immersive_player_previous),
                    targetSize = 56.dp,
                    size = 28.dp,
                    onClick = actions.previous,
                )

                Spacer(Modifier.width(18.dp))

                // Play / Pause prominent hero button (64dp interactive target)
                val playInteractionSource = remember { MutableInteractionSource() }
                val isPlayHovered by playInteractionSource.collectIsHoveredAsState()
                val isPlayFocused by playInteractionSource.collectIsFocusedAsState()
                val isPlayPressed by playInteractionSource.collectIsPressedAsState()
                val reduceMotion = rememberReduceMotion()

                val playScale by animateFloatAsState(
                    targetValue = when {
                        reduceMotion -> 1f
                        isPlayPressed -> 0.92f
                        isPlayHovered || isPlayFocused -> 1.05f
                        else -> 1f
                    },
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "playScale",
                )

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer {
                            scaleX = playScale
                            scaleY = playScale
                        }
                        .shadow(if (isPlayHovered || isPlayFocused) 12.dp else 6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .then(
                            if (isPlayFocused) Modifier.border(2.5.dp, tint, CircleShape) else Modifier
                        )
                        .desktopClickable(minTargetSize = 64.dp, shape = CircleShape) {
                            Haptics.click()
                            actions.playPause()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.buffering) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp),
                        )
                    } else {
                        Crossfade(
                            targetState = state.playing,
                            animationSpec = tween(160, easing = FastOutSlowInEasing),
                            label = "playPauseCrossfade",
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(
                                    if (playing) R.string.immersive_player_pause else R.string.immersive_player_play,
                                ),
                                tint = Color.Black,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.width(18.dp))

                DesktopControlIcon(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.immersive_player_next),
                    targetSize = 56.dp,
                    size = 28.dp,
                    onClick = actions.next,
                )

                Spacer(Modifier.width(18.dp))

                DesktopControlIcon(
                    icon = if (state.repeatMode == ImmersiveRepeat.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = when (state.repeatMode) {
                        ImmersiveRepeat.ONE -> stringResource(R.string.immersive_player_repeat_one)
                        ImmersiveRepeat.ALL -> stringResource(R.string.immersive_player_repeat_all)
                        else -> stringResource(R.string.immersive_player_repeat_off)
                    },
                    tint = if (state.repeatMode != ImmersiveRepeat.OFF) tint else tint.copy(alpha = 0.45f),
                    active = state.repeatMode != ImmersiveRepeat.OFF,
                    activeColor = tint,
                    targetSize = 52.dp,
                    size = 22.dp,
                    onClick = actions.cycleRepeat,
                )
            }

            // Right segment: Volume, Devices, Lyrics/Queue toggles (Arrangement.End)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DesktopControlIcon(
                    icon = Icons.AutoMirrored.Rounded.VolumeDown,
                    contentDescription = stringResource(R.string.immersive_player_volume_down),
                    tint = tint.copy(alpha = 0.55f),
                    targetSize = 48.dp,
                    size = 20.dp,
                    onClick = {
                        val current = volume.level.value
                        volume.drag((current - 0.05f).coerceAtLeast(0f))
                        volume.release()
                    },
                )
                Spacer(Modifier.width(4.dp))
                ThinSlider(
                    value = volume.level.value,
                    onValueChange = volume::drag,
                    onValueChangeFinished = volume::release,
                    idleHeight = 6.dp,
                    activeHeight = 10.dp,
                    modifier = Modifier.width(124.dp),
                )
                Spacer(Modifier.width(4.dp))
                DesktopControlIcon(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = stringResource(R.string.immersive_player_volume_up),
                    tint = tint.copy(alpha = 0.55f),
                    targetSize = 48.dp,
                    size = 20.dp,
                    onClick = {
                        val current = volume.level.value
                        volume.drag((current + 0.05f).coerceAtMost(1f))
                        volume.release()
                    },
                )

                Spacer(Modifier.width(16.dp))

                DesktopControlIcon(
                    icon = Icons.Rounded.Devices,
                    contentDescription = stringResource(R.string.immersive_player_devices),
                    tint = if (state.deviceName != null) tint else tint.copy(alpha = 0.6f),
                    active = state.deviceName != null,
                    targetSize = 52.dp,
                    size = 24.dp,
                    onClick = onOpenDevices,
                )

                Spacer(Modifier.width(12.dp))

                DesktopControlIcon(
                    icon = if (rightTab == WidescreenRightTab.LYRICS) Icons.Rounded.Lyrics else Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = stringResource(
                        if (rightTab == WidescreenRightTab.LYRICS) R.string.immersive_player_lyrics else R.string.immersive_player_queue,
                    ),
                    tint = tint,
                    active = true,
                    targetSize = 52.dp,
                    size = 24.dp,
                    onClick = if (rightTab == WidescreenRightTab.LYRICS) onToggleQueue else onToggleLyrics,
                )
            }
        }
    }
}

/**
 * Focus- and hover-responsive desktop control glyph with minimum 48-56dp hit target.
 * Supports smooth spring press feedback, focus rings, hover glow, and icon crossfades.
 */
@Composable
private fun DesktopControlIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    targetSize: Dp = 52.dp,
    tint: Color = Color.White,
    active: Boolean = false,
    activeColor: Color = LocalPlayerTint.current,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = rememberReduceMotion()

    val scale by animateFloatAsState(
        targetValue = when {
            reduceMotion -> 1f
            isPressed -> 0.90f
            isHovered || isFocused -> 1.06f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "desktopControlScale",
    )

    val effectiveTint = if (active) activeColor else tint
    val bgAlpha = when {
        isFocused -> 0.28f
        isHovered -> 0.16f
        active -> 0.12f
        else -> 0f
    }
    val animatedBgAlpha by animateFloatAsState(bgAlpha, tween(160), label = "desktopControlBg")

    Box(
        modifier = modifier
            .size(targetSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(effectiveTint.copy(alpha = animatedBgAlpha))
            .then(
                if (isFocused) Modifier.border(2.dp, effectiveTint.copy(alpha = 0.65f), CircleShape) else Modifier
            )
            .desktopClickable(minTargetSize = targetSize, shape = CircleShape) {
                Haptics.click()
                onClick()
            }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = icon,
            animationSpec = tween(160, easing = FastOutSlowInEasing),
            label = "desktopControlCrossfade",
        ) { targetIcon ->
            Icon(
                imageVector = targetIcon,
                contentDescription = null,
                tint = effectiveTint,
                modifier = Modifier.size(size),
            )
        }
    }
}
