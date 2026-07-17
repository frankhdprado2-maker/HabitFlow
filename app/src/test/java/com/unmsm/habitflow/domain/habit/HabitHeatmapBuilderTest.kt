package com.unmsm.habitflow.domain.habit

import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitEvent
import com.unmsm.habitflow.domain.model.HabitStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitHeatmapBuilderTest {
    private val zone = ZoneId.of("America/Lima")
    private val today = LocalDate.of(2026, 7, 17)
    private val month = YearMonth.from(today)
    private val habit = Habit("habit", "Leer", "book", "Diario", "20:00", "Estudio")

    @Test fun emptyHistoryHasNoActivityAndShowsScheduledEmptyDays() {
        val heatmap = build()
        assertFalse(heatmap.hasActivity)
        assertEquals(31, heatmap.days.size)
        assertEquals(HeatmapDayState.ScheduledEmpty, heatmap.day(today.minusDays(1)).state)
    }

    @Test fun completedEventMarksItsRealLocalDate() {
        val heatmap = build(event(today, HabitStatus.Completed))
        assertTrue(heatmap.hasActivity)
        assertEquals(HeatmapDayState.Completed, heatmap.day(today).state)
        assertEquals(1f, heatmap.day(today).intensity)
    }

    @Test fun severalEventsOnTheSameDayStillProduceOneCompletedCell() {
        val heatmap = build(
            event(today, HabitStatus.Pending, 8),
            event(today, HabitStatus.Completed, 20)
        )
        assertEquals(HeatmapDayState.Completed, heatmap.day(today).state)
        assertEquals(1, heatmap.days.count { it.state == HeatmapDayState.Completed })
    }

    @Test fun pendingProgressProducesPartialIntensity() {
        val heatmap = build(event(today, HabitStatus.Pending))
        assertEquals(HeatmapDayState.Partial, heatmap.day(today).state)
        assertEquals(0.5f, heatmap.day(today).intensity)
    }

    @Test fun datesOutsideTheDisplayedMonthAreIgnored() {
        val heatmap = build(event(LocalDate.of(2026, 6, 30), HabitStatus.Completed))
        assertFalse(heatmap.hasActivity)
        assertTrue(heatmap.days.none { it.state == HeatmapDayState.Completed })
    }

    @Test fun eventOnNonScheduledDayDoesNotInventScheduledActivity() {
        val weekdays = habit.copy(frequency = "Lun-Vie", schedule = HabitFrequency.fromLegacy("Lun-Vie"))
        val saturday = LocalDate.of(2026, 7, 11)
        val heatmap = HabitHeatmapBuilder.build(weekdays, listOf(event(saturday, HabitStatus.Completed)), month, today, zone)
        assertEquals(HeatmapDayState.NotScheduled, heatmap.day(saturday).state)
        assertFalse(heatmap.hasActivity)
    }

    @Test fun rebuildingAfterUndoRemovesTheCompletedCell() {
        val completed = event(today, HabitStatus.Completed)
        assertEquals(HeatmapDayState.Completed, build(completed).day(today).state)
        assertEquals(HeatmapDayState.ScheduledEmpty, build().day(today).state)
    }

    @Test fun skippedAndFutureDatesHaveDistinctStates() {
        val heatmap = build(event(today.minusDays(1), HabitStatus.Skipped))
        assertEquals(HeatmapDayState.Skipped, heatmap.day(today.minusDays(1)).state)
        assertEquals(HeatmapDayState.Future, heatmap.day(today.plusDays(1)).state)
    }

    @Test fun scheduleVersionsPreserveHistoricalCalendarMeaning() {
        val oldDaily = HabitFrequency(
            type = HabitFrequencyType.DAILY,
            effectiveFrom = LocalDate.of(2026, 7, 1),
            effectiveTo = LocalDate.of(2026, 7, 9)
        )
        val newMondays = HabitFrequency(
            type = HabitFrequencyType.SPECIFIC_WEEKDAYS,
            weekdays = setOf(java.time.DayOfWeek.MONDAY),
            effectiveFrom = LocalDate.of(2026, 7, 10)
        )
        val heatmap = HabitHeatmapBuilder.build(
            habit.copy(schedule = newMondays),
            emptyList(),
            month,
            today,
            zone,
            listOf(oldDaily, newMondays)
        )
        assertEquals(HeatmapDayState.ScheduledEmpty, heatmap.day(LocalDate.of(2026, 7, 8)).state)
        assertEquals(HeatmapDayState.NotScheduled, heatmap.day(LocalDate.of(2026, 7, 14).plusDays(1)).state)
        assertEquals(HeatmapDayState.ScheduledEmpty, heatmap.day(LocalDate.of(2026, 7, 13)).state)
    }

    private fun build(vararg events: HabitEvent): HabitHeatmap =
        HabitHeatmapBuilder.build(habit, events.toList(), month, today, zone)

    private fun HabitHeatmap.day(date: LocalDate) = days.single { it.date == date }

    private fun event(date: LocalDate, status: HabitStatus, hour: Int = 12) = HabitEvent(
        id = UUID.randomUUID().toString(),
        habitId = habit.id,
        habitName = habit.name,
        status = status,
        timestamp = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    )
}
