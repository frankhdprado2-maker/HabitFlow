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
import com.unmsm.habitflow.ui.state.EditProfileUiState
import com.unmsm.habitflow.ui.state.HabitDetailUiState
import com.unmsm.habitflow.ui.state.HistoryUiState
import com.unmsm.habitflow.ui.state.HomeUiState
import com.unmsm.habitflow.ui.state.ManualHabitUiState
import com.unmsm.habitflow.ui.state.NotificationsUiState
import com.unmsm.habitflow.ui.state.ProfileUiState
import com.unmsm.habitflow.ui.state.SettingsUiState
import com.unmsm.habitflow.ui.state.StatsUiState
import com.unmsm.habitflow.ui.state.ThemeUiState
import com.unmsm.habitflow.ui.state.VoiceMessageUi
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
class ThemeViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {
    val state: StateFlow<ThemeUiState> = settingsRepository.settings
        .map { ThemeUiState(darkMode = it.darkMode, accentColor = it.accentColor) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeUiState())
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val habitRepository: HabitRepository
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
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state

    init {
        viewModelScope.launch {
            when (val result = authRepository.me()) {
                is AppResult.Success -> {
                    val user = result.data
                    _state.update {
                        it.copy(
                            name = user.name,
                            username = user.username,
                            goal = user.goal,
                            avatarKey = user.avatarKey ?: "avatar_lavender",
                            categories = user.categories.ifEmpty { listOf("Estudio", "Salud") }
                        )
                    }
                }
                is AppResult.Error -> _state.update { it.copy(error = result.message) }
            }
        }
    }

    fun updateName(value: String) = _state.update { it.copy(name = value, error = null) }
    fun updateUsername(value: String) = _state.update { it.copy(username = value, error = null) }
    fun updateGoal(value: String) = _state.update { it.copy(goal = value, error = null) }
    fun updateAvatar(value: String) = _state.update { it.copy(avatarKey = value, error = null) }
    fun toggleCategory(value: String) = _state.update { state ->
        val categories = if (value in state.categories) state.categories - value else state.categories + value
        state.copy(categories = categories, error = null)
    }

    fun save() {
        val current = state.value
        if (current.name.trim().length < 2) {
            _state.update { it.copy(error = "Escribe tu nombre.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = authRepository.updateProfile(current.name, current.username, current.goal, current.avatarKey, current.categories)) {
                is AppResult.Success -> _state.update { it.copy(loading = false, saved = true) }
                is AppResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
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
    fun setAccentColor(value: String) = viewModelScope.launch { settingsRepository.setAccentColor(value) }
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
    val state: StateFlow<AchievementsUiState> = combine(
        habitRepository.observeAchievements(),
        habitRepository.observeEvents(),
        habitRepository.observeCosmeticRewards(),
        habitRepository.observePlanRecommendations()
    ) { achievements, events, cosmetics, plans ->
        val completed = events.count { it.status == HabitStatus.Completed }
        val xp = completed * 10 + achievements.filter { it.unlocked }.sumOf { it.xp }
        AchievementsUiState(
            level = xp / 250,
            xp = xp,
            achievements = achievements,
            cosmetics = cosmetics.map { reward -> reward.copy(unlocked = reward.unlocked || xp >= reward.cost) },
            plans = plans
        )
    }
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
    private var greeted = false

    fun startConversation() {
        if (greeted || state.value.messages.isNotEmpty()) return
        greeted = true
        val greeting = "Hola, como estas? Cuentame por voz que habito hiciste hoy o cual quieres crear."
        _state.update {
            it.copy(messages = it.messages + VoiceMessageUi("assistant", greeting))
        }
        voiceController.speak(greeting)
    }

    fun showError(message: String) = _state.update {
        it.copy(listening = false, recording = false, transcribing = false, error = message)
    }

    fun toggleRecording() {
        if (state.value.recording) {
            stopRecordingAndSend()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        val result = voiceController.startRecording()
        result.fold(
            onSuccess = {
                _state.update {
                    it.copy(
                        listening = true,
                        recording = true,
                        transcribing = false,
                        response = "",
                        error = null
                    )
                }
            },
            onFailure = { error ->
                _state.update {
                    it.copy(
                        listening = false,
                        recording = false,
                        transcribing = false,
                        error = error.message ?: "No pude iniciar el microfono."
                    )
                }
            }
        )
    }

    private fun stopRecordingAndSend() {
        val result = voiceController.stopRecording()
        result.fold(
            onSuccess = { audioFile ->
                _state.update {
                    it.copy(
                        listening = false,
                        recording = false,
                        transcribing = true,
                        response = "Transcribiendo...",
                        error = null
                    )
                }
                viewModelScope.launch {
                    when (val transcription = voiceRepository.transcribe(audioFile)) {
                        is AppResult.Success -> {
                            audioFile.delete()
                            _state.update { it.copy(transcribing = false) }
                            sendText(transcription.data)
                        }
                        is AppResult.Error -> {
                            audioFile.delete()
                            voiceController.speak(transcription.message)
                            _state.update {
                                it.copy(
                                    transcribing = false,
                                    response = "",
                                    messages = it.messages + VoiceMessageUi("assistant", transcription.message),
                                    error = transcription.message
                                )
                            }
                        }
                    }
                }
            },
            onFailure = { error ->
                _state.update {
                    it.copy(
                        listening = false,
                        recording = false,
                        transcribing = false,
                        response = "",
                        error = error.message ?: "No pude guardar el audio."
                    )
                }
            }
        )
    }

    fun sendText(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        _state.update {
            it.copy(
                listening = false,
                recording = false,
                transcribing = false,
                transcript = clean,
                response = "Procesando...",
                messages = it.messages + VoiceMessageUi("user", clean),
                quickReplies = emptyList(),
                error = null
            )
        }
        viewModelScope.launch {
            val habits = habitRepository.activeHabits()
            val recentEvents = habitRepository.recentEvents()
            val achievements = habitRepository.achievementsSnapshot()
            val categories = habits.map { it.category }.filter { it.isNotBlank() }.distinct()
            val conversationId = state.value.conversationId
            when (val result = voiceRepository.command(clean, habits, recentEvents, achievements, categories, conversationId)) {
                is AppResult.Success -> {
                    habitRepository.applyVoiceCommand(result.data)
                    result.data.plan?.let { habitRepository.savePlanRecommendation(it) }
                    voiceController.speak(result.data.response)
                    _state.update {
                        it.copy(
                            response = result.data.response,
                            messages = it.messages + VoiceMessageUi("assistant", result.data.response),
                            quickReplies = result.data.quickReplies,
                            conversationId = result.data.conversationId ?: it.conversationId,
                            error = null
                        )
                    }
                }
                is AppResult.Error -> {
                    voiceController.speak(result.message)
                    _state.update {
                        it.copy(
                            response = "",
                            messages = it.messages + VoiceMessageUi("assistant", result.message),
                            error = result.message
                        )
                    }
                }
            }
        }
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
