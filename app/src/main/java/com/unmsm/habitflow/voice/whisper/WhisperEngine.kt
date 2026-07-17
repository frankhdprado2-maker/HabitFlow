package com.unmsm.habitflow.voice.whisper

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal interface WhisperInferenceEngine {
    suspend fun initialize(modelPath: String)
    suspend fun transcribe(samples: FloatArray, language: String = "es"): String
    fun cancel()
    suspend fun release()
}

@Singleton
class WhisperEngine @Inject constructor() : WhisperInferenceEngine {
    private val inferenceMutex = Mutex()
    private var loadedPath: String? = null
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default

    override suspend fun initialize(modelPath: String) = inferenceMutex.withLock {
        if (loadedPath == modelPath) return@withLock
        withContext(dispatcher) {
            WhisperNative.ensureLoaded()
            runCatching { WhisperNative.initializeModel(modelPath) }
                .getOrElse { throw it.toWhisperError() }
        }
        loadedPath = modelPath
    }

    override suspend fun transcribe(samples: FloatArray, language: String): String = inferenceMutex.withLock {
        if (loadedPath == null) {
            throw WhisperError(WhisperErrorType.ModelNotFound, "No se encontró el modelo local de voz.")
        }
        val threads = (Runtime.getRuntime().availableProcessors() - 1).coerceIn(2, 4)
        withContext(dispatcher) {
            runCatching { WhisperNative.transcribe(samples, language, threads) }
                .getOrElse { throw it.toWhisperError() }
                .cleanWhisperText()
                .ifBlank { throw WhisperError(WhisperErrorType.EmptyText, "No se detectó voz con claridad.") }
        }
    }

    override fun cancel() {
        runCatching { WhisperNative.cancelTranscription() }
    }

    override suspend fun release() = inferenceMutex.withLock {
        withContext(dispatcher) { runCatching { WhisperNative.releaseModel() } }
        loadedPath = null
    }
}

internal fun String.cleanWhisperText(): String =
    replace(Regex("\\[[^]]+]"), " ")
        .replace(Regex("<\\|[^|]+\\|>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
