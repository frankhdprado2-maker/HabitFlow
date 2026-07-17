package com.unmsm.habitflow.voice.whisper

internal object WhisperNative {
    private val loadFailure: Throwable? = runCatching { System.loadLibrary("whisper_jni") }.exceptionOrNull()

    fun ensureLoaded() {
        loadFailure?.let {
            throw WhisperError(
                WhisperErrorType.NativeLibraryUnavailable,
                "No se pudo cargar la biblioteca local de voz.",
                it
            )
        }
    }

    external fun initializeModel(modelPath: String): Int
    external fun transcribe(samples: FloatArray, language: String, threadCount: Int): String
    external fun cancelTranscription()
    external fun releaseModel()
}
