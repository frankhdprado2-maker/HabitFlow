package com.unmsm.habitflow.data

import com.unmsm.habitflow.data.remote.dto.VoiceConversationResponse
import com.unmsm.habitflow.data.remote.dto.VoiceWeeklySummaryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceConversationMapperTest {
    @Test
    fun weeklySummaryBecomesAnEvidenceBackedCoachResult() {
        val response = VoiceConversationResponse(
            sessionId = "session-1",
            assistantMessage = "Completaste 5 de 7 actividades.",
            intent = "GENERATE_WEEKLY_SUMMARY",
            suggestions = listOf("Plan de hoy"),
            weeklySummary = VoiceWeeklySummaryDto(
                headline = "Una semana en progreso",
                summary = "Completaste 5 de 7 actividades.",
                highlights = listOf("Tu tasa semanal fue 71%.", "Leer fue tu hábito más constante."),
                recommendation = "Mantén una meta pequeña.",
                dataSufficient = true
            )
        )

        val result = response.toDomainCommandResult()

        assertEquals("GENERATE_WEEKLY_SUMMARY", result.intent)
        assertEquals("Una semana en progreso", result.plan?.title)
        assertEquals("Completaste 5 de 7 actividades.", result.response)
        assertEquals("Plan de hoy", result.quickReplies.single())
        assertTrue(result.plan?.actions.orEmpty().contains("Tu tasa semanal fue 71%."))
        assertTrue(result.events.isEmpty())
    }
}
