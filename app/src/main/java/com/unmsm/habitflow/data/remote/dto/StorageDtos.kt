package com.unmsm.habitflow.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UploadUrlRequest(
    @Json(name = "file_name") val fileName: String,
    @Json(name = "content_type") val contentType: String
)

@JsonClass(generateAdapter = true)
data class UploadUrlResponse(
    @Json(name = "upload_url") val uploadUrl: String,
    @Json(name = "file_id") val fileId: String
)

@JsonClass(generateAdapter = true)
data class ConfirmUploadRequest(@Json(name = "file_id") val fileId: String)

@JsonClass(generateAdapter = true)
data class StorageFileDto(
    val id: String,
    @Json(name = "file_name") val fileName: String,
    val url: String? = null
)
