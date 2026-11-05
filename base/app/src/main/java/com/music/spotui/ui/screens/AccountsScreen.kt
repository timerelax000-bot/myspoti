// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.myspoti.host.android.MyspotiRuntime
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.data.AccountStore
import com.music.spotui.data.entity.AccountModel
import com.music.spotui.data.offline.OfflineAccounts
import com.music.spotui.ui.theme.AppAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AccountsScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accounts by AccountStore.accounts.collectAsState()
    val activeAccount by AccountStore.activeAccount.collectAsState()
    val switchState by com.music.spotui.data.AccountSessionManager.switchState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var accountToRemove by remember { mutableStateOf<AccountModel?>(null) }
    var accountToRename by remember { mutableStateOf<AccountModel?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val isSwitching = switchState is com.music.spotui.data.AccountSwitchState.Switching
    val switchingStage = (switchState as? com.music.spotui.data.AccountSwitchState.Switching)?.stage.orEmpty()
    val switchError = (switchState as? com.music.spotui.data.AccountSwitchState.Failed)?.message

    LaunchedEffect(switchState) {
        if (switchState is com.music.spotui.data.AccountSwitchState.Success) {
            com.music.spotui.data.AccountSessionManager.resetStateToIdle()
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Header with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Spotify Accounts",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Account", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))

            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No Spotify accounts added yet. Click 'Add Account' to sign in.",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(accounts) { account ->
                        val isActive = account.id == activeAccount?.id
                        val interactionSource = remember { MutableInteractionSource() }
                        val isFocused by interactionSource.collectIsFocusedAsState()

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = if (isFocused) 3.dp else if (isActive) 2.dp else 1.dp,
                                    color = if (isFocused) AppAccent else if (isActive) AppAccent.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(16.dp),
                                )
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {
                                    if (!isActive && !isSwitching) {
                                        scope.launch {
                                            com.music.spotui.data.AccountSessionManager.switchAccount(context, account.id)
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) Color(0xFF19241C) else Color(0xFF161620),
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        if (account.imageUrl.isNotBlank()) {
                                            GlideImage(
                                                model = account.imageUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape),
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                                            }
                                        }

                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                val displayName = account.customLabel.ifBlank { account.name.ifBlank { account.spotifyUserId } }
                                                Text(
                                                    text = displayName,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                                                )
                                                if (isActive) {
                                                    Badge(containerColor = AppAccent) {
                                                        Text("ACTIVE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                    }
                                                }
                                                if (account.isDefault) {
                                                    Badge(containerColor = Color(0xFF282836)) {
                                                        Text("DEFAULT", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = if (account.customLabel.isNotBlank()) "${account.name} • ${account.email.ifBlank { account.spotifyUserId }}" else account.email.ifBlank { "Spotify: ${account.spotifyUserId}" },
                                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.6f)),
                                            )
                                        }
                                    }

                                    // Switch Button
                                    if (!isActive) {
                                        OutlinedButton(
                                            onClick = {
                                                if (!isSwitching) {
                                                    scope.launch {
                                                        com.music.spotui.data.AccountSessionManager.switchAccount(context, account.id)
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.dp, AppAccent),
                                        ) {
                                            Text("Switch", color = AppAccent, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(14.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                Spacer(Modifier.height(8.dp))

                                // Management Row: Rename, Set Default, Clear Cache, Remove
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Rename
                                    TextButton(onClick = {
                                        accountToRename = account
                                        renameInput = account.customLabel.ifBlank { account.name }
                                    }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Rename", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    }

                                    // Set Default
                                    if (!account.isDefault) {
                                        TextButton(onClick = {
                                            scope.launch { AccountStore.setDefaultAccount(context, account.id) }
                                        }) {
                                            Icon(Icons.Filled.StarBorder, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Set Default", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                        }
                                    }

                                    // Clear Cache
                                    TextButton(onClick = {
                                        scope.launch { AccountStore.clearAccountCache(context, account.id) }
                                    }) {
                                        Icon(Icons.Rounded.CleaningServices, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Clear Cache", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    }

                                    // Remove
                                    IconButton(onClick = { accountToRemove = account }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Remove Account", tint = Color(0xFFFF5252).copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isSwitching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AppAccent)
                    Spacer(Modifier.height(16.dp))
                    Text("Switching Spotify account...", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    if (switchingStage.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Stage: $switchingStage",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        switchError?.let { err ->
            AlertDialog(
                onDismissRequest = { com.music.spotui.data.AccountSessionManager.resetStateToIdle() },
                title = { Text("Account Switch Failed") },
                text = { Text(err) },
                confirmButton = {
                    TextButton(onClick = { com.music.spotui.data.AccountSessionManager.resetStateToIdle() }) {
                        Text("OK", color = AppAccent)
                    }
                },
                containerColor = Color(0xFF1A1A24),
                titleContentColor = Color.White,
                textContentColor = Color.White.copy(alpha = 0.8f),
            )
        }
    }

    // Add Account Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Spotify Account") },
            text = { Text("To add a new Spotify account, Myspoti will open the official Spotify login page in a secure isolated profile. Proceed?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        scope.launch {
                            val newUuid = java.util.UUID.randomUUID().toString()
                            com.music.spotui.data.AccountSessionManager.switchAccount(context, newUuid, isNewLogin = true)
                            val activity = context as? app.myspoti.MainActivity
                            activity?.showLogin() ?: run {
                                MyspotiRuntime.host.openLogin()
                            }
                            onBack()
                        }
                    },
                ) {
                    Text("Proceed", color = AppAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1A1A24),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f),
        )
    }

    // Rename Dialog
    accountToRename?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToRename = null },
            title = { Text("Rename Account") },
            text = {
                Column {
                    Text("Enter a custom label for this account:")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            AccountStore.renameAccount(context, acc.id, renameInput)
                            accountToRename = null
                        }
                    },
                ) {
                    Text("Save", color = AppAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToRename = null }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1A1A24),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f),
        )
    }

    // Remove Account Confirmation Dialog
    accountToRemove?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToRemove = null },
            title = { Text("Remove Account") },
            text = { Text("Are you sure you want to remove ${acc.name.ifBlank { acc.spotifyUserId }} from Myspoti? This will not delete your Spotify account from Spotify servers.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            AccountStore.remove(context, acc.id)
                            accountToRemove = null
                        }
                    },
                ) {
                    Text("Remove", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToRemove = null }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1A1A24),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f),
        )
    }
}
