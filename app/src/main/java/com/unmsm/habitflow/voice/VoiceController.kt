package com.unmsm.habitflow.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import com.unmsm.habitflow.voice.whisper.WhisperState
import com.unmsm.habitflow.voice.whisper.WhisperTranscriber
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class VoiceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transcriber: WhisperTranscriber
) {
    private var tts: TextToSpeech? = null
    val state: StateFlow<WhisperState> = transcriber.state

    suspend fun prepareModel(): Result<Unit> = transcriber.prepareModel()
    suspend fun startRecording(): Result<Unit> = transcriber.startRecording()
    suspend fun stopRecordingAndTranscribe(): Result<String> = transcriber.stopAndTranscribe()
    suspend fun cancelListening() = transcriber.cancel()

    suspend fun handleAppBackgrounded() {
        when (backgroundVoiceAction(state.value)) {
            BackgroundVoiceAction.StopAndTranscribe -> stopRecordingAndTranscribe()
            BackgroundVoiceAction.KeepTranscribing,
            BackgroundVoiceAction.None -> Unit
        }
    }

    fun speak(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        stopSpeaking()
        val current = tts
        if (current == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("es", "PE")
                    tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "habitflow-voice")
                }
            }
        } else {
            current.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "habitflow-voice")
        }
    }

    fun stopSpeaking() {
        runCatching { tts?.stop() }
    }

    suspend fun shutdown() {
        cancelListening()
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
    }
}

internal enum class BackgroundVoiceAction {
    StopAndTranscribe,
    KeepTranscribing,
    None
}

internal fun backgroundVoiceAction(state: WhisperState): BackgroundVoiceAction = when (state) {
    is WhisperState.Recording -> BackgroundVoiceAction.StopAndTranscribe
    is WhisperState.Processing -> BackgroundVoiceAction.KeepTranscribing
    else -> BackgroundVoiceAction.None
}
