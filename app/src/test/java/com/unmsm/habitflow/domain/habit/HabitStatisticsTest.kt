package com.unmsm.habitflow.domain.habit

import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitEvent
import com.unmsm.habitflow.domain.model.HabitStatus
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HabitStatisticsTest {
    private val zone = ZoneId.of("America/Lima")
    private val habit = Habit("h", "Leer", "book", "Diario", "08:00", "Estudio")
    private fun event(date: LocalDate, status: HabitStatus) = HabitEvent(date.toString(), "h", "Leer", status, date.atTime(8, 0).atZone(zone).toInstant().toEpochMilli())

    @Test fun `denominator uses only scheduled days and deduplicates completions`() {
        val from = LocalDate.of(2026, 7, 1)
        val stats = HabitStatisticsCalculator.calculate(habit, listOf(event(from, HabitStatus.Completed), event(from, HabitStatus.Completed)), from, from.plusDays(1), zone)
        assertEquals(2, stats.scheduledDays)
        assertEquals(1, stats.completedDays)
        assertEquals(.5, stats.completionRate, 0.0)
    }

    @Test fun `no data does not invent average or trend evidence`() {
        val date = LocalDate.of(2026, 7, 1)
        val stats = HabitStatisticsCalculator.calculate(habit, emptyList(), date, date, zone)
        assertNull(stats.dailyAverage)
        assertNull(stats.bestDay)
    }
}
