package com.unmsm.habitflow.voice

sealed interface VoiceRecognitionState {
    data object Idle : VoiceRecognitionState
    data object RequestingPermission : VoiceRecognitionState
    data object Listening : VoiceRecognitionState
    data object ProcessingSpeech : VoiceRecognitionState
    data class PartialResult(val text: String) : VoiceRecognitionState
    data class Result(val text: String) : VoiceRecognitionState
    data class Error(val error: VoiceRecognitionError) : VoiceRecognitionState
    data object Cancelled : VoiceRecognitionState
}
