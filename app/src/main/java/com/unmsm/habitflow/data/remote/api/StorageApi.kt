package com.unmsm.habitflow.data.remote.api

import com.unmsm.habitflow.data.remote.dto.ConfirmUploadRequest
import com.unmsm.habitflow.data.remote.dto.StorageFileDto
import com.unmsm.habitflow.data.remote.dto.UploadUrlRequest
import com.unmsm.habitflow.data.remote.dto.UploadUrlResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface StorageApi {
    @POST("storage/upload-url")
    suspend fun uploadUrl(@Body request: UploadUrlRequest): UploadUrlResponse

    @POST("storage/confirm")
    suspend fun confirm(@Body request: ConfirmUploadRequest): StorageFileDto

    @GET("storage/files")
    suspend fun files(): List<StorageFileDto>

    @DELETE("storage/file/{id}")
    suspend fun delete(@Path("id") id: String)
}
