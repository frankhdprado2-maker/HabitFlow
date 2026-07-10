package com.unmsm.habitflow.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String? = null,
    val username: String? = null,
    val goal: String? = null
)

@JsonClass(generateAdapter = true)
data class RegisterResponse(
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null
)

@JsonClass(generateAdapter = true)
data class GoogleLoginRequest(
    @Json(name = "token") val idToken: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "token_type") val tokenType: String = "bearer"
)

@JsonClass(generateAdapter = true)
data class ProfileUpdateRequest(
    val name: String,
    val username: String? = null,
    val bio: String? = null,
    val goal: String? = null,
    @Json(name = "primary_goal") val primaryGoal: String? = null,
    val timezone: String? = "America/Lima",
    @Json(name = "avatar_key") val avatarKey: String? = null,
    val categories: List<String> = emptyList(),
    @Json(name = "preferred_categories") val preferredCategories: List<String> = emptyList(),
    @Json(name = "onboarding_completed") val onboardingCompleted: Boolean? = null,
    @Json(name = "theme_mode") val themeMode: String? = null,
    @Json(name = "accent_theme") val accentTheme: String? = null,
    @Json(name = "voice_response_enabled") val voiceResponseEnabled: Boolean? = null,
    val locale: String? = "es-PE"
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String = "",
    val name: String = "",
    val username: String? = null,
    val email: String = "",
    val bio: String? = null,
    val goal: String? = null,
    @Json(name = "primary_goal") val primaryGoal: String? = null,
    val timezone: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "avatar_key") val avatarKey: String? = null,
    val categories: List<String> = emptyList(),
    @Json(name = "preferred_categories") val preferredCategories: List<String> = emptyList(),
    @Json(name = "onboarding_completed") val onboardingCompleted: Boolean? = null,
    @Json(name = "theme_mode") val themeMode: String? = null,
    @Json(name = "accent_theme") val accentTheme: String? = null,
    @Json(name = "voice_response_enabled") val voiceResponseEnabled: Boolean? = null,
    val locale: String? = null,
    @Json(name = "profile_complete") val profileComplete: Boolean = false
)
