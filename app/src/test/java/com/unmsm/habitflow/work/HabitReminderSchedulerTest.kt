package com.unmsm.habitflow.work

import com.unmsm.habitflow.domain.habit.HabitFrequency
import com.unmsm.habitflow.domain.habit.HabitFrequencyType
import com.unmsm.habitflow.domain.model.Habit
import java.time.DayOfWeek
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HabitReminderSchedulerTest {
    @Test fun `uses next scheduled weekday without duplicate work identity`() {
        val habit = Habit("h", "Leer", "book", "Lun", "08:00", "Estudio", schedule = HabitFrequency(
            type = HabitFrequencyType.SPECIFIC_WEEKDAYS, weekdays = setOf(DayOfWeek.MONDAY), timezone = "America/Lima"
        ))
        assertEquals(Instant.parse("2026-07-20T13:00:00Z"), nextReminder(habit, Instant.parse("2026-07-17T12:00:00Z")))
        assertEquals("habit-reminder-h", HabitReminderScheduler.tag("h"))
    }
    @Test fun `disabled reminder is not scheduled`() {
        assertNull(nextReminder(Habit("h", "Leer", "book", "Diario", "Sin hora", "Estudio"), Instant.now()))
    }
}
