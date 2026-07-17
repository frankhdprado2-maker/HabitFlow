package com.unmsm.habitflow.domain.habit

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitFrequencyTest {
    @Test fun migratesKnownLegacyDailyWeekdaysAndWeeklyValues() {
        assertEquals(HabitFrequencyType.DAILY, HabitFrequency.fromLegacy("Diario").type)
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            HabitFrequency.fromLegacy("Lun-Vie").weekdays
        )
        assertEquals(3, HabitFrequency.fromLegacy("3 veces por semana").timesPerWeek)
    }

    @Test fun ambiguousLegacyValueIsPreservedForReview() {
        val migrated = HabitFrequency.fromLegacy("Cuando tenga tiempo")
        assertEquals(HabitFrequencyType.LEGACY_REVIEW, migrated.type)
        assertTrue(migrated.needsReview)
        assertEquals("Cuando tenga tiempo", migrated.originalText)
    }

    @Test fun supportsEveryStructuredScheduleTypeDeterministically() {
        val monday = LocalDate.of(2026, 7, 13)
        assertTrue(HabitFrequency().isScheduled(monday))
        assertTrue(HabitFrequency(HabitFrequencyType.SPECIFIC_WEEKDAYS, weekdays = setOf(DayOfWeek.MONDAY)).isScheduled(monday))
        assertFalse(HabitFrequency(HabitFrequencyType.SPECIFIC_WEEKDAYS, weekdays = setOf(DayOfWeek.TUESDAY)).isScheduled(monday))
        assertTrue(
            HabitFrequency(
                HabitFrequencyType.INTERVAL_DAYS,
                intervalDays = 3,
                startDate = LocalDate.of(2026, 7, 10)
            ).isScheduled(monday)
        )
        assertTrue(HabitFrequency(HabitFrequencyType.MONTHLY_DATES, monthlyDays = setOf(13)).isScheduled(monday))
        assertTrue(HabitFrequency(HabitFrequencyType.ONE_TIME, startDate = monday).isScheduled(monday))
        assertFalse(HabitFrequency(HabitFrequencyType.ONE_TIME, startDate = monday).isScheduled(monday.plusDays(1)))
    }

    @Test fun startEndAndActiveBoundsAreApplied() {
        val schedule = HabitFrequency(
            startDate = LocalDate.of(2026, 7, 10),
            endDate = LocalDate.of(2026, 7, 20)
        )
        assertFalse(schedule.isScheduled(LocalDate.of(2026, 7, 9)))
        assertTrue(schedule.isScheduled(LocalDate.of(2026, 7, 15)))
        assertFalse(schedule.isScheduled(LocalDate.of(2026, 7, 21)))
        assertFalse(schedule.copy(active = false).isScheduled(LocalDate.of(2026, 7, 15)))
    }

    @Test fun validatesRequiredFieldsWithoutGuessing() {
        assertTrue(HabitFrequency(HabitFrequencyType.SPECIFIC_WEEKDAYS).validationError()!!.isNotBlank())
        assertTrue(HabitFrequency(HabitFrequencyType.TIMES_PER_WEEK, timesPerWeek = 8).validationError()!!.isNotBlank())
        assertTrue(HabitFrequency(HabitFrequencyType.INTERVAL_DAYS, intervalDays = 2).validationError()!!.isNotBlank())
        assertTrue(HabitFrequency(HabitFrequencyType.MONTHLY_DATES, monthlyDays = setOf(0)).validationError()!!.isNotBlank())
        assertTrue(HabitFrequency(HabitFrequencyType.ONE_TIME).validationError()!!.isNotBlank())
        assertNull(HabitFrequency(HabitFrequencyType.TIMES_PER_WEEK, timesPerWeek = 4).validationError())
    }
}
