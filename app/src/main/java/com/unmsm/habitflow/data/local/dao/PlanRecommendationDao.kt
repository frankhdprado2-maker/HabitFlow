package com.unmsm.habitflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.unmsm.habitflow.data.local.entity.PlanRecommendationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanRecommendationDao {
    @Query("SELECT * FROM plan_recommendations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PlanRecommendationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: PlanRecommendationEntity)
}
