package com.unmsm.habitflow.data.remote.api

import com.unmsm.habitflow.data.remote.dto.VoiceCommandRequest
import com.unmsm.habitflow.data.remote.dto.VoiceCommandResponse
import com.unmsm.habitflow.data.remote.dto.VoiceConversationRequest
import com.unmsm.habitflow.data.remote.dto.VoiceConversationResponse
import com.unmsm.habitflow.data.remote.dto.VoiceTranscriptionResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST

interface VoiceApi {
    @POST("ai/voice-command")
    suspend fun command(@Body request: VoiceCommandRequest): VoiceCommandResponse

    @POST("ai/conversation")
    suspend fun conversation(@Body request: VoiceConversationRequest): VoiceConversationResponse

    @Multipart
    @POST("ai/transcribe")
    suspend fun transcribe(
        @Part audio: MultipartBody.Part,
        @Part("language") language: RequestBody
    ): VoiceTranscriptionResponse
}
