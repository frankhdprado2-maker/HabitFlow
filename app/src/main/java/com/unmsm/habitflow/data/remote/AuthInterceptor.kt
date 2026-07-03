package com.unmsm.habitflow.data.remote

import com.unmsm.habitflow.data.auth.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.accessToken()
        val original = chain.request()
        val request = if (token.isNullOrBlank() || original.url.encodedPath.contains("auth/login") || original.url.encodedPath.contains("auth/register")) {
            original
        } else {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
