// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.car

import android.app.Activity
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.data.preferences.CarModePref
import com.music.spotui.di.Graph
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.theme.AppAccent

/**
 * Dedicated Car Mode Interface:
 * - Simple, high-contrast, distraction-free driving UI.
 * - Massive touch targets (>72dp) for effortless, safe interaction while driving.
 * - Adapts cleanly between Portrait and Landscape orientations.
 * - Integrated on-device voice commands via [CarVoiceCommander].
 * - Dedicated Car Mode Queue view.
 * - Driving-adapted notification alerts.
 */
@Composable
fun CarModeScreen(
    navController: NavController,
    onExit: () -> Unit = {
        CarModeManager.exitCarMode(stopPlayback = true)
        navController.popBackStack()
    },
) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE ||
        CarModePref.orientationMode == "landscape"

    val currentSongState = Graph.currentSongState
    val title by currentSongState.title
    val artist by currentSongState.singer
    val album by currentSongState.album
    val image by currentSongState.coverUri
    val isLiked = currentSongState.likeState.value
    val isPlaying = currentSongState.playingState.value
    val isShuffle = currentSongState.shuffle.value

    val voiceCommander = remember { CarVoiceCommander(context) }
    val voiceState by voiceCommander.state.collectAsState()
    val notification by CarNotificationManager.currentNotification.collectAsState()

    var showQueue by remember { mutableStateOf(false) }

    // Keep screen on if requested
    DisposableEffect(CarModePref.keepScreenOn) {
        val activity = context as? Activity
        if (CarModePref.keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            voiceCommander.destroy()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler(enabled = showQueue) { showQueue = false }
    BackHandler(enabled = !showQueue) { onExit() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
            .systemBarsPadding(),
    ) {
        if (isLandscape) {
            CarModeLandscapeLayout(
                title = title,
                artist = artist,
                album = album,
                image = image,
                isPlaying = isPlaying,
                isLiked = isLiked,
                isShuffle = isShuffle,
                onPlayPause = { if (isPlaying) SongPlayer.pause() else SongPlayer.play() },
                onNext = { SongPlayer.swipeNext() },
                onPrevious = { SongPlayer.previous() },
                onToggleLike = {
                    val on = !currentSongState.likeState.value
                    currentSongState.likeState.value = on
                    val uri = SongPlayer.nowPlaying.value.uri
                    if (uri.isNotBlank()) SongPlayer.setSaved(uri, on)
                },
                onToggleShuffle = {
                    val sh = !currentSongState.shuffle.value
                    currentSongState.shuffle.value = sh
                    SongPlayer.setShuffle(sh)
                },
                onOpenQueue = { showQueue = true },
                onVoiceClick = { voiceCommander.startListening() },
                onExit = onExit,
            )
        } else {
            CarModePortraitLayout(
                title = title,
                artist = artist,
                album = album,
                image = image,
                isPlaying = isPlaying,
                isLiked = isLiked,
                isShuffle = isShuffle,
                onPlayPause = { if (isPlaying) SongPlayer.pause() else SongPlayer.play() },
                onNext = { SongPlayer.swipeNext() },
                onPrevious = { SongPlayer.previous() },
                onToggleLike = {
                    val on = !currentSongState.likeState.value
                    currentSongState.likeState.value = on
                    val uri = SongPlayer.nowPlaying.value.uri
                    if (uri.isNotBlank()) SongPlayer.setSaved(uri, on)
                },
                onToggleShuffle = {
                    val sh = !currentSongState.shuffle.value
                    currentSongState.shuffle.value = sh
                    SongPlayer.setShuffle(sh)
                },
                onOpenQueue = { showQueue = true },
                onVoiceClick = { voiceCommander.startListening() },
                onExit = onExit,
            )
        }

        // Driving Notification Banner
        AnimatedVisibility(
            visible = notification != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            notification?.let { item ->
                CarNotificationBanner(
                    item = item,
                    onDismiss = { CarNotificationManager.dismiss() },
                )
            }
        }

        // Voice Command Active Overlay
        AnimatedVisibility(
            visible = voiceState !is VoiceState.Idle,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CarVoiceModal(
                voiceState = voiceState,
                onDismiss = { voiceCommander.stopListening() },
            )
        }

        // Dedicated Car Mode Queue Sheet
        AnimatedVisibility(
            visible = showQueue,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            CarQueueSheet(
                onClose = { showQueue = false },
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun CarModePortraitLayout(
    title: String,
    artist: String,
    album: String,
    image: String,
    isPlaying: Boolean,
    isLiked: Boolean,
    isShuffle: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenQueue: () -> Unit,
    onVoiceClick: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top Bar: Exit button, Title indicator, Voice button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onExit,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Exit Car Mode", tint = Color.White, modifier = Modifier.size(28.dp))
            }

            Text(
                text = "CAR MODE",
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 2.sp,
            )

            if (CarModePref.voiceCommandsEnabled) {
                IconButton(
                    onClick = onVoiceClick,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(AppAccent.copy(alpha = 0.18f)),
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Voice Commands", tint = AppAccent, modifier = Modifier.size(28.dp))
                }
            } else {
                Spacer(Modifier.size(54.dp))
            }
        }

        // Center Artwork
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (image.isNotBlank()) {
                GlideImage(
                    model = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .sizeIn(maxWidth = 320.dp, maxHeight = 320.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1E1E28)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(80.dp))
                }
            }
        }

        // Song & Artist Metadata
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title.ifBlank { "Not Playing" },
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = artist.ifBlank { "Select a song to start" },
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(24.dp))

        // Massive Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // Previous Button (72dp)
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
            ) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous Track", tint = Color.White, modifier = Modifier.size(38.dp))
            }

            // Massive Play / Pause (96dp)
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(AppAccent),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(54.dp),
                )
            }

            // Next Button (72dp)
            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next Track", tint = Color.White, modifier = Modifier.size(38.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        // Secondary Shortcuts: Favorite, Shuffle, Queue
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconButton(
                onClick = onToggleLike,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isLiked) AppAccent else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(32.dp),
                )
            }

            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) AppAccent else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(30.dp),
                )
            }

            IconButton(
                onClick = onOpenQueue,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.QueueMusic,
                    contentDescription = "Queue",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun CarModeLandscapeLayout(
    title: String,
    artist: String,
    album: String,
    image: String,
    isPlaying: Boolean,
    isLiked: Boolean,
    isShuffle: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenQueue: () -> Unit,
    onVoiceClick: () -> Unit,
    onExit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left Column: Artwork + Song Metadata
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (image.isNotBlank()) {
                GlideImage(
                    model = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(190.dp)
                        .clip(RoundedCornerShape(20.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1E28)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(60.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = title.ifBlank { "Not Playing" },
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = artist.ifBlank { "Select a song to start" },
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.width(32.dp))

        // Right Column: Controls & Utilities
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Exit", tint = Color.White)
                }

                if (CarModePref.voiceCommandsEnabled) {
                    IconButton(
                        onClick = onVoiceClick,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(AppAccent.copy(alpha = 0.18f)),
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = "Voice", tint = AppAccent)
                    }
                }
            }

            // Massive Center Playback Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(38.dp))
                }

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(AppAccent),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(54.dp),
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(38.dp))
                }
            }

            // Bottom Utilities: Like, Shuffle, Queue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleLike, modifier = Modifier.size(56.dp)) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) AppAccent else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(30.dp),
                    )
                }

                IconButton(onClick = onToggleShuffle, modifier = Modifier.size(56.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) AppAccent else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp),
                    )
                }

                IconButton(onClick = onOpenQueue, modifier = Modifier.size(56.dp)) {
                    Icon(
                        imageVector = Icons.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CarNotificationBanner(
    item: CarNotificationItem,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(AppAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = AppAccent, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = item.appName.ifBlank { item.title },
                        color = AppAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.text.isNotBlank()) {
                        Text(
                            text = item.text,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun CarVoiceModal(
    voiceState: VoiceState,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1B1B26))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (voiceState) {
                is VoiceState.Listening -> {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(AppAccent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(44.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Listening...", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Try: \"Play my liked songs\", \"Skip this song\", \"Pause music\"",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                is VoiceState.Recognized -> {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AppAccent, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("\"${voiceState.text}\"", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(voiceState.actionFeedback, color = AppAccent, fontSize = 15.sp, textAlign = TextAlign.Center)
                }
                is VoiceState.Error -> {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(voiceState.message, color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center)
                }
                VoiceState.Idle -> {}
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun CarQueueSheet(
    onClose: () -> Unit,
) {
    val q by SongPlayer.engineQueue

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C12))
            .clickable(enabled = false) {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = AppAccent, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("CAR MODE QUEUE", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Queue", tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                q.current?.let { nowPlayingSong ->
                    item {
                        Text(
                            "NOW PLAYING",
                            color = AppAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                    item {
                        CarQueueRow(
                            title = nowPlayingSong.title,
                            artist = nowPlayingSong.singer,
                            coverUri = nowPlayingSong.coverUri,
                            isNowPlaying = true,
                            onClick = {},
                        )
                    }
                }

                if (q.queued.isNotEmpty()) {
                    item {
                        Text(
                            "QUEUED TRACKS",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                        )
                    }
                    itemsIndexed(q.queued, key = { i, item -> "q_" + i + "_" + item.uid + "_" + item.uri }) { _, item ->
                        CarQueueRow(
                            title = item.song.title,
                            artist = item.song.singer,
                            coverUri = item.song.coverUri,
                            isNowPlaying = false,
                            onClick = { SongPlayer.skipTo(item) },
                        )
                    }
                }

                if (q.nextUp.isNotEmpty()) {
                    item {
                        Text(
                            "NEXT UP",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                        )
                    }
                    itemsIndexed(q.nextUp, key = { i, item -> "n_" + i + "_" + item.uid + "_" + item.uri }) { _, item ->
                        CarQueueRow(
                            title = item.song.title,
                            artist = item.song.singer,
                            coverUri = item.song.coverUri,
                            isNowPlaying = false,
                            onClick = { SongPlayer.skipTo(item) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun CarQueueRow(
    title: String,
    artist: String,
    coverUri: String,
    isNowPlaying: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isNowPlaying) AppAccent.copy(alpha = 0.15f) else Color(0xFF181822))
            .border(
                1.dp,
                if (isNowPlaying) AppAccent else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (coverUri.isNotBlank()) {
            GlideImage(
                model = coverUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF282836)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(28.dp))
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isNowPlaying) AppAccent else Color.White,
                fontSize = 17.sp,
                fontWeight = if (isNowPlaying) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = artist,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (isNowPlaying) {
            Icon(Icons.Filled.GraphicEq, contentDescription = "Playing", tint = AppAccent, modifier = Modifier.size(28.dp))
        }
    }
}
