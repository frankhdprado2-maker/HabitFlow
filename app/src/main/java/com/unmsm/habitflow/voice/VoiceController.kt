package com.unmsm.habitflow.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val VOICE_LOG_TAG = "HabitFlowVoice"

enum class VoiceRecognitionMode {
    OnDevice,
    System
}

internal data class SpeechRecognitionConfig(
    val language: String,
    val preferOffline: Boolean,
    val partialResults: Boolean = true,
    val maxResults: Int = 3
)

internal interface SpeechRecognitionCallback {
    fun onReadyForSpeech()
    fun onBeginningOfSpeech()
    fun onRmsChanged(rmsDb: Float)
    fun onPartialResults(texts: List<String>)
    fun onEndOfSpeech()
    fun onResults(texts: List<String>)
    fun onError(error: Int)
}

internal interface SpeechRecognitionService {
    val mode: VoiceRecognitionMode
    fun startListening(config: SpeechRecognitionConfig, callback: SpeechRecognitionCallback)
    fun stopListening()
    fun cancel()
    fun destroy()
}

internal interface SpeechRecognitionServiceFactory {
    fun isRecognitionAvailable(): Boolean
    fun isOnDeviceRecognitionAvailable(): Boolean
    fun isCurrentThreadMain(): Boolean
    fun create(preferOnDevice: Boolean): SpeechRecognitionService
}

@Singleton
class VoiceController private constructor(
    private val speechFactory: SpeechRecognitionServiceFactory,
    private val context: Context?
) {
    @Inject
    constructor(
        @ApplicationContext context: Context
    ) : this(AndroidSpeechRecognitionServiceFactory(context), context)

    internal constructor(
        speechFactory: SpeechRecognitionServiceFactory
    ) : this(speechFactory, null)

    private var tts: TextToSpeech? = null
    private var speechService: SpeechRecognitionService? = null
    private var listening = false
    private var cancelledByUser = false
    private var currentLanguageIndex = 0
    private var activeCallbacks: ActiveVoiceCallbacks? = null
    private val recognitionLanguages = listOf("es-PE", "es-ES", "es")
    private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    val state: StateFlow<VoiceRecognitionState> = _state.asStateFlow()

    fun isSpeechRecognitionAvailable(): Boolean = speechFactory.isRecognitionAvailable()

    fun isOnDeviceSpeechRecognitionAvailable(): Boolean = speechFactory.isOnDeviceRecognitionAvailable()

    fun startListening(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (VoiceRecognitionError) -> Unit
    ): Result<VoiceRecognitionMode> =
        runCatching {
            if (!speechFactory.isRecognitionAvailable()) {
                error("El reconocimiento de voz no esta disponible en este dispositivo.")
            }
            if (!speechFactory.isCurrentThreadMain()) {
                error("El reconocimiento de voz debe iniciarse desde el hilo principal.")
            }
            if (listening) {
                error("Ya estoy escuchando. Deten la escucha actual antes de iniciar otra.")
            }

            stopSpeaking()
            cancelledByUser = false
            currentLanguageIndex = 0
            activeCallbacks = ActiveVoiceCallbacks(onPartial, onFinal, onError)
            _state.value = VoiceRecognitionState.ProcessingSpeech

            startCurrentLanguage()
        }

    fun stopListening() {
        if (listening) {
            runCatching { speechService?.stopListening() }
        }
        listening = false
        activeCallbacks = null
        _state.value = VoiceRecognitionState.Idle
    }

    fun cancelListening() {
        cancelledByUser = true
        if (listening || speechService != null) {
            runCatching { speechService?.cancel() }
        }
        listening = false
        activeCallbacks = null
        _state.value = VoiceRecognitionState.Cancelled
    }

    fun speak(text: String) {
        val clean = text.trim()
        val appContext = context ?: return
        if (clean.isBlank()) return
        stopListening()
        val currentTts = tts
        if (currentTts == null) {
            tts = TextToSpeech(appContext) { status ->
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
        cancelListening()
        destroySpeechRecognizer()
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
    }

    private fun startCurrentLanguage(): VoiceRecognitionMode {
        val callbacks = activeCallbacks ?: error("No hay callbacks activos para reconocimiento de voz.")
        val service = getOrCreateSpeechService()
        val language = recognitionLanguages[currentLanguageIndex]
        val config = SpeechRecognitionConfig(
            language = language,
            preferOffline = true
        )
        val callback = createRecognitionCallback(callbacks)
        service.startListening(config, callback)
        listening = true
        return service.mode
    }

    private fun getOrCreateSpeechService(): SpeechRecognitionService {
        speechService?.let { return it }
        val preferOnDevice = speechFactory.isOnDeviceRecognitionAvailable()
        return speechFactory.create(preferOnDevice).also { speechService = it }
    }

    private fun createRecognitionCallback(callbacks: ActiveVoiceCallbacks): SpeechRecognitionCallback =
        object : SpeechRecognitionCallback {
            override fun onReadyForSpeech() {
                listening = true
                _state.value = VoiceRecognitionState.Listening
            }

            override fun onBeginningOfSpeech() {
                listening = true
                _state.value = VoiceRecognitionState.Listening
            }

            override fun onRmsChanged(rmsDb: Float) = Unit

            override fun onPartialResults(texts: List<String>) {
                val partialText = texts.bestSpeechText()
                if (partialText.isNotBlank()) {
                    _state.value = VoiceRecognitionState.PartialResult(partialText)
                    callbacks.onPartial(partialText)
                }
            }

            override fun onEndOfSpeech() {
                listening = false
                _state.value = VoiceRecognitionState.ProcessingSpeech
            }

            override fun onResults(texts: List<String>) {
                listening = false
                val finalText = texts.bestSpeechText()
                if (finalText.isBlank()) {
                    emitRecognitionError(
                        VoiceRecognitionError(
                            type = VoiceErrorType.NoMatch,
                            message = "No pude entenderte. Intentalo nuevamente o escribe el mensaje.",
                            code = SpeechRecognizer.ERROR_NO_MATCH
                        ),
                        callbacks
                    )
                    return
                }
                activeCallbacks = null
                _state.value = VoiceRecognitionState.Result(finalText)
                callbacks.onFinal(finalText)
            }

            override fun onError(error: Int) {
                logSpeechRecognizerError(error)
                listening = false
                if (cancelledByUser) {
                    _state.value = VoiceRecognitionState.Cancelled
                    return
                }
                if (shouldRetryWithNextLanguage(error)) {
                    currentLanguageIndex += 1
                    _state.value = VoiceRecognitionState.ProcessingSpeech
                    runCatching { startCurrentLanguage() }
                        .onFailure { failure ->
                            emitRecognitionError(
                                VoiceRecognitionError(
                                    type = VoiceErrorType.ServiceUnavailable,
                                    message = failure.message ?: "No pude reiniciar el reconocimiento por voz.",
                                    code = error
                                ),
                                callbacks
                            )
                        }
                    return
                }
                emitRecognitionError(speechError(error), callbacks)
            }
        }

    private fun shouldRetryWithNextLanguage(error: Int): Boolean =
        (error == ERROR_LANGUAGE_NOT_SUPPORTED || error == ERROR_LANGUAGE_UNAVAILABLE) &&
            currentLanguageIndex < recognitionLanguages.lastIndex

    private fun emitRecognitionError(error: VoiceRecognitionError, callbacks: ActiveVoiceCallbacks) {
        activeCallbacks = null
        _state.value = VoiceRecognitionState.Error(error)
        callbacks.onError(error)
    }

    private fun destroySpeechRecognizer() {
        runCatching { speechService?.destroy() }
        speechService = null
        listening = false
        activeCallbacks = null
    }

    private fun speechError(error: Int): VoiceRecognitionError =
        when (error) {
            SpeechRecognizer.ERROR_AUDIO -> VoiceRecognitionError(
                VoiceErrorType.Audio,
                "No pude acceder al audio. Revisa el microfono.",
                error
            )
            SpeechRecognizer.ERROR_CLIENT -> VoiceRecognitionError(
                VoiceErrorType.Client,
                "El servicio de voz cancelo la escucha. Intenta nuevamente.",
                error
            )
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceRecognitionError(
                VoiceErrorType.InsufficientPermissions,
                "Necesito permiso para usar el microfono.",
                error
            )
            SpeechRecognizer.ERROR_NETWORK -> VoiceRecognitionError(
                VoiceErrorType.Network,
                "El servicio de reconocimiento del dispositivo necesita conexion o no esta disponible.",
                error
            )
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> VoiceRecognitionError(
                VoiceErrorType.NetworkTimeout,
                "El reconocimiento por voz tardo demasiado. Puedes escribir el mensaje.",
                error
            )
            SpeechRecognizer.ERROR_NO_MATCH -> VoiceRecognitionError(
                VoiceErrorType.NoMatch,
                "No pude entenderte. Intentalo nuevamente o escribe el mensaje.",
                error
            )
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceRecognitionError(
                VoiceErrorType.RecognizerBusy,
                "El microfono esta ocupado. Espera un momento e intentalo nuevamente.",
                error
            )
            SpeechRecognizer.ERROR_SERVER -> VoiceRecognitionError(
                VoiceErrorType.Server,
                "El servicio de reconocimiento del dispositivo no pudo responder.",
                error
            )
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceRecognitionError(
                VoiceErrorType.SpeechTimeout,
                "No detecte voz. Intenta hablar mas cerca del microfono o escribe el mensaje.",
                error
            )
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> VoiceRecognitionError(
                VoiceErrorType.TooManyRequests,
                "El reconocimiento recibio demasiados intentos. Espera un momento.",
                error
            )
            ERROR_LANGUAGE_NOT_SUPPORTED,
            ERROR_LANGUAGE_UNAVAILABLE -> VoiceRecognitionError(
                VoiceErrorType.ServiceUnavailable,
                "El idioma de reconocimiento no esta disponible en este dispositivo.",
                error
            )
            else -> VoiceRecognitionError(
                VoiceErrorType.Unknown,
                "No pude procesar el audio. Intentalo nuevamente o continua manualmente.",
                error
            )
        }
}

private data class ActiveVoiceCallbacks(
    val onPartial: (String) -> Unit,
    val onFinal: (String) -> Unit,
    val onError: (VoiceRecognitionError) -> Unit
)

private class AndroidSpeechRecognitionServiceFactory(
    private val context: Context
) : SpeechRecognitionServiceFactory {
    override fun isRecognitionAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun isOnDeviceRecognitionAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    override fun isCurrentThreadMain(): Boolean = Looper.myLooper() == Looper.getMainLooper()

    override fun create(preferOnDevice: Boolean): SpeechRecognitionService {
        if (preferOnDevice && isOnDeviceRecognitionAvailable()) {
            runCatching {
                return AndroidSpeechRecognitionService(
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context),
                    VoiceRecognitionMode.OnDevice
                )
            }.onFailure { error ->
                logRecognizerCreationError(error)
            }
        }
        return AndroidSpeechRecognitionService(
            SpeechRecognizer.createSpeechRecognizer(context),
            VoiceRecognitionMode.System
        )
    }
}

private class AndroidSpeechRecognitionService(
    private val recognizer: SpeechRecognizer,
    override val mode: VoiceRecognitionMode
) : SpeechRecognitionService {
    override fun startListening(config: SpeechRecognitionConfig, callback: SpeechRecognitionCallback) {
        recognizer.setRecognitionListener(callback.toAndroidListener())
        recognizer.startListening(config.toIntent())
    }

    override fun stopListening() {
        recognizer.stopListening()
    }

    override fun cancel() {
        recognizer.cancel()
    }

    override fun destroy() {
        recognizer.destroy()
    }
}

private fun SpeechRecognitionCallback.toAndroidListener(): RecognitionListener =
    object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = this@toAndroidListener.onReadyForSpeech()
        override fun onBeginningOfSpeech() = this@toAndroidListener.onBeginningOfSpeech()
        override fun onRmsChanged(rmsdB: Float) = this@toAndroidListener.onRmsChanged(rmsdB)
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = this@toAndroidListener.onEndOfSpeech()
        override fun onError(error: Int) = this@toAndroidListener.onError(error)
        override fun onResults(results: Bundle?) =
            this@toAndroidListener.onResults(results.speechTexts())
        override fun onPartialResults(partialResults: Bundle?) =
            this@toAndroidListener.onPartialResults(partialResults.speechTexts())
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

private fun SpeechRecognitionConfig.toIntent(): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partialResults)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Dime que habito quieres registrar")
    }

private fun Bundle?.speechTexts(): List<String> =
    this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()

private fun List<String>.bestSpeechText(): String =
    firstOrNull { it.isNotBlank() }?.trim().orEmpty()

private fun logSpeechRecognizerError(error: Int) {
    runCatching { Log.e(VOICE_LOG_TAG, "SpeechRecognizer error code=$error") }
}

private fun logRecognizerCreationError(error: Throwable) {
    runCatching { Log.e(VOICE_LOG_TAG, "Could not create on-device SpeechRecognizer", error) }
}

private const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
private const val ERROR_LANGUAGE_UNAVAILABLE = 13
