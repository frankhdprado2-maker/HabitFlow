package com.unmsm.habitflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.unmsm.habitflow.data.local.entity.HabitScheduleVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitScheduleDao {
    @Query("SELECT * FROM habit_schedule_versions WHERE habitId = :habitId ORDER BY effectiveFrom")
    fun observeVersionsForHabit(habitId: String): Flow<List<HabitScheduleVersionEntity>>

    @Query("SELECT * FROM habit_schedule_versions WHERE habitId = :habitId ORDER BY effectiveFrom")
    suspend fun versionsForHabit(habitId: String): List<HabitScheduleVersionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(version: HabitScheduleVersionEntity)

    @Query("UPDATE habit_schedule_versions SET effectiveTo = :effectiveTo WHERE habitId = :habitId AND effectiveTo IS NULL")
    suspend fun closeCurrentVersion(habitId: String, effectiveTo: String)
}
