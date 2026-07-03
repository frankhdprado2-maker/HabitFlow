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

    fun login() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = authRepository.login(state.value.email, state.value.password)) {
                is AppResult.Success -> _state.update { it.copy(loading = false, loggedIn = true) }
                is AppResult.Error -> _state.update { it.copy(loading = false, error = "Credenciales inválidas o servidor no disponible.") }
            }
        }
    }

    fun googleLogin(idToken: String) {
        viewModelScope.launch {
            when (val result = authRepository.googleLogin(idToken)) {
                is AppResult.Success -> _state.update { it.copy(loggedIn = true, error = null) }
                is AppResult.Error -> _state.update { it.copy(error = result.message) }
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

    fun updateName(value: String) = _state.update { it.copy(name = value) }
    fun updateUsername(value: String) = _state.update { it.copy(username = value) }
    fun updateEmail(value: String) = _state.update { it.copy(email = value) }
    fun updatePassword(value: String) = _state.update { it.copy(password = value) }
    fun updateGoal(value: String) = _state.update { it.copy(goal = value) }
    fun nextStep() = _state.update { it.copy(step = (it.step + 1).coerceAtMost(3)) }
    fun previousStep() = _state.update { it.copy(step = (it.step - 1).coerceAtLeast(1)) }

    fun register() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val current = state.value
            when (val result = authRepository.register(current.name, current.email, current.password, current.username, current.goal)) {
                is AppResult.Success -> _state.update { it.copy(loading = false, registered = true) }
                is AppResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
