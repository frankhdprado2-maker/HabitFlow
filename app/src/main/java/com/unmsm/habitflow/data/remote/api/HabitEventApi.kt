package com.unmsm.habitflow.data.remote.api

import com.unmsm.habitflow.data.remote.dto.GeoEventDto
import com.unmsm.habitflow.data.remote.dto.GeoEventRequest
import com.unmsm.habitflow.data.remote.dto.GraphQlRequest
import com.unmsm.habitflow.data.remote.dto.GraphQlResponse
import com.unmsm.habitflow.data.remote.dto.HabitEventSyncDto
import com.unmsm.habitflow.data.remote.dto.HabitSyncDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT

interface HabitEventApi {
    @PUT("habits/{id}")
    suspend fun upsertHabit(@Path("id") id: String, @Body request: HabitSyncDto): HabitSyncDto

    @GET("habits")
    suspend fun listHabits(): List<HabitSyncDto>

    @PUT("habit-events/{id}")
    suspend fun upsertEvent(@Path("id") id: String, @Body request: HabitEventSyncDto): HabitEventSyncDto

    @GET("habit-events")
    suspend fun listEvents(): List<HabitEventSyncDto>

    @POST("geo-events/")
    suspend fun create(@Body request: GeoEventRequest): GeoEventDto

    @GET("geo-events/")
    suspend fun list(): List<GeoEventDto>

    @GET("geo-events/{id}")
    suspend fun detail(@Path("id") id: String): GeoEventDto

    @DELETE("geo-events/{id}")
    suspend fun delete(@Path("id") id: String)

    @POST("graphql")
    suspend fun graphql(@Body request: GraphQlRequest): GraphQlResponse
}
