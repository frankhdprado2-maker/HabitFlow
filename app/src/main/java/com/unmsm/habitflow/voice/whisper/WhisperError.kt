package com.unmsm.habitflow.voice.whisper

enum class WhisperErrorType {
    PermissionDenied,
    PermissionPermanentlyDenied,
    MicrophoneUnavailable,
    MicrophoneBusy,
    ModelNotFound,
    ModelCorrupt,
    ModelIncompatible,
    NativeLibraryUnavailable,
    NativeFailure,
    EmptyRecording,
    RecordingTooShort,
    RecordingTooLong,
    OutOfMemory,
    InferenceCancelled,
    EmptyText,
    TranscriptionFailed,
    RecordingInterrupted
}

class WhisperError(
    val type: WhisperErrorType,
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)

internal fun Throwable.toWhisperError(): WhisperError {
    if (this is WhisperError) return this
    if (this is OutOfMemoryError) {
        return WhisperError(
            WhisperErrorType.OutOfMemory,
            "El dispositivo no tiene memoria suficiente para ejecutar Whisper.",
            this
        )
    }
    val nativeMessage = message.orEmpty()
    return when {
        nativeMessage.contains("INFERENCE_CANCELLED") -> WhisperError(
            WhisperErrorType.InferenceCancelled,
            "La transcripción fue cancelada.",
            this
        )
        nativeMessage.contains("MODEL_INCOMPATIBLE") -> WhisperError(
            WhisperErrorType.ModelIncompatible,
            "No se pudo inicializar el modelo de voz.",
            this
        )
        nativeMessage.contains("MODEL_NOT_FOUND") || nativeMessage.contains("MODEL_NOT_INITIALIZED") -> WhisperError(
            WhisperErrorType.ModelNotFound,
            "No se encontró el modelo local de voz.",
            this
        )
        else -> WhisperError(
            WhisperErrorType.TranscriptionFailed,
            "No se detectó voz con claridad. No se envió audio fuera de tu dispositivo.",
            this
        )
    }
}
