package com.unmsm.habitflow.ui.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceInterpretationStateTest {
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
