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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SpaceBar
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
import com.music.spotui.data.entity.AlbumsModel
import com.music.spotui.data.entity.ArtistsModel
import com.music.spotui.data.entity.SearchResults
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.navigation.albumRoute
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.navigation.playlistRoute
import com.music.spotui.ui.theme.AppAccent
import com.music.spotui.ui.viewmodel.SearchViewModel

/**
 * TV Search Screen:
 * - Left pane: Virtual alphanumeric on-screen keyboard + search query display.
 * - Right pane: Categorized search results (Tracks, Artists, Albums, Playlists).
 * - Full remote / D-pad focusability, plus hardware keyboard input support.
 */
@Composable
fun TvSearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    var queryText by remember { mutableStateOf(viewModel.query) }
    val resultsState by viewModel.results.collectAsState()

    // Trigger search when query updates
    LaunchedEffect(queryText) {
        viewModel.query = queryText
        if (queryText.isNotBlank()) {
            viewModel.search(queryText)
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .padding(24.dp)
            .onKeyEvent { event ->
                // Physical keyboard support
                if (event.type == KeyEventType.KeyDown) {
                    val unicode = event.utf16CodePoint
                    if (unicode in 32..126) {
                        queryText += unicode.toChar()
                        true
                    } else if (event.key == Key.Backspace && queryText.isNotEmpty()) {
                        queryText = queryText.dropLast(1)
                        true
                    } else false
                } else false
            },
    ) {
        // Left Column: Search Field & Virtual Keyboard (approx 340dp)
        Column(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .padding(end = 24.dp),
        ) {
            // Search Query Display
            TvSearchQueryBox(
                query = queryText,
                onClear = { queryText = "" },
            )

            Spacer(Modifier.height(16.dp))

            // Virtual Keyboard
            TvVirtualKeyboard(
                onKeyPress = { char -> queryText += char },
                onBackspace = { if (queryText.isNotEmpty()) queryText = queryText.dropLast(1) },
                onSpace = { queryText += " " },
                onClear = { queryText = "" },
            )
        }

        // Right Column: Search Results
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            when (val res = resultsState) {
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
                    val searchData = res.data ?: SearchResults()
                    val hasResults = searchData.songs.isNotEmpty() ||
                        searchData.artists.isNotEmpty() ||
                        searchData.albums.isNotEmpty() ||
                        searchData.playlists.isNotEmpty()

                    if (queryText.isBlank()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Use the virtual keyboard or a connected remote to search",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 16.sp,
                            )
                        }
                    } else if (!hasResults) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No results found for \"$queryText\"",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 16.sp,
                            )
                        }
                    } else {
                        TvSearchResultsView(
                            results = searchData,
                            navController = navController,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSearchQueryBox(
    query: String,
    onClear: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) AppAccent else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
            )
            .background(Color(0xFF191924))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = if (isFocused) AppAccent else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = query.ifBlank { "Type to search..." },
            color = if (query.isBlank()) Color.White.copy(alpha = 0.4f) else Color.White,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (query.isNotBlank()) {
            IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = "Clear",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** 6-column TV alphanumeric virtual keypad */
@Composable
private fun TvVirtualKeyboard(
    onKeyPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onClear: () -> Unit,
) {
    val keys = listOf(
        listOf('A', 'B', 'C', 'D', 'E', 'F'),
        listOf('G', 'H', 'I', 'J', 'K', 'L'),
        listOf('M', 'N', 'O', 'P', 'Q', 'R'),
        listOf('S', 'T', 'U', 'V', 'W', 'X'),
        listOf('Y', 'Z', '1', '2', '3', '4'),
        listOf('5', '6', '7', '8', '9', '0'),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { char ->
                    TvKeyButton(
                        text = char.toString(),
                        onClick = { onKeyPress(char) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Control Keys Row: Space, Backspace, Clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TvKeyButton(
                icon = Icons.Filled.SpaceBar,
                onClick = onSpace,
                modifier = Modifier.weight(2f),
            )
            TvKeyButton(
                icon = Icons.Filled.Backspace,
                onClick = onBackspace,
                modifier = Modifier.weight(2f),
            )
            TvKeyButton(
                text = "CLR",
                onClick = onClear,
                modifier = Modifier.weight(2f),
            )
        }
    }
}

@Composable
private fun TvKeyButton(
    text: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) AppAccent else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .background(if (isFocused) AppAccent else Color(0xFF222230))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionCenter) {
                    onClick(); true
                } else false
            },
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color.Black else Color.White,
                modifier = Modifier.size(18.dp),
            )
        } else if (text != null) {
            Text(
                text = text,
                color = if (isFocused) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun TvSearchResultsView(
    results: SearchResults,
    navController: NavController,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        // Tracks Shelf
        if (results.songs.isNotEmpty()) {
            item {
                TvResultShelfHeader("Songs")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    items(results.songs) { track ->
                        TvTrackCard(
                            track = track,
                            onClick = { SongPlayer.playSong(track.url) },
                        )
                    }
                }
            }
        }

        // Artists Shelf
        if (results.artists.isNotEmpty()) {
            item {
                TvResultShelfHeader("Artists")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    items(results.artists) { artist ->
                        TvArtistCard(
                            artist = artist,
                            onClick = { navController.navigate(artistRoute(artist.name, artist.id)) },
                        )
                    }
                }
            }
        }

        // Albums Shelf
        if (results.albums.isNotEmpty()) {
            item {
                TvResultShelfHeader("Albums")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    items(results.albums) { album ->
                        TvAlbumCard(
                            album = album,
                            onClick = {
                                val id = album.spotifyId.ifBlank { album.id.toString() }
                                navController.navigate(albumRoute(album.name, album.artists, id))
                            },
                        )
                    }
                }
            }
        }

        // Playlists Shelf
        if (results.playlists.isNotEmpty()) {
            item {
                TvResultShelfHeader("Playlists")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    items(results.playlists) { playlist ->
                        TvPlaylistCard(
                            playlist = playlist,
                            onClick = {
                                val id = playlist.spotifyId.ifBlank { playlist.uri.substringAfterLast(':') }
                                navController.navigate(playlistRoute(id, playlist.name))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvResultShelfHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun TvTrackCard(track: SongsModel, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .width(280.dp)
            .height(68.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) AppAccent else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            )
            .background(if (isFocused) Color(0xFF222232) else Color(0xFF191924))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionCenter) {
                    onClick(); true
                } else false
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (track.coverUri.isNotBlank()) {
            GlideImage(
                model = track.coverUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isFocused) AppAccent else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.singer,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun TvArtistCard(artist: ArtistsModel, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) AppAccent else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            )
            .background(if (isFocused) Color(0xFF222232) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionCenter) {
                    onClick(); true
                } else false
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlideImage(
            model = artist.coverUri,
            contentDescription = artist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = artist.name,
            color = if (isFocused) AppAccent else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun TvAlbumCard(album: AlbumsModel, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) AppAccent else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            )
            .background(if (isFocused) Color(0xFF222232) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionCenter) {
                    onClick(); true
                } else false
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlideImage(
            model = album.coverUri,
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = album.name,
            color = if (isFocused) AppAccent else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = album.artists,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun TvPlaylistCard(playlist: com.music.spotui.data.entity.LibraryEntry, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) AppAccent else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            )
            .background(if (isFocused) Color(0xFF222232) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionCenter) {
                    onClick(); true
                } else false
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlideImage(
            model = playlist.coverUri,
            contentDescription = playlist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = playlist.name,
            color = if (isFocused) AppAccent else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = playlist.subtitle,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
