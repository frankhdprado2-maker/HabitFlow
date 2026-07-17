package com.unmsm.habitflow.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unmsm.habitflow.data.repository.AuthRepository
import com.unmsm.habitflow.data.repository.HabitRepository
import com.unmsm.habitflow.data.repository.SettingsRepository
import com.unmsm.habitflow.data.repository.VoiceRepository
import com.unmsm.habitflow.data.toDomainCommandResult
import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitInterpretationResult
import com.unmsm.habitflow.domain.model.HabitStatus
import com.unmsm.habitflow.domain.model.InterpretedHabit
import com.unmsm.habitflow.domain.model.VoiceCommandResult
import com.unmsm.habitflow.domain.model.VoiceEventResult
import com.unmsm.habitflow.domain.habit.HabitHeatmapBuilder
import com.unmsm.habitflow.domain.habit.HeatmapDayState
import com.unmsm.habitflow.domain.habit.HabitFrequency
import com.unmsm.habitflow.domain.habit.HabitFrequencyType
import com.unmsm.habitflow.domain.habit.AggregationMode
import com.unmsm.habitflow.domain.habit.HabitMeasurement
import com.unmsm.habitflow.domain.habit.MeasurementType
import com.unmsm.habitflow.domain.habit.HabitStatisticsCalculator
import com.unmsm.habitflow.ui.state.AchievementsUiState
import com.unmsm.habitflow.ui.state.CoachUiState
import com.unmsm.habitflow.ui.state.EditProfileUiState
import com.unmsm.habitflow.ui.state.HabitAssociationOptionUi
import com.unmsm.habitflow.ui.state.HabitDetailUiState
import com.unmsm.habitflow.ui.state.HistoryUiState
import com.unmsm.habitflow.ui.state.HomeUiState
import com.unmsm.habitflow.ui.state.InterpretedHabitUi
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
import com.unmsm.habitflow.voice.isCoachRequest
import com.unmsm.habitflow.voice.VoiceErrorType
import com.unmsm.habitflow.voice.VoiceController
import com.unmsm.habitflow.voice.whisper.WhisperErrorType
import com.unmsm.habitflow.voice.whisper.WhisperState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.Normalizer
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.ZoneId
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
        .map {
            ThemeUiState(
                themeMode = it.themeMode,
                accentColor = it.accentColor,
                dynamicColor = it.dynamicColor,
                textScale = it.textScale
            )
        }
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
                is AppResult.Success -> {
                    _userName.value = result.data.name.ifBlank { result.data.username }
                    if (result.data.email.equals(DEMO_ANALYSIS_EMAIL, ignoreCase = true)) {
                        habitRepository.seedDemoAccountData()
                    }
                }
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

private const val DEMO_ANALYSIS_EMAIL = "demo.analisis.2026@habitflow.app"

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository
) : ViewModel() {
    private val habitId = savedStateHandle.get<String>("habitId") ?: "study"
    private val _note = MutableStateFlow("")
    private val _progressValue = MutableStateFlow("")
    private val detailData = combine(
        habitRepository.observeHabit(habitId),
        habitRepository.observeEventsForHabit(habitId),
        habitRepository.observeScheduleVersions(habitId)
    ) { habit, events, schedules -> Triple(habit, events, schedules) }
    val state: StateFlow<HabitDetailUiState> = combine(
        detailData,
        combine(_note, _progressValue) { note, progress -> note to progress },
        habitRepository.observeTimezone()
    ) { data, input, timezone ->
        val (note, progressValue) = input
        val (habit, events, schedules) = data
        val zoneId = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.of("America/Lima"))
        val today = LocalDate.now(zoneId)
        val heatmap = habit?.let {
            HabitHeatmapBuilder.build(it, events, YearMonth.from(today), today, zoneId, schedules)
        } ?: com.unmsm.habitflow.domain.habit.HabitHeatmap()
        val scheduled = heatmap.days.count { it.state !in setOf(HeatmapDayState.NotScheduled, HeatmapDayState.Future) }
        val completed = heatmap.days.count { it.state == HeatmapDayState.Completed }
        HabitDetailUiState(
            habit = habit,
            events = events,
            note = note,
            progressValue = progressValue,
            completionPercent = if (scheduled == 0) 0 else completed * 100 / scheduled,
            heatmap = heatmap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitDetailUiState())

    fun updateNote(value: String) = _note.update { value }
    fun updateProgressValue(value: String) = _progressValue.update { value }

    fun recordProgress() {
        val habit = state.value.habit ?: return
        val value = state.value.progressValue.toDoubleOrNull() ?: return
        viewModelScope.launch {
            habitRepository.recordProgress(habit, value, habit.measurement.unit)
            _progressValue.value = ""
        }
    }

    fun markToday() {
        val habit = state.value.habit ?: return
        viewModelScope.launch { habitRepository.markHabit(habit, HabitStatus.Completed, state.value.note) }
    }

    fun addNote() {
        val habit = state.value.habit ?: return
        val note = state.value.note.trim()
        if (note.isBlank()) return
        viewModelScope.launch {
            habitRepository.addNote(habit, note)
            _note.value = ""
        }
    }
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val voiceRepository: VoiceRepository
) : ViewModel() {
    private val coach = MutableStateFlow(CoachUiState())
    val state: StateFlow<StatsUiState> = combine(
        habitRepository.observeHabits(),
        habitRepository.observeEvents(),
        coach,
        habitRepository.observeTimezone()
    ) { habits, events, coachState, timezone ->
            val zoneId = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.of("America/Lima"))
            val today = LocalDate.now(zoneId)
            val completedEvents = events.filter { it.status == HabitStatus.Completed }
            val weekStartDate = today.minusDays(6)
            val weekly = (0..6).map { index ->
                val date = weekStartDate.plusDays(index.toLong())
                habits.count { habit -> HabitStatisticsCalculator.calculate(habit, events, date, date, zoneId).completedDays == 1 }
            }
            val currentWeek = weekly.sum()
            val previousWeek = habits.sumOf { HabitStatisticsCalculator.calculate(it, events, weekStartDate.minusDays(7), weekStartDate.minusDays(1), zoneId).completedDays }
            val monthStartDate = today.withDayOfMonth(1)
            val monthStats = habits.map { HabitStatisticsCalculator.calculate(it, events, monthStartDate, today, zoneId) }
            val scheduledMonth = monthStats.sumOf { it.scheduledDays }
            val completedMonth = monthStats.sumOf { it.completedDays }
            val completionRates = habits.associate { habit ->
                habit.id to HabitStatisticsCalculator.calculate(habit, events, today.minusDays(29), today, zoneId).completionRate.toFloat()
            }
            StatsUiState(
                currentStreak = habits.maxOfOrNull { it.streak } ?: 0,
                bestStreak = habits.maxOfOrNull { it.bestStreak } ?: 0,
                monthPercent = if (scheduledMonth == 0) 0 else (completedMonth * 100 / scheduledMonth),
                weekly = weekly,
                habits = habits,
                totalCompleted = completedEvents.size,
                weeklyComparison = currentWeek - previousWeek,
                hasData = completedEvents.isNotEmpty(),
                habitCompletionRates = completionRates,
                coach = coachState
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun updateCoachQuestion(value: String) = coach.update { it.copy(question = value, error = null) }

    fun askCoach(question: String = state.value.coach.question) {
        val clean = question.trim()
        if (clean.isBlank() || coach.value.loading) return
        coach.update {
            it.copy(
                question = clean,
                loading = true,
                title = "",
                answer = "",
                evidence = emptyList(),
                error = null
            )
        }
        viewModelScope.launch {
            val habits = habitRepository.activeHabits()
            val events = habitRepository.recentEvents()
            val achievements = habitRepository.achievementsSnapshot()
            when (
                val result = voiceRepository.conversation(
                    text = clean,
                    habits = habits,
                    recentEvents = events,
                    achievements = achievements,
                    categories = habits.map { it.category }.filter { it.isNotBlank() }.distinct()
                )
            ) {
                is AppResult.Success -> {
                    val insight = result.data.toDomainCommandResult()
                    coach.update { current ->
                        current.copy(
                            loading = false,
                            title = insight.plan?.title ?: "Insight de HabitFlow",
                            answer = insight.response,
                            evidence = insight.plan?.actions.orEmpty(),
                            suggestions = insight.quickReplies.ifEmpty { current.suggestions },
                            error = null
                        )
                    }
                }
                is AppResult.Error -> coach.update {
                    it.copy(
                        loading = false,
                        error = "No pude analizar tus datos ahora. ${result.message}"
                    )
                }
            }
        }
    }
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

    fun setThemeMode(value: String) = viewModelScope.launch { settingsRepository.setThemeMode(value) }
    fun toggleDynamicColor(value: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColor(value) }
    fun setTextScale(value: String) = viewModelScope.launch { settingsRepository.setTextScale(value) }
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
    private var latestVoiceOperationId: Long? = null

    init {
        viewModelScope.launch {
            voiceController.state.collect { whisperState ->
                when (whisperState) {
                    WhisperState.ModelNotPrepared -> _state.update { it.copy(phase = VoiceAssistantPhase.ModelNotPrepared) }
                    WhisperState.PreparingModel -> _state.update {
                        it.copy(phase = VoiceAssistantPhase.PreparingModel, response = "Preparando el modelo local de voz…", error = null)
                    }
                    WhisperState.Ready -> _state.update {
                        it.copy(
                            phase = VoiceAssistantPhase.Ready,
                            listening = false,
                            recording = false,
                            transcribing = false,
                            response = "",
                            error = null
                        )
                    }
                    is WhisperState.Recording -> _state.update {
                        latestVoiceOperationId = whisperState.operationId
                        it.copy(
                            phase = VoiceAssistantPhase.Recording(whisperState.durationMillis, whisperState.audioLevel),
                            listening = true,
                            recording = true,
                            transcribing = false,
                            recordingDurationMillis = whisperState.durationMillis,
                            audioLevel = whisperState.audioLevel,
                            response = "Grabando…",
                            error = null
                        )
                    }
                    is WhisperState.Processing -> if (latestVoiceOperationId == whisperState.operationId) _state.update {
                        it.copy(
                            phase = VoiceAssistantPhase.Transcribing,
                            listening = false,
                            recording = false,
                            transcribing = true,
                            response = "Whisper está convirtiendo tu voz en texto…",
                            error = null
                        )
                    }
                    is WhisperState.Result -> if (latestVoiceOperationId == whisperState.operationId) _state.update {
                        latestVoiceOperationId = null
                        it.copy(
                            phase = VoiceAssistantPhase.Ready,
                            transcript = whisperState.text,
                            partialTranscript = "",
                            listening = false,
                            recording = false,
                            transcribing = false,
                            response = "Revisa la transcripción antes de enviarla.",
                            error = null
                        )
                    }
                    is WhisperState.Error -> if (
                        whisperState.operationId == null || latestVoiceOperationId == whisperState.operationId
                    ) {
                        latestVoiceOperationId = null
                        showError(
                            whisperState.error.message,
                            whisperState.error.type.toVoiceErrorType()
                        )
                    }
                    is WhisperState.Cancelled -> if (latestVoiceOperationId == whisperState.operationId) _state.update {
                        latestVoiceOperationId = null
                        it.copy(
                            phase = VoiceAssistantPhase.Ready,
                            listening = false,
                            recording = false,
                            transcribing = false,
                            recordingDurationMillis = 0,
                            audioLevel = 0f,
                            response = "Grabación cancelada. No se envió audio fuera de tu dispositivo.",
                            error = null
                        )
                    }
                }
            }
        }
        viewModelScope.launch { voiceController.prepareModel() }
    }

    fun startConversation() {
        if (greeted || state.value.messages.isNotEmpty()) return
        greeted = true
        val greeting = "Hola. Cuéntame qué hábito hiciste hoy o cuál quieres crear."
        _state.update {
            it.copy(
                phase = VoiceAssistantPhase.Idle,
                messages = it.messages + VoiceMessageUi("assistant", greeting),
                quickReplies = listOf("Resumen semanal", "Plan de hoy", "Qué me falta hoy")
            )
        }
        speak(greeting)
    }

    fun showError(
        message: String,
        type: VoiceErrorType = VoiceErrorType.Unknown,
        permissionPermanentlyDenied: Boolean = false
    ) = _state.update {
        it.copy(
            phase = VoiceAssistantPhase.Error(type, message),
            listening = false,
            recording = false,
            transcribing = false,
            response = "",
            permissionPermanentlyDenied = permissionPermanentlyDenied,
            error = message
        )
    }

    fun toggleRecording() {
        when (state.value.phase) {
            is VoiceAssistantPhase.Recording -> stopRecording()
            VoiceAssistantPhase.PreparingModel,
            VoiceAssistantPhase.Transcribing,
            VoiceAssistantPhase.Processing,
            VoiceAssistantPhase.Speaking -> Unit
            else -> startRecording()
        }
    }

    fun requestMicrophonePermission() {
        _state.update {
            it.copy(
                phase = VoiceAssistantPhase.RequestingPermission,
                permissionPermanentlyDenied = false,
                error = null
            )
        }
    }

    fun cancelListening() {
        viewModelScope.launch { voiceController.cancelListening() }
    }

    private fun startRecording() {
        viewModelScope.launch { voiceController.startRecording() }
    }

    fun stopRecording() {
        viewModelScope.launch { voiceController.stopRecordingAndTranscribe() }
    }

    fun sendText(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        val coachRequest = isCoachRequest(clean)
        val currentState = state.value
        if (currentState.phase == VoiceAssistantPhase.Processing || currentState.savingInterpretation) return
        if (state.value.interpretedHabits.isNotEmpty()) {
            when {
                LocalVoiceCommandParser.isAffirmative(clean) -> {
                    confirmInterpretedHabits()
                    return
                }
                LocalVoiceCommandParser.isNegativeOrCorrection(clean) -> {
                    cancelInterpretedHabits("De acuerdo. No guardé nada; puedes corregir la transcripción.")
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
                response = if (coachRequest) "Analizando tus datos..." else "Interpretando tu hábito...",
                messages = it.messages + VoiceMessageUi("user", clean),
                quickReplies = emptyList(),
                pendingSummary = null,
                interpretationText = "",
                interpretedHabits = emptyList(),
                interpretationConfidence = 0.0,
                savingInterpretation = false,
                coachTitle = "",
                coachHighlights = emptyList(),
                permissionPermanentlyDenied = false,
                error = null
            )
        }
        viewModelScope.launch {
            val habits = habitRepository.activeHabits()
            if (coachRequest) {
                requestCoach(clean, habits)
                return@launch
            }
            when (val result = voiceRepository.interpretHabit(text = clean, timezone = "America/Lima")) {
                is AppResult.Success -> {
                    if (result.data.intent == "query_habit" || result.data.intent == "plan_habit") {
                        requestCoach(clean, habits)
                    } else {
                        handleHabitInterpretation(
                            text = clean,
                            result = result.data,
                            existingHabits = habits
                        )
                    }
                }
                is AppResult.Error -> {
                    val message = if (result.message.contains("conectar", ignoreCase = true)) {
                        "La interpretación inteligente necesita conexión. Conservé tu texto para reintentar."
                    } else {
                        "No se pudo interpretar el hábito. ${result.message}"
                    }
                    showError(message, VoiceErrorType.Network)
                }
            }
        }
    }

    private suspend fun requestCoach(text: String, habits: List<Habit>) {
        val events = habitRepository.recentEvents()
        val achievements = habitRepository.achievementsSnapshot()
        when (
            val result = voiceRepository.conversation(
                text = text,
                habits = habits,
                recentEvents = events,
                achievements = achievements,
                categories = habits.map { it.category }.filter { it.isNotBlank() }.distinct(),
                sessionId = state.value.conversationId
            )
        ) {
            is AppResult.Success -> handleVoiceResult(result.data.toDomainCommandResult(), localMode = false)
            is AppResult.Error -> showError(
                "El coach necesita conexión para analizar tus datos. Conservé tu pregunta para reintentar.",
                VoiceErrorType.Network
            )
        }
    }

    fun updateInterpretedHabit(index: Int, field: String, value: String) {
        _state.update { current ->
            current.copy(
                interpretedHabits = current.interpretedHabits.mapIndexed { itemIndex, item ->
                    if (itemIndex != index) {
                        item
                    } else {
                        when (field) {
                            "name" -> item.copy(name = value)
                            "quantity" -> item.copy(quantity = value)
                            "unit" -> item.copy(unit = value)
                            "date" -> item.copy(date = value)
                            "notes" -> item.copy(notes = value)
                            "action" -> item.copy(action = value)
                            else -> item
                        }
                    }
                },
                error = null
            )
        }
    }

    fun updateInterpretedHabitAssociation(index: Int, habitId: String?) {
        _state.update { current ->
            val selected = current.habitAssociationOptions.firstOrNull { it.id == habitId }
            current.copy(
                interpretedHabits = current.interpretedHabits.mapIndexed { itemIndex, item ->
                    if (itemIndex != index) {
                        item
                    } else {
                        item.copy(
                            name = selected?.name ?: item.name,
                            existingHabitId = selected?.id,
                            existingHabitName = selected?.name
                        )
                    }
                },
                error = null
            )
        }
    }

    fun confirmInterpretedHabits() {
        val current = state.value
        if (current.savingInterpretation || current.interpretedHabits.isEmpty()) return
        val validationError = validateInterpretedHabits(current.interpretedHabits)
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }
        val events = current.interpretedHabits.map { item ->
            VoiceEventResult(
                habitId = item.existingHabitId,
                habitName = item.name.trim(),
                status = item.action.toHabitStatusFromInterpretation(),
                quantity = item.quantity.replace(",", ".").toDoubleOrNull(),
                unit = item.unit.trim().takeIf { it.isNotBlank() },
                date = item.date.trim()
            )
        }
        val command = VoiceCommandResult(
            intent = "registrar_habito",
            response = current.pendingSummary.orEmpty(),
            events = events
        )

        viewModelScope.launch {
            _state.update { it.copy(savingInterpretation = true, error = null) }
            val result = habitRepository.applyVoiceCommand(command)
            when (result) {
                is AppResult.Success -> {
                    val message = registeredMessage(events)
                    speak(message)
                    _state.update {
                        it.copy(
                            phase = VoiceAssistantPhase.Completed,
                            response = message,
                            messages = it.messages + VoiceMessageUi("assistant", message),
                            quickReplies = listOf("Registrar otro", "Ver progreso"),
                            pendingSummary = null,
                            interpretedHabits = emptyList(),
                            habitAssociationOptions = emptyList(),
                            savingInterpretation = false,
                            error = null
                        )
                    }
                }
                is AppResult.Error -> _state.update {
                    it.copy(
                        phase = VoiceAssistantPhase.AwaitingConfirmation,
                        savingInterpretation = false,
                        error = "No se pudieron guardar todos los hábitos: ${result.message}"
                    )
                }
                null -> _state.update {
                    it.copy(
                        phase = VoiceAssistantPhase.AwaitingConfirmation,
                        savingInterpretation = false,
                        error = "No encontré hábitos válidos para registrar."
                    )
                }
            }
        }
    }

    fun cancelInterpretedHabits(message: String = "Listo, cancelé la acción. No guardé ningún hábito.") {
        speak(message)
        _state.update {
            it.copy(
                phase = VoiceAssistantPhase.Idle,
                response = message,
                messages = it.messages + VoiceMessageUi("assistant", message),
                quickReplies = listOf("Intentar de nuevo", "Registrar manualmente"),
                pendingSummary = null,
                interpretedHabits = emptyList(),
                habitAssociationOptions = emptyList(),
                savingInterpretation = false,
                error = null
            )
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

    private fun handleHabitInterpretation(
        text: String,
        result: HabitInterpretationResult,
        existingHabits: List<Habit>
    ) {
        val options = existingHabits
            .sortedBy { it.name.lowercase(Locale("es", "PE")) }
            .map { HabitAssociationOptionUi(id = it.id, name = it.name) }

        if (result.intent == "query_habit") {
            val message = "Detecté una consulta de hábitos. Las consultas inteligentes estarán disponibles posteriormente."
            speak(message)
            _state.update {
                it.copy(
                    phase = VoiceAssistantPhase.Completed,
                    response = message,
                    messages = it.messages + VoiceMessageUi("assistant", message),
                    quickReplies = listOf("Registrar un hábito", "Ver progreso"),
                    interpretationText = text,
                    interpretedHabits = emptyList(),
                    habitAssociationOptions = options,
                    interpretationConfidence = result.confidence,
                    pendingSummary = null,
                    error = null
                )
            }
            return
        }

        if (result.habits.isEmpty()) {
            showError(
                "No pude identificar un hábito para registrar. Puedes corregir la transcripción e intentarlo otra vez.",
                VoiceErrorType.NoMatch
            )
            return
        }

        val interpreted = result.habits.map { habit ->
            habit.toEditableHabit(existingHabits)
        }
        val summary = result.confirmationMessage.ifBlank {
            if (interpreted.size == 1) {
                "Revisa el hábito antes de guardarlo."
            } else {
                "Revisa estos ${interpreted.size} hábitos antes de guardarlos."
            }
        }
        speak(summary)
        _state.update {
            it.copy(
                phase = VoiceAssistantPhase.AwaitingConfirmation,
                response = summary,
                messages = it.messages + VoiceMessageUi("assistant", summary),
                quickReplies = listOf("Confirmar", "Corregir", "Cancelar"),
                pendingSummary = summary,
                interpretationText = text,
                interpretedHabits = interpreted,
                habitAssociationOptions = options,
                interpretationConfidence = result.confidence,
                savingInterpretation = false,
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

        val message = if (localMode && result.intent != "aclaracion") {
            "Estoy usando el modo sin conexion. ${result.response}"
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
                coachTitle = result.plan?.title.orEmpty(),
                coachHighlights = result.plan?.actions.orEmpty(),
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
        voiceController.stopSpeaking()
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
    fun updateFrequencyType(value: String) = _state.update { it.copy(frequencyType = value, error = null) }
    fun toggleWeekday(value: String) = _state.update {
        it.copy(weekdays = if (value in it.weekdays) it.weekdays - value else it.weekdays + value, error = null)
    }
    fun updateTimesPerWeek(value: String) = _state.update { it.copy(timesPerWeek = value, error = null) }
    fun updateIntervalDays(value: String) = _state.update { it.copy(intervalDays = value, error = null) }
    fun updateMonthlyDays(value: String) = _state.update { it.copy(monthlyDays = value, error = null) }
    fun updateStartDate(value: String) = _state.update { it.copy(startDate = value, error = null) }
    fun updateEndDate(value: String) = _state.update { it.copy(endDate = value, error = null) }
    fun updateTimezone(value: String) = _state.update { it.copy(timezone = value, error = null) }
    fun updateReminderTime(value: String) = _state.update { it.copy(reminderTime = value, error = null) }
    fun updateMeasurementType(value: String) = _state.update {
        it.copy(measurementType = value, measurementUnit = when (value) {
            MeasurementType.COUNT.name -> "unidades"
            MeasurementType.DURATION.name -> "min"
            MeasurementType.QUANTITY.name -> "ml"
            else -> ""
        }, error = null)
    }
    fun updateTargetValue(value: String) = _state.update { it.copy(targetValue = value, error = null) }
    fun updateMeasurementUnit(value: String) = _state.update { it.copy(measurementUnit = value, error = null) }
    fun updateAllowPartial(value: Boolean) = _state.update { it.copy(allowPartialProgress = value, error = null) }
    fun updateAggregationMode(value: String) = _state.update { it.copy(aggregationMode = value, error = null) }

    fun save() {
        val current = state.value
        if (current.name.trim().length < 2) {
            _state.update { it.copy(error = "Escribe el nombre del habito.") }
            return
        }
        if (current.startDate.isNotBlank() && runCatching { LocalDate.parse(current.startDate.trim()) }.isFailure) {
            _state.update { it.copy(error = "La fecha inicial debe usar YYYY-MM-DD.") }
            return
        }
        if (current.endDate.isNotBlank() && runCatching { LocalDate.parse(current.endDate.trim()) }.isFailure) {
            _state.update { it.copy(error = "La fecha final debe usar YYYY-MM-DD.") }
            return
        }
        if (runCatching { ZoneId.of(current.timezone.trim()) }.isFailure) {
            _state.update { it.copy(error = "La zona horaria no es válida.") }
            return
        }
        val schedule = current.toHabitFrequency()
        val scheduleError = schedule.validationError()
        if (scheduleError != null) {
            _state.update { it.copy(error = scheduleError) }
            return
        }
        val measurement = current.toHabitMeasurement()
        measurement.validationError()?.let { error ->
            _state.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            habitRepository.createHabit(
                name = current.name.trim(),
                icon = current.name.trim().take(2).uppercase(),
                schedule = schedule,
                time = current.reminderTime.ifBlank { "Sin hora" },
                category = current.category.ifBlank { "General" },
                measurement = measurement
            )
            _state.update { it.copy(loading = false, saved = true) }
        }
    }
}

private fun ManualHabitUiState.toHabitMeasurement() = HabitMeasurement(
    type = runCatching { MeasurementType.valueOf(measurementType) }.getOrDefault(MeasurementType.BOOLEAN),
    targetValue = targetValue.toDoubleOrNull() ?: Double.NaN,
    unit = measurementUnit.trim(),
    allowPartialProgress = allowPartialProgress,
    aggregationMode = runCatching { AggregationMode.valueOf(aggregationMode) }.getOrDefault(AggregationMode.ADD)
)

private fun ManualHabitUiState.toHabitFrequency(): HabitFrequency {
    val type = runCatching { HabitFrequencyType.valueOf(frequencyType) }.getOrDefault(HabitFrequencyType.DAILY)
    val zone = runCatching { ZoneId.of(timezone.trim()) }.getOrDefault(ZoneId.of("America/Lima"))
    val parsedStart = startDate.trim().takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val parsedEnd = endDate.trim().takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    return HabitFrequency(
        type = type,
        weekdays = weekdays.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }.toSet(),
        timesPerWeek = timesPerWeek.toIntOrNull(),
        intervalDays = intervalDays.toIntOrNull(),
        monthlyDays = monthlyDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet(),
        startDate = parsedStart,
        endDate = parsedEnd,
        timezone = zone.id,
        effectiveFrom = parsedStart ?: LocalDate.now(zone)
    )
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

private fun InterpretedHabit.toEditableHabit(existingHabits: List<Habit>): InterpretedHabitUi {
    val matchedHabit = existingHabitId
        ?.let { id -> existingHabits.firstOrNull { it.id == id } }
        ?: bestHabitMatch(name, existingHabits)
    return InterpretedHabitUi(
        name = matchedHabit?.name ?: name,
        action = action.ifBlank { "unknown" },
        quantity = quantity?.let(::formatQuantity).orEmpty(),
        unit = unit.orEmpty(),
        date = date,
        notes = notes.orEmpty(),
        existingHabitId = matchedHabit?.id,
        existingHabitName = matchedHabit?.name
    )
}

private fun bestHabitMatch(name: String, habits: List<Habit>): Habit? {
    val target = normalizeForMatch(name)
    if (target.isBlank()) return null
    return habits.firstOrNull { normalizeForMatch(it.name) == target }
        ?: habits.firstOrNull { habit ->
            val candidate = normalizeForMatch(habit.name)
            target in candidate || candidate in target
        }
        ?: habits.firstOrNull { habit ->
            val targetTokens = target.split(" ").filter { it.length > 2 }.toSet()
            val candidateTokens = normalizeForMatch(habit.name).split(" ").filter { it.length > 2 }.toSet()
            targetTokens.isNotEmpty() && targetTokens.intersect(candidateTokens).size >= 2
        }
}

private fun normalizeForMatch(value: String): String =
    Normalizer.normalize(value.lowercase(Locale("es", "PE")), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun String.toHabitStatusFromInterpretation(): HabitStatus =
    when (lowercase(Locale("es", "PE")).trim()) {
        "completed" -> HabitStatus.Completed
        "planned", "created", "unknown" -> HabitStatus.Pending
        else -> HabitStatus.Pending
    }

private fun WhisperErrorType.toVoiceErrorType(): VoiceErrorType =
    when (this) {
        WhisperErrorType.PermissionDenied,
        WhisperErrorType.PermissionPermanentlyDenied -> VoiceErrorType.InsufficientPermissions
        WhisperErrorType.MicrophoneBusy -> VoiceErrorType.RecognizerBusy
        WhisperErrorType.EmptyRecording,
        WhisperErrorType.EmptyText,
        WhisperErrorType.RecordingTooShort -> VoiceErrorType.NoMatch
        WhisperErrorType.InferenceCancelled,
        WhisperErrorType.RecordingInterrupted -> VoiceErrorType.Client
        WhisperErrorType.MicrophoneUnavailable,
        WhisperErrorType.ModelNotFound,
        WhisperErrorType.ModelCorrupt,
        WhisperErrorType.ModelIncompatible,
        WhisperErrorType.NativeLibraryUnavailable -> VoiceErrorType.ServiceUnavailable
        else -> VoiceErrorType.Unknown
    }

private fun validateInterpretedHabits(items: List<InterpretedHabitUi>): String? {
    items.forEach { item ->
        if (item.name.trim().length < 2) return "Cada hábito necesita un nombre."
        val quantity = item.quantity.trim().replace(",", ".")
        if (quantity.isNotBlank() && (quantity.toDoubleOrNull() == null || quantity.toDouble() <= 0.0)) {
            return "La cantidad debe ser positiva."
        }
        if (runCatching { LocalDate.parse(item.date.trim()) }.isFailure) {
            return "La fecha debe tener formato YYYY-MM-DD."
        }
    }
    return null
}

private fun registeredMessage(events: List<VoiceEventResult>): String {
    val names = events.map { it.habitName }.distinct()
    return if (names.size == 1) {
        "Listo. Registré ${names.first()}."
    } else {
        "Listo. Registré ${names.size} hábitos: ${names.take(3).joinToString(", ")}."
    }
}
