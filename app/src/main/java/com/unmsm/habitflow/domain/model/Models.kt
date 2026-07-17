package com.unmsm.habitflow.domain.model

import com.unmsm.habitflow.domain.habit.HabitFrequency

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
    val avatarUrl: String? = null,
    val avatarKey: String? = null,
    val categories: List<String> = emptyList(),
    val primaryGoal: String = "",
    val preferredCategories: List<String> = emptyList(),
    val onboardingCompleted: Boolean = false,
    val themeMode: String = "system",
    val accentTheme: String = "mint",
    val voiceResponseEnabled: Boolean = true,
    val locale: String = "es-PE",
    val profileComplete: Boolean = false
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
    val bestStreak: Int = 0,
    val schedule: HabitFrequency = HabitFrequency.fromLegacy(frequency)
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

data class PlanRecommendation(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val actions: List<String>,
    val createdAt: Long,
    val accepted: Boolean = false
)

data class CosmeticReward(
    val id: String,
    val name: String,
    val description: String,
    val kind: String,
    val cost: Int,
    val unlocked: Boolean,
    val equipped: Boolean = false
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
    val status: HabitStatus? = null,
    val question: String? = null,
    val quickReplies: List<String> = emptyList(),
    val events: List<VoiceEventResult> = emptyList(),
    val plan: VoicePlanResult? = null,
    val conversationId: String? = null
)

data class VoiceEventResult(
    val habitId: String?,
    val habitName: String,
    val status: HabitStatus,
    val quantity: Double? = null,
    val unit: String? = null,
    val date: String? = null
)

data class VoicePlanResult(
    val title: String,
    val summary: String,
    val category: String,
    val actions: List<String>
)

data class InterpretedHabit(
    val name: String,
    val action: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val date: String,
    val notes: String? = null,
    val existingHabitId: String? = null
)

data class HabitInterpretationResult(
    val intent: String,
    val habits: List<InterpretedHabit>,
    val confidence: Double,
    val needsConfirmation: Boolean,
    val confirmationMessage: String
)
