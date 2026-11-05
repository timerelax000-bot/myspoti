// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.tv

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.data.AccountStore
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.theme.AppAccent

/**
 * TV-specific left-side navigation rail.
 *
 * Compact when unfocused (icons only, ~72dp), expands with labels when focused (~200dp).
 * Designed for D-pad navigation with strong focus indicators.
 */
@Composable
fun TvNavRail(
    navController: NavHostController,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val width by animateDpAsState(
        targetValue = if (expanded) 200.dp else 72.dp,
        animationSpec = tween(250),
        label = "railWidth",
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .background(Color(0xFF0A0A0F))
            .padding(vertical = 16.dp)
            .onFocusChanged { expanded = it.hasFocus }
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Profile avatar at top
        TvProfileChip(
            expanded = expanded,
            onClick = { onNavigate(Routes.Accounts.route) },
        )

        Spacer(Modifier.height(24.dp))

        // Navigation items
        val items = listOf(
            TvNavItem(Icons.Filled.Home, "Home", Routes.Home.route),
            TvNavItem(Icons.Filled.Search, "Search", Routes.Search.route),
            TvNavItem(Icons.Filled.LibraryMusic, "Library", Routes.Library.route),
            TvNavItem(Icons.Filled.People, "Friends", Routes.Friends.route),
            TvNavItem(Icons.Filled.Download, "Downloads", Routes.Downloads.route),
            TvNavItem(Icons.Filled.Settings, "Settings", Routes.Settings.route),
        )

        items.forEachIndexed { index, item ->
            val selected = currentRoute?.startsWith(item.route) == true ||
                (item.route == Routes.Home.route && currentRoute == null)

            TvNavRailItem(
                icon = item.icon,
                label = item.label,
                selected = selected,
                expanded = expanded,
                onClick = { onNavigate(item.route) },
            )

            if (index < items.lastIndex) Spacer(Modifier.height(4.dp))
        }
    }
}

private data class TvNavItem(
    val icon: ImageVector,
    val label: String,
    val route: String,
)

@Composable
private fun TvNavRailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    val backgroundColor = when {
        focused -> AppAccent.copy(alpha = 0.25f)
        selected -> Color.White.copy(alpha = 0.1f)
        else -> Color.Transparent
    }
    val contentColor = when {
        focused -> AppAccent
        selected -> Color.White
        else -> Color.White.copy(alpha = 0.6f)
    }
    val borderColor = if (focused) AppAccent else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionCenter) {
                    onClick(); true
                } else false
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
        if (expanded) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun TvProfileChip(expanded: Boolean, onClick: () -> Unit) {
    val account by AccountStore.activeAccount.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                2.dp,
                if (focused) AppAccent else Color.Transparent,
                RoundedCornerShape(12.dp),
            )
            .background(if (focused) AppAccent.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val imageUrl = account?.imageUrl.orEmpty()
        if (imageUrl.isNotBlank()) {
            GlideImage(
                model = imageUrl,
                contentDescription = "Profile",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(32.dp).clip(CircleShape),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = "Profile",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp),
            )
        }
        if (expanded) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = account?.name?.ifBlank { "Account" } ?: "Account",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}
