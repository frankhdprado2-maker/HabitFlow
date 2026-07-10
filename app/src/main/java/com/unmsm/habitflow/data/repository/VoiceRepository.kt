package com.unmsm.habitflow.data.repository

import com.unmsm.habitflow.data.remote.api.VoiceApi
import com.unmsm.habitflow.data.remote.dto.VoiceHabitContextDto
import com.unmsm.habitflow.data.remote.dto.VoiceCommandRequest
import com.unmsm.habitflow.data.toDomain
import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.VoiceCommandResult
import com.unmsm.habitflow.util.AppResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRepository @Inject constructor(
    private val voiceApi: VoiceApi
) {
    suspend fun command(
        text: String,
        habits: List<Habit> = emptyList(),
        conversationId: String? = null
    ): AppResult<VoiceCommandResult> =
        runNetwork {
            voiceApi.command(
                VoiceCommandRequest(
                    text = text,
                    habits = habits.map { habit ->
                        VoiceHabitContextDto(
                            id = habit.id,
                            name = habit.name,
                            category = habit.category
                        )
                    },
                    conversationId = conversationId
                )
            ).toDomain()
        }
}
