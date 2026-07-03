package com.unmsm.habitflow.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unmsm.habitflow.data.repository.HabitRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncHabitEventsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val habitRepository: HabitRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        runCatching {
            habitRepository.syncPending()
            Result.success()
        }.getOrElse {
            Result.retry()
        }
}
