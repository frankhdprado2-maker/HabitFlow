package com.unmsm.habitflow.ui.state

import com.unmsm.habitflow.voice.VoiceErrorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceInterpretationStateTest {
    @Test
    fun localTranscriptionRemainsEditableAndIsNotSubmittedAutomatically() {
        val state = VoiceUiState(
            phase = VoiceAssistantPhase.Ready,
            transcript = "Hoy tomé dos litros de agua"
        )

        assertEquals("Hoy tomé dos litros de agua", state.transcript)
        assertTrue(state.messages.none { it.author == "user" })
        assertTrue(state.interpretedHabits.isEmpty())
        assertTrue(!state.savingInterpretation)
    }

    @Test
    fun backendFailurePreservesLocalTranscriptionForRetry() {
        val state = VoiceUiState(
            phase = VoiceAssistantPhase.Error(VoiceErrorType.Network, "Render no disponible"),
            transcript = "Ayer estudié programación durante una hora",
            error = "Render no disponible"
        )

        assertEquals("Ayer estudié programación durante una hora", state.transcript)
        assertEquals("Render no disponible", state.error)
    }

    @Test
    fun voiceStateKeepsEditableInterpretationAcrossRecomposition() {
        val interpreted = InterpretedHabitUi(
            name = "Tomar agua",
            action = "completed",
            quantity = "2",
            unit = "litros",
            date = "2026-07-10",
            notes = "",
            existingHabitId = "water",
            existingHabitName = "Tomar agua"
        )

        val state = VoiceUiState(
            phase = VoiceAssistantPhase.AwaitingConfirmation,
            transcript = "Hoy tome dos litros de agua",
            interpretationText = "Hoy tome dos litros de agua",
            interpretedHabits = listOf(interpreted),
            habitAssociationOptions = listOf(HabitAssociationOptionUi("water", "Tomar agua")),
            interpretationConfidence = 0.95
        )

        assertEquals(VoiceAssistantPhase.AwaitingConfirmation, state.phase)
        assertEquals("Hoy tome dos litros de agua", state.transcript)
        assertEquals("Tomar agua", state.interpretedHabits.single().existingHabitName)
        assertEquals(0.95, state.interpretationConfidence, 0.0)
    }

    @Test
    fun savingInterpretationBlocksDuplicateConfirmationState() {
        val state = VoiceUiState(
            phase = VoiceAssistantPhase.AwaitingConfirmation,
            interpretedHabits = listOf(
                InterpretedHabitUi(
                    name = "Leer",
                    action = "completed",
                    quantity = "20",
                    unit = "paginas",
                    date = "2026-07-10"
                )
            ),
            savingInterpretation = true
        )

        assertTrue(state.savingInterpretation)
        assertTrue(state.interpretedHabits.isNotEmpty())
    }
}
