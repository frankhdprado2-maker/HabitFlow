package com.unmsm.habitflow.data.repository

import com.unmsm.habitflow.data.remote.api.VoiceApi
import com.unmsm.habitflow.data.remote.dto.VoiceAchievementContextDto
import com.unmsm.habitflow.data.remote.dto.VoiceCommandRequest
import com.unmsm.habitflow.data.remote.dto.VoiceEventContextDto
import com.unmsm.habitflow.data.remote.dto.VoiceHabitContextDto
import com.unmsm.habitflow.data.toDomain
import com.unmsm.habitflow.domain.model.Achievement
import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitEvent
import com.unmsm.habitflow.domain.model.VoiceCommandResult
import com.unmsm.habitflow.util.AppResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class VoiceRepository @Inject constructor(
    private val voiceApi: VoiceApi
) {
    suspend fun transcribe(audioFile: File, language: String = "es"): AppResult<String> =
        runNetwork {
            val audioBody = audioFile.asRequestBody("audio/mp4".toMediaType())
            val audioPart = MultipartBody.Part.createFormData(
                name = "audio",
                filename = audioFile.name,
                body = audioBody
            )
            val languageBody = language.toRequestBody("text/plain".toMediaType())
            voiceApi.transcribe(audioPart, languageBody).transcript
        }

    suspend fun command(
        text: String,
        habits: List<Habit> = emptyList(),
        recentEvents: List<HabitEvent> = emptyList(),
        achievements: List<Achievement> = emptyList(),
        categories: List<String> = emptyList(),
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
                    recentEvents = recentEvents.map { event ->
                        VoiceEventContextDto(
                            habitId = event.habitId,
                            habitName = event.habitName,
                            status = event.status.name,
                            timestamp = event.timestamp
                        )
                    },
                    achievements = achievements.map { achievement ->
                        VoiceAchievementContextDto(
                            id = achievement.id,
                            title = achievement.title,
                            description = achievement.description,
                            requirement = achievement.requirement,
                            unlocked = achievement.unlocked,
                            xp = achievement.xp
                        )
                    },
                    categories = categories,
                    conversationId = conversationId
                )
            ).toDomain()
        }
}
