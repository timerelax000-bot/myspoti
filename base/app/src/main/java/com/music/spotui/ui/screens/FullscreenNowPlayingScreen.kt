// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.di.Graph
import com.music.spotui.di.Palette
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.theme.AppAccent
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * iPhone-inspired Fullscreen Now Playing Screen for Myspoti:
 * - Large album artwork with subtle scale animation (expands when playing, contracts on pause).
 * - Dynamic background color gradient derived from artwork cover using Palette API.
 * - Glassmorphic / blurred styling optimized for high-resolution displays (e.g. HONOR 400 Pro).
 * - Real-time progress bar, track controls, and like/shuffle toggles.
 * - Standalone in-app experience separated from system lock screen.
 */
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FullscreenNowPlayingScreen(
    navController: NavController,
    onClose: () -> Unit = { navController.popBackStack() },
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val keepAwake = com.music.spotui.data.preferences.LyricsKeepAwakePref.on
    DisposableEffect(keepAwake) {
        view.keepScreenOn = keepAwake
        onDispose { view.keepScreenOn = false }
    }
    val currentSongState = Graph.currentSongState
    val title by currentSongState.title
    val artist by currentSongState.singer
    val album by currentSongState.album
    val image by currentSongState.coverUri
    val isLiked = currentSongState.likeState.value
    val isPlaying = currentSongState.playingState.value
    val isShuffle = currentSongState.shuffle.value

    var coverColor by remember { mutableStateOf(Color(0xFF1E1E2A)) }

    // Dynamic background color extraction
    LaunchedEffect(image) {
        if (image.isNotBlank()) {
            Palette().extractFirstColorFromImageUrl(context, image) { extracted ->
                coverColor = extracted
            }
        } else {
            coverColor = Color(0xFF1E1E2A)
        }
    }

    // Real-time position tracking
    var currentPositionMs by remember { mutableLongStateOf(SongPlayer.getCurrentPosition()) }
    var totalDurationMs by remember { mutableLongStateOf(SongPlayer.getDuration()) }
    var isUserScrubbing by remember { mutableStateOf(false) }
    var userScrubPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        while (true) {
            if (isPlaying && !isUserScrubbing) {
                currentPositionMs = SongPlayer.getCurrentPosition()
                totalDurationMs = SongPlayer.getDuration()
            }
            delay(300L)
        }
    }

    // Artwork scaling animation (Apple Music style)
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.92f,
        animationSpec = tween(durationMillis = 350),
        label = "ArtworkScale",
    )

    BackHandler { onClose() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        coverColor.copy(alpha = 0.85f),
                        coverColor.copy(alpha = 0.40f),
                        Color(0xFF0A0A10),
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Text(
                    text = "NOW PLAYING",
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                )

                IconButton(
                    onClick = {
                        val uri = SongPlayer.nowPlaying.value.uri
                        if (uri.isNotBlank()) SongPlayer.setSaved(uri, !isLiked)
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isLiked) AppAccent else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Large Center Album Artwork with Glow & Scale
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer(scaleX = artworkScale, scaleY = artworkScale)
                        .sizeIn(maxWidth = 320.dp, maxHeight = 320.dp)
                        .aspectRatio(1f)
                        .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = coverColor)
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (image.isNotBlank()) {
                        GlideImage(
                            model = image,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E1E28)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(90.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Song & Artist Info with Marquee
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = title.ifBlank { "Not Playing" },
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (album.isNotBlank()) "$artist • $album" else artist.ifBlank { "Unknown Artist" },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(24.dp))

            // Progress Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                val duration = totalDurationMs.coerceAtLeast(1L)
                val currentPos = if (isUserScrubbing) userScrubPosition.toLong() else currentPositionMs.coerceIn(0L, duration)

                Slider(
                    value = currentPos.toFloat(),
                    onValueChange = {
                        isUserScrubbing = true
                        userScrubPosition = it
                    },
                    onValueChangeFinished = {
                        SongPlayer.seekTo(userScrubPosition.toLong())
                        currentPositionMs = userScrubPosition.toLong()
                        isUserScrubbing = false
                    },
                    valueRange = 0f..duration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatTimeMs(currentPos),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                    )
                    Text(
                        text = formatTimeMs(duration),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(
                    onClick = {
                        val sh = !currentSongState.shuffle.value
                        currentSongState.shuffle.value = sh
                        SongPlayer.setShuffle(sh)
                    },
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) AppAccent else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp),
                    )
                }

                IconButton(
                    onClick = { SongPlayer.previous() },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
                }

                IconButton(
                    onClick = { if (isPlaying) SongPlayer.pause() else SongPlayer.play() },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(AppAccent),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(46.dp),
                    )
                }

                IconButton(
                    onClick = { SongPlayer.swipeNext() },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                }

                IconButton(
                    onClick = { navController.navigate(Routes.Queue.route) },
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(
                        Icons.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun formatTimeMs(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format(Locale.US, "%01d:%02d", minutes, seconds)
}
