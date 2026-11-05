// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.di.Graph
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.theme.AppAccent

/**
 * TV Application Shell:
 * - Permanent left-side TV navigation rail
 * - Dynamic content area switching between TV Home, Search, Library, Friends, Downloads, Settings
 * - Bottom TV Mini-player bar when playing in the background
 * - Full remote control support (D-pad, Back, Media keys)
 */
@Composable
fun TvShell(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    var selectedSection by remember { mutableStateOf(Routes.Home.route) }
    val currentSongState = Graph.currentSongState
    val playingTrackTitle by currentSongState.title
    val playingTrackArtist by currentSongState.singer
    val playingTrackImage by currentSongState.coverUri
    val isPlaying = currentSongState.playingState.value

    // Handle back button on TV: if not in Home, back returns to Home
    BackHandler(enabled = selectedSection != Routes.Home.route) {
        selectedSection = Routes.Home.route
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14)),
    ) {
        // Permanent TV Navigation Rail
        TvNavRail(
            navController = navController,
            currentRoute = selectedSection,
            onNavigate = { route ->
                when (route) {
                    Routes.Home.route, Routes.Search.route, Routes.Library.route,
                    Routes.Friends.route, Routes.Downloads.route, Routes.Settings.route -> {
                        selectedSection = route
                    }
                    else -> navController.navigate(route)
                }
            },
        )

        // Main TV Content Container
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Crossfade(targetState = selectedSection, label = "tvContentTransition") { section ->
                    when (section) {
                        Routes.Home.route -> TvHomeScreen(navController = navController)
                        Routes.Search.route -> TvSearchScreen(navController = navController)
                        Routes.Library.route -> TvLibraryScreen(navController = navController)
                        Routes.Friends.route -> com.music.spotui.ui.screens.FriendsScreen(navController = navController)
                        Routes.Downloads.route -> com.music.spotui.ui.screens.DownloadsScreen(navController = navController)
                        Routes.Settings.route -> com.music.spotui.ui.screens.SettingsScreen(navController = navController)
                        else -> TvHomeScreen(navController = navController)
                    }
                }
            }

            // TV Mini Player Bar if something is loaded / playing
            if (playingTrackTitle.isNotBlank()) {
                TvMiniPlayerBar(
                    title = playingTrackTitle,
                    artist = playingTrackArtist,
                    imageUrl = playingTrackImage,
                    isPlaying = isPlaying,
                    onPlayPause = {
                        if (isPlaying) SongPlayer.pause() else SongPlayer.play()
                    },
                    onNext = { SongPlayer.swipeNext() },
                    onOpenPlayer = { navController.navigate(Routes.Player.route) },
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun TvMiniPlayerBar(
    title: String,
    artist: String,
    imageUrl: String,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val barInteraction = remember { MutableInteractionSource() }
    val isBarFocused by barInteraction.collectIsFocusedAsState()

    val playInteraction = remember { MutableInteractionSource() }
    val isPlayFocused by playInteraction.collectIsFocusedAsState()

    val nextInteraction = remember { MutableInteractionSource() }
    val isNextFocused by nextInteraction.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(Color(0xFF14141E))
            .border(
                width = if (isBarFocused) 2.dp else 1.dp,
                color = if (isBarFocused) AppAccent else Color.White.copy(alpha = 0.1f),
            )
            .clickable(interactionSource = barInteraction, indication = null, onClick = onOpenPlayer)
            .focusable(interactionSource = barInteraction)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (imageUrl.isNotBlank()) {
            GlideImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Play/Pause Button
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .border(
                    width = if (isPlayFocused) 2.dp else 0.dp,
                    color = if (isPlayFocused) AppAccent else Color.Transparent,
                    shape = RoundedCornerShape(21.dp),
                )
                .background(if (isPlayFocused) AppAccent else Color.White.copy(alpha = 0.12f))
                .clickable(interactionSource = playInteraction, indication = null, onClick = onPlayPause)
                .focusable(interactionSource = playInteraction)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionCenter) {
                        onPlayPause(); true
                    } else false
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = if (isPlayFocused) Color.Black else Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        // Next Button
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .border(
                    width = if (isNextFocused) 2.dp else 0.dp,
                    color = if (isNextFocused) AppAccent else Color.Transparent,
                    shape = RoundedCornerShape(21.dp),
                )
                .background(if (isNextFocused) AppAccent else Color.White.copy(alpha = 0.12f))
                .clickable(interactionSource = nextInteraction, indication = null, onClick = onNext)
                .focusable(interactionSource = nextInteraction)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionCenter) {
                        onNext(); true
                    } else false
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint = if (isNextFocused) Color.Black else Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
