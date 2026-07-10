package com.unmsm.habitflow.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.unmsm.habitflow.data.local.dao.AchievementDao
import com.unmsm.habitflow.data.local.dao.HabitDao
import com.unmsm.habitflow.data.local.dao.HabitEventDao
import com.unmsm.habitflow.data.local.dao.NotificationDao
import com.unmsm.habitflow.data.local.dao.UserProfileDao
import com.unmsm.habitflow.data.local.entity.AchievementEntity
import com.unmsm.habitflow.data.local.entity.HabitEntity
import com.unmsm.habitflow.data.local.entity.HabitEventEntity
import com.unmsm.habitflow.data.local.entity.NotificationEntity
import com.unmsm.habitflow.data.local.entity.UserProfileEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitEventEntity::class,
        AchievementEntity::class,
        NotificationEntity::class,
        UserProfileEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class HabitFlowDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitEventDao(): HabitEventDao
    abstract fun achievementDao(): AchievementDao
    abstract fun notificationDao(): NotificationDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_profile (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        username TEXT NOT NULL,
                        email TEXT NOT NULL,
                        bio TEXT NOT NULL,
                        goal TEXT NOT NULL,
                        timezone TEXT NOT NULL,
                        avatarUrl TEXT,
                        avatarKey TEXT,
                        categoriesCsv TEXT NOT NULL,
                        profileComplete INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
