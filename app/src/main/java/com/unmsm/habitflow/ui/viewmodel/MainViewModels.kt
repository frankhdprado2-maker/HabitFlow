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
import com.unmsm.habitflow.domain.model.VoiceCommandResult
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
import com.unmsm.habitflow.ui.state.VoiceAssistantPhase
import com.unmsm.habitflow.ui.state.VoiceMessageUi
import com.unmsm.habitflow.ui.state.VoiceUiState
import com.unmsm.habitflow.util.AppResult
import com.unmsm.habitflow.voice.LocalVoiceCommandParser
import com.unmsm.habitflow.voice.VoiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Locale
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
    private val _lastEventId = MutableStateFlow<String?>(null)
    private val _lastActionMessage = MutableStateFlow<String?>(null)
    val state: StateFlow<HomeUiState> = combine(
        habitRepository.observeHabits(),
        habitRepository.observeEvents(),
        _voice,
        _userName,
        _lastActionMessage
    ) { habits, events, voice, userName, lastMessage ->
        val todayStart = startOfToday()
        val completedIds = events
            .asSequence()
            .filter { it.timestamp >= todayStart && it.status == HabitStatus.Completed }
            .map { it.habitId }
            .toSet()
        HomeUiState(
            userName = firstName(userName),
            habits = habits,
            completedHabitIds = completedIds,
            completedToday = completedIds.size.coerceAtMost(habits.size),
            streak = habits.maxOfOrNull { it.streak } ?: 0,
            bestStreak = habits.maxOfOrNull { it.bestStreak } ?: 0,
            totalCompleted = events.count { it.status == HabitStatus.Completed },
            voiceText = voice.first,
            voiceResponse = voice.second,
            loading = false,
            lastActionMessage = lastMessage
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
        viewModelScope.launch {
            when (val result = habitRepository.markHabit(habit, status)) {
                is AppResult.Success -> {
                    _lastEventId.value = result.data.id
                    _lastActionMessage.value = if (status == HabitStatus.Completed) {
                        "Registré ${habit.name}. Puedes deshacerlo si fue un toque accidental."
                    } else {
                        "Salté ${habit.name} por hoy."
                    }
                }
                is AppResult.Error -> _lastActionMessage.value = result.message
            }
        }
    }

    fun undoLastAction() {
        val eventId = _lastEventId.value ?: return
        viewModelScope.launch {
            when (val result = habitRepository.undoEvent(eventId)) {
                is AppResult.Success -> {
                    _lastEventId.value = null
                    _lastActionMessage.value = "Acción deshecha."
                }
                is AppResult.Error -> _lastActionMessage.value = result.message
            }
        }
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
    val state: StateFlow<StatsUiState> = combine(
        habitRepository.observeHabits(),
        habitRepository.observeEvents()
    ) { habits, events ->
            val completedEvents = events.filter { it.status == HabitStatus.Completed }
            val weekStart = startOfToday() - 6 * DAY_MS
            val previousWeekStart = weekStart - 7 * DAY_MS
            val weekly = (0..6).map { index ->
                val dayStart = weekStart + index * DAY_MS
                val dayEnd = dayStart + DAY_MS
                completedEvents.count { it.timestamp in dayStart until dayEnd }
            }
            val currentWeek = completedEvents.count { it.timestamp >= weekStart }
            val previousWeek = completedEvents.count { it.timestamp in previousWeekStart until weekStart }
            val monthStart = startOfMonth()
            val monthEvents = completedEvents.count { it.timestamp >= monthStart }
            val possibleMonthSlots = (habits.size.coerceAtLeast(1) * currentDayOfMonth()).coerceAtLeast(1)
            StatsUiState(
                currentStreak = habits.maxOfOrNull { it.streak } ?: 0,
                bestStreak = habits.maxOfOrNull { it.bestStreak } ?: 0,
                monthPercent = ((monthEvents.toFloat() / possibleMonthSlots) * 100).toInt().coerceIn(0, 100),
                weekly = weekly,
                habits = habits,
                totalCompleted = completedEvents.size,
                weeklyComparison = currentWeek - previousWeek,
                hasData = completedEvents.isNotEmpty()
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
                            goal = user.primaryGoal.ifBlank { user.goal },
                            avatarKey = user.avatarKey ?: "avatar_lavender",
                            categories = user.preferredCategories.ifEmpty { user.categories }.ifEmpty { listOf("Estudio", "Salud") }
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
    fun toggleVoiceResponse(value: Boolean) = viewModelScope.launch { settingsRepository.setVoiceResponseEnabled(value) }
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
    settingsRepository: SettingsRepository,
    private val voiceController: VoiceController
) : ViewModel() {
    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state
    private val voiceResponseEnabled = settingsRepository.settings
        .map { it.voiceResponseEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    private var greeted = false
    private var pendingResult: VoiceCommandResult? = null

    fun startConversation() {
        if (greeted || state.value.messages.isNotEmpty()) return
        greeted = true
        val greeting = "Hola. Cuéntame qué hábito hiciste hoy o cuál quieres crear."
        _state.update {
            it.copy(
                phase = VoiceAssistantPhase.Idle,
                messages = it.messages + VoiceMessageUi("assistant", greeting),
                quickReplies = listOf("Ya corrí treinta minutos", "Quiero leer veinte páginas", "Qué me falta hoy")
            )
        }
        speak(greeting)
    }

    fun showError(message: String) = _state.update {
        it.copy(
            phase = VoiceAssistantPhase.Error,
            listening = false,
            recording = false,
            transcribing = false,
            response = "",
            error = message
        )
    }

    fun toggleRecording() {
        when (state.value.phase) {
            VoiceAssistantPhase.Listening,
            VoiceAssistantPhase.PartialResult -> stopListeningAndUsePartial()
            VoiceAssistantPhase.Processing,
            VoiceAssistantPhase.Speaking -> Unit
            else -> startListening()
        }
    }

    fun cancelListening() {
        voiceController.cancelListening()
        _state.update {
            it.copy(
                phase = VoiceAssistantPhase.Idle,
                listening = false,
                recording = false,
                partialTranscript = "",
                response = "",
                error = null
            )
        }
    }

    private fun startListening() {
        if (!voiceController.isSpeechRecognitionAvailable()) {
            showError("El reconocimiento de voz no está disponible. Puedes escribir o registrar manualmente.")
            return
        }
        val result = voiceController.startListening(
            onPartial = { partial ->
                _state.update {
                    it.copy(
                        phase = VoiceAssistantPhase.PartialResult,
                        partialTranscript = partial,
                        transcript = partial,
                        listening = true,
                        recording = true,
                        error = null
                    )
                }
            },
            onFinal = { finalText ->
                _state.update { it.copy(partialTranscript = "", listening = false, recording = false) }
                sendText(finalText)
            },
            onError = { message ->
                _state.update {
                    it.copy(
                        phase = VoiceAssistantPhase.Error,
                        listening = false,
                        recording = false,
                        partialTranscript = "",
                        response = "",
                        messages = it.messages + VoiceMessageUi("assistant", message),
                        error = message
                    )
                }
            }
        )
        result.fold(
            onSuccess = {
                _state.update {
                    it.copy(
                        phase = VoiceAssistantPhase.Listening,
                        listening = true,
                        recording = true,
                        transcribing = false,
                        partialTranscript = "",
                        response = "Escuchando...",
                        error = null
                    )
                }
            },
            onFailure = { error ->
                _state.update {
                    it.copy(
                        phase = VoiceAssistantPhase.Error,
                        listening = false,
                        recording = false,
                        transcribing = false,
                        error = error.message ?: "No pude iniciar el micrófono."
                    )
                }
            }
        )
    }

    private fun stopListeningAndUsePartial() {
        val partial = state.value.partialTranscript.ifBlank { state.value.transcript }.trim()
        voiceController.stopListening()
        _state.update { it.copy(listening = false, recording = false, partialTranscript = "") }
        if (partial.isBlank()) {
            showError("No escuché una frase completa. Intenta nuevamente o escríbela.")
        } else {
            sendText(partial)
        }
    }

    fun sendText(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        if (pendingResult != null) {
            when {
                LocalVoiceCommandParser.isAffirmative(clean) -> {
                    confirmPendingAction()
                    return
                }
                LocalVoiceCommandParser.isNegativeOrCorrection(clean) -> {
                    cancelPendingAction("De acuerdo. Dime la corrección o registra el hábito manualmente.")
                    return
                }
            }
        }
        _state.update {
            it.copy(
                phase = VoiceAssistantPhase.Processing,
                listening = false,
                recording = false,
                transcribing = false,
                transcript = clean,
                partialTranscript = "",
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
                    handleVoiceResult(result.data, localMode = false)
                }
                is AppResult.Error -> {
                    val local = LocalVoiceCommandParser.parse(clean, habits)
                    handleVoiceResult(local, localMode = true)
                }
            }
        }
    }

    fun confirmPendingAction() {
        val result = pendingResult ?: return
        pendingResult = null
        viewModelScope.launch {
            result.plan?.let { habitRepository.savePlanRecommendation(it) }
            habitRepository.applyVoiceCommand(result)
            val message = "Listo. Lo agregué a tu rutina."
            speak(message)
            _state.update {
                it.copy(
                    phase = VoiceAssistantPhase.Completed,
                    response = message,
                    messages = it.messages + VoiceMessageUi("assistant", message),
                    quickReplies = listOf("Qué me falta hoy", "Registrar otro", "Ver progreso"),
                    pendingSummary = null,
                    error = null
                )
            }
        }
    }

    fun cancelPendingAction(message: String = "Listo, cancelé la acción.") {
        pendingResult = null
        speak(message)
        _state.update {
            it.copy(
                phase = VoiceAssistantPhase.Idle,
                response = message,
                messages = it.messages + VoiceMessageUi("assistant", message),
                quickReplies = listOf("Intentar de nuevo", "Registrar manualmente"),
                pendingSummary = null,
                error = null
            )
        }
    }

    private suspend fun handleVoiceResult(result: VoiceCommandResult, localMode: Boolean) {
        if (result.events.isNotEmpty()) {
            val summary = confirmationSummary(result)
            pendingResult = result.copy(response = summary)
            speak(summary)
            _state.update {
                it.copy(
                    phase = VoiceAssistantPhase.AwaitingConfirmation,
                    response = summary,
                    messages = it.messages + VoiceMessageUi("assistant", summary),
                    quickReplies = listOf("Confirmar", "Corregir", "Cancelar"),
                    conversationId = result.conversationId ?: it.conversationId,
                    pendingSummary = summary,
                    error = null
                )
            }
            return
        }

        result.plan?.let { habitRepository.savePlanRecommendation(it) }
        val message = if (localMode && result.intent != "aclaracion") {
            "Modo local: ${result.response}"
        } else {
            result.response
        }
        speak(message)
        _state.update {
            it.copy(
                phase = VoiceAssistantPhase.Completed,
                response = message,
                messages = it.messages + VoiceMessageUi("assistant", message),
                quickReplies = result.quickReplies,
                conversationId = result.conversationId ?: it.conversationId,
                pendingSummary = null,
                error = null
            )
        }
    }

    private fun confirmationSummary(result: VoiceCommandResult): String {
        val event = result.events.firstOrNull()
        val name = event?.habitName ?: result.habitName ?: "este hábito"
        val action = when (event?.status ?: result.status) {
            HabitStatus.Skipped -> "saltaré"
            HabitStatus.Failed -> "registraré como pendiente"
            HabitStatus.Pending -> "crearé"
            else -> "registraré como completado"
        }
        val quantity = event?.let {
            if (it.quantity != null && !it.unit.isNullOrBlank()) " (${formatQuantity(it.quantity)} ${it.unit})" else ""
        }.orEmpty()
        return "Antes de hacerlo: $action $name$quantity. ¿Lo confirmas?"
    }

    private fun speak(message: String) {
        if (voiceResponseEnabled.value) {
            voiceController.speak(message)
        }
    }

    override fun onCleared() {
        voiceController.shutdown()
        super.onCleared()
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

private const val DAY_MS = 86_400_000L

private fun startOfToday(): Long =
    Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun startOfMonth(): Long =
    Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun currentDayOfMonth(): Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

private fun firstName(value: String): String {
    val clean = value.trim().replace(Regex("\\s+"), " ")
    if (clean.isBlank()) return "Estudiante"
    val lowerParticles = setOf("de", "del", "la", "las", "los")
    val parts = clean.split(" ")
    return when {
        parts.size >= 2 && parts[0].lowercase(Locale("es", "PE")) in lowerParticles -> parts.take(2).joinToString(" ")
        else -> parts.first()
    }
}

private fun formatQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(Locale.US, value)
