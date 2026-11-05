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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.music.spotui.data.entity.LibraryEntry
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.navigation.albumRoute
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.navigation.folderRoute
import com.music.spotui.ui.navigation.playlistRoute
import com.music.spotui.ui.navigation.showRoute
import com.music.spotui.ui.theme.AppAccent
import com.music.spotui.ui.viewmodel.LibraryViewModel

enum class TvLibraryCategory(val title: String) {
    ALL("All"),
    LIKED("Liked Songs"),
    PLAYLISTS("Playlists"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PODCASTS("Podcasts"),
    FOLDERS("Folders"),
}

/**
 * TV Library Screen:
 * - Top category filter bar (D-pad navigable)
 * - Large-card grid for 10-foot viewing
 * - Direct playback & navigation for Playlists, Albums, Artists, Folders, and Liked Songs
 */
@Composable
fun TvLibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val entriesState by viewModel.entries.collectAsState()
    var selectedCategory by remember { mutableStateOf(TvLibraryCategory.ALL) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .padding(24.dp),
    ) {
        // Header & Category Chips
        Text(
            text = "Your Library",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TvLibraryCategory.values().forEach { category ->
                TvCategoryChip(
                    category = category,
                    selected = selectedCategory == category,
                    onSelect = { selectedCategory = category },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Content Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val res = entriesState) {
                is Response.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppAccent)
                    }
                }
                is Response.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = res.error,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                        )
                    }
                }
                is Response.Success -> {
                    val allEntries = res.data.orEmpty()
                    val filtered = when (selectedCategory) {
                        TvLibraryCategory.ALL -> allEntries
                        TvLibraryCategory.LIKED -> allEntries.filter { it.isLikedSongs }
                        TvLibraryCategory.PLAYLISTS -> allEntries.filter { it.isPlaylist && !it.isLikedSongs && !it.isFolder }
                        TvLibraryCategory.ALBUMS -> allEntries.filter { !it.isPlaylist && !it.isFolder }
                        TvLibraryCategory.ARTISTS -> allEntries.filter { it.subtitle.contains("Artist", true) }
                        TvLibraryCategory.PODCASTS -> allEntries.filter { it.subtitle.contains("Podcast", true) || it.subtitle.contains("Show", true) }
                        TvLibraryCategory.FOLDERS -> allEntries.filter { it.isFolder }
                    }

                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No items in ${selectedCategory.title}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 16.sp,
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(filtered, key = { it.uri }) { entry ->
                                TvLibraryCard(
                                    entry = entry,
                                    onClick = { openLibraryEntry(entry, navController) },
                                    onPlay = { playLibraryEntry(entry) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvCategoryChip(
    category: TvLibraryCategory,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .border(
                width = if (isFocused) 2.dp else if (selected) 1.dp else 0.dp,
                color = if (isFocused) AppAccent else if (selected) AppAccent.copy(alpha = 0.6f) else Color.Transparent,
                shape = RoundedCornerShape(19.dp),
            )
            .background(
                when {
                    isFocused -> AppAccent
                    selected -> AppAccent.copy(alpha = 0.2f)
                    else -> Color(0xFF1E1E28)
                }
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionCenter) {
                    onSelect(); true
                } else false
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = category.title,
            color = if (isFocused) Color.Black else if (selected) AppAccent else Color.White.copy(alpha = 0.8f),
            fontWeight = if (selected || isFocused) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
        )
    }
}

private val LibraryEntry.isLikedSongs: Boolean
    get() = uri == "spotify:collection:tracks" || spotifyId == "liked-songs" || name.equals("Liked Songs", ignoreCase = true)

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun TvLibraryCard(
    entry: LibraryEntry,
    onClick: () -> Unit,
    onPlay: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.06f else 1.0f, label = "cardScale")

    val isArtist = entry.subtitle.contains("Artist", ignoreCase = true)
    val imageShape = if (isArtist) CircleShape else RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) AppAccent else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .background(if (isFocused) Color(0xFF222230) else Color(0xFF161620))
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
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (entry.isLikedSongs) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(imageShape)
                    .background(Color(0xFF43187A)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }
        } else if (entry.isFolder) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(imageShape)
                    .background(Color(0xFF282836)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(48.dp),
                )
            }
        } else if (entry.coverUri.isNotBlank()) {
            GlideImage(
                model = entry.coverUri,
                contentDescription = entry.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(136.dp)
                    .clip(imageShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(imageShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = entry.name,
            color = if (isFocused) AppAccent else Color.White,
            fontSize = 14.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = entry.subtitle.ifBlank {
                if (entry.isFolder) "Folder" else if (entry.isPlaylist) "Playlist" else "Album"
            },
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun playLibraryEntry(entry: LibraryEntry) {
    if (entry.isLikedSongs) {
        SongPlayer.playSong("spotify:collection:tracks")
    } else if (entry.uri.isNotBlank()) {
        SongPlayer.playSong(entry.uri)
    }
}

private fun openLibraryEntry(entry: LibraryEntry, navController: NavController) {
    if (entry.isLikedSongs) {
        navController.navigate(Routes.Liked.route)
        return
    }
    val id = entry.spotifyId.ifBlank { entry.uri.substringAfterLast(':') }
    if (entry.isFolder) {
        navController.navigate(folderRoute(entry.folderUri.ifBlank { entry.uri }, entry.name))
    } else if (entry.isPlaylist) {
        navController.navigate(playlistRoute(id, entry.name))
    } else {
        navController.navigate(albumRoute(entry.name, entry.subtitle, id))
    }
}
