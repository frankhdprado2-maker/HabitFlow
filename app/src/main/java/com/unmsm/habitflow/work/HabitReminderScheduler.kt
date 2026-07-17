package com.unmsm.habitflow.work

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.unmsm.habitflow.domain.model.Habit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitReminderScheduler @Inject constructor(@ApplicationContext context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun schedule(habit: Habit, now: Instant = Instant.now()) {
        val next = nextReminder(habit, now) ?: run { cancel(habit.id); return }
        val request = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInitialDelay(Duration.between(now, next).coerceAtLeast(Duration.ZERO).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString(KEY_HABIT_ID, habit.id).build())
            .addTag(tag(habit.id))
            .build()
        workManager.enqueueUniqueWork(tag(habit.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun snooze(habitId: String, minutes: Long) {
        val request = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInitialDelay(minutes, TimeUnit.MINUTES)
            .setInputData(Data.Builder().putString(KEY_HABIT_ID, habitId).build())
            .addTag(tag(habitId)).build()
        workManager.enqueueUniqueWork(tag(habitId), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(habitId: String) = workManager.cancelUniqueWork(tag(habitId))

    companion object {
        const val KEY_HABIT_ID = "habit_id"
        fun tag(habitId: String) = "habit-reminder-$habitId"
    }
}

internal fun nextReminder(habit: Habit, now: Instant): Instant? {
    if (!habit.isActive || habit.reminderTime == "Sin hora") return null
    val time = runCatching { LocalTime.parse(habit.reminderTime) }.getOrNull() ?: return null
    val zone = runCatching { ZoneId.of(habit.schedule.timezone) }.getOrDefault(ZoneId.of("America/Lima"))
    var date = now.atZone(zone).toLocalDate()
    repeat(367) {
        val candidate = date.atTime(time).atZone(zone).toInstant()
        if (candidate > now && habit.schedule.isScheduled(date)) return candidate
        date = date.plusDays(1)
    }
    return null
}
