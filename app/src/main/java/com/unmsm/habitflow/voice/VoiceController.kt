package com.unmsm.habitflow.voice

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class VoiceController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var listening = false
    private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    val state: StateFlow<VoiceRecognitionState> = _state.asStateFlow()

    fun isSpeechRecognitionAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun isOnDeviceSpeechRecognitionAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    fun startListening(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (VoiceRecognitionError) -> Unit
    ): Result<Unit> =
        runCatching {
            if (!isSpeechRecognitionAvailable()) {
                error("El reconocimiento de voz no esta disponible en este dispositivo.")
            }
            _state.value = VoiceRecognitionState.ProcessingSpeech
            stopSpeaking()
            stopListening()

            val preferOffline = isOnDeviceSpeechRecognitionAvailable()
            val recognizer = createSpeechRecognizer(preferOffline).also { speechRecognizer = it }
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                    _state.value = VoiceRecognitionState.Listening
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    listening = false
                    _state.value = VoiceRecognitionState.ProcessingSpeech
                }

                override fun onError(error: Int) {
                    listening = false
                    val recognitionError = speechError(error)
                    releaseSpeechRecognizer()
                    _state.value = VoiceRecognitionState.Error(recognitionError)
                    onError(recognitionError)
                }

                override fun onResults(results: Bundle?) {
                    listening = false
                    val finalText = results.bestSpeechText()
                    if (finalText.isBlank()) {
                        val recognitionError = VoiceRecognitionError(
                            VoiceErrorType.NoMatch,
                            "No pude entenderte. Intentalo nuevamente o escribe el mensaje."
                        )
                        releaseSpeechRecognizer()
                        _state.value = VoiceRecognitionState.Error(recognitionError)
                        onError(recognitionError)
                    } else {
                        releaseSpeechRecognizer()
                        _state.value = VoiceRecognitionState.Result(finalText)
                        onFinal(finalText)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partialText = partialResults.bestSpeechText()
                    if (partialText.isNotBlank()) {
                        _state.value = VoiceRecognitionState.PartialResult(partialText)
                        onPartial(partialText)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            recognizer.startListening(recognizerIntent(preferOffline))
            listening = true
        }

    fun stopListening() {
        runCatching { speechRecognizer?.stopListening() }
        runCatching { speechRecognizer?.cancel() }
        releaseSpeechRecognizer()
        _state.value = VoiceRecognitionState.Idle
    }

    fun cancelListening() {
        stopListening()
        _state.value = VoiceRecognitionState.Cancelled
    }

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

    private fun releaseSpeechRecognizer() {
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
        listening = false
    }

    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    private fun createSpeechRecognizer(preferOffline: Boolean): SpeechRecognizer =
        if (preferOffline && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

    private fun recognizerIntent(preferOffline: Boolean): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 4)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dime que habito quieres registrar")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && preferOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

    private fun Bundle?.bestSpeechText(): String =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()

    private fun speechError(error: Int): VoiceRecognitionError =
        when (error) {
            SpeechRecognizer.ERROR_AUDIO -> VoiceRecognitionError(
                VoiceErrorType.Audio,
                "No pude acceder al audio. Revisa el microfono."
            )
            SpeechRecognizer.ERROR_CLIENT -> VoiceRecognitionError(
                VoiceErrorType.Client,
                "Se cancelo la escucha. Intenta nuevamente."
            )
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceRecognitionError(
                VoiceErrorType.InsufficientPermissions,
                "Necesito permiso para usar el microfono."
            )
            SpeechRecognizer.ERROR_NETWORK -> VoiceRecognitionError(
                VoiceErrorType.Network,
                "El reconocimiento por voz no esta disponible en este momento."
            )
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> VoiceRecognitionError(
                VoiceErrorType.NetworkTimeout,
                "El reconocimiento por voz tardo demasiado. Puedes escribir el mensaje."
            )
            SpeechRecognizer.ERROR_NO_MATCH -> VoiceRecognitionError(
                VoiceErrorType.NoMatch,
                "No pude entenderte. Intentalo nuevamente o escribe el mensaje."
            )
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceRecognitionError(
                VoiceErrorType.RecognizerBusy,
                "El microfono esta ocupado. Espera un momento e intentalo nuevamente."
            )
            SpeechRecognizer.ERROR_SERVER -> VoiceRecognitionError(
                VoiceErrorType.Server,
                "El reconocimiento por voz no esta disponible en este momento."
            )
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceRecognitionError(
                VoiceErrorType.SpeechTimeout,
                "No escuche ninguna frase."
            )
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> VoiceRecognitionError(
                VoiceErrorType.TooManyRequests,
                "El reconocimiento recibio demasiados intentos. Espera un momento."
            )
            else -> VoiceRecognitionError(
                VoiceErrorType.Unknown,
                "No pude procesar el audio. Intentalo nuevamente o continua manualmente."
            )
        }
}
