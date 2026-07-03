package com.unmsm.habitflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unmsm.habitflow.data.repository.AuthRepository
import com.unmsm.habitflow.ui.state.LoginUiState
import com.unmsm.habitflow.ui.state.RegisterUiState
import com.unmsm.habitflow.util.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState(loggedIn = authRepository.isLoggedIn()))
    val state: StateFlow<LoginUiState> = _state

    fun updateEmail(value: String) = _state.update { it.copy(email = value, error = null) }
    fun updatePassword(value: String) = _state.update { it.copy(password = value, error = null) }
    fun showError(message: String) = _state.update { it.copy(loading = false, error = message) }

    fun login() {
        val current = state.value
        if (!isEmailValid(current.email) || current.password.isBlank()) {
            showError("Ingresa un email valido y tu contrasena.")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (authRepository.login(state.value.email, state.value.password)) {
                is AppResult.Success -> _state.update { it.copy(loading = false, loggedIn = true) }
                is AppResult.Error -> _state.update { it.copy(loading = false, error = "Credenciales invalidas o servidor no disponible.") }
            }
        }
    }

    fun googleLogin(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = authRepository.googleLogin(idToken)) {
                is AppResult.Success -> _state.update { it.copy(loading = false, loggedIn = true, error = null) }
                is AppResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state

    fun updateName(value: String) = _state.update { it.copy(name = value, error = null) }
    fun updateUsername(value: String) = _state.update { it.copy(username = value, error = null) }
    fun updateEmail(value: String) = _state.update { it.copy(email = value, error = null) }
    fun updatePassword(value: String) = _state.update { it.copy(password = value, error = null) }
    fun updateGoal(value: String) = _state.update { it.copy(goal = value, error = null) }

    fun nextStep() {
        val message = stepError(state.value)
        if (message != null) {
            _state.update { it.copy(error = message) }
            return
        }
        _state.update { it.copy(step = (it.step + 1).coerceAtMost(3), error = null) }
    }

    fun previousStep() = _state.update { it.copy(step = (it.step - 1).coerceAtLeast(1), error = null) }

    fun register() {
        val current = state.value
        val message = finalError(current)
        if (message != null) {
            _state.update { it.copy(error = message) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = authRepository.register(current.name, current.email, current.password, current.username, current.goal)) {
                is AppResult.Success -> _state.update { it.copy(loading = false, registered = true) }
                is AppResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    private fun stepError(state: RegisterUiState): String? =
        when (state.step) {
            1 -> if (state.name.trim().length < 2) "Ingresa tu nombre completo." else null
            2 -> accountError(state)
            else -> null
        }

    private fun finalError(state: RegisterUiState): String? =
        when {
            state.name.trim().length < 2 -> "Ingresa tu nombre completo."
            accountError(state) != null -> accountError(state)
            else -> null
        }

    private fun accountError(state: RegisterUiState): String? =
        when {
            !isEmailValid(state.email) -> "Ingresa un email valido, por ejemplo nombre@gmail.com."
            state.password.length < 6 -> "La contrasena debe tener al menos 6 caracteres."
            else -> null
        }
}

private fun isEmailValid(value: String): Boolean =
    value.trim().matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
