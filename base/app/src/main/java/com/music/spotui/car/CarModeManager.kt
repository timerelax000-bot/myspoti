// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.car

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import app.myspoti.host.android.Logbook
import com.music.spotui.data.preferences.CarModePref
import com.music.spotui.di.SongPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CarBluetoothDevice(
    val name: String,
    val address: String,
    val isTrusted: Boolean,
)

/**
 * Manages Car Mode lifecycle, Bluetooth automation, and activation triggers.
 */
object CarModeManager {
    private val _isCarModeActive = MutableStateFlow(false)
    val isCarModeActive: StateFlow<Boolean> = _isCarModeActive.asStateFlow()

    private var receiverRegistered = false
    private var connectedCarDevice: String? = null

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            context ?: return
            if (!CarModePref.autoBluetoothEnabled) return

            val action = intent.action
            val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            val address = device?.address ?: return
            val trusted = CarModePref.trustedDevices

            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    if (trusted.contains(address)) {
                        Logbook.log("CarMode: Connected to trusted car device: $address")
                        connectedCarDevice = address
                        enterCarMode(context)
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    if (connectedCarDevice == address && CarModePref.autoExitOnDisconnect) {
                        Logbook.log("CarMode: Disconnected from car device: $address")
                        connectedCarDevice = null
                        exitCarMode(stopPlayback = true)
                    }
                }
            }
        }
    }

    fun init(context: Context) {
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
            context.applicationContext.registerReceiver(bluetoothReceiver, filter)
            receiverRegistered = true
        }
    }

    fun enterCarMode(context: Context? = null) {
        if (_isCarModeActive.value) return
        _isCarModeActive.value = true
        Logbook.log("CarMode: Entered")

        if (CarModePref.startPlaybackOnEnter && !SongPlayer.isPlaying()) {
            SongPlayer.play("car_mode_enter")
        }
    }

    fun exitCarMode(stopPlayback: Boolean = false) {
        if (!_isCarModeActive.value) return
        _isCarModeActive.value = false
        Logbook.log("CarMode: Exited (stopPlayback=$stopPlayback)")
        if (stopPlayback) {
            SongPlayer.setDisconnectGuard()
        }
    }

    /** Returns bonded / paired Bluetooth devices to allow user selection. */
    @SuppressLint("MissingPermission")
    fun getPairedBluetoothDevices(context: Context): List<CarBluetoothDevice> {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!hasPermission) return emptyList()

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter() ?: return emptyList()

        return try {
            val bonded = adapter.bondedDevices.orEmpty()
            val trusted = CarModePref.trustedDevices
            bonded.map { device ->
                CarBluetoothDevice(
                    name = device.name ?: device.address,
                    address = device.address,
                    isTrusted = trusted.contains(device.address),
                )
            }
        } catch (e: Exception) {
            Logbook.log("CarMode getPairedBluetoothDevices error: ${e.message}")
            emptyList()
        }
    }
}
