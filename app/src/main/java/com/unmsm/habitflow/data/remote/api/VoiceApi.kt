package com.unmsm.habitflow.data.remote.api

import com.unmsm.habitflow.data.remote.dto.VoiceCommandRequest
import com.unmsm.habitflow.data.remote.dto.VoiceCommandResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface VoiceApi {
    @POST("ai/voice-command")
    suspend fun command(@Body request: VoiceCommandRequest): VoiceCommandResponse
}
