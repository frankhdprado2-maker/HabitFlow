package com.unmsm.habitflow.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.unmsm.habitflow.data.repository.HabitRepository
import com.unmsm.habitflow.domain.model.HabitStatus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HabitReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: HabitRepository
    @Inject lateinit var scheduler: HabitReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val id = intent.getStringExtra(HabitReminderScheduler.KEY_HABIT_ID) ?: return pending.finish()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habit = repository.habitById(id) ?: return@launch
                when (intent.action) {
                    "COMPLETE" -> repository.markHabit(habit, HabitStatus.Completed, "Desde notificación")
                    "SKIP" -> repository.markHabit(habit, HabitStatus.Skipped, "Omitido desde notificación")
                    "SNOOZE" -> scheduler.snooze(id, 15)
                }
            } finally { pending.finish() }
        }
    }
}

@AndroidEntryPoint
class ReminderRescheduleReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: HabitRepository
    @Inject lateinit var scheduler: HabitReminderScheduler
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { repository.activeHabits().forEach(scheduler::schedule) } finally { pending.finish() }
        }
    }
}
