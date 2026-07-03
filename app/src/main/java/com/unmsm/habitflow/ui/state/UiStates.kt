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
    val loggedIn: Boolean = false
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
    val userName: String = "Frankie",
    val habits: List<Habit> = emptyList(),
    val completedToday: Int = 0,
    val streak: Int = 12,
    val voiceText: String = "",
    val voiceResponse: String = "",
    val loading: Boolean = true
)

data class HabitDetailUiState(
    val habit: Habit? = null,
    val events: List<HabitEvent> = emptyList(),
    val note: String = "",
    val completionPercent: Int = 78
)

data class StatsUiState(
    val currentStreak: Int = 12,
    val monthPercent: Int = 78,
    val weekly: List<Int> = listOf(4, 3, 5, 4, 2, 3, 4),
    val habits: List<Habit> = emptyList()
)

data class HistoryUiState(
    val filter: String = "Todos",
    val events: List<HabitEvent> = emptyList()
)

data class ProfileUiState(
    val user: User = User("local", "Frankie", "frankie", "frankie@unmsm.edu.pe"),
    val achievements: List<Achievement> = emptyList(),
    val friends: List<Pair<String, Int>> = listOf("Ariana" to 9, "Luis" to 7, "Camila" to 5)
)

data class SettingsUiState(
    val settings: SettingsState = SettingsState()
)

data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList()
)

data class AchievementsUiState(
    val level: Int = 4,
    val xp: Int = 640,
    val achievements: List<Achievement> = emptyList()
)

data class VoiceUiState(
    val listening: Boolean = false,
    val transcript: String = "",
    val response: String = "",
    val error: String? = null
)
