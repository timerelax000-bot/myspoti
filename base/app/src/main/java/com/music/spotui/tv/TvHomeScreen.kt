// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.HomeFeedModel
import com.music.spotui.data.entity.HomeItem
import com.music.spotui.data.entity.HomeSection
import com.music.spotui.data.entity.withoutDj
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.navigation.albumRoute
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.navigation.playlistRoute
import com.music.spotui.ui.navigation.showRoute
import com.music.spotui.ui.theme.AppAccent
import com.music.spotui.ui.viewmodel.HomeViewModel

/**
 * Android TV Home Screen.
 *
 * Designed specifically for 10-foot UI:
 * - Large hero banner with featured content and direct play action
 * - High-contrast horizontal shelves
 * - D-pad directional navigation and clear focus borders
 * - Real Spotify data from [HomeViewModel]
 */
@Composable
fun TvHomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val homeState by viewModel.home.collectAsState()
    var friendsActivity by remember { mutableStateOf<List<app.myspoti.api.FriendActivity>>(emptyList()) }

    LaunchedEffect(Unit) {
        runCatching { app.myspoti.host.android.MyspotiRuntime.api.users.friendActivity() }
            .onSuccess { friendsActivity = it.filter { f -> f.track != null } }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14)),
    ) {
        when (val res = homeState) {
            is Response.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppAccent)
                }
            }
            is Response.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Unable to load TV Home",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = res.error,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.retry() },
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
            is Response.Success -> {
                val feed = res.data
                val sections = feed.sections.withoutDj()

                if (sections.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No content available",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                        )
                    }
                } else {
                    val featured = sections.firstOrNull()?.items?.firstOrNull()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                    ) {
                        // Hero Section
                        if (featured != null) {
                            item(key = "hero") {
                                TvHeroBanner(
                                    item = featured,
                                    onPlay = { playHomeItem(featured) },
                                    onOpen = { openHomeItem(featured, navController) },
                                )
                            }
                        }

                        // Friends Are Listening Shelf
                        if (friendsActivity.isNotEmpty()) {
                            item(key = "friends_shelf") {
                                TvFriendsShelf(
                                    friends = friendsActivity,
                                    onItemClick = { f ->
                                        f.track?.let { SongPlayer.playSong(it.uri, contextUri = f.contextUri) }
                                    },
                                )
                            }
                        }

                        // Content Shelves
                        items(sections, key = { it.title }) { section ->
                            TvShelf(
                                section = section,
                                onItemClick = { item -> openHomeItem(item, navController) },
                                onItemPlay = { item -> playHomeItem(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun TvHeroBanner(
    item: HomeItem,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
) {
    val playInteraction = remember { MutableInteractionSource() }
    val isPlayFocused by playInteraction.collectIsFocusedAsState()

    val openInteraction = remember { MutableInteractionSource() }
    val isOpenFocused by openInteraction.collectIsFocusedAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(horizontal = 32.dp)
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181822)),
    ) {
        Box(Modifier.fillMaxSize()) {
            // Background Artwork with Gradient
            if (item.imageUrl.isNotBlank()) {
                GlideImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0F0F14),
                                Color(0xFF0F0F14).copy(alpha = 0.85f),
                                Color.Transparent,
                            ),
                            startX = 0f,
                            endX = 900f,
                        )
                    )
            )

            // Hero Details & Remote Buttons
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(520.dp)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "FEATURED",
                    color = AppAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Play Button
                    Button(
                        onClick = onPlay,
                        interactionSource = playInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlayFocused) Color.White else AppAccent,
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .border(
                                width = if (isPlayFocused) 3.dp else 0.dp,
                                color = if (isPlayFocused) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(24.dp),
                            )
                            .focusable(interactionSource = playInteraction)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionCenter) {
                                    onPlay(); true
                                } else false
                            },
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("Play Now", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    // Browse Button
                    OutlinedButton(
                        onClick = onOpen,
                        interactionSource = openInteraction,
                        modifier = Modifier
                            .height(48.dp)
                            .border(
                                width = if (isOpenFocused) 3.dp else 1.dp,
                                color = if (isOpenFocused) AppAccent else Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(24.dp),
                            )
                            .focusable(interactionSource = openInteraction)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionCenter) {
                                    onOpen(); true
                                } else false
                            },
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Text(
                            text = "Browse",
                            color = if (isOpenFocused) AppAccent else Color.White,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvShelf(
    section: HomeSection,
    onItemClick: (HomeItem) -> Unit,
    onItemPlay: (HomeItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = section.title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(section.items, key = { it.name + it.imageUrl }) { item ->
                TvCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onPlay = { onItemPlay(item) },
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun TvCard(
    item: HomeItem,
    onClick: () -> Unit,
    onPlay: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        label = "tvCardScale",
    )

    val isArtist = item is HomeItem.Artist
    val cardShape = if (isArtist) CircleShape else RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .width(170.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) AppAccent else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .background(if (isFocused) Color(0xFF222230) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter -> { onClick(); true }
                        Key.MediaPlayPause, Key.MediaPlay -> { onPlay(); true }
                        else -> false
                    }
                } else false
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (item.imageUrl.isNotBlank()) {
            GlideImage(
                model = item.imageUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(150.dp)
                    .clip(cardShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(cardShape)
                    .background(Color.White.copy(alpha = 0.1f)),
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = item.name,
            color = if (isFocused) AppAccent else Color.White,
            fontSize = 14.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        val subtitle = when (item) {
            is HomeItem.Album -> item.artists.ifBlank { item.subtitle }
            is HomeItem.Playlist -> item.subtitle
            is HomeItem.Show -> item.subtitle
            is HomeItem.Artist -> "Artist"
        }
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun playHomeItem(item: HomeItem) {
    val uri = when (item) {
        is HomeItem.Playlist -> "spotify:playlist:${item.id}"
        is HomeItem.Album -> "spotify:album:${item.id}"
        is HomeItem.Artist -> "spotify:artist:${item.id}"
        is HomeItem.Show -> if (item.isEpisode) "spotify:episode:${item.id}" else "spotify:show:${item.id}"
    }
    SongPlayer.playSong(uri)
}

private fun openHomeItem(item: HomeItem, navController: NavController) {
    when (item) {
        is HomeItem.Playlist -> navController.navigate(playlistRoute(item.id, item.name))
        is HomeItem.Album -> navController.navigate(albumRoute(item.name, item.artists, item.id))
        is HomeItem.Artist -> navController.navigate(artistRoute(item.name, item.id))
        is HomeItem.Show -> navController.navigate(showRoute(item.id, item.name))
    }
}

@Composable
private fun TvFriendsShelf(
    friends: List<app.myspoti.api.FriendActivity>,
    onItemClick: (app.myspoti.api.FriendActivity) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Friends Are Listening",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(friends, key = { it.user.uri }) { friend ->
                TvFriendCard(
                    friend = friend,
                    onClick = { onItemClick(friend) },
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun TvFriendCard(
    friend: app.myspoti.api.FriendActivity,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        label = "tvFriendCardScale",
    )

    Column(
        modifier = Modifier
            .width(170.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) AppAccent else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .background(if (isFocused) Color(0xFF222230) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter)) {
                    onClick(); true
                } else false
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val cover = friend.trackImageUrl ?: friend.track?.album?.uri?.let { com.music.spotui.ui.components.spotifyImageUrl(it) }
        if (!cover.isNullOrBlank()) {
            GlideImage(
                model = cover,
                contentDescription = friend.track?.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = friend.user.name,
            color = if (isFocused) AppAccent else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        friend.track?.let { t ->
            Text(
                text = t.name,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
