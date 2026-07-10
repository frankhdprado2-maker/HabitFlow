package com.unmsm.habitflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unmsm.habitflow.data.repository.AuthRepository
import com.unmsm.habitflow.data.repository.HabitRepository
import com.unmsm.habitflow.data.repository.SettingsRepository
import com.unmsm.habitflow.ui.state.LoginUiState
import com.unmsm.habitflow.ui.state.ProfileSetupUiState
import com.unmsm.habitflow.ui.state.RegisterUiState
import com.unmsm.habitflow.util.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    suspend fun needsProfileSetup(): Boolean {
        if (!authRepository.isLoggedIn()) return false
        return when (val result = authRepository.me()) {
            is AppResult.Success -> !result.data.profileComplete
            is AppResult.Error -> false
        }
    }
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState(loggedIn = authRepository.isLoggedIn()))
    val state: StateFlow<LoginUiState> = _state

    fun updateEmail(value: String) = _state.update { it.copy(email = value, error = null, needsProfile = false) }
    fun updatePassword(value: String) = _state.update { it.copy(password = value, error = null, needsProfile = false) }
    fun showError(message: String) = _state.update { it.copy(loading = false, error = message, needsProfile = false) }
    fun beginExternalLogin() = _state.update { it.copy(loading = true, error = null, needsProfile = false) }

    fun login() {
        val current = state.value
        if (!isEmailValid(current.email) || current.password.isBlank()) {
            showError("Ingresa un email valido y tu contrasena.")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, needsProfile = false) }
            when (authRepository.login(state.value.email, state.value.password)) {
                is AppResult.Success -> finishLogin()
                is AppResult.Error -> _state.update { it.copy(loading = false, error = "Credenciales invalidas o servidor no disponible.") }
            }
        }
    }

    fun googleLogin(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, needsProfile = false) }
            when (val result = authRepository.googleLogin(idToken)) {
                is AppResult.Success -> finishLogin()
                is AppResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    private suspend fun finishLogin() {
        when (val profile = authRepository.me()) {
            is AppResult.Success -> {
                _state.update {
                    it.copy(
                        loading = false,
                        loggedIn = profile.data.profileComplete,
                        needsProfile = !profile.data.profileComplete,
                        error = null
                    )
                }
            }
            is AppResult.Error -> _state.update { it.copy(loading = false, loggedIn = true, error = null) }
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

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileSetupUiState())
    val state: StateFlow<ProfileSetupUiState> = _state

    init {
        viewModelScope.launch {
            when (val result = authRepository.me()) {
                is AppResult.Success -> {
                    val user = result.data
                    _state.update {
                        it.copy(
                            name = user.name,
                            username = user.username,
                            goal = user.primaryGoal.ifBlank { user.goal },
                            bio = user.bio,
                            avatarKey = user.avatarKey ?: "avatar_lavender",
                            categories = user.preferredCategories.ifEmpty { user.categories }.ifEmpty { listOf("Estudio", "Salud") },
                            darkMode = user.themeMode == "dark",
                            accentColor = user.accentTheme,
                            voiceResponseEnabled = user.voiceResponseEnabled
                        )
                    }
                }
                is AppResult.Error -> Unit
            }
        }
    }

    fun updateName(value: String) = _state.update { it.copy(name = value, error = null) }
    fun updateUsername(value: String) = _state.update { it.copy(username = value, error = null) }
    fun updateGoal(value: String) = _state.update { it.copy(goal = value, error = null) }
    fun updateBio(value: String) = _state.update { it.copy(bio = value, error = null) }
    fun updateAvatar(value: String) = _state.update { it.copy(avatarKey = value, error = null) }
    fun setAccentColor(value: String) = _state.update { it.copy(accentColor = value, error = null) }
    fun toggleDarkMode(value: Boolean) = _state.update { it.copy(darkMode = value, error = null) }
    fun toggleReminders(value: Boolean) = _state.update { it.copy(remindersEnabled = value, error = null) }
    fun toggleVoiceResponse(value: Boolean) = _state.update { it.copy(voiceResponseEnabled = value, error = null) }
    fun toggleCategory(value: String) = _state.update { state ->
        val categories = if (value in state.categories) state.categories - value else state.categories + value
        state.copy(categories = categories, error = null)
    }
    fun toggleTemplate(value: String) = _state.update { state ->
        val selected = if (value in state.selectedTemplates) state.selectedTemplates - value else (state.selectedTemplates + value).take(3)
        state.copy(selectedTemplates = selected, error = null)
    }

    fun nextStep() {
        val message = onboardingStepError(state.value)
        if (message != null) {
            _state.update { it.copy(error = message) }
            return
        }
        _state.update { it.copy(step = (it.step + 1).coerceAtMost(it.maxSteps), error = null) }
    }

    fun previousStep() = _state.update { it.copy(step = (it.step - 1).coerceAtLeast(1), error = null) }

    fun save() {
        val current = state.value
        if (current.name.trim().length < 2) {
            _state.update { it.copy(error = "Escribe tu nombre para guardar tu perfil.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            settingsRepository.setDarkMode(current.darkMode)
            settingsRepository.setAccentColor(current.accentColor)
            settingsRepository.setNotifications(current.remindersEnabled)
            settingsRepository.setVoiceResponseEnabled(current.voiceResponseEnabled)
            when (
                val result = authRepository.updateProfile(
                    name = current.name,
                    username = current.username,
                    goal = current.goal,
                    avatarKey = current.avatarKey,
                    categories = current.categories,
                    bio = current.bio,
                    themeMode = if (current.darkMode) "dark" else "light",
                    accentTheme = current.accentColor,
                    voiceResponseEnabled = current.voiceResponseEnabled,
                    onboardingCompleted = true
                )
            ) {
                is AppResult.Success -> {
                    current.selectedTemplates.forEach { template ->
                        val habit = starterHabit(template)
                        habitRepository.createHabit(
                            name = habit.name,
                            icon = habit.icon,
                            frequency = habit.frequency,
                            time = habit.time,
                            category = habit.category
                        )
                    }
                    settingsRepository.setOnboardingCompleted(true)
                    _state.update { it.copy(loading = false, saved = true) }
                }
                is AppResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}

private fun onboardingStepError(state: ProfileSetupUiState): String? =
    when (state.step) {
        2 -> when {
            state.name.trim().length < 2 -> "Escribe tu nombre visible."
            state.username.trim().length < 3 -> "El username debe tener al menos 3 caracteres."
            else -> null
        }
        3 -> if (state.categories.isEmpty()) "Elige al menos una categoría." else null
        4 -> if (state.selectedTemplates.isEmpty()) "Elige entre uno y tres hábitos iniciales." else null
        else -> null
    }

private data class StarterHabit(
    val name: String,
    val icon: String,
    val frequency: String,
    val time: String,
    val category: String
)

private fun starterHabit(template: String): StarterHabit =
    when (template) {
        "Leer 20 minutos" -> StarterHabit(template, "LE", "Diario", "20:30", "Lectura")
        "Beber agua" -> StarterHabit(template, "AG", "Diario", "10:00", "Salud")
        "Dormir temprano" -> StarterHabit(template, "ZZ", "Diario", "22:30", "Sueño")
        "Caminar o correr" -> StarterHabit(template, "GO", "Lun-Mié-Vie", "18:30", "Ejercicio")
        "Preparar tareas del día" -> StarterHabit(template, "TA", "Diario", "07:30", "Productividad")
        "Meditar" -> StarterHabit(template, "ME", "Diario", "21:30", "Bienestar")
        else -> StarterHabit("Estudiar 25 minutos", "ES", "Lun-Vie", "08:00", "Estudio")
    }

private fun isEmailValid(value: String): Boolean =
    value.trim().matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
