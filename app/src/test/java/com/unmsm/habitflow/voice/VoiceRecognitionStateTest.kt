package com.unmsm.habitflow.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceRecognitionStateTest {
    @Test
    fun recognitionStateModelsPartialFinalCancellationAndErrors() {
        val partial: VoiceRecognitionState = VoiceRecognitionState.PartialResult("Hoy medite")
        val result: VoiceRecognitionState = VoiceRecognitionState.Result("Hoy medite diez minutos")
        val cancelled: VoiceRecognitionState = VoiceRecognitionState.Cancelled
        val error: VoiceRecognitionState = VoiceRecognitionState.Error(
            VoiceRecognitionError(
                type = VoiceErrorType.NoMatch,
                message = "No se detecto ninguna voz."
            )
        )

        assertEquals("Hoy medite", (partial as VoiceRecognitionState.PartialResult).text)
        assertEquals("Hoy medite diez minutos", (result as VoiceRecognitionState.Result).text)
        assertEquals(VoiceRecognitionState.Cancelled, cancelled)
        assertTrue(error is VoiceRecognitionState.Error)
        assertEquals(VoiceErrorType.NoMatch, (error as VoiceRecognitionState.Error).error.type)
    }
}
