package com.unmsm.habitflow.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitFrequencyMigrationTest {
    @Test fun migrationFiveToSixPreservesAndBackfillsLegacyFrequency() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "frequency-migration-${System.nanoTime()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE habits (
                                id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, icon TEXT NOT NULL,
                                frequency TEXT NOT NULL, reminderTime TEXT NOT NULL, category TEXT NOT NULL,
                                isActive INTEGER NOT NULL, streak INTEGER NOT NULL, bestStreak INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE TABLE habit_events (id TEXT NOT NULL PRIMARY KEY, habitId TEXT NOT NULL, habitName TEXT NOT NULL, status TEXT NOT NULL, timestamp INTEGER NOT NULL, note TEXT NOT NULL, synced INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE achievements (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, description TEXT NOT NULL, requirement TEXT NOT NULL, unlocked INTEGER NOT NULL, xp INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE notifications (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, message TEXT NOT NULL, kind TEXT NOT NULL, timestamp INTEGER NOT NULL, read INTEGER NOT NULL)")
                        db.execSQL(
                            """
                            CREATE TABLE user_profile (
                                id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, username TEXT NOT NULL,
                                email TEXT NOT NULL, bio TEXT NOT NULL, goal TEXT NOT NULL,
                                primaryGoal TEXT NOT NULL, timezone TEXT NOT NULL, avatarUrl TEXT,
                                avatarKey TEXT, categoriesCsv TEXT NOT NULL, preferredCategoriesCsv TEXT NOT NULL,
                                onboardingCompleted INTEGER NOT NULL, themeMode TEXT NOT NULL,
                                accentTheme TEXT NOT NULL, voiceResponseEnabled INTEGER NOT NULL,
                                locale TEXT NOT NULL, profileComplete INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE TABLE plan_recommendations (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, summary TEXT NOT NULL, category TEXT NOT NULL, actionsCsv TEXT NOT NULL, createdAt INTEGER NOT NULL, accepted INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE cosmetic_rewards (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, description TEXT NOT NULL, kind TEXT NOT NULL, cost INTEGER NOT NULL, unlocked INTEGER NOT NULL, equipped INTEGER NOT NULL)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        try {
            val db = helper.writableDatabase
            db.execSQL("INSERT INTO habits VALUES ('daily', 'Agua', 'water', 'Diario', '10:00', 'Salud', 1, 4, 8)")
            db.execSQL("INSERT INTO habits VALUES ('weekdays', 'Estudiar', 'book', 'Lun-Vie', '18:00', 'Estudio', 1, 2, 5)")
            db.execSQL("INSERT INTO habits VALUES ('unknown', 'Otro', 'star', 'Cuando pueda', 'Sin hora', 'General', 1, 1, 1)")

            HabitFlowDatabase.MIGRATION_5_6.migrate(db)

            db.query("SELECT id, frequencyType, weekdaysCsv, frequencyNeedsReview, frequencyOriginal FROM habits ORDER BY id").use { cursor ->
                val rows = mutableMapOf<String, List<String>>()
                while (cursor.moveToNext()) {
                    rows[cursor.getString(0)] = listOf(cursor.getString(1), cursor.getString(2), cursor.getInt(3).toString(), cursor.getString(4))
                }
                assertEquals(listOf("DAILY", "", "0", "Diario"), rows["daily"])
                assertEquals("SPECIFIC_WEEKDAYS", rows["weekdays"]?.get(0))
                assertEquals("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY", rows["weekdays"]?.get(1))
                assertEquals(listOf("LEGACY_REVIEW", "", "1", "Cuando pueda"), rows["unknown"])
            }
            db.query("SELECT COUNT(*) FROM habit_schedule_versions").use { cursor ->
                cursor.moveToFirst()
                assertEquals(3, cursor.getInt(0))
            }
            db.execSQL("PRAGMA user_version = 6")
            helper.close()
            val migrated = Room.databaseBuilder(context, HabitFlowDatabase::class.java, name)
                .addMigrations(HabitFlowDatabase.MIGRATION_5_6)
                .build()
            try {
                migrated.openHelper.writableDatabase
            } finally {
                migrated.close()
            }
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }
}
