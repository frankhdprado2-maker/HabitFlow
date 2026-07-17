package com.unmsm.habitflow.voice.whisper

sealed interface WhisperState {
    data object ModelNotPrepared : WhisperState
    data object PreparingModel : WhisperState
    data object Ready : WhisperState
    data class Recording(val durationMillis: Long, val audioLevel: Float) : WhisperState
    data object Processing : WhisperState
    data class Result(val text: String) : WhisperState
    data class Error(val error: WhisperError) : WhisperState
    data object Cancelled : WhisperState
}

data class RecordingMetrics(
    val durationMillis: Long = 0,
    val audioLevel: Float = 0f,
    val reachedLimit: Boolean = false
)
