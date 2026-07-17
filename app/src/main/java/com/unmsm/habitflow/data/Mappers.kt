package com.unmsm.habitflow.data

import com.unmsm.habitflow.data.local.entity.AchievementEntity
import com.unmsm.habitflow.data.local.entity.CosmeticRewardEntity
import com.unmsm.habitflow.data.local.entity.HabitEntity
import com.unmsm.habitflow.data.local.entity.HabitEventEntity
import com.unmsm.habitflow.data.local.entity.HabitScheduleVersionEntity
import com.unmsm.habitflow.data.local.entity.NotificationEntity
import com.unmsm.habitflow.data.local.entity.PlanRecommendationEntity
import com.unmsm.habitflow.data.local.entity.UserProfileEntity
import com.unmsm.habitflow.data.remote.dto.GeoEventDto
import com.unmsm.habitflow.data.remote.dto.HabitInterpretationResponse
import com.unmsm.habitflow.data.remote.dto.UserDto
import com.unmsm.habitflow.data.remote.dto.VoiceCommandResponse
import com.unmsm.habitflow.data.remote.dto.VoiceConversationActionDto
import com.unmsm.habitflow.data.remote.dto.VoiceConversationResponse
import com.unmsm.habitflow.domain.model.Achievement
import com.unmsm.habitflow.domain.model.AppNotification
import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitEvent
import com.unmsm.habitflow.domain.model.HabitInterpretationResult
import com.unmsm.habitflow.domain.model.HabitStatus
import com.unmsm.habitflow.domain.model.InterpretedHabit
import com.unmsm.habitflow.domain.model.NotificationKind
import com.unmsm.habitflow.domain.model.CosmeticReward
import com.unmsm.habitflow.domain.model.PlanRecommendation
import com.unmsm.habitflow.domain.model.User
import com.unmsm.habitflow.domain.model.VoiceCommandResult
import com.unmsm.habitflow.domain.model.VoiceEventResult
import com.unmsm.habitflow.domain.model.VoicePlanResult
import com.unmsm.habitflow.domain.habit.HabitFrequency
import com.unmsm.habitflow.domain.habit.HabitFrequencyType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

fun UserDto.toDomain() = User(
    id = id.ifBlank { email },
    name = name.ifBlank { username ?: "Estudiante" },
    username = username ?: email.substringBefore("@"),
    email = email,
    bio = bio.orEmpty(),
    goal = goal.orEmpty(),
    primaryGoal = primaryGoal ?: goal.orEmpty(),
    timezone = timezone ?: "America/Lima",
    avatarUrl = avatarUrl,
    avatarKey = avatarKey,
    categories = categories,
    preferredCategories = preferredCategories.ifEmpty { categories },
    onboardingCompleted = onboardingCompleted ?: profileComplete,
    themeMode = themeMode ?: "system",
    accentTheme = accentTheme ?: "mint",
    voiceResponseEnabled = voiceResponseEnabled ?: true,
    locale = locale ?: "es-PE",
    profileComplete = profileComplete
)

fun User.toEntity() = UserProfileEntity(
    id = id,
    name = name,
    username = username,
    email = email,
    bio = bio,
    goal = goal,
    primaryGoal = primaryGoal.ifBlank { goal },
    timezone = timezone,
    avatarUrl = avatarUrl,
    avatarKey = avatarKey,
    categoriesCsv = categories.joinToString(","),
    preferredCategoriesCsv = preferredCategories.ifEmpty { categories }.joinToString(","),
    onboardingCompleted = onboardingCompleted,
    themeMode = themeMode,
    accentTheme = accentTheme,
    voiceResponseEnabled = voiceResponseEnabled,
    locale = locale,
    profileComplete = profileComplete
)

fun UserProfileEntity.toDomain() = User(
    id = id,
    name = name,
    username = username,
    email = email,
    bio = bio,
    goal = goal,
    primaryGoal = primaryGoal.ifBlank { goal },
    timezone = timezone,
    avatarUrl = avatarUrl,
    avatarKey = avatarKey,
    categories = categoriesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() },
    preferredCategories = preferredCategoriesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
        .ifEmpty { categoriesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() } },
    onboardingCompleted = onboardingCompleted,
    themeMode = themeMode,
    accentTheme = accentTheme,
    voiceResponseEnabled = voiceResponseEnabled,
    locale = locale,
    profileComplete = profileComplete
)

fun HabitEntity.toDomain() = Habit(
    id = id,
    name = name,
    icon = icon,
    frequency = frequency,
    reminderTime = reminderTime,
    category = category,
    isActive = isActive,
    streak = streak,
    bestStreak = bestStreak,
    schedule = structuredFrequency()
)

fun Habit.toEntity() = HabitEntity(
    id = id,
    name = name,
    icon = icon,
    frequency = schedule.displayText(),
    reminderTime = reminderTime,
    category = category,
    isActive = isActive,
    streak = streak,
    bestStreak = bestStreak,
    frequencyType = schedule.type.name,
    weekdaysCsv = schedule.weekdays.joinToString(",") { it.name },
    timesPerWeek = schedule.timesPerWeek,
    intervalDays = schedule.intervalDays,
    monthlyDaysCsv = schedule.monthlyDays.sorted().joinToString(","),
    scheduleStartDate = schedule.startDate?.toString(),
    scheduleEndDate = schedule.endDate?.toString(),
    scheduleTimezone = schedule.timezone,
    scheduleActive = schedule.active,
    frequencyNeedsReview = schedule.needsReview,
    frequencyOriginal = schedule.originalText.ifBlank { frequency },
    scheduleEffectiveFrom = schedule.effectiveFrom?.toString()
)

fun HabitFrequency.toVersionEntity(habitId: String, id: String): HabitScheduleVersionEntity =
    HabitScheduleVersionEntity(
        id = id,
        habitId = habitId,
        frequencyType = type.name,
        weekdaysCsv = weekdays.joinToString(",") { it.name },
        timesPerWeek = timesPerWeek,
        intervalDays = intervalDays,
        monthlyDaysCsv = monthlyDays.sorted().joinToString(","),
        startDate = startDate?.toString(),
        endDate = endDate?.toString(),
        timezone = timezone,
        active = active,
        needsReview = needsReview,
        originalText = originalText,
        effectiveFrom = effectiveFrom?.toString(),
        effectiveTo = effectiveTo?.toString()
    )

fun HabitScheduleVersionEntity.toDomain(): HabitFrequency = HabitFrequency(
    type = runCatching { HabitFrequencyType.valueOf(frequencyType) }.getOrDefault(HabitFrequencyType.LEGACY_REVIEW),
    weekdays = weekdaysCsv.csvValues().mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }.toSet(),
    timesPerWeek = timesPerWeek,
    intervalDays = intervalDays,
    monthlyDays = monthlyDaysCsv.csvValues().mapNotNull(String::toIntOrNull).toSet(),
    startDate = startDate.toLocalDateOrNull(),
    endDate = endDate.toLocalDateOrNull(),
    timezone = timezone,
    active = active,
    needsReview = needsReview,
    originalText = originalText,
    effectiveFrom = effectiveFrom.toLocalDateOrNull(),
    effectiveTo = effectiveTo.toLocalDateOrNull()
)

private fun HabitEntity.structuredFrequency(): HabitFrequency {
    val type = runCatching { HabitFrequencyType.valueOf(frequencyType) }.getOrDefault(HabitFrequencyType.LEGACY_REVIEW)
    if (type == HabitFrequencyType.LEGACY_REVIEW) return HabitFrequency.fromLegacy(frequencyOriginal.ifBlank { frequency }, scheduleTimezone)
        .copy(needsReview = true)
    return HabitFrequency(
        type = type,
        weekdays = weekdaysCsv.csvValues().mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }.toSet(),
        timesPerWeek = timesPerWeek,
        intervalDays = intervalDays,
        monthlyDays = monthlyDaysCsv.csvValues().mapNotNull(String::toIntOrNull).toSet(),
        startDate = scheduleStartDate.toLocalDateOrNull(),
        endDate = scheduleEndDate.toLocalDateOrNull(),
        timezone = scheduleTimezone,
        active = scheduleActive,
        needsReview = frequencyNeedsReview,
        originalText = frequencyOriginal.ifBlank { frequency },
        effectiveFrom = scheduleEffectiveFrom.toLocalDateOrNull()
    )
}

private fun String.csvValues() = split(",").map(String::trim).filter(String::isNotBlank)
private fun String?.toLocalDateOrNull() = this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

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

fun PlanRecommendationEntity.toDomain() = PlanRecommendation(
    id = id,
    title = title,
    summary = summary,
    category = category,
    actions = actionsCsv.split("|").map { it.trim() }.filter { it.isNotBlank() },
    createdAt = createdAt,
    accepted = accepted
)

fun PlanRecommendation.toEntity() = PlanRecommendationEntity(
    id = id,
    title = title,
    summary = summary,
    category = category,
    actionsCsv = actions.joinToString("|"),
    createdAt = createdAt,
    accepted = accepted
)

fun CosmeticRewardEntity.toDomain() = CosmeticReward(id, name, description, kind, cost, unlocked, equipped)

fun CosmeticReward.toEntity() = CosmeticRewardEntity(id, name, description, kind, cost, unlocked, equipped)

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
    response = response.ifBlank { "Listo, entendi tu comando." },
    habitId = habitId,
    habitName = habitName,
    status = status?.toHabitStatus(),
    question = question,
    quickReplies = quickReplies,
    events = events.map {
        VoiceEventResult(
            habitId = it.habitId,
            habitName = it.habitName,
            status = it.status.toHabitStatus(),
            quantity = it.quantity,
            unit = it.unit
        )
    },
    plan = plan?.let {
        VoicePlanResult(
            title = it.title,
            summary = it.summary,
            category = it.category,
            actions = it.actions
        )
    },
    conversationId = conversationId
)

fun VoiceConversationResponse.toDomainCommandResult(): VoiceCommandResult {
    val event = action?.toVoiceEventResult()
    val plan = dailyPlan?.let {
        VoicePlanResult(
            title = "Plan de hoy",
            summary = it.summary,
            category = "IA",
            actions = it.items.map { item ->
                "${item.suggestedTime} - ${item.habitName}: ${item.reason}"
            }
        )
    } ?: weeklySummary?.let {
        VoicePlanResult(
            title = it.headline.ifBlank { "Resumen de tu semana" },
            summary = it.summary,
            category = "Progreso",
            actions = it.highlights + listOfNotNull(it.recommendation.takeIf { text -> text.isNotBlank() })
        )
    } ?: adaptiveRecommendation?.let {
        VoicePlanResult(
            title = it.title.ifBlank { "Recomendacion adaptativa" },
            summary = it.message.ifBlank { it.reason },
            category = "Recomendacion",
            actions = listOf(it.reason).filter { text -> text.isNotBlank() }
        )
    }
    return VoiceCommandResult(
        intent = if (event != null) "registrar_habito" else intent,
        response = assistantMessage,
        habitId = event?.habitId,
        habitName = event?.habitName,
        status = event?.status,
        quickReplies = suggestions,
        events = listOfNotNull(event),
        plan = plan,
        conversationId = sessionId
    )
}

fun HabitInterpretationResponse.toDomain() = HabitInterpretationResult(
    intent = intent,
    habits = habits.map {
        InterpretedHabit(
            name = it.name,
            action = it.action,
            quantity = it.quantity,
            unit = it.unit,
            date = it.date,
            notes = it.notes,
            existingHabitId = it.existingHabitId
        )
    },
    confidence = confidence,
    needsConfirmation = needsConfirmation,
    confirmationMessage = confirmationMessage
)

private fun VoiceConversationActionDto.toVoiceEventResult(): VoiceEventResult? {
    val cleanType = type.uppercase()
    val status = when (cleanType) {
        "CREATE_HABIT" -> HabitStatus.Pending
        "COMPLETE_HABIT" -> HabitStatus.Completed
        "SKIP_HABIT" -> HabitStatus.Skipped
        else -> return null
    }
    val habitName = payload["name"]?.asPayloadString()?.trim()
        ?: payload["habit_name"]?.asPayloadString()?.trim()
        ?: return null
    return VoiceEventResult(
        habitId = payload["habit_id"]?.asPayloadString()?.takeIf { it.isNotBlank() },
        habitName = habitName,
        status = status,
        quantity = payload["duration_minutes"]?.asPayloadString()?.toDoubleOrNull()
            ?: payload["quantity"]?.asPayloadString()?.toDoubleOrNull(),
        unit = payload["unit"]?.asPayloadString() ?: payload["duration_unit"]?.asPayloadString()
    )
}

private fun Any.asPayloadString(): String? =
    when (this) {
        is String -> this
        is Number -> this.toString()
        is Boolean -> this.toString()
        else -> null
    }

fun String.toHabitStatus(): HabitStatus =
    when (lowercase()) {
        "completed", "completado", "complete", "done" -> HabitStatus.Completed
        "skipped", "saltado" -> HabitStatus.Skipped
        "failed", "fallado" -> HabitStatus.Failed
        else -> HabitStatus.Pending
    }
