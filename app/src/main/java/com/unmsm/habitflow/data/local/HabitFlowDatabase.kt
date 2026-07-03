package com.unmsm.habitflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.unmsm.habitflow.data.local.dao.AchievementDao
import com.unmsm.habitflow.data.local.dao.HabitDao
import com.unmsm.habitflow.data.local.dao.HabitEventDao
import com.unmsm.habitflow.data.local.dao.NotificationDao
import com.unmsm.habitflow.data.local.entity.AchievementEntity
import com.unmsm.habitflow.data.local.entity.HabitEntity
import com.unmsm.habitflow.data.local.entity.HabitEventEntity
import com.unmsm.habitflow.data.local.entity.NotificationEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitEventEntity::class,
        AchievementEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HabitFlowDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitEventDao(): HabitEventDao
    abstract fun achievementDao(): AchievementDao
    abstract fun notificationDao(): NotificationDao
}
