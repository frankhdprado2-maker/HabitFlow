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

    @Query("SELECT * FROM habit_events WHERE synced = 0 ORDER BY timestamp")
    suspend fun unsynced(): List<HabitEventEntity>

    @Query("SELECT * FROM habit_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<HabitEventEntity>

    @Query("SELECT COUNT(*) FROM habit_events WHERE status = 'Completed' AND timestamp >= :dayStart")
    fun observeCompletedSince(dayStart: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: HabitEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<HabitEventEntity>)

    @Query("UPDATE habit_events SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}
