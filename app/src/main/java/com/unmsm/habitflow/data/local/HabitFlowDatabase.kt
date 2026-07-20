package com.unmsm.habitflow.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.unmsm.habitflow.data.local.dao.AchievementDao
import com.unmsm.habitflow.data.local.dao.CosmeticRewardDao
import com.unmsm.habitflow.data.local.dao.HabitDao
import com.unmsm.habitflow.data.local.dao.HabitEventDao
import com.unmsm.habitflow.data.local.dao.HabitScheduleDao
import com.unmsm.habitflow.data.local.dao.NotificationDao
import com.unmsm.habitflow.data.local.dao.PlanRecommendationDao
import com.unmsm.habitflow.data.local.dao.UserProfileDao
import com.unmsm.habitflow.data.local.entity.AchievementEntity
import com.unmsm.habitflow.data.local.entity.CosmeticRewardEntity
import com.unmsm.habitflow.data.local.entity.HabitEntity
import com.unmsm.habitflow.data.local.entity.HabitEventEntity
import com.unmsm.habitflow.data.local.entity.HabitScheduleVersionEntity
import com.unmsm.habitflow.data.local.entity.NotificationEntity
import com.unmsm.habitflow.data.local.entity.PlanRecommendationEntity
import com.unmsm.habitflow.data.local.entity.UserProfileEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitEventEntity::class,
        AchievementEntity::class,
        NotificationEntity::class,
        UserProfileEntity::class,
        PlanRecommendationEntity::class,
        CosmeticRewardEntity::class,
        HabitScheduleVersionEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class HabitFlowDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitEventDao(): HabitEventDao
    abstract fun habitScheduleDao(): HabitScheduleDao
    abstract fun achievementDao(): AchievementDao
    abstract fun notificationDao(): NotificationDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun planRecommendationDao(): PlanRecommendationDao
    abstract fun cosmeticRewardDao(): CosmeticRewardDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS plan_recommendations (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        summary TEXT NOT NULL,
                        category TEXT NOT NULL,
                        actionsCsv TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        accepted INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cosmetic_rewards (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        kind TEXT NOT NULL,
                        cost INTEGER NOT NULL,
                        unlocked INTEGER NOT NULL,
                        equipped INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN primaryGoal TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN preferredCategoriesCsv TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN onboardingCompleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'system'")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN accentTheme TEXT NOT NULL DEFAULT 'mint'")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN voiceResponseEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN locale TEXT NOT NULL DEFAULT 'es-PE'")
                db.execSQL("UPDATE user_profile SET primaryGoal = goal WHERE primaryGoal = ''")
                db.execSQL("UPDATE user_profile SET preferredCategoriesCsv = categoriesCsv WHERE preferredCategoriesCsv = ''")
                db.execSQL("UPDATE user_profile SET onboardingCompleted = profileComplete WHERE profileComplete = 1")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN frequencyType TEXT NOT NULL DEFAULT 'LEGACY_REVIEW'")
                db.execSQL("ALTER TABLE habits ADD COLUMN weekdaysCsv TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE habits ADD COLUMN timesPerWeek INTEGER")
                db.execSQL("ALTER TABLE habits ADD COLUMN intervalDays INTEGER")
                db.execSQL("ALTER TABLE habits ADD COLUMN monthlyDaysCsv TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE habits ADD COLUMN scheduleStartDate TEXT")
                db.execSQL("ALTER TABLE habits ADD COLUMN scheduleEndDate TEXT")
                db.execSQL("ALTER TABLE habits ADD COLUMN scheduleTimezone TEXT NOT NULL DEFAULT 'America/Lima'")
                db.execSQL("ALTER TABLE habits ADD COLUMN scheduleActive INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE habits ADD COLUMN frequencyNeedsReview INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE habits ADD COLUMN frequencyOriginal TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE habits ADD COLUMN scheduleEffectiveFrom TEXT")
                db.execSQL("UPDATE habits SET frequencyOriginal = frequency")
                db.execSQL(
                    """
                    UPDATE habits SET
                        frequencyType = CASE
                            WHEN lower(trim(frequency)) IN ('diario', 'todos los días', 'todos los dias', 'cada día', 'cada dia') THEN 'DAILY'
                            WHEN lower(trim(frequency)) = 'lun-vie' THEN 'SPECIFIC_WEEKDAYS'
                            WHEN lower(trim(frequency)) = 'mar-jue-sab' THEN 'SPECIFIC_WEEKDAYS'
                            WHEN lower(trim(frequency)) GLOB '[1-7] veces*semana' THEN 'TIMES_PER_WEEK'
                            WHEN lower(trim(frequency)) GLOB 'cada [1-9]* día*' OR lower(trim(frequency)) GLOB 'cada [1-9]* dia*' THEN 'INTERVAL_DAYS'
                            ELSE 'LEGACY_REVIEW'
                        END,
                        weekdaysCsv = CASE
                            WHEN lower(trim(frequency)) = 'lun-vie' THEN 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY'
                            WHEN lower(trim(frequency)) = 'mar-jue-sab' THEN 'TUESDAY,THURSDAY,SATURDAY'
                            ELSE ''
                        END,
                        timesPerWeek = CASE
                            WHEN lower(trim(frequency)) GLOB '[1-7] veces*semana' THEN CAST(substr(trim(frequency), 1, 1) AS INTEGER)
                            ELSE NULL
                        END,
                        intervalDays = CASE
                            WHEN lower(trim(frequency)) GLOB 'cada [1-9]* día*' OR lower(trim(frequency)) GLOB 'cada [1-9]* dia*'
                                THEN CAST(substr(trim(frequency), 6) AS INTEGER)
                            ELSE NULL
                        END,
                        frequencyNeedsReview = CASE
                            WHEN lower(trim(frequency)) IN ('diario', 'todos los días', 'todos los dias', 'cada día', 'cada dia', 'lun-vie', 'mar-jue-sab') THEN 0
                            WHEN lower(trim(frequency)) GLOB '[1-7] veces*semana' THEN 0
                            ELSE 1
                        END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS habit_schedule_versions (
                        id TEXT NOT NULL PRIMARY KEY,
                        habitId TEXT NOT NULL,
                        frequencyType TEXT NOT NULL,
                        weekdaysCsv TEXT NOT NULL,
                        timesPerWeek INTEGER,
                        intervalDays INTEGER,
                        monthlyDaysCsv TEXT NOT NULL,
                        startDate TEXT,
                        endDate TEXT,
                        timezone TEXT NOT NULL,
                        active INTEGER NOT NULL,
                        needsReview INTEGER NOT NULL,
                        originalText TEXT NOT NULL,
                        effectiveFrom TEXT,
                        effectiveTo TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_schedule_versions_habitId ON habit_schedule_versions(habitId)")
                db.execSQL(
                    """
                    INSERT INTO habit_schedule_versions (
                        id, habitId, frequencyType, weekdaysCsv, timesPerWeek, intervalDays,
                        monthlyDaysCsv, startDate, endDate, timezone, active, needsReview,
                        originalText, effectiveFrom, effectiveTo
                    ) SELECT
                        id || '-migration-v6', id, frequencyType, weekdaysCsv, timesPerWeek, intervalDays,
                        monthlyDaysCsv, scheduleStartDate, scheduleEndDate, scheduleTimezone, scheduleActive,
                        frequencyNeedsReview, frequencyOriginal, scheduleEffectiveFrom, NULL
                    FROM habits
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN measurementType TEXT NOT NULL DEFAULT 'BOOLEAN'")
                db.execSQL("ALTER TABLE habits ADD COLUMN targetValue REAL NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE habits ADD COLUMN measurementUnit TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE habits ADD COLUMN allowPartialProgress INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habits ADD COLUMN aggregationMode TEXT NOT NULL DEFAULT 'ADD'")
                db.execSQL("ALTER TABLE habit_events ADD COLUMN value REAL")
                db.execSQL("ALTER TABLE habit_events ADD COLUMN normalizedValue REAL")
                db.execSQL("ALTER TABLE habit_events ADD COLUMN unit TEXT")
                db.execSQL("ALTER TABLE habit_events ADD COLUMN aggregationMode TEXT")
                db.execSQL("ALTER TABLE habit_events ADD COLUMN idempotencyKey TEXT")
                db.execSQL("ALTER TABLE habit_events ADD COLUMN source TEXT NOT NULL DEFAULT 'MANUAL'")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habit_events_idempotencyKey ON habit_events(idempotencyKey)")
            }
        }
    }
}
