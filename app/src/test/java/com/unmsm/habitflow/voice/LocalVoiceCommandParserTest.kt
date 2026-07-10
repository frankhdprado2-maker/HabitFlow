package com.unmsm.habitflow.voice

import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalVoiceCommandParserTest {
    private val habits = listOf(
        Habit(
            id = "run",
            name = "Correr 30 minutos",
            icon = "GO",
            frequency = "Diario",
            reminderTime = "18:00",
            category = "Ejercicio"
        ),
        Habit(
            id = "read",
            name = "Leer 20 páginas",
            icon = "LE",
            frequency = "Diario",
            reminderTime = "21:00",
            category = "Lectura"
        )
    )

    @Test
    fun personalNameIsNotParsedAsHabit() {
        val result = LocalVoiceCommandParser.parse("Hola, soy Frank.", habits)

        assertEquals("aclaracion", result.intent)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun spokenCompletionRequiresConfirmation() {
        val result = LocalVoiceCommandParser.parse("Ya corrí treinta minutos.", habits)

        assertEquals("registrar_habito", result.intent)
        assertEquals("run", result.events.single().habitId)
        assertEquals(HabitStatus.Completed, result.events.single().status)
        assertEquals(30.0, result.events.single().quantity ?: 0.0, 0.0)
    }

    @Test
    fun createHabitFromNaturalPhrase() {
        val result = LocalVoiceCommandParser.parse("Quiero leer veinte páginas todos los días a las nueve.", emptyList())

        assertEquals("registrar_habito", result.intent)
        assertEquals("Leer veinte paginas", result.events.single().habitName)
        assertEquals(HabitStatus.Pending, result.events.single().status)
    }
}
