package com.music.spotui.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.data.preferences.ImmersiveSettingsPref

/**
 * Advanced settings screen organizing all Immersive Experience 2.0 features into clean,
 * structured categories.
 */
@Composable
fun ImmersiveExperienceSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    ImmersiveSettingsPref.init(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F13))
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Immersive Experience 2.0",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Category 1: Appearance & Background
            SettingsSectionHeader(title = "Appearance & Background", icon = Icons.Rounded.Palette)
            val dynamicBg by ImmersiveSettingsPref.dynamicBackgrounds.collectAsState()
            SwitchSettingRow("Dynamic Backgrounds", "Vibrant, moving ambient mesh backgrounds", dynamicBg) {
                ImmersiveSettingsPref.setDynamicBackgrounds(context, it)
            }

            // Category 2: Canvas & Performance
            SettingsSectionHeader(title = "Canvas & Performance", icon = Icons.Rounded.Movie)
            val autoSaver by ImmersiveSettingsPref.autoCanvasSaverMobileDataEnabled.collectAsState()
            SwitchSettingRow(
                "Automatic Canvas Saver on Mobile Data",
                "Automatically reduces Canvas video quality and prevents unnecessary video downloads when using mobile data.",
                autoSaver,
            ) {
                ImmersiveSettingsPref.setAutoCanvasSaverMobileData(context, it)
            }

            val qualityLimit by ImmersiveSettingsPref.canvasQualityLimit.collectAsState()
            ChoiceSettingRow(
                "Canvas Quality Limit",
                when (qualityLimit) {
                    "480p" -> "480p"
                    "artwork_only" -> "Artwork Only"
                    else -> "Auto"
                },
            ) {
                val next = when (qualityLimit) {
                    "auto" -> "480p"
                    "480p" -> "artwork_only"
                    else -> "auto"
                }
                ImmersiveSettingsPref.setCanvasQualityLimit(context, next)
            }

            val animatedArt by ImmersiveSettingsPref.animatedArtworkEnabled.collectAsState()
            SwitchSettingRow("Animated Artwork", "Floating motion, subtle scale and ambient sheen over cover art", animatedArt) {
                ImmersiveSettingsPref.setAnimatedArtwork(context, it)
            }

            val gestureCtrl by ImmersiveSettingsPref.gestureControlsEnabled.collectAsState()
            SwitchSettingRow("Gesture Controls", "Swipe left/right to skip tracks, double tap to like, long press for menu", gestureCtrl) {
                ImmersiveSettingsPref.setGestureControls(context, it)
            }

            val visualizerOn by ImmersiveSettingsPref.visualizerEnabled.collectAsState()
            SwitchSettingRow("Subtle Visualizer", "Fluid waveform and pulse rings surrounding artwork", visualizerOn) {
                ImmersiveSettingsPref.setVisualizer(context, it)
            }

            if (visualizerOn) {
                val visInt by ImmersiveSettingsPref.visualizerIntensity.collectAsState()
                ChoiceSettingRow("Visualizer Intensity", visInt.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }) {
                    val next = when (visInt) {
                        "low" -> "medium"
                        "medium" -> "high"
                        else -> "low"
                    }
                    ImmersiveSettingsPref.setVisualizerIntensity(context, next)
                }
            }

            // Category 3: Lyrics
            SettingsSectionHeader(title = "Lyrics Engine", icon = Icons.Rounded.Lyrics)
            val autoResolver by ImmersiveSettingsPref.autoResolverEnabled.collectAsState()
            SwitchSettingRow("Automatic Lyrics Resolver", "Auto-select best synchronized lyrics source", autoResolver) {
                ImmersiveSettingsPref.autoResolverEnabled.value = it
            }

            // Category 4: Music Video & Audio Handoff
            SettingsSectionHeader(title = "Music Video & Audio Handoff", icon = Icons.Rounded.OndemandVideo)
            val smartAudioHandoff by ImmersiveSettingsPref.smartAudioHandoff.collectAsState()
            SwitchSettingRow("2-Second Long Press Audio Handoff", "Hold video button for 2 seconds to toggle audio between Spotify and Video while keeping video visible", smartAudioHandoff) {
                ImmersiveSettingsPref.smartAudioHandoff.value = it
            }

            // Category 5: Performance & Accessibility
            SettingsSectionHeader(title = "Performance & Accessibility", icon = Icons.Rounded.Speed)
            val reducedMotion by ImmersiveSettingsPref.reducedMotion.collectAsState()
            SwitchSettingRow("Reduced Motion", "Simplify animations for lower battery/CPU usage", reducedMotion) {
                ImmersiveSettingsPref.reducedMotion.value = it
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(20.dp))
        Spacer(Modifier.padding(horizontal = 4.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1DB954))
    }
}

@Composable
private fun SwitchSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Text(text = subtitle, fontSize = 13.sp, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ChoiceSettingRow(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 14.sp, color = Color(0xFF1DB954), fontWeight = FontWeight.SemiBold)
    }
}
