package com.unmsm.habitflow.data.remote.api

import com.unmsm.habitflow.data.remote.dto.VoiceCommandRequest
import com.unmsm.habitflow.data.remote.dto.VoiceCommandResponse
import com.unmsm.habitflow.data.remote.dto.VoiceConversationRequest
import com.unmsm.habitflow.data.remote.dto.VoiceConversationResponse
import com.unmsm.habitflow.data.remote.dto.HabitInterpretationRequest
import com.unmsm.habitflow.data.remote.dto.HabitInterpretationResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface VoiceApi {
    @POST("ai/voice-command")
    suspend fun command(@Body request: VoiceCommandRequest): VoiceCommandResponse

    @POST("ai/conversation")
    suspend fun conversation(@Body request: VoiceConversationRequest): VoiceConversationResponse

    @POST("ai/interpret-habit")
    suspend fun interpretHabit(@Body request: HabitInterpretationRequest): HabitInterpretationResponse
}
