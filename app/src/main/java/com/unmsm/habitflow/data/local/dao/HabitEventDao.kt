package com.unmsm.habitflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.unmsm.habitflow.data.local.entity.HabitEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitEventDao {
    @Query("SELECT * FROM habit_events ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<HabitEventEntity>>

    @Query("SELECT * FROM habit_events WHERE habitId = :habitId ORDER BY timestamp DESC")
    fun observeForHabit(habitId: String): Flow<List<HabitEventEntity>>

    @Query("SELECT * FROM habit_events WHERE habitId = :habitId ORDER BY timestamp DESC")
    suspend fun forHabitOnce(habitId: String): List<HabitEventEntity>

    @Query("SELECT * FROM habit_events WHERE habitId = :habitId ORDER BY timestamp DESC LIMIT 1")
    suspend fun latestForHabit(habitId: String): HabitEventEntity?

    @Query("SELECT * FROM habit_events WHERE habitId = :habitId AND status = 'Completed' AND timestamp >= :start AND timestamp < :end ORDER BY timestamp LIMIT 1")
    suspend fun completedForLocalDay(habitId: String, start: Long, end: Long): HabitEventEntity?

    @Query("SELECT * FROM habit_events WHERE synced = 0 ORDER BY timestamp")
    suspend fun unsynced(): List<HabitEventEntity>

    @Query("SELECT * FROM habit_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<HabitEventEntity>

    @Query("SELECT * FROM habit_events WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): HabitEventEntity?

    @Query("SELECT * FROM habit_events WHERE idempotencyKey = :key LIMIT 1")
    suspend fun findByIdempotencyKey(key: String): HabitEventEntity?

    @Query("SELECT COUNT(*) FROM habit_events")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM habit_events WHERE status = 'Completed' AND timestamp >= :dayStart")
    fun observeCompletedSince(dayStart: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: HabitEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<HabitEventEntity>)

    @Query("UPDATE habit_events SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE habit_events SET note = :note, synced = 0 WHERE id = :id")
    suspend fun updateNote(id: String, note: String)

    @Query("DELETE FROM habit_events WHERE id = :id")
    suspend fun deleteById(id: String)
}
