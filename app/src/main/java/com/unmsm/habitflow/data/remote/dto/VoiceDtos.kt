package com.unmsm.habitflow.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VoiceCommandRequest(
    val text: String,
    val locale: String = "es-PE"
)

@JsonClass(generateAdapter = true)
data class VoiceCommandResponse(
    val intent: String = "consulta",
    val response: String = "",
    @Json(name = "habit_id") val habitId: String? = null,
    @Json(name = "habit_name") val habitName: String? = null,
    val status: String? = null
)
