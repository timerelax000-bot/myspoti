// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.spotui.car.CarBluetoothDevice
import com.music.spotui.car.CarModeManager
import com.music.spotui.car.CarNotificationManager
import com.music.spotui.data.preferences.CarModePref
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.theme.AppAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarModeSettingsScreen(
    navController: NavController,
    onBack: () -> Unit = { navController.popBackStack() },
) {
    val context = LocalContext.current
    var pairedDevices by remember { mutableStateOf<List<CarBluetoothDevice>>(emptyList()) }
    var hasBluetoothPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBluetoothPermission = isGranted
        if (isGranted) {
            pairedDevices = CarModeManager.getPairedBluetoothDevices(context)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            hasBluetoothPermission = granted
            if (granted) {
                pairedDevices = CarModeManager.getPairedBluetoothDevices(context)
            }
        } else {
            hasBluetoothPermission = true
            pairedDevices = CarModeManager.getPairedBluetoothDevices(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Car Mode Settings", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        CarModeManager.enterCarMode(context)
                        navController.navigate(Routes.CarMode.route)
                    }) {
                        Text("Launch", color = AppAccent, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F14)),
            )
        },
        containerColor = Color(0xFF0F0F14),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "AUTOMATION",
                    color = AppAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }

            // Auto-Activate Toggle
            item {
                CarSettingSwitchRow(
                    title = "Auto-activate on Bluetooth",
                    subtitle = "Automatically open Car Mode when connected to trusted car devices",
                    checked = CarModePref.autoBluetoothEnabled,
                    onCheckedChange = { CarModePref.setAutoBluetooth(context, it) },
                )
            }

            // Auto-Exit Toggle
            item {
                CarSettingSwitchRow(
                    title = "Auto-exit on disconnect",
                    subtitle = "Exit Car Mode when the car Bluetooth device disconnects",
                    checked = CarModePref.autoExitOnDisconnect,
                    onCheckedChange = { CarModePref.setAutoExit(context, it) },
                )
            }

            // Start Playback Toggle
            item {
                CarSettingSwitchRow(
                    title = "Start playback on enter",
                    subtitle = "Resume music automatically when entering Car Mode",
                    checked = CarModePref.startPlaybackOnEnter,
                    onCheckedChange = { CarModePref.setStartPlayback(context, it) },
                )
            }

            item {
                Text(
                    text = "DISPLAY & CONTROLS",
                    color = AppAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
            }

            // Keep Screen On Toggle
            item {
                CarSettingSwitchRow(
                    title = "Keep screen on",
                    subtitle = "Prevent screen from sleeping while in Car Mode",
                    checked = CarModePref.keepScreenOn,
                    onCheckedChange = { CarModePref.setKeepScreenOn(context, it) },
                )
            }

            // Voice Commands Toggle
            item {
                CarSettingSwitchRow(
                    title = "Enable voice commands",
                    subtitle = "Use speech recognition for hands-free playback control",
                    checked = CarModePref.voiceCommandsEnabled,
                    onCheckedChange = { CarModePref.setVoiceCommands(context, it) },
                )
            }

            // Orientation Selection
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF191924))
                        .padding(16.dp),
                ) {
                    Text("Orientation Behavior", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        listOf("auto" to "Auto Sensor", "portrait" to "Portrait", "landscape" to "Landscape").forEach { (mode, label) ->
                            val selected = CarModePref.orientationMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { CarModePref.setOrientation(context, mode) },
                                label = { Text(label, color = if (selected) Color.Black else Color.White) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppAccent,
                                    containerColor = Color(0xFF282836),
                                ),
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "NOTIFICATIONS",
                    color = AppAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
            }

            item {
                CarSettingSwitchRow(
                    title = "Show incoming notifications",
                    subtitle = "Display subtle alerts for messages and notifications while driving",
                    checked = CarModePref.notificationsEnabled,
                    onCheckedChange = { CarModePref.setNotifications(context, it) },
                )
            }

            item {
                val hasAccess = CarNotificationManager.isNotificationAccessGranted(context)
                if (!hasAccess) {
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccent.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Grant Notification Listener Access", color = AppAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Text(
                    text = "TRUSTED CAR DEVICES",
                    color = AppAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
            }

            if (!hasBluetoothPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) },
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Grant Bluetooth Permission", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (pairedDevices.isEmpty()) {
                item {
                    Text(
                        text = "No paired Bluetooth devices found. Pair your car in Android Bluetooth Settings first.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                items(pairedDevices) { device ->
                    val isTrusted = CarModePref.trustedDevices.contains(device.address)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF191924))
                            .clickable {
                                if (isTrusted) {
                                    CarModePref.removeTrustedDevice(context, device.address)
                                } else {
                                    CarModePref.addTrustedDevice(context, device.address)
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = if (isTrusted) AppAccent else Color.White.copy(alpha = 0.5f))
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(device.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(device.address, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                        }
                        Checkbox(
                            checked = isTrusted,
                            onCheckedChange = { checked ->
                                if (checked) CarModePref.addTrustedDevice(context, device.address)
                                else CarModePref.removeTrustedDevice(context, device.address)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = AppAccent),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CarSettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF191924))
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = AppAccent, checkedTrackColor = AppAccent.copy(alpha = 0.4f)),
        )
    }
}
