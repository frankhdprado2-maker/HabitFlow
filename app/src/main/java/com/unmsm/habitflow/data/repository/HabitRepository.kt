package com.unmsm.habitflow.data.repository

import com.unmsm.habitflow.data.local.dao.AchievementDao
import com.unmsm.habitflow.data.local.dao.CosmeticRewardDao
import com.unmsm.habitflow.data.local.dao.HabitDao
import com.unmsm.habitflow.data.local.dao.HabitEventDao
import com.unmsm.habitflow.data.local.dao.HabitScheduleDao
import com.unmsm.habitflow.data.local.dao.NotificationDao
import com.unmsm.habitflow.data.local.dao.PlanRecommendationDao
import com.unmsm.habitflow.data.local.dao.UserProfileDao
import com.unmsm.habitflow.data.remote.api.HabitEventApi
import com.unmsm.habitflow.data.remote.dto.GeoEventRequest
import com.unmsm.habitflow.data.toDomain
import com.unmsm.habitflow.data.toEntity
import com.unmsm.habitflow.data.toVersionEntity
import com.unmsm.habitflow.domain.model.Achievement
import com.unmsm.habitflow.domain.model.AppNotification
import com.unmsm.habitflow.domain.model.CosmeticReward
import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitEvent
import com.unmsm.habitflow.domain.model.HabitStatus
import com.unmsm.habitflow.domain.model.NotificationKind
import com.unmsm.habitflow.domain.model.PlanRecommendation
import com.unmsm.habitflow.domain.model.VoiceCommandResult
import com.unmsm.habitflow.domain.model.VoiceEventResult
import com.unmsm.habitflow.domain.model.VoicePlanResult
import com.unmsm.habitflow.domain.habit.HabitStreakCalculator
import com.unmsm.habitflow.domain.habit.HabitFrequency
import com.unmsm.habitflow.domain.habit.AggregationMode
import com.unmsm.habitflow.domain.habit.HabitMeasurement
import com.unmsm.habitflow.domain.habit.MeasurementNormalizer
import com.unmsm.habitflow.domain.habit.MeasurementType
import com.unmsm.habitflow.util.AppResult
import com.unmsm.habitflow.work.HabitReminderScheduler
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val eventDao: HabitEventDao,
    private val habitScheduleDao: HabitScheduleDao,
    private val achievementDao: AchievementDao,
    private val notificationDao: NotificationDao,
    private val planRecommendationDao: PlanRecommendationDao,
    private val cosmeticRewardDao: CosmeticRewardDao,
    private val userProfileDao: UserProfileDao,
    private val eventApi: HabitEventApi,
    private val reminderScheduler: HabitReminderScheduler
) {
    private val eventMutationMutex = Mutex()

    fun observeHabits(): Flow<List<Habit>> =
        habitDao.observeActive().map { habits -> habits.map { it.toDomain() } }

    suspend fun activeHabits(): List<Habit> = habitDao.activeOnce().map { it.toDomain() }
    suspend fun habitById(id: String): Habit? = habitDao.findById(id)?.toDomain()

    suspend fun recentEvents(limit: Int = 80): List<HabitEvent> = eventDao.recent(limit).map { it.toDomain() }

    suspend fun achievementsSnapshot(): List<Achievement> = achievementDao.allOnce().map { it.toDomain() }

    fun observeHabit(id: String): Flow<Habit?> = habitDao.observeById(id).map { it?.toDomain() }

    fun observeEvents(): Flow<List<HabitEvent>> =
        eventDao.observeAll().map { events -> events.map { it.toDomain() } }

    fun observeEventsForHabit(habitId: String): Flow<List<HabitEvent>> =
        eventDao.observeForHabit(habitId).map { events -> events.map { it.toDomain() } }

    fun observeScheduleVersions(habitId: String): Flow<List<HabitFrequency>> =
        habitScheduleDao.observeVersionsForHabit(habitId).map { versions -> versions.map { it.toDomain() } }

    fun observeAchievements(): Flow<List<Achievement>> =
        achievementDao.observeAll().map { items -> items.map { it.toDomain() } }

    fun observePlanRecommendations(): Flow<List<PlanRecommendation>> =
        planRecommendationDao.observeAll().map { items -> items.map { it.toDomain() } }

    fun observeCosmeticRewards(): Flow<List<CosmeticReward>> =
        cosmeticRewardDao.observeAll().map { items -> items.map { it.toDomain() } }

    fun observeNotifications(): Flow<List<AppNotification>> =
        notificationDao.observeAll().map { items -> items.map { it.toDomain() } }

    fun observeTimezone(): Flow<String> =
        userProfileDao.observeCurrent().map { profile -> profile?.timezone?.takeIf { it.isNotBlank() } ?: DEFAULT_TIMEZONE }

    suspend fun ensureSeedData() {
        if (habitDao.count() == 0) {
            habitDao.upsertAll(
                listOf(
                    Habit("study", "Estudiar algoritmos", "school", "Lun-Vie", "08:00", "Universidad", streak = 6, bestStreak = 11),
                    Habit("water", "Tomar agua", "water_drop", "Diario", "10:00", "Salud", streak = 12, bestStreak = 18),
                    Habit("run", "Correr 30 minutos", "directions_run", "Mar-Jue-Sab", "18:30", "Ejercicio", streak = 3, bestStreak = 9),
                    Habit("read", "Leer 20 páginas", "menu_book", "Diario", "21:00", "Crecimiento", streak = 4, bestStreak = 15)
                ).map { it.toEntity() }
            )
            achievementDao.upsertAll(
                listOf(
                    Achievement("first", "Primer registro", "Marcaste tu primer hábito.", "Completa 1 hábito", true, 100),
                    Achievement("week", "Semana sólida", "Mantén una racha de 7 días.", "Racha de 7 días", false, 250),
                    Achievement("focus", "Modo enfoque", "Completa 5 hábitos de estudio.", "5 registros de estudio", true, 180)
                ).map { it.toEntity() }
            )
            notificationDao.upsertAll(
                listOf(
                    AppNotification("risk", "Racha en peligro", "Te falta Estudiar algoritmos antes de dormir.", NotificationKind.StreakRisk, System.currentTimeMillis()),
                    AppNotification("weekly", "Resumen semanal", "Completaste 76% de tus hábitos esta semana.", NotificationKind.WeeklySummary, System.currentTimeMillis() - 86_400_000)
                ).map { it.toEntity() }
            )
        }

        if (cosmeticRewardDao.count() == 0) {
            cosmeticRewardDao.upsertAll(
                listOf(
                    CosmeticReward("avatar_frame_mint", "Marco menta", "Un marco suave para tu avatar.", "avatar_frame", 120, false),
                    CosmeticReward("theme_coral", "Acento coral", "Variante calida de la paleta clay.", "theme", 220, false),
                    CosmeticReward("badge_iron", "Constancia de hierro", "Insignia por sostener tu rutina.", "badge", 350, false)
                ).map { it.toEntity() }
            )
        }
    }

    suspend fun seedDemoAccountData() {
        if (habitDao.count() == 0) {
            habitDao.upsertAll(demoHabits().map { it.toEntity() })
            achievementDao.upsertAll(
                listOf(
                    Achievement("demo-first", "Primer registro", "Completaste tu primer hábito.", "Completa 1 hábito", true, 100),
                    Achievement("demo-week", "Semana sólida", "Mantuviste una rutina durante una semana.", "Racha de 7 días", true, 250),
                    Achievement("demo-focus", "Modo enfoque", "Completaste varias sesiones de estudio.", "5 registros de estudio", true, 180)
                ).map { it.toEntity() }
            )
            notificationDao.upsertAll(
                listOf(
                    AppNotification(
                        "demo-summary",
                        "Tu resumen está listo",
                        "Pregunta al Coach cómo fue tu semana.",
                        NotificationKind.WeeklySummary,
                        System.currentTimeMillis()
                    ),
                    AppNotification(
                        "demo-risk",
                        "Rutina para revisar",
                        "Correr fue el hábito más difícil de sostener esta semana.",
                        NotificationKind.StreakRisk,
                        System.currentTimeMillis() - DAY_MS
                    )
                ).map { it.toEntity() }
            )
        }
        if (eventDao.count() == 0) {
            eventDao.upsertAll(demoHabitEvents().map { it.toEntity() })
        }
    }

    suspend fun savePlanRecommendation(plan: VoicePlanResult) {
        planRecommendationDao.upsert(
            PlanRecommendation(
                id = UUID.randomUUID().toString(),
                title = plan.title,
                summary = plan.summary,
                category = plan.category,
                actions = plan.actions,
                createdAt = System.currentTimeMillis()
            ).toEntity()
        )
    }

    suspend fun createHabit(name: String, icon: String, frequency: String, time: String, category: String) {
        createHabit(name, icon, HabitFrequency.fromLegacy(frequency), time, category)
    }

    suspend fun createHabit(name: String, icon: String, schedule: HabitFrequency, time: String, category: String) {
        createHabit(name, icon, schedule, time, category, HabitMeasurement())
    }

    suspend fun createHabit(
        name: String,
        icon: String,
        schedule: HabitFrequency,
        time: String,
        category: String,
        measurement: HabitMeasurement
    ) {
        require(schedule.validationError() == null) { schedule.validationError().orEmpty() }
        require(measurement.validationError() == null) { measurement.validationError().orEmpty() }
        val habitId = UUID.randomUUID().toString()
        val habit = Habit(
                id = habitId,
                name = name,
                icon = icon,
                frequency = schedule.displayText(),
                reminderTime = time,
                category = category,
                schedule = schedule,
                measurement = measurement
            )
        habitDao.upsert(habit.toEntity())
        habitScheduleDao.upsert(schedule.toVersionEntity(habitId, UUID.randomUUID().toString()))
        reminderScheduler.schedule(habit)
    }

    suspend fun updateHabitSchedule(habitId: String, schedule: HabitFrequency, effectiveFrom: LocalDate): AppResult<Unit> =
        runCatching {
            require(schedule.validationError() == null) { schedule.validationError().orEmpty() }
            val habit = habitDao.findById(habitId)?.toDomain() ?: error("No se encontró el hábito.")
            val versionedSchedule = schedule.copy(effectiveFrom = effectiveFrom, effectiveTo = null)
            habitScheduleDao.closeCurrentVersion(habitId, effectiveFrom.minusDays(1).toString())
            habitScheduleDao.upsert(versionedSchedule.toVersionEntity(habitId, UUID.randomUUID().toString()))
            habitDao.upsert(
                habit.copy(
                    frequency = versionedSchedule.displayText(),
                    schedule = versionedSchedule
                ).toEntity()
            )
            habitDao.findById(habitId)?.toDomain()?.let(reminderScheduler::schedule)
            recalculateStreak(habitId, userZoneId())
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Error(it.message ?: "No se pudo actualizar la frecuencia.", it) }
        )

    suspend fun applyVoiceCommand(result: VoiceCommandResult): AppResult<HabitEvent>? {
        if (result.intent != "registrar_habito") return null
        val events = result.events.ifEmpty {
            val habitName = result.habitName?.trim().orEmpty()
            val status = result.status ?: return null
            if (habitName.isBlank()) return null
            listOf(VoiceEventResult(result.habitId, habitName, status))
        }

        var lastResult: AppResult<HabitEvent>? = null
        for (event in events) {
            val habitName = event.habitName.trim()
            if (habitName.isBlank()) continue
            val habit = event.habitId?.let { habitDao.findById(it)?.toDomain() }
                ?: habitDao.findByName(habitName)?.toDomain()
                ?: Habit(
                    id = event.habitId ?: UUID.randomUUID().toString(),
                    name = habitName.replaceFirstChar { it.uppercase() },
                    icon = "mic",
                    frequency = "Diario",
                    reminderTime = "Sin hora",
                    category = "Voz"
                ).also { habitDao.upsert(it.toEntity()) }

            lastResult = if (
                event.status == HabitStatus.Completed &&
                habit.measurement.type != MeasurementType.BOOLEAN &&
                event.quantity != null && !event.unit.isNullOrBlank()
            ) {
                recordProgress(
                    habit = habit,
                    value = event.quantity,
                    unit = event.unit,
                    aggregationMode = event.aggregationMode,
                    idempotencyKey = event.idempotencyKey,
                    source = "VOICE",
                    timestamp = event.eventTimestamp(userZoneId())
                )
            } else {
                markHabit(habit, event.status, voiceNote(event), event.eventTimestamp(userZoneId()))
            }
        }
        return lastResult
    }

    suspend fun recordProgress(
        habit: Habit,
        value: Double,
        unit: String,
        aggregationMode: AggregationMode = habit.measurement.aggregationMode,
        idempotencyKey: String? = null,
        source: String = "MANUAL",
        timestamp: Long = System.currentTimeMillis()
    ): AppResult<HabitEvent> = eventMutationMutex.withLock {
        runCatching {
            require(habit.measurement.type != MeasurementType.BOOLEAN) { "Este hábito solo admite completado o no completado." }
            idempotencyKey?.let { key ->
                eventDao.findByIdempotencyKey(key)?.toDomain()?.let { return@withLock AppResult.Success(it) }
            }
            val normalized = MeasurementNormalizer.normalize(value, unit, habit.measurement.unit)
            val zoneId = userZoneId()
            val localDate = java.time.Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
            val dayStart = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val dayEnd = localDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val prior = eventDao.forHabitOnce(habit.id)
                .map { it.toDomain() }
                .filter { it.timestamp in dayStart until dayEnd && it.normalizedValue != null }
            val total = aggregateProgress(prior + HabitEvent(
                id = "candidate",
                habitId = habit.id,
                habitName = habit.name,
                status = HabitStatus.Pending,
                timestamp = timestamp,
                value = value,
                normalizedValue = normalized.value,
                unit = normalized.unit,
                aggregationMode = aggregationMode
            ))
            val event = HabitEvent(
                id = UUID.randomUUID().toString(),
                habitId = habit.id,
                habitName = habit.name,
                status = if (total >= normalizedTarget(habit.measurement)) HabitStatus.Completed else HabitStatus.Pending,
                timestamp = timestamp,
                note = "Progreso: $value $unit",
                value = value,
                normalizedValue = normalized.value,
                unit = normalized.unit,
                aggregationMode = aggregationMode,
                idempotencyKey = idempotencyKey,
                source = source
            )
            eventDao.upsert(event.toEntity())
            recalculateStreak(habit.id, zoneId)
            event
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(it.message ?: "No se pudo registrar el progreso.", it) }
        )
    }

    suspend fun correctProgress(eventId: String, value: Double, unit: String): AppResult<HabitEvent> =
        eventMutationMutex.withLock {
            runCatching {
                val existing = eventDao.findById(eventId)?.toDomain() ?: error("No se encontró el progreso.")
                val habit = habitDao.findById(existing.habitId)?.toDomain() ?: error("No se encontró el hábito.")
                val normalized = MeasurementNormalizer.normalize(value, unit, habit.measurement.unit)
                val corrected = existing.copy(value = value, normalizedValue = normalized.value, unit = normalized.unit, synced = false)
                eventDao.upsert(corrected.toEntity())
                recalculateProgressStatusesForDay(habit, corrected.timestamp)
                recalculateStreak(habit.id, userZoneId())
                eventDao.findById(eventId)!!.toDomain()
            }.fold(
                onSuccess = { AppResult.Success(it) },
                onFailure = { AppResult.Error(it.message ?: "No se pudo corregir el progreso.", it) }
            )
        }

    private suspend fun recalculateProgressStatusesForDay(habit: Habit, timestamp: Long) {
        val zoneId = userZoneId()
        val date = java.time.Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
        val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val events = eventDao.forHabitOnce(habit.id).map { it.toDomain() }
            .filter { it.timestamp in start until end && it.normalizedValue != null }
        val complete = aggregateProgress(events) >= normalizedTarget(habit.measurement)
        events.forEach { eventDao.upsert(it.copy(status = if (complete) HabitStatus.Completed else HabitStatus.Pending).toEntity()) }
    }

    private fun voiceNote(event: VoiceEventResult): String {
        val quantity = event.quantity
        val unit = event.unit.orEmpty()
        return if (quantity != null && unit.isNotBlank()) {
            "Registrado por voz: ${quantity.toString().trimEnd('0').trimEnd('.')} $unit"
        } else {
            "Registrado por voz"
        }
    }

    suspend fun markHabit(
        habit: Habit,
        status: HabitStatus,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): AppResult<HabitEvent> = eventMutationMutex.withLock {
        val zoneId = userZoneId()
        if (status == HabitStatus.Completed) {
            val localDate = java.time.Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
            val dayStart = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val dayEnd = localDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val existing = eventDao.completedForLocalDay(habit.id, dayStart, dayEnd)?.toDomain()
            if (existing != null) {
                if (note.isNotBlank() && note != existing.note) eventDao.updateNote(existing.id, note)
                recalculateStreak(habit.id, zoneId)
                return@withLock AppResult.Success(existing.copy(note = note.ifBlank { existing.note }))
            }
        }
        val event = HabitEvent(
            id = UUID.randomUUID().toString(),
            habitId = habit.id,
            habitName = habit.name,
            status = status,
            timestamp = timestamp,
            note = note,
            synced = false
        )
        eventDao.upsert(event.toEntity())
        recalculateStreak(habit.id, zoneId)
        val syncResult = syncEvent(event)
        when (syncResult) {
            is AppResult.Success -> syncResult
            is AppResult.Error -> AppResult.Success(event)
        }
    }

    suspend fun undoEvent(eventId: String): AppResult<Unit> = eventMutationMutex.withLock {
        val event = eventDao.findById(eventId)?.toDomain()
            ?: return@withLock AppResult.Error("No encontré la acción para deshacer.")
        eventDao.deleteById(eventId)
        recalculateStreak(event.habitId, userZoneId())
        AppResult.Success(Unit)
    }

    suspend fun addNote(habit: Habit, note: String): AppResult<HabitEvent> {
        val existing = eventDao.latestForHabit(habit.id)?.toDomain()
        if (existing != null) {
            eventDao.updateNote(existing.id, note)
            return AppResult.Success(existing.copy(note = note, synced = false))
        }
        return markHabit(habit, HabitStatus.Pending, note)
    }

    private suspend fun recalculateStreak(habitId: String, zoneId: ZoneId) {
        val habit = habitDao.findById(habitId)?.toDomain() ?: return
        val events = eventDao.forHabitOnce(habitId).map { it.toDomain() }
        val versions = habitScheduleDao.versionsForHabit(habitId).map { it.toDomain() }
        val metrics = HabitStreakCalculator.calculate(habit, events, LocalDate.now(zoneId), zoneId, versions)
        habitDao.upsert(habit.copy(streak = metrics.currentStreak, bestStreak = metrics.bestStreak).toEntity())
    }

    private suspend fun userZoneId(): ZoneId {
        val configured = userProfileDao.currentOnce()?.timezone.orEmpty()
        return runCatching { ZoneId.of(configured) }.getOrDefault(ZoneId.of(DEFAULT_TIMEZONE))
    }

    suspend fun syncEvent(event: HabitEvent): AppResult<HabitEvent> =
        runNetwork {
            eventApi.create(
                GeoEventRequest(
                    eventType = event.habitName,
                    deviceId = event.habitId,
                    notes = "status=${event.status.name}; note=${event.note}",
                    metadata = mapOf(
                        "habit_id" to event.habitId,
                        "habit_name" to event.habitName,
                        "status" to event.status.name,
                        "platform" to "android",
                        "app_version" to "1.0"
                    )
                )
            )
            eventDao.markSynced(event.id)
            event.copy(synced = true)
        }

    suspend fun syncPending() {
        eventDao.unsynced().forEach { entity ->
            syncEvent(entity.toDomain())
        }
    }

    suspend fun pullRemoteHistory(): AppResult<Unit> =
        runNetwork {
            val remoteEvents = eventApi.list().map { it.toEntity() }
            eventDao.upsertAll(remoteEvents)
            val zoneId = userZoneId()
            remoteEvents.map { it.habitId }.distinct().forEach { recalculateStreak(it, zoneId) }
        }
}

private fun demoHabits(): List<Habit> = listOf(
    Habit("demo-water", "Tomar agua", "water_drop", "Diario", "10:00", "Salud", streak = 13, bestStreak = 13),
    Habit("demo-study", "Estudiar 45 minutos", "school", "Lun-Vie", "18:00", "Estudio", streak = 3, bestStreak = 6),
    Habit("demo-run", "Correr 30 minutos", "directions_run", "Mar-Jue-Sab", "19:00", "Ejercicio", streak = 1, bestStreak = 4),
    Habit("demo-read", "Leer 20 páginas", "menu_book", "Diario", "21:00", "Crecimiento", streak = 4, bestStreak = 7)
)

private fun demoHabitEvents(): List<HabitEvent> {
    val definitions = listOf(
        DemoHabitHistory("demo-water", "Tomar agua", completedDays = (0..13).filter { it != 6 }, skippedDays = listOf(6)),
        DemoHabitHistory("demo-study", "Estudiar 45 minutos", completedDays = listOf(0, 1, 2, 4, 5, 7, 8, 9, 11, 12), skippedDays = listOf(3, 6, 10, 13)),
        DemoHabitHistory("demo-run", "Correr 30 minutos", completedDays = listOf(1, 4, 8, 11), skippedDays = listOf(0, 3, 6, 10, 13)),
        DemoHabitHistory("demo-read", "Leer 20 páginas", completedDays = listOf(0, 2, 4, 6, 8, 10, 12), skippedDays = listOf(1, 5, 9, 13))
    )
    return definitions.flatMap { history ->
        val completed = history.completedDays.map { daysAgo ->
            demoEvent(history, daysAgo, HabitStatus.Completed)
        }
        val skipped = history.skippedDays.map { daysAgo ->
            demoEvent(history, daysAgo, HabitStatus.Skipped)
        }
        completed + skipped
    }
}

private fun demoEvent(history: DemoHabitHistory, daysAgo: Int, status: HabitStatus): HabitEvent {
    val timestamp = LocalDate.now()
        .minusDays(daysAgo.toLong())
        .atTime(if (history.habitId == "demo-water") 10 else 19, 0)
        .atZone(ZoneId.of("America/Lima"))
        .toInstant()
        .toEpochMilli()
    return HabitEvent(
        id = "demo-${history.habitId}-$daysAgo-${status.name.lowercase()}",
        habitId = history.habitId,
        habitName = history.habitName,
        status = status,
        timestamp = timestamp,
        note = "Dato de demostración",
        synced = true
    )
}

private data class DemoHabitHistory(
    val habitId: String,
    val habitName: String,
    val completedDays: List<Int>,
    val skippedDays: List<Int>
)

private const val DAY_MS = 86_400_000L
private const val DEFAULT_TIMEZONE = "America/Lima"

internal fun aggregateProgress(events: List<HabitEvent>): Double {
    var total = 0.0
    events.sortedWith(compareBy<HabitEvent> { it.timestamp }.thenBy { it.id }).forEach { event ->
        val value = event.normalizedValue ?: return@forEach
        total = when (event.aggregationMode ?: AggregationMode.ADD) {
            AggregationMode.ADD -> total + value
            AggregationMode.SET_TOTAL, AggregationMode.REPLACE -> value
        }
    }
    return total
}

private fun normalizedTarget(measurement: HabitMeasurement): Double =
    MeasurementNormalizer.normalize(measurement.targetValue, measurement.unit).value

private fun VoiceEventResult.eventTimestamp(zoneId: ZoneId): Long =
    date
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?.atStartOfDay(zoneId)
        ?.toInstant()
        ?.toEpochMilli()
        ?: System.currentTimeMillis()
