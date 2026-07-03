package com.unmsm.habitflow.data.repository

import com.unmsm.habitflow.data.auth.TokenManager
import com.unmsm.habitflow.data.remote.api.AuthApi
import com.unmsm.habitflow.data.remote.dto.GoogleLoginRequest
import com.unmsm.habitflow.data.remote.dto.LoginRequest
import com.unmsm.habitflow.data.remote.dto.RegisterRequest
import com.unmsm.habitflow.data.toDomain
import com.unmsm.habitflow.domain.model.User
import com.unmsm.habitflow.util.AppResult
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) {
    suspend fun login(email: String, password: String): AppResult<Unit> =
        runNetwork {
            val cleanEmail = email.trim()
            val tokens = authApi.login(LoginRequest(cleanEmail, password))
            tokenManager.save(tokens.accessToken, tokens.refreshToken)
        }

    suspend fun register(name: String, email: String, password: String, username: String, goal: String): AppResult<Unit> =
        runNetwork {
            val cleanEmail = email.trim()
            authApi.register(RegisterRequest(cleanEmail, password))
            val tokens = authApi.login(LoginRequest(cleanEmail, password))
            tokenManager.save(tokens.accessToken, tokens.refreshToken)
        }

    suspend fun googleLogin(idToken: String): AppResult<Unit> =
        runNetwork {
            val tokens = authApi.loginGoogle(GoogleLoginRequest(idToken))
            tokenManager.save(tokens.accessToken, tokens.refreshToken)
        }

    suspend fun me(): AppResult<User> =
        runNetwork { authApi.me().toDomain() }

    fun isLoggedIn(): Boolean = !tokenManager.accessToken().isNullOrBlank()

    fun logout() = tokenManager.clear()
}

suspend inline fun <T> runNetwork(crossinline block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (error: Throwable) {
        val message = when (error) {
            is HttpException -> when (error.code()) {
                400 -> "La cuenta ya existe o la solicitud no es valida."
                401 -> "Credenciales invalidas o token rechazado."
                422 -> "Email o datos invalidos. Revisa el correo y la contrasena."
                500 -> "El servidor fallo. Revisa las variables en Render."
                else -> "Error del servidor HTTP ${error.code()}."
            }
            is IOException -> "No hay conexion con el servidor."
            else -> error.message ?: "No se pudo completar la operacion"
        }
        AppResult.Error(message, error)
    }
