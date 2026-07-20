package com.unmsm.habitflow.domain.habit

import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitEvent
import com.unmsm.habitflow.domain.model.HabitStatus
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class HeatmapDayState {
    NotScheduled,
    ScheduledEmpty,
    Partial,
    Completed,
    Skipped,
    Future
}

data class HeatmapDay(
    val date: LocalDate,
    val state: HeatmapDayState,
    val intensity: Float
)

data class HabitHeatmap(
    val days: List<HeatmapDay> = emptyList(),
    val hasActivity: Boolean = false
)

object HabitHeatmapBuilder {
    fun build(
        habit: Habit,
        events: List<HabitEvent>,
        month: YearMonth,
        today: LocalDate,
        zoneId: ZoneId,
        scheduleHistory: List<HabitFrequency> = emptyList()
    ): HabitHeatmap {
        val eventsByDate = events.asSequence()
            .filter { it.habitId == habit.id }
            .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate() }
        val days = (1..month.lengthOfMonth()).map { dayOfMonth ->
            val date = month.atDay(dayOfMonth)
            val dayEvents = eventsByDate[date].orEmpty()
            val state = when {
                date.isAfter(today) -> HeatmapDayState.Future
                !HabitStreakCalculator.isScheduled(habit, date, scheduleHistory) -> HeatmapDayState.NotScheduled
                dayEvents.any { it.status == HabitStatus.Completed } -> HeatmapDayState.Completed
                dayEvents.any { it.status == HabitStatus.Pending } -> HeatmapDayState.Partial
                dayEvents.any { it.status == HabitStatus.Skipped || it.status == HabitStatus.Failed } -> HeatmapDayState.Skipped
                else -> HeatmapDayState.ScheduledEmpty
            }
            HeatmapDay(date, state, state.intensity())
        }
        return HabitHeatmap(
            days = days,
            hasActivity = days.any { it.state in activityStates }
        )
    }
}

private val activityStates = setOf(HeatmapDayState.Partial, HeatmapDayState.Completed, HeatmapDayState.Skipped)

private fun HeatmapDayState.intensity(): Float = when (this) {
    HeatmapDayState.Completed -> 1f
    HeatmapDayState.Partial -> 0.5f
    HeatmapDayState.Skipped -> 0.2f
    else -> 0f
}
