package com.unmsm.habitflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val frequency: String,
    val reminderTime: String,
    val category: String,
    val isActive: Boolean,
    val streak: Int,
    val bestStreak: Int
)

@Entity(tableName = "habit_events")
data class HabitEventEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val habitName: String,
    val status: String,
    val timestamp: Long,
    val note: String,
    val synced: Boolean
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val requirement: String,
    val unlocked: Boolean,
    val xp: Int
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val kind: String,
    val timestamp: Long,
    val read: Boolean
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val username: String,
    val email: String,
    val bio: String,
    val goal: String,
    val timezone: String,
    val avatarUrl: String?,
    val avatarKey: String?,
    val categoriesCsv: String,
    val profileComplete: Boolean
)

@Entity(tableName = "plan_recommendations")
data class PlanRecommendationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val actionsCsv: String,
    val createdAt: Long,
    val accepted: Boolean
)

@Entity(tableName = "cosmetic_rewards")
data class CosmeticRewardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val kind: String,
    val cost: Int,
    val unlocked: Boolean,
    val equipped: Boolean
)
