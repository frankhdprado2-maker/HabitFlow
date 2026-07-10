package com.unmsm.habitflow.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VoiceCommandRequest(
    val text: String,
    val locale: String = "es-PE",
    val habits: List<VoiceHabitContextDto> = emptyList(),
    @Json(name = "recent_events") val recentEvents: List<VoiceEventContextDto> = emptyList(),
    val achievements: List<VoiceAchievementContextDto> = emptyList(),
    val categories: List<String> = emptyList(),
    @Json(name = "conversation_id") val conversationId: String? = null
)

@JsonClass(generateAdapter = true)
data class VoiceHabitContextDto(
    val id: String,
    val name: String,
    val category: String = ""
)

@JsonClass(generateAdapter = true)
data class VoiceEventContextDto(
    @Json(name = "habit_id") val habitId: String,
    @Json(name = "habit_name") val habitName: String,
    val status: String,
    val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class VoiceAchievementContextDto(
    val id: String,
    val title: String,
    val description: String = "",
    val requirement: String = "",
    val unlocked: Boolean = false,
    val xp: Int = 0
)

@JsonClass(generateAdapter = true)
data class VoiceEventDto(
    @Json(name = "habit_id") val habitId: String? = null,
    @Json(name = "habit_name") val habitName: String,
    val status: String,
    val quantity: Double? = null,
    val unit: String? = null
)

@JsonClass(generateAdapter = true)
data class VoiceCommandResponse(
    val intent: String = "consulta",
    val response: String = "",
    @Json(name = "habit_id") val habitId: String? = null,
    @Json(name = "habit_name") val habitName: String? = null,
    val status: String? = null,
    val question: String? = null,
    @Json(name = "quick_replies") val quickReplies: List<String> = emptyList(),
    val events: List<VoiceEventDto> = emptyList(),
    val plan: VoicePlanDto? = null,
    @Json(name = "conversation_id") val conversationId: String? = null
)

@JsonClass(generateAdapter = true)
data class VoicePlanDto(
    val title: String,
    val summary: String,
    val category: String,
    val actions: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class VoiceTranscriptionResponse(
    val transcript: String,
    val language: String = "es"
)
