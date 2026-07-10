package com.unmsm.habitflow.voice

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var listening = false

    fun isSpeechRecognitionAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ): Result<Unit> =
        runCatching {
            if (!isSpeechRecognitionAvailable()) {
                error("El reconocimiento de voz no está disponible en este dispositivo.")
            }
            stopSpeaking()
            stopListening()
            val recognizer = createSpeechRecognizer().also { speechRecognizer = it }
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    listening = false
                }

                override fun onError(error: Int) {
                    listening = false
                    onError(speechErrorMessage(error))
                }

                override fun onResults(results: Bundle?) {
                    listening = false
                    val finalText = results.bestSpeechText()
                    if (finalText.isBlank()) {
                        onError("No pude entender el audio. Intenta nuevamente o escríbelo.")
                    } else {
                        onFinal(finalText)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partialText = partialResults.bestSpeechText()
                    if (partialText.isNotBlank()) onPartial(partialText)
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            recognizer.startListening(recognizerIntent())
            listening = true
        }

    fun stopListening() {
        runCatching { speechRecognizer?.stopListening() }
        runCatching { speechRecognizer?.cancel() }
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
        listening = false
    }

    fun cancelListening() = stopListening()

    fun startRecording(): Result<File> =
        runCatching {
            stopSilently()
            val directory = File(context.cacheDir, "voice").apply { mkdirs() }
            val file = File.createTempFile("habitflow-voice-", ".m4a", directory)
            val mediaRecorder = createRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16_000)
                setAudioEncodingBitRate(64_000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            currentFile = file
            file
        }

    fun stopRecording(): Result<File> =
        runCatching {
            val file = currentFile ?: error("No hay una grabacion activa.")
            val mediaRecorder = recorder ?: error("No hay una grabacion activa.")
            try {
                mediaRecorder.stop()
            } finally {
                mediaRecorder.release()
                recorder = null
                currentFile = null
            }
            if (!file.exists() || file.length() < 1_000L) {
                file.delete()
                error("No pude capturar audio suficiente.")
            }
            file
        }

    fun cancelRecording() {
        val file = currentFile
        stopSilently()
        file?.delete()
    }

    fun speak(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        stopListening()
        val currentTts = tts
        if (currentTts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("es", "PE")
                    tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "habitflow-voice")
                }
            }
        } else {
            currentTts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "habitflow-voice")
        }
    }

    fun stopSpeaking() {
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        cancelRecording()
        stopListening()
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
    }

    private fun stopSilently() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        currentFile = null
    }

    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    private fun createSpeechRecognizer(): SpeechRecognizer =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

    private fun recognizerIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dime qué hábito quieres registrar")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

    private fun Bundle?.bestSpeechText(): String =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()

    private fun speechErrorMessage(error: Int): String =
        when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "No pude acceder al audio. Revisa el micrófono."
            SpeechRecognizer.ERROR_CLIENT -> "Se canceló la escucha. Intenta nuevamente."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Activa el permiso de micrófono para dictar."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "No hay conexión para reconocer voz. Puedes escribir el hábito."
            SpeechRecognizer.ERROR_NO_MATCH -> "No pude entender el audio. Intenta nuevamente o escríbelo."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El micrófono está ocupado. Espera un momento."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No escuché nada. Toca el micrófono cuando estés listo."
            else -> "El reconocimiento de voz falló. Puedes escribirlo manualmente."
        }
}
