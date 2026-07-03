package com.unmsm.habitflow.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeoEventRequest(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @Json(name = "event_type") val eventType: String,
    @Json(name = "device_id") val deviceId: String,
    val notes: String,
    val metadata: Map<String, String> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class GeoEventDto(
    val id: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "event_type") val eventType: String = "",
    @Json(name = "device_id") val deviceId: String? = null,
    val notes: String? = null,
    @Json(name = "recorded_at") val recordedAt: String? = null,
    val metadata: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class GraphQlRequest(val query: String)

@JsonClass(generateAdapter = true)
data class GraphQlResponse(val data: Map<String, Any?>? = null)
