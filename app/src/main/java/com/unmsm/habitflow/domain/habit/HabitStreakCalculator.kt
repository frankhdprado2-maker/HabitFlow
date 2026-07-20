package com.unmsm.habitflow.domain.habit

import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitEvent
import com.unmsm.habitflow.domain.model.HabitStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

data class HabitStreakMetrics(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val completedScheduledDays: Int = 0,
    val scheduledDays: Int = 0,
    val completionRate: Double = 0.0,
    val unit: StreakUnit = StreakUnit.Day
)

enum class StreakUnit { Day, Week }

object HabitStreakCalculator {
    fun isScheduled(habit: Habit, date: LocalDate, scheduleHistory: List<HabitFrequency> = emptyList()): Boolean =
        scheduleFor(habit, date, scheduleHistory).isScheduled(date)

    fun calculate(
        habit: Habit,
        events: List<HabitEvent>,
        today: LocalDate,
        zoneId: ZoneId,
        scheduleHistory: List<HabitFrequency> = emptyList()
    ): HabitStreakMetrics {
        val schedule = habit.schedule
        val completedDates = events.asSequence()
            .filter { it.habitId == habit.id && it.status == HabitStatus.Completed }
            .map { it.localDate(zoneId) }
            .filter { !it.isAfter(today) }
            .toSet()
        if (completedDates.isEmpty() || schedule.type == HabitFrequencyType.LEGACY_REVIEW) return HabitStreakMetrics()
        return when (schedule.type) {
            HabitFrequencyType.TIMES_PER_WEEK -> weeklyMetrics(completedDates, schedule.timesPerWeek ?: return HabitStreakMetrics(), today)
            else -> dailyMetrics(habit, scheduleHistory, completedDates, today)
        }
    }

    private fun dailyMetrics(
        habit: Habit,
        scheduleHistory: List<HabitFrequency>,
        completedDates: Set<LocalDate>,
        today: LocalDate
    ): HabitStreakMetrics {
        val firstDate = completedDates.minOrNull() ?: return HabitStreakMetrics()
        val scheduled = generateSequence(firstDate) { date ->
            date.plusDays(1).takeUnless { it.isAfter(today) }
        }.filter { isScheduled(habit, it, scheduleHistory) }.toList()
        if (scheduled.isEmpty()) return HabitStreakMetrics()
        val completedScheduled = scheduled.filter(completedDates::contains).toSet()

        var best = 0
        var run = 0
        scheduled.forEach { date ->
            if (date in completedScheduled) {
                run += 1
                best = maxOf(best, run)
            } else {
                run = 0
            }
        }

        val currentCandidates = scheduled.asReversed().let { descending ->
            if (descending.firstOrNull() == today && today !in completedScheduled) descending.drop(1) else descending
        }
        val current = currentCandidates.takeWhile(completedScheduled::contains).size
        return HabitStreakMetrics(
            currentStreak = current,
            bestStreak = best,
            completedScheduledDays = completedScheduled.size,
            scheduledDays = scheduled.size,
            completionRate = completedScheduled.size.toDouble() / scheduled.size,
            unit = StreakUnit.Day
        )
    }

    private fun weeklyMetrics(
        completedDates: Set<LocalDate>,
        timesPerWeek: Int,
        today: LocalDate
    ): HabitStreakMetrics {
        val weekFields = WeekFields.ISO
        fun weekKey(date: LocalDate) = date.get(weekFields.weekBasedYear()) to date.get(weekFields.weekOfWeekBasedYear())
        val completedByWeek = completedDates.groupingBy(::weekKey).eachCount()
        val firstMonday = completedDates.minOrNull()!!.with(DayOfWeek.MONDAY)
        val currentMonday = today.with(DayOfWeek.MONDAY)
        val weeks = generateSequence(firstMonday) { monday ->
            monday.plusWeeks(1).takeUnless { it.isAfter(currentMonday) }
        }.toList()
        val satisfied = weeks.filter { completedByWeek[weekKey(it)]?.let { it >= timesPerWeek } == true }.toSet()

        var best = 0
        var run = 0
        weeks.forEach { monday ->
            if (monday in satisfied) {
                run += 1
                best = maxOf(best, run)
            } else {
                run = 0
            }
        }
        val currentCandidates = weeks.asReversed().let { descending ->
            if (descending.firstOrNull() == currentMonday && currentMonday !in satisfied) descending.drop(1) else descending
        }
        val current = currentCandidates.takeWhile(satisfied::contains).size
        val scheduledDays = weeks.size * timesPerWeek
        val completedForGoal = completedByWeek.values.sumOf { minOf(it, timesPerWeek) }
        return HabitStreakMetrics(
            currentStreak = current,
            bestStreak = best,
            completedScheduledDays = completedForGoal,
            scheduledDays = scheduledDays,
            completionRate = if (scheduledDays == 0) 0.0 else completedForGoal.toDouble() / scheduledDays,
            unit = StreakUnit.Week
        )
    }

    private fun scheduleFor(habit: Habit, date: LocalDate, history: List<HabitFrequency>): HabitFrequency =
        history.filter { it.isEffectiveOn(date) }
            .maxByOrNull { it.effectiveFrom ?: LocalDate.MIN }
            ?: habit.schedule
}

private fun HabitEvent.localDate(zoneId: ZoneId): LocalDate =
    java.time.Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
