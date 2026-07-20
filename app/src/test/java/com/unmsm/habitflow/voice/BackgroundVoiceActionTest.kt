package com.unmsm.habitflow.voice

import com.unmsm.habitflow.voice.whisper.WhisperState
import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundVoiceActionTest {
    @Test
    fun recordingIsStoppedAndTranscribedWhenAppMovesToBackground() {
        assertEquals(
            BackgroundVoiceAction.StopAndTranscribe,
            backgroundVoiceAction(WhisperState.Recording(operationId = 1, durationMillis = 500, audioLevel = 0.4f))
        )
    }

    @Test
    fun activeTranscriptionContinuesWhenAppMovesToBackground() {
        assertEquals(
            BackgroundVoiceAction.KeepTranscribing,
            backgroundVoiceAction(WhisperState.Processing(operationId = 1))
        )
    }

    @Test
    fun inactiveVoiceSessionNeedsNoBackgroundAction() {
        assertEquals(BackgroundVoiceAction.None, backgroundVoiceAction(WhisperState.Ready))
    }
}
