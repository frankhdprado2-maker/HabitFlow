package com.unmsm.habitflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.unmsm.habitflow.data.local.entity.CosmeticRewardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CosmeticRewardDao {
    @Query("SELECT * FROM cosmetic_rewards ORDER BY cost, name")
    fun observeAll(): Flow<List<CosmeticRewardEntity>>

    @Query("SELECT COUNT(*) FROM cosmetic_rewards")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rewards: List<CosmeticRewardEntity>)
}
