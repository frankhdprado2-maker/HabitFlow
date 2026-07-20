package com.unmsm.habitflow.data.repository

import com.unmsm.habitflow.data.auth.TokenManager
import com.unmsm.habitflow.data.local.HabitFlowDatabase
import com.unmsm.habitflow.data.local.dao.UserProfileDao
import com.unmsm.habitflow.data.remote.api.AuthApi
import com.unmsm.habitflow.data.remote.dto.GoogleLoginRequest
import com.unmsm.habitflow.data.remote.dto.LoginRequest
import com.unmsm.habitflow.data.remote.dto.ProfileUpdateRequest
import com.unmsm.habitflow.data.remote.dto.RegisterRequest
import com.unmsm.habitflow.data.toEntity
import com.unmsm.habitflow.data.toDomain
import com.unmsm.habitflow.domain.model.User
import com.unmsm.habitflow.util.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.EOFException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val database: HabitFlowDatabase,
    private val userProfileDao: UserProfileDao
) {
    suspend fun login(email: String, password: String): AppResult<Unit> =
        runNetwork {
            val cleanEmail = email.trim()
            val tokens = authApi.login(LoginRequest(cleanEmail, password))
            tokenManager.save(tokens.accessToken, tokens.refreshToken)
            clearLocalData()
        }

    suspend fun register(name: String, email: String, password: String, username: String, goal: String): AppResult<Unit> =
        runNetwork {
            val cleanEmail = email.trim()
            val register = authApi.register(
                RegisterRequest(
                    email = cleanEmail,
                    password = password,
                    name = name.trim(),
                    username = username.trim().ifBlank { null },
                    goal = goal.trim().ifBlank { null }
                )
            )
            if (!register.accessToken.isNullOrBlank() && !register.refreshToken.isNullOrBlank()) {
                tokenManager.save(register.accessToken, register.refreshToken)
            } else {
                val tokens = authApi.login(LoginRequest(cleanEmail, password))
                tokenManager.save(tokens.accessToken, tokens.refreshToken)
            }
            clearLocalData()
        }

    suspend fun googleLogin(idToken: String): AppResult<Unit> =
        runNetwork {
            require(idToken.isNotBlank()) { "Google no devolvio token. Revisa el Web Client ID." }
            val tokens = authApi.loginGoogle(GoogleLoginRequest(idToken))
            tokenManager.save(tokens.accessToken, tokens.refreshToken)
            clearLocalData()
        }

    suspend fun warmUp(): AppResult<Unit> =
        runNetwork {
            authApi.health()
            Unit
        }

    suspend fun me(): AppResult<User> =
        try {
            val user = authApi.me().toDomain()
            userProfileDao.upsert(user.toEntity())
            AppResult.Success(user)
        } catch (error: Throwable) {
            val fallback = userProfileDao.observeCurrent().first()
            if (fallback != null) {
                AppResult.Success(fallback.toDomain())
            } else {
                networkError(error)
            }
        }

    suspend fun updateProfile(
        name: String,
        username: String,
        goal: String,
        avatarKey: String? = null,
        categories: List<String> = emptyList(),
        bio: String = "",
        themeMode: String = "system",
        accentTheme: String = "mint",
        voiceResponseEnabled: Boolean = true,
        onboardingCompleted: Boolean = true,
        locale: String = "es-PE"
    ): AppResult<User> =
        runNetwork {
            val cleanGoal = goal.trim()
            val user = authApi.updateMe(
                ProfileUpdateRequest(
                    name = name.trim(),
                    username = username.trim().ifBlank { null },
                    bio = bio.trim().ifBlank { null },
                    goal = cleanGoal.ifBlank { null },
                    primaryGoal = cleanGoal.ifBlank { null },
                    avatarKey = avatarKey,
                    categories = categories,
                    preferredCategories = categories,
                    onboardingCompleted = onboardingCompleted,
                    themeMode = themeMode,
                    accentTheme = accentTheme,
                    voiceResponseEnabled = voiceResponseEnabled,
                    locale = locale
                )
            ).toDomain()
            userProfileDao.upsert(user.toEntity())
            user
        }

    fun isLoggedIn(): Boolean = !tokenManager.accessToken().isNullOrBlank()

    suspend fun logout() {
        tokenManager.clear()
        clearLocalData()
    }

    private suspend fun clearLocalData() {
        withContext(Dispatchers.IO) {
            database.clearAllTables()
        }
    }
}

suspend inline fun <T> runNetwork(crossinline block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (error: Throwable) {
        networkError(error)
    }

fun <T> networkError(error: Throwable): AppResult<T> {
    val message = when (error) {
        is SocketTimeoutException -> "No pudimos conectar con HabitFlow. Intentalo nuevamente."
        is EOFException -> "HabitFlow respondio vacio. Intentalo nuevamente."
        is HttpException -> when (error.code()) {
            400 -> "La solicitud no fue aceptada. Revisa los datos e intentalo nuevamente."
            401 -> "Tu sesion fue rechazada. Inicia sesion nuevamente."
            403 -> "No tienes permiso para completar esta accion."
            404 -> "No encontramos el servicio de HabitFlow. Revisa la URL configurada."
            422 -> "Email o datos invalidos. Revisa el correo y la contrasena."
            502 -> "El servicio inteligente no pudo procesar la solicitud. Intentalo nuevamente."
            503 -> "HabitFlow esta iniciando. Intentalo nuevamente en unos segundos."
            in 500..599 -> "No pudimos conectar con HabitFlow. Intentalo nuevamente."
            else -> "No pudimos completar la solicitud. Intentalo nuevamente."
        }
        is IOException -> "No pudimos conectar con HabitFlow. Intentalo nuevamente."
        is IllegalArgumentException -> error.message ?: "No se pudo completar la operacion."
        else -> "No se pudo completar la operacion."
    }
    return AppResult.Error(message, error)
}
