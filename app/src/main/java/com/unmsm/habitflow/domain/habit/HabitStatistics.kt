package com.unmsm.habitflow.domain.habit

import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitEvent
import com.unmsm.habitflow.domain.model.HabitStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HabitStatistics(
    val scheduledDays: Int,
    val completedDays: Int,
    val completionRate: Double,
    val accumulatedProgress: Double,
    val dailyAverage: Double?,
    val bestDay: LocalDate?,
    val bestHour: Int?,
    val skipReasons: List<Pair<String, Int>>
)

object HabitStatisticsCalculator {
    fun calculate(habit: Habit, events: List<HabitEvent>, from: LocalDate, to: LocalDate, zoneId: ZoneId): HabitStatistics {
        require(!to.isBefore(from))
        val dates = generateSequence(from) { it.plusDays(1).takeUnless { next -> next.isAfter(to) } }.toList()
        val scheduled = dates.filter(habit.schedule::isScheduled)
        val relevant = events.filter { it.habitId == habit.id }.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate() }
        val completed = scheduled.filter { date -> relevant[date].orEmpty().any { it.status == HabitStatus.Completed } }
        val progress = relevant.filterKeys { it in dates }.values.sumOf { daily ->
            var total = 0.0
            daily.sortedBy(HabitEvent::timestamp).forEach { event ->
                val value = event.normalizedValue ?: return@forEach
                total = when (event.aggregationMode ?: AggregationMode.ADD) {
                    AggregationMode.ADD -> total + value
                    AggregationMode.SET_TOTAL, AggregationMode.REPLACE -> value
                }
            }
            total
        }
        val completedEvents = relevant.filterKeys { it in completed }.values.flatten().filter { it.status == HabitStatus.Completed }
        val bestHour = completedEvents.groupingBy { Instant.ofEpochMilli(it.timestamp).atZone(zoneId).hour }.eachCount()
            .maxWithOrNull(compareBy<Map.Entry<Int, Int>> { it.value }.thenByDescending { it.key })?.key
        val bestDay = completed.maxByOrNull { relevant[it].orEmpty().size }
        val reasons = events.filter { it.habitId == habit.id && it.status == HabitStatus.Skipped && it.note.isNotBlank() }
            .groupingBy(HabitEvent::note).eachCount().entries.sortedByDescending { it.value }.map { it.key to it.value }
        return HabitStatistics(
            scheduledDays = scheduled.size,
            completedDays = completed.size,
            completionRate = if (scheduled.isEmpty()) 0.0 else completed.size.toDouble() / scheduled.size,
            accumulatedProgress = progress,
            dailyAverage = progress.takeIf { relevant.isNotEmpty() }?.div(relevant.size),
            bestDay = bestDay,
            bestHour = bestHour,
            skipReasons = reasons
        )
    }
}
