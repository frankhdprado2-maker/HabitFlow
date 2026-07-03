package com.unmsm.habitflow.data.remote.api

import com.unmsm.habitflow.data.remote.dto.GeoEventDto
import com.unmsm.habitflow.data.remote.dto.GeoEventRequest
import com.unmsm.habitflow.data.remote.dto.GraphQlRequest
import com.unmsm.habitflow.data.remote.dto.GraphQlResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface HabitEventApi {
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
