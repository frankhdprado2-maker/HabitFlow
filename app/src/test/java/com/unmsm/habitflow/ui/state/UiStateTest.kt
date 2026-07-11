package com.unmsm.habitflow.ui.state

import com.unmsm.habitflow.voice.VoiceErrorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStateTest {
    @Test
    fun loginStateStartsWithGoogleIdle() {
        val state = LoginUiState()

        assertEquals(GoogleLoginState.Idle, state.googleState)
        assertTrue(state.error == null)
    }

    @Test
    fun voiceErrorKeepsTypedCause() {
        val phase = VoiceAssistantPhase.Error(
            type = VoiceErrorType.NetworkTimeout,
            message = "El reconocimiento por voz tardo demasiado."
        )

        assertEquals(VoiceErrorType.NetworkTimeout, phase.type)
        assertEquals("El reconocimiento por voz tardo demasiado.", phase.message)
    }
}
