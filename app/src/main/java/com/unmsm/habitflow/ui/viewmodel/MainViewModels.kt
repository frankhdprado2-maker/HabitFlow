package com.unmsm.habitflow.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unmsm.habitflow.data.repository.AuthRepository
import com.unmsm.habitflow.data.repository.HabitRepository
import com.unmsm.habitflow.data.repository.SettingsRepository
import com.unmsm.habitflow.data.repository.VoiceRepository
import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitStatus
import com.unmsm.habitflow.ui.state.AchievementsUiState
import com.unmsm.habitflow.ui.state.HabitDetailUiState
import com.unmsm.habitflow.ui.state.HistoryUiState
import com.unmsm.habitflow.ui.state.HomeUiState
import com.unmsm.habitflow.ui.state.ManualHabitUiState
import com.unmsm.habitflow.ui.state.NotificationsUiState
import com.unmsm.habitflow.ui.state.ProfileUiState
import com.unmsm.habitflow.ui.state.SettingsUiState
import com.unmsm.habitflow.ui.state.StatsUiState
import com.unmsm.habitflow.ui.state.VoiceUiState
import com.unmsm.habitflow.util.AppResult
import com.unmsm.habitflow.voice.VoiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val habitRepository: HabitRepository,
    private val voiceRepository: VoiceRepository,
    private val voiceController: VoiceController
) : ViewModel() {
    private val _voice = MutableStateFlow("" to "")
    private val _userName = MutableStateFlow("Estudiante")
    val state: StateFlow<HomeUiState> = combine(habitRepository.observeHabits(), _voice, _userName) { habits, voice, userName ->
        HomeUiState(
            userName = userName,
            habits = habits,
            completedToday = habits.count { it.streak > 0 }.coerceAtMost(habits.size),
            streak = habits.maxOfOrNull { it.streak } ?: 0,
            voiceText = voice.first,
            voiceResponse = voice.second,
            loading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            when (val result = authRepository.me()) {
                is AppResult.Success -> _userName.value = result.data.name.ifBlank { result.data.username }
                is AppResult.Error -> Unit
            }
        }
    }

    fun mark(habit: Habit, status: HabitStatus = HabitStatus.Completed) {
        viewModelScope.launch { habitRepository.markHabit(habit, status) }
    }

    fun listen() {
        voiceController.start(
            onResult = { text ->
                _voice.value = text to "Procesando..."
                viewModelScope.launch {
                    when (val result = voiceRepository.command(text)) {
                        is AppResult.Success -> {
                            habitRepository.applyVoiceCommand(result.data)
                            _voice.value = text to result.data.response
                            voiceController.speak(result.data.response)
                        }
                        is AppResult.Error -> _voice.value = text to result.message
                    }
                }
            },
            onError = { error -> _voice.value = "" to error }
        )
    }
}

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository
) : ViewModel() {
    private val habitId = savedStateHandle.get<String>("habitId") ?: "study"
    private val _note = MutableStateFlow("")
    val state: StateFlow<HabitDetailUiState> = combine(
        habitRepository.observeHabit(habitId),
        habitRepository.observeEventsForHabit(habitId),
        _note
    ) { habit, events, note ->
        HabitDetailUiState(habit = habit, events = events, note = note)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitDetailUiState())

    fun updateNote(value: String) = _note.update { value }

    fun markToday() {
        val habit = state.value.habit ?: return
        viewModelScope.launch { habitRepository.markHabit(habit, HabitStatus.Completed, state.value.note) }
    }

    fun addNote() = markToday()
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    habitRepository: HabitRepository
) : ViewModel() {
    val state: StateFlow<StatsUiState> = habitRepository.observeHabits()
        .map { habits ->
            StatsUiState(
                currentStreak = habits.maxOfOrNull { it.streak } ?: 0,
                monthPercent = 0,
                habits = habits
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    habitRepository: HabitRepository
) : ViewModel() {
    private val filter = MutableStateFlow("Todos")
    val state: StateFlow<HistoryUiState> = combine(habitRepository.observeEvents(), filter) { events, selected ->
        val filtered = when (selected) {
            "Completados" -> events.filter { it.status == HabitStatus.Completed }
            "Saltados" -> events.filter { it.status == HabitStatus.Skipped }
            else -> events
        }
        HistoryUiState(selected, filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setFilter(value: String) = filter.update { value }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    authRepository: AuthRepository,
    habitRepository: HabitRepository
) : ViewModel() {
    private val user = MutableStateFlow(ProfileUiState().user)
    val state: StateFlow<ProfileUiState> = combine(user, habitRepository.observeAchievements()) { currentUser, achievements ->
        ProfileUiState(user = currentUser, achievements = achievements)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    init {
        viewModelScope.launch {
            when (val result = authRepository.me()) {
                is AppResult.Success -> user.value = result.data
                is AppResult.Error -> Unit
            }
        }
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val loggingOut = MutableStateFlow(false)
    val state: StateFlow<SettingsUiState> = combine(settingsRepository.settings, loggingOut) { settings, isLoggingOut ->
        SettingsUiState(settings, isLoggingOut)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun toggleDarkMode(value: Boolean) = viewModelScope.launch { settingsRepository.setDarkMode(value) }
    fun toggleNotifications(value: Boolean) = viewModelScope.launch { settingsRepository.setNotifications(value) }
    fun toggleBiometric(value: Boolean) = viewModelScope.launch { settingsRepository.setBiometric(value) }
    fun togglePublicProfile(value: Boolean) = viewModelScope.launch { settingsRepository.setPublicProfile(value) }
    fun logout(onComplete: () -> Unit) = viewModelScope.launch {
        loggingOut.value = true
        authRepository.logout()
        loggingOut.value = false
        onComplete()
    }
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    habitRepository: HabitRepository
) : ViewModel() {
    val state: StateFlow<NotificationsUiState> = habitRepository.observeNotifications()
        .map { NotificationsUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUiState())
}

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    habitRepository: HabitRepository
) : ViewModel() {
    val state: StateFlow<AchievementsUiState> = habitRepository.observeAchievements()
        .map { AchievementsUiState(achievements = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AchievementsUiState())
}

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceRepository: VoiceRepository,
    private val habitRepository: HabitRepository,
    private val voiceController: VoiceController
) : ViewModel() {
    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state

    fun listen() {
        _state.update { it.copy(listening = true, error = null) }
        voiceController.start(
            onResult = { text ->
                _state.update { it.copy(listening = false, transcript = text, response = "Procesando...") }
                viewModelScope.launch {
                    when (val result = voiceRepository.command(text)) {
                        is AppResult.Success -> {
                            habitRepository.applyVoiceCommand(result.data)
                            _state.update { it.copy(response = result.data.response) }
                            voiceController.speak(result.data.response)
                        }
                        is AppResult.Error -> _state.update { it.copy(response = "", error = result.message) }
                    }
                }
            },
            onError = { error -> _state.update { it.copy(listening = false, error = error) } }
        )
    }
}

@HiltViewModel
class ManualHabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ManualHabitUiState())
    val state: StateFlow<ManualHabitUiState> = _state

    fun updateName(value: String) = _state.update { it.copy(name = value, error = null) }
    fun updateCategory(value: String) = _state.update { it.copy(category = value, error = null) }
    fun updateFrequency(value: String) = _state.update { it.copy(frequency = value, error = null) }
    fun updateReminderTime(value: String) = _state.update { it.copy(reminderTime = value, error = null) }

    fun save() {
        val current = state.value
        if (current.name.trim().length < 2) {
            _state.update { it.copy(error = "Escribe el nombre del habito.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            habitRepository.createHabit(
                name = current.name.trim(),
                icon = current.name.trim().take(2).uppercase(),
                frequency = current.frequency.ifBlank { "Diario" },
                time = current.reminderTime.ifBlank { "Sin hora" },
                category = current.category.ifBlank { "General" }
            )
            _state.update { it.copy(loading = false, saved = true) }
        }
    }
}
