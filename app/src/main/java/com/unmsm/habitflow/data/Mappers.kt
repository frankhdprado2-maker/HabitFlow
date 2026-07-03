package com.unmsm.habitflow.data

import com.unmsm.habitflow.data.local.entity.AchievementEntity
import com.unmsm.habitflow.data.local.entity.HabitEntity
import com.unmsm.habitflow.data.local.entity.HabitEventEntity
import com.unmsm.habitflow.data.local.entity.NotificationEntity
import com.unmsm.habitflow.data.remote.dto.GeoEventDto
import com.unmsm.habitflow.data.remote.dto.UserDto
import com.unmsm.habitflow.data.remote.dto.VoiceCommandResponse
import com.unmsm.habitflow.domain.model.Achievement
import com.unmsm.habitflow.domain.model.AppNotification
import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitEvent
import com.unmsm.habitflow.domain.model.HabitStatus
import com.unmsm.habitflow.domain.model.NotificationKind
import com.unmsm.habitflow.domain.model.User
import com.unmsm.habitflow.domain.model.VoiceCommandResult
import java.time.Instant

fun UserDto.toDomain() = User(
    id = id.ifBlank { email },
    name = name.ifBlank { username ?: "Estudiante" },
    username = username ?: email.substringBefore("@"),
    email = email,
    bio = bio.orEmpty(),
    goal = goal ?: "Organizar mi semestre",
    timezone = timezone ?: "America/Lima",
    avatarUrl = avatarUrl
)

fun HabitEntity.toDomain() = Habit(id, name, icon, frequency, reminderTime, category, isActive, streak, bestStreak)

fun Habit.toEntity() = HabitEntity(id, name, icon, frequency, reminderTime, category, isActive, streak, bestStreak)

fun HabitEventEntity.toDomain() = HabitEvent(
    id = id,
    habitId = habitId,
    habitName = habitName,
    status = status.toHabitStatus(),
    timestamp = timestamp,
    note = note,
    synced = synced
)

fun HabitEvent.toEntity() = HabitEventEntity(id, habitId, habitName, status.name, timestamp, note, synced)

fun GeoEventDto.toEntity() = HabitEventEntity(
    id = id.ifBlank { "${deviceId.orEmpty()}-${recordedAt.orEmpty()}" },
    habitId = deviceId.orEmpty(),
    habitName = eventType.ifBlank { metadata?.get("habit_name").orEmpty() },
    status = metadata?.get("status") ?: notes?.substringAfter("status=", "Completed")?.substringBefore(";") ?: "Completed",
    timestamp = recordedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: System.currentTimeMillis(),
    note = notes.orEmpty(),
    synced = true
)

fun AchievementEntity.toDomain() = Achievement(id, title, description, requirement, unlocked, xp)

fun Achievement.toEntity() = AchievementEntity(id, title, description, requirement, unlocked, xp)

fun NotificationEntity.toDomain() = AppNotification(
    id = id,
    title = title,
    message = message,
    kind = runCatching { NotificationKind.valueOf(kind) }.getOrDefault(NotificationKind.Reminder),
    timestamp = timestamp,
    read = read
)

fun AppNotification.toEntity() = NotificationEntity(id, title, message, kind.name, timestamp, read)

fun VoiceCommandResponse.toDomain() = VoiceCommandResult(
    intent = intent,
    response = response.ifBlank { "Listo, entendí tu comando." },
    habitId = habitId,
    habitName = habitName,
    status = status?.toHabitStatus()
)

fun String.toHabitStatus(): HabitStatus =
    when (lowercase()) {
        "completed", "completado", "complete", "done" -> HabitStatus.Completed
        "skipped", "saltado" -> HabitStatus.Skipped
        "failed", "fallado" -> HabitStatus.Failed
        else -> HabitStatus.Pending
    }
