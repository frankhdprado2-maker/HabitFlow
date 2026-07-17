package com.unmsm.habitflow.domain.habit

import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitEvent
import com.unmsm.habitflow.domain.model.HabitStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitStreakCalculatorTest {
    private val lima = ZoneId.of("America/Lima")
    private val today = LocalDate.of(2026, 7, 17)
    private val daily = Habit("habit", "Leer", "book", "Diario", "20:00", "Estudio")

    @Test fun duplicateCompletedEventsOnSameLocalDayCountOnce() {
        val metrics = calculate(
            event(today, HabitStatus.Completed, hour = 8),
            event(today, HabitStatus.Completed, hour = 20)
        )
        assertEquals(1, metrics.currentStreak)
        assertEquals(1, metrics.completedScheduledDays)
    }

    @Test fun noteAfterCompletionDoesNotAddToStreak() {
        val metrics = calculate(
            event(today, HabitStatus.Completed),
            event(today, HabitStatus.Pending, note = "Me sentí bien")
        )
        assertEquals(1, metrics.currentStreak)
        assertEquals(1, metrics.completedScheduledDays)
    }

    @Test fun consecutiveScheduledDaysBuildCurrentAndBestStreak() {
        val metrics = calculate(
            event(today.minusDays(2), HabitStatus.Completed),
            event(today.minusDays(1), HabitStatus.Completed),
            event(today, HabitStatus.Completed)
        )
        assertEquals(3, metrics.currentStreak)
        assertEquals(3, metrics.bestStreak)
    }

    @Test fun omittedScheduledDayBreaksTheStreak() {
        val metrics = calculate(
            event(today.minusDays(2), HabitStatus.Completed),
            event(today, HabitStatus.Completed)
        )
        assertEquals(1, metrics.currentStreak)
        assertEquals(1, metrics.bestStreak)
        assertEquals(2.0 / 3.0, metrics.completionRate, 0.0001)
    }

    @Test fun nonScheduledDayDoesNotIncreaseAWeekdayHabit() {
        val weekdays = daily.copy(frequency = "Lun-Vie", schedule = HabitFrequency.fromLegacy("Lun-Vie"))
        val saturday = LocalDate.of(2026, 7, 18)
        val metrics = HabitStreakCalculator.calculate(
            weekdays,
            listOf(event(saturday, HabitStatus.Completed)),
            saturday,
            lima
        )
        assertEquals(0, metrics.currentStreak)
        assertEquals(0, metrics.completedScheduledDays)
    }

    @Test fun pastEventRecalculatesHistoricalBestWithoutCorruptingCurrent() {
        val metrics = calculate(
            event(today.minusDays(10), HabitStatus.Completed),
            event(today.minusDays(9), HabitStatus.Completed),
            event(today, HabitStatus.Completed)
        )
        assertEquals(1, metrics.currentStreak)
        assertEquals(2, metrics.bestStreak)
    }

    @Test fun futureEventsAreIgnored() {
        val metrics = calculate(event(today.plusDays(1), HabitStatus.Completed))
        assertEquals(HabitStreakMetrics(), metrics)
    }

    @Test fun removingCompletionRecalculatesInsteadOfSubtractingBlindly() {
        val events = listOf(
            event(today.minusDays(2), HabitStatus.Completed),
            event(today.minusDays(1), HabitStatus.Completed),
            event(today, HabitStatus.Completed)
        )
        val metricsAfterUndo = HabitStreakCalculator.calculate(daily, events.filterIndexed { index, _ -> index != 1 }, today, lima)
        assertEquals(1, metricsAfterUndo.currentStreak)
        assertEquals(1, metricsAfterUndo.bestStreak)
    }

    @Test fun timezoneControlsTheCompletionDate() {
        val instantNearMidnight = LocalDate.of(2026, 7, 17)
            .atTime(LocalTime.of(2, 0))
            .atZone(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
        val event = HabitEvent("zone", daily.id, daily.name, HabitStatus.Completed, instantNearMidnight)

        val limaMetrics = HabitStreakCalculator.calculate(daily, listOf(event), LocalDate.of(2026, 7, 16), lima)
        val utcMetrics = HabitStreakCalculator.calculate(daily, listOf(event), LocalDate.of(2026, 7, 16), ZoneId.of("UTC"))

        assertEquals(1, limaMetrics.completedScheduledDays)
        assertEquals(0, utcMetrics.completedScheduledDays)
    }

    @Test fun partialProgressDoesNotCompleteTheDayUntilTargetIsReached() {
        val partial = calculate(event(today, HabitStatus.Pending, note = "10 de 20 páginas"))
        val completed = calculate(event(today, HabitStatus.Completed, note = "20 de 20 páginas"))
        assertEquals(0, partial.completedScheduledDays)
        assertEquals(1, completed.completedScheduledDays)
    }

    @Test fun timesPerWeekUsesConsecutiveSatisfiedWeeksNotDailyStreaks() {
        val weekly = daily.copy(
            frequency = "3 veces por semana",
            schedule = HabitFrequency.fromLegacy("3 veces por semana")
        )
        val events = listOf(0L, 1L, 2L, 7L, 8L, 9L).map { daysAgo ->
            event(today.minusDays(daysAgo), HabitStatus.Completed)
        }
        val metrics = HabitStreakCalculator.calculate(weekly, events, today, lima)
        assertEquals(StreakUnit.Week, metrics.unit)
        assertEquals(2, metrics.currentStreak)
        assertEquals(2, metrics.bestStreak)
    }

    private fun calculate(vararg events: HabitEvent): HabitStreakMetrics =
        HabitStreakCalculator.calculate(daily, events.toList(), today, lima)

    private fun event(
        date: LocalDate,
        status: HabitStatus,
        hour: Int = 12,
        note: String = ""
    ) = HabitEvent(
        id = UUID.randomUUID().toString(),
        habitId = daily.id,
        habitName = daily.name,
        status = status,
        timestamp = date.atTime(hour, 0).atZone(lima).toInstant().toEpochMilli(),
        note = note
    )
}
