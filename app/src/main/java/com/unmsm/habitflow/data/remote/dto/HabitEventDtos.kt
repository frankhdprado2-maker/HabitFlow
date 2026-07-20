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

@JsonClass(generateAdapter = true)
data class HabitSyncDto(
    val id: String,
    val name: String,
    val icon: String,
    val frequency: String,
    @Json(name = "reminder_time") val reminderTime: String,
    val category: String,
    @Json(name = "is_active") val isActive: Boolean,
    @Json(name = "frequency_type") val frequencyType: String,
    @Json(name = "weekdays_csv") val weekdaysCsv: String,
    @Json(name = "times_per_week") val timesPerWeek: Int? = null,
    @Json(name = "interval_days") val intervalDays: Int? = null,
    @Json(name = "monthly_days_csv") val monthlyDaysCsv: String,
    @Json(name = "schedule_start_date") val scheduleStartDate: String? = null,
    @Json(name = "schedule_end_date") val scheduleEndDate: String? = null,
    @Json(name = "schedule_timezone") val scheduleTimezone: String,
    @Json(name = "schedule_active") val scheduleActive: Boolean,
    @Json(name = "frequency_needs_review") val frequencyNeedsReview: Boolean,
    @Json(name = "frequency_original") val frequencyOriginal: String,
    @Json(name = "schedule_effective_from") val scheduleEffectiveFrom: String? = null,
    @Json(name = "measurement_type") val measurementType: String,
    @Json(name = "target_value") val targetValue: Double,
    @Json(name = "measurement_unit") val measurementUnit: String,
    @Json(name = "allow_partial_progress") val allowPartialProgress: Boolean,
    @Json(name = "aggregation_mode") val aggregationMode: String
)

@JsonClass(generateAdapter = true)
data class HabitEventSyncDto(
    val id: String,
    @Json(name = "habit_id") val habitId: String,
    @Json(name = "habit_name") val habitName: String,
    val status: String,
    val timestamp: Long,
    val note: String = "",
    val value: Double? = null,
    @Json(name = "normalized_value") val normalizedValue: Double? = null,
    val unit: String? = null,
    @Json(name = "aggregation_mode") val aggregationMode: String? = null,
    @Json(name = "idempotency_key") val idempotencyKey: String? = null,
    val source: String = "MANUAL"
)
