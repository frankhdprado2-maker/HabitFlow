package com.unmsm.habitflow.voice.whisper

sealed interface WhisperState {
    data object ModelNotPrepared : WhisperState
    data object PreparingModel : WhisperState
    data object Ready : WhisperState
    data class Recording(val operationId: Long, val durationMillis: Long, val audioLevel: Float) : WhisperState
    data class Processing(val operationId: Long) : WhisperState
    data class Result(val operationId: Long, val text: String) : WhisperState
    data class Error(val error: WhisperError, val operationId: Long? = null) : WhisperState
    data class Cancelled(val operationId: Long) : WhisperState
}

data class RecordingMetrics(
    val durationMillis: Long = 0,
    val audioLevel: Float = 0f,
    val reachedLimit: Boolean = false
)
