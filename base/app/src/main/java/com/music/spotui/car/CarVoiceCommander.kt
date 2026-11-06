// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package com.music.spotui.car

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import app.myspoti.host.android.Logbook
import com.music.spotui.di.Graph
import com.music.spotui.di.SongPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    data class Recognized(val text: String, val actionFeedback: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

/**
 * Android SpeechRecognizer integration for Car Mode.
 *
 * Dispatches verified voice commands directly to [SongPlayer] without requiring third-party cloud AI APIs.
 * Handles speech recognition errors, unsupported hardware, and missing permissions gracefully.
 */
class CarVoiceCommander(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null
    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    @Volatile
    private var commandExecuted = false

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        if (!isAvailable()) {
            _state.value = VoiceState.Error("Voice recognition not available on this device")
            return
        }

        commandExecuted = false

        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _state.value = VoiceState.Listening
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        if (commandExecuted) {
                            Logbook.log("CarVoiceCommander: onError($error) ignored after command execution")
                            return
                        }
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Didn't catch that. Tap mic to try again."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection required"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognition busy"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out. Tap mic to try again."
                            else -> "Voice recognition error. Tap mic to try again."
                        }
                        _state.value = VoiceState.Error(message)
                    }

                    override fun onResults(results: Bundle?) {
                        if (commandExecuted) return
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull()?.trim()
                        if (spoken.isNullOrBlank()) {
                            _state.value = VoiceState.Error("Could not understand command")
                        } else {
                            commandExecuted = true
                            handleCommand(spoken)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer?.startListening(intent)
            _state.value = VoiceState.Listening
        } catch (e: Exception) {
            Logbook.log("CarVoiceCommander start error: ${e.message}")
            _state.value = VoiceState.Error(e.message ?: "Voice recognition failed")
        }
    }

    fun stopListening() {
        try {
            recognizer?.stopListening()
        } catch (e: Exception) {
            // ignore
        }
        _state.value = VoiceState.Idle
    }

    fun destroy() {
        try {
            recognizer?.destroy()
        } catch (e: Exception) {
            // ignore
        }
        recognizer = null
        _state.value = VoiceState.Idle
    }

    private fun handleCommand(spokenText: String) {
        val lower = spokenText.lowercase(Locale.ROOT)
        Logbook.log("Car voice command: \"$spokenText\"")

        val actionFeedback = when {
            lower.contains("pause") || lower.contains("stop") || lower.contains("pauza") || lower.contains("stani") || lower.contains("zaustavi") -> {
                SongPlayer.pause("car_voice")
                "Paused playback"
            }
            lower.contains("play my liked") || lower.contains("liked songs") || lower.contains("favorites") || lower.contains("omiljene") -> {
                SongPlayer.playSong("spotify:collection:tracks")
                "Playing Liked Songs"
            }
            lower.contains("driving playlist") || lower.contains("driving mix") || lower.contains("za vožnju") -> {
                Graph.api.let { _ ->
                    SongPlayer.playSong("spotify:playlist:37i9dQZF1DXdLEN7aqioXM")
                }
                "Playing Driving Playlist"
            }
            lower.contains("energetic") || lower.contains("energy") -> {
                SongPlayer.playSong("spotify:playlist:37i9dQZF1DX76Wlfdnj7AP")
                "Playing Energetic Mix"
            }
            lower.contains("skip") || lower.contains("next") || lower.contains("sledeca") || lower.contains("sledeća") || lower.contains("dalje") -> {
                SongPlayer.next("car_voice")
                "Playing next song"
            }
            lower.contains("previous") || lower.contains("back") || lower.contains("restart") || lower.contains("prethodna") || lower.contains("nazad") -> {
                SongPlayer.previous("car_voice")
                "Playing previous song"
            }
            lower.contains("shuffle on") || lower.contains("turn on shuffle") || lower.contains("enable shuffle") || lower.contains("uključi nasumično") -> {
                SongPlayer.setShuffle(true)
                "Shuffle turned on"
            }
            lower.contains("shuffle off") || lower.contains("turn off shuffle") || lower.contains("disable shuffle") || lower.contains("isključi nasumično") -> {
                SongPlayer.setShuffle(false)
                "Shuffle turned off"
            }
            lower.contains("resume") || lower.trim() == "play" || lower.trim() == "pusti" || lower.trim() == "play music" || lower.trim() == "pusti muziku" || lower.contains("nastavi") || lower.contains("plej") -> {
                SongPlayer.play("car_voice")
                "Resumed playback"
            }
            lower.startsWith("play ") || lower.startsWith("pusti ") -> {
                val query = lower.removePrefix("play ").removePrefix("pusti ").trim()
                if (query.isNotBlank()) {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).apply {
                        SongPlayer.playSong("spotify:search:$query")
                    }
                    "Searching & playing \"$query\""
                } else {
                    "Unrecognized command: \"$spokenText\""
                }
            }
            else -> {
                "Unrecognized command: \"$spokenText\""
            }
        }

        _state.value = VoiceState.Recognized(spokenText, actionFeedback)
    }
}
