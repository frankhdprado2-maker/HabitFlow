package com.unmsm.habitflow.domain.model

data class User(
    val id: String,
    val name: String,
    val username: String,
    val email: String,
    val bio: String = "",
    val goal: String = "Ser constante",
    val timezone: String = "America/Lima",
    val level: Int = 0,
    val xp: Int = 0,
    val avatarUrl: String? = null
)

data class Habit(
    val id: String,
    val name: String,
    val icon: String,
    val frequency: String,
    val reminderTime: String,
    val category: String,
    val isActive: Boolean = true,
    val streak: Int = 0,
    val bestStreak: Int = 0
)

enum class HabitStatus {
    Completed,
    Skipped,
    Failed,
    Pending
}

data class HabitEvent(
    val id: String,
    val habitId: String,
    val habitName: String,
    val status: HabitStatus,
    val timestamp: Long,
    val note: String = "",
    val synced: Boolean = false
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val requirement: String,
    val unlocked: Boolean,
    val xp: Int
)

enum class NotificationKind {
    StreakRisk,
    AchievementUnlocked,
    Reminder,
    WeeklySummary,
    FriendRequest,
    GoalCompleted
}

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val kind: NotificationKind,
    val timestamp: Long,
    val read: Boolean = false
)

data class VoiceCommandResult(
    val intent: String,
    val response: String,
    val habitId: String? = null,
    val habitName: String? = null,
    val status: HabitStatus? = null
)
