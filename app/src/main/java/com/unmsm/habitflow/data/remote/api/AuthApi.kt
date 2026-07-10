package com.unmsm.habitflow.data.remote.api

import com.unmsm.habitflow.data.remote.dto.GoogleLoginRequest
import com.unmsm.habitflow.data.remote.dto.LoginRequest
import com.unmsm.habitflow.data.remote.dto.ProfileUpdateRequest
import com.unmsm.habitflow.data.remote.dto.RefreshTokenRequest
import com.unmsm.habitflow.data.remote.dto.RegisterResponse
import com.unmsm.habitflow.data.remote.dto.RegisterRequest
import com.unmsm.habitflow.data.remote.dto.TokenResponse
import com.unmsm.habitflow.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("auth/google")
    suspend fun loginGoogle(@Body request: GoogleLoginRequest): TokenResponse

    @POST("auth/refresh-token")
    suspend fun refresh(@Body request: RefreshTokenRequest): TokenResponse

    @GET("auth/me")
    suspend fun me(): UserDto

    @PUT("auth/me")
    suspend fun updateMe(@Body request: ProfileUpdateRequest): UserDto
}
