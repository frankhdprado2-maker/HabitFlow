package com.unmsm.habitflow.ui.state

import com.unmsm.habitflow.data.repository.SettingsState
import com.unmsm.habitflow.domain.model.Achievement
import com.unmsm.habitflow.domain.model.AppNotification
import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitEvent
import com.unmsm.habitflow.domain.model.User

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
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
    val completedToday: Int = 0,
    val streak: Int = 0,
    val voiceText: String = "",
    val voiceResponse: String = "",
    val loading: Boolean = true
)

data class HabitDetailUiState(
    val habit: Habit? = null,
    val events: List<HabitEvent> = emptyList(),
    val note: String = "",
    val completionPercent: Int = 0
)

data class StatsUiState(
    val currentStreak: Int = 0,
    val monthPercent: Int = 0,
    val weekly: List<Int> = List(7) { 0 },
    val habits: List<Habit> = emptyList()
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
    val darkMode: Boolean = true,
    val accentColor: String = "violet"
)

data class ProfileSetupUiState(
    val name: String = "",
    val username: String = "",
    val goal: String = "",
    val avatarKey: String = "avatar_lavender",
    val categories: List<String> = listOf("Estudio", "Salud"),
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
    val achievements: List<Achievement> = emptyList()
)

data class VoiceMessageUi(
    val author: String,
    val text: String
)

data class VoiceUiState(
    val listening: Boolean = false,
    val transcript: String = "",
    val response: String = "",
    val messages: List<VoiceMessageUi> = emptyList(),
    val quickReplies: List<String> = emptyList(),
    val conversationId: String? = null,
    val error: String? = null
)

data class ManualHabitUiState(
    val name: String = "",
    val category: String = "General",
    val frequency: String = "Diario",
    val reminderTime: String = "Sin hora",
    val loading: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)
