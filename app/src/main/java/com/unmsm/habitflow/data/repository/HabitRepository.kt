package com.unmsm.habitflow.data.repository

import android.provider.Settings
import com.unmsm.habitflow.data.local.dao.AchievementDao
import com.unmsm.habitflow.data.local.dao.HabitDao
import com.unmsm.habitflow.data.local.dao.HabitEventDao
import com.unmsm.habitflow.data.local.dao.NotificationDao
import com.unmsm.habitflow.data.remote.api.HabitEventApi
import com.unmsm.habitflow.data.remote.dto.GeoEventRequest
import com.unmsm.habitflow.data.toDomain
import com.unmsm.habitflow.data.toEntity
import com.unmsm.habitflow.domain.model.Achievement
import com.unmsm.habitflow.domain.model.AppNotification
import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitEvent
import com.unmsm.habitflow.domain.model.HabitStatus
import com.unmsm.habitflow.domain.model.NotificationKind
import com.unmsm.habitflow.domain.model.VoiceCommandResult
import com.unmsm.habitflow.domain.model.VoiceEventResult
import com.unmsm.habitflow.util.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val eventDao: HabitEventDao,
    private val achievementDao: AchievementDao,
    private val notificationDao: NotificationDao,
    private val eventApi: HabitEventApi
) {
    fun observeHabits(): Flow<List<Habit>> = habitDao.observeActive().map { habits -> habits.map { it.toDomain() } }

    suspend fun activeHabits(): List<Habit> = habitDao.activeOnce().map { it.toDomain() }

    fun observeHabit(id: String): Flow<Habit?> = habitDao.observeById(id).map { it?.toDomain() }

    fun observeEvents(): Flow<List<HabitEvent>> = eventDao.observeAll().map { events -> events.map { it.toDomain() } }

    fun observeEventsForHabit(habitId: String): Flow<List<HabitEvent>> =
        eventDao.observeForHabit(habitId).map { events -> events.map { it.toDomain() } }

    fun observeAchievements(): Flow<List<Achievement>> =
        achievementDao.observeAll().map { items -> items.map { it.toDomain() } }

    fun observeNotifications(): Flow<List<AppNotification>> =
        notificationDao.observeAll().map { items -> items.map { it.toDomain() } }

    suspend fun ensureSeedData() {
        if (habitDao.count() > 0) return
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

    suspend fun createHabit(name: String, icon: String, frequency: String, time: String, category: String) {
        habitDao.upsert(
            Habit(
                id = UUID.randomUUID().toString(),
                name = name,
                icon = icon,
                frequency = frequency,
                reminderTime = time,
                category = category
            ).toEntity()
        )
    }

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

            lastResult = markHabit(habit, event.status, voiceNote(event))
        }
        return lastResult
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

    suspend fun markHabit(habit: Habit, status: HabitStatus, note: String = ""): AppResult<HabitEvent> {
        val event = HabitEvent(
            id = UUID.randomUUID().toString(),
            habitId = habit.id,
            habitName = habit.name,
            status = status,
            timestamp = System.currentTimeMillis(),
            note = note,
            synced = false
        )
        eventDao.upsert(event.toEntity())
        return syncEvent(event)
    }

    suspend fun addNote(habit: Habit, note: String): AppResult<HabitEvent> =
        markHabit(habit, HabitStatus.Completed, note)

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
        }
}
