package com.unmsm.habitflow.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unmsm.habitflow.R
import com.unmsm.habitflow.data.repository.HabitRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class HabitReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val repository: HabitRepository,
    private val scheduler: HabitReminderScheduler
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val habitId = inputData.getString(HabitReminderScheduler.KEY_HABIT_ID) ?: return Result.failure()
        val habit = repository.habitById(habitId) ?: return Result.success()
        createChannel(context)
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(habitId.hashCode(), notification(context, habitId, habit.name))
        }
        scheduler.schedule(habit)
        return Result.success()
    }
}

private fun notification(context: Context, habitId: String, name: String) =
    NotificationCompat.Builder(context, REMINDER_CHANNEL)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Es hora de $name")
        .setContentText("Registra tu progreso en HabitFlow")
        .setAutoCancel(true)
        .addAction(0, "Completar", reminderIntent(context, habitId, "COMPLETE", 1))
        .addAction(0, "Posponer 15 min", reminderIntent(context, habitId, "SNOOZE", 2))
        .addAction(0, "Saltar hoy", reminderIntent(context, habitId, "SKIP", 3))
        .build()

private fun reminderIntent(context: Context, habitId: String, action: String, suffix: Int): PendingIntent =
    PendingIntent.getBroadcast(
        context, habitId.hashCode() * 10 + suffix,
        Intent(context, HabitReminderReceiver::class.java).setAction(action).putExtra(HabitReminderScheduler.KEY_HABIT_ID, habitId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

internal fun createChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(REMINDER_CHANNEL, "Recordatorios de hábitos", NotificationManager.IMPORTANCE_DEFAULT))
    }
}

private const val REMINDER_CHANNEL = "habit_reminders"
