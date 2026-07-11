package com.unmsm.habitflow.voice

enum class VoiceErrorType {
    Audio,
    Client,
    InsufficientPermissions,
    Network,
    NetworkTimeout,
    NoMatch,
    RecognizerBusy,
    Server,
    SpeechTimeout,
    TooManyRequests,
    ServiceUnavailable,
    Unknown
}

data class VoiceRecognitionError(
    val type: VoiceErrorType,
    val message: String
)
