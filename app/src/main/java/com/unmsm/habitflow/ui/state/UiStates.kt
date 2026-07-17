package com.unmsm.habitflow.ui.state

import com.unmsm.habitflow.data.repository.SettingsState
import com.unmsm.habitflow.domain.model.Achievement
import com.unmsm.habitflow.domain.model.AppNotification
import com.unmsm.habitflow.domain.model.CosmeticReward
import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitEvent
import com.unmsm.habitflow.domain.model.PlanRecommendation
import com.unmsm.habitflow.domain.model.User
import com.unmsm.habitflow.domain.habit.HabitHeatmap
import com.unmsm.habitflow.voice.VoiceErrorType

sealed interface GoogleLoginState {
    data object Idle : GoogleLoginState
    data object OpeningGoogle : GoogleLoginState
    data object ContactingBackend : GoogleLoginState
    data object LoadingProfile : GoogleLoginState
    data object Success : GoogleLoginState
    data class Error(val message: String) : GoogleLoginState
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val googleState: GoogleLoginState = GoogleLoginState.Idle,
    val error: String? = null,
    val loggedIn: Boolean = false,
    val needsProfile: Boolean = false
)

data class RegisterUiState(
    val step: Int = 1,
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val goal: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val registered: Boolean = false
)

data class HomeUiState(
    val userName: String = "Estudiante",
    val habits: List<Habit> = emptyList(),
    val completedHabitIds: Set<String> = emptySet(),
    val completedToday: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val totalCompleted: Int = 0,
    val voiceText: String = "",
    val voiceResponse: String = "",
    val loading: Boolean = true,
    val offline: Boolean = false,
    val lastActionMessage: String? = null
)

data class HabitDetailUiState(
    val habit: Habit? = null,
    val events: List<HabitEvent> = emptyList(),
    val note: String = "",
    val completionPercent: Int = 0,
    val heatmap: HabitHeatmap = HabitHeatmap()
)

data class StatsUiState(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val monthPercent: Int = 0,
    val weekly: List<Int> = List(7) { 0 },
    val habits: List<Habit> = emptyList(),
    val totalCompleted: Int = 0,
    val weeklyComparison: Int = 0,
    val hasData: Boolean = false,
    val habitCompletionRates: Map<String, Float> = emptyMap(),
    val coach: CoachUiState = CoachUiState()
)

data class CoachUiState(
    val question: String = "",
    val loading: Boolean = false,
    val title: String = "",
    val answer: String = "",
    val evidence: List<String> = emptyList(),
    val suggestions: List<String> = listOf(
        "Resumen semanal",
        "Qué me recomiendas para mejorar mi rutina",
        "Plan de hoy"
    ),
    val error: String? = null
)

data class HistoryUiState(
    val filter: String = "Todos",
    val events: List<HabitEvent> = emptyList()
)

data class ProfileUiState(
    val user: User = User("local", "Estudiante", "estudiante", ""),
    val achievements: List<Achievement> = emptyList(),
    val friends: List<Pair<String, Int>> = emptyList()
)

data class EditProfileUiState(
    val name: String = "",
    val username: String = "",
    val goal: String = "",
    val avatarKey: String = "avatar_lavender",
    val categories: List<String> = listOf("Estudio", "Salud"),
    val loading: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

data class SettingsUiState(
    val settings: SettingsState = SettingsState(),
    val loggingOut: Boolean = false
)

data class ThemeUiState(
    val themeMode: String = "system",
    val accentColor: String = "mint",
    val dynamicColor: Boolean = false,
    val textScale: String = "standard"
)

data class ProfileSetupUiState(
    val step: Int = 1,
    val maxSteps: Int = 6,
    val name: String = "",
    val username: String = "",
    val goal: String = "",
    val bio: String = "",
    val avatarKey: String = "avatar_lavender",
    val categories: List<String> = listOf("Estudio", "Salud"),
    val selectedTemplates: List<String> = listOf("Estudiar 25 minutos"),
    val darkMode: Boolean = false,
    val accentColor: String = "mint",
    val remindersEnabled: Boolean = true,
    val voiceResponseEnabled: Boolean = true,
    val loading: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList()
)

data class AchievementsUiState(
    val level: Int = 0,
    val xp: Int = 0,
    val achievements: List<Achievement> = emptyList(),
    val cosmetics: List<CosmeticReward> = emptyList(),
    val plans: List<PlanRecommendation> = emptyList()
)

data class VoiceMessageUi(
    val author: String,
    val text: String
)

data class HabitAssociationOptionUi(
    val id: String,
    val name: String
)

data class InterpretedHabitUi(
    val name: String,
    val action: String,
    val quantity: String = "",
    val unit: String = "",
    val date: String = "",
    val notes: String = "",
    val existingHabitId: String? = null,
    val existingHabitName: String? = null
)

sealed interface VoiceAssistantPhase {
    data object Idle : VoiceAssistantPhase
    data object ModelNotPrepared : VoiceAssistantPhase
    data object PreparingModel : VoiceAssistantPhase
    data object Ready : VoiceAssistantPhase
    data object RequestingPermission : VoiceAssistantPhase
    data class Recording(val durationMillis: Long, val audioLevel: Float) : VoiceAssistantPhase
    data object Transcribing : VoiceAssistantPhase
    data object Processing : VoiceAssistantPhase
    data object AwaitingConfirmation : VoiceAssistantPhase
    data object Speaking : VoiceAssistantPhase
    data object Completed : VoiceAssistantPhase
    data class Error(val type: VoiceErrorType, val message: String) : VoiceAssistantPhase
}

data class VoiceUiState(
    val phase: VoiceAssistantPhase = VoiceAssistantPhase.Idle,
    val listening: Boolean = false,
    val recording: Boolean = false,
    val transcribing: Boolean = false,
    val recordingDurationMillis: Long = 0,
    val audioLevel: Float = 0f,
    val transcript: String = "",
    val partialTranscript: String = "",
    val response: String = "",
    val messages: List<VoiceMessageUi> = emptyList(),
    val quickReplies: List<String> = emptyList(),
    val conversationId: String? = null,
    val pendingSummary: String? = null,
    val interpretationText: String = "",
    val interpretedHabits: List<InterpretedHabitUi> = emptyList(),
    val habitAssociationOptions: List<HabitAssociationOptionUi> = emptyList(),
    val interpretationConfidence: Double = 0.0,
    val savingInterpretation: Boolean = false,
    val coachTitle: String = "",
    val coachHighlights: List<String> = emptyList(),
    val permissionPermanentlyDenied: Boolean = false,
    val error: String? = null
)

data class ManualHabitUiState(
    val name: String = "",
    val category: String = "General",
    val frequency: String = "Diario",
    val frequencyType: String = "DAILY",
    val weekdays: Set<String> = emptySet(),
    val timesPerWeek: String = "3",
    val intervalDays: String = "2",
    val monthlyDays: String = "1",
    val startDate: String = "",
    val endDate: String = "",
    val timezone: String = "America/Lima",
    val reminderTime: String = "Sin hora",
    val loading: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)
