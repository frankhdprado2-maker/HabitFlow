package com.unmsm.habitflow.util

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : AppResult<Nothing>
}

inline fun <T> runAppCatching(block: () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (error: Throwable) {
        AppResult.Error(error.message ?: "Ocurrió un error inesperado", error)
    }
