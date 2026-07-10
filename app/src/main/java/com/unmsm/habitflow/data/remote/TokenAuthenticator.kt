package com.unmsm.habitflow.data.remote

import com.unmsm.habitflow.data.auth.TokenManager
import com.unmsm.habitflow.data.remote.dto.RefreshTokenRequest
import com.unmsm.habitflow.data.remote.dto.TokenResponse
import com.squareup.moshi.Moshi
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named

class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    @Named("baseUrl") private val baseUrl: String,
    private val moshi: Moshi
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.contains("/auth/")) return null
        val refreshToken = tokenManager.refreshToken() ?: return null
        if (responseCount(response) >= 2) return null

        val requestAdapter = moshi.adapter(RefreshTokenRequest::class.java)
        val responseAdapter = moshi.adapter(TokenResponse::class.java)
        val refreshRequest = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/auth/refresh-token")
            .post(requestAdapter.toJson(RefreshTokenRequest(refreshToken)).toRequestBody(JSON))
            .build()

        return runCatching {
            OkHttpClient().newCall(refreshRequest).execute().use { refreshResponse ->
                if (!refreshResponse.isSuccessful) return null
                val body = refreshResponse.body.string()
                val tokens = responseAdapter.fromJson(body) ?: return null
                tokenManager.save(tokens.accessToken, tokens.refreshToken)
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokens.accessToken}")
                    .build()
            }
        }.getOrNull()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
