package com.unmsm.habitflow.data.repository

import com.unmsm.habitflow.domain.habit.AggregationMode
import com.unmsm.habitflow.domain.model.HabitEvent
import com.unmsm.habitflow.domain.model.HabitStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitProgressAggregationTest {
    private fun event(value: Double, mode: AggregationMode, timestamp: Long) = HabitEvent(
        id = timestamp.toString(), habitId = "habit", habitName = "Agua",
        status = HabitStatus.Pending, timestamp = timestamp, normalizedValue = value,
        aggregationMode = mode
    )

    @Test fun `add accumulates partial progress`() {
        assertEquals(750.0, aggregateProgress(listOf(event(250.0, AggregationMode.ADD, 1), event(500.0, AggregationMode.ADD, 2))), 0.0)
    }

    @Test fun `set total replaces accumulated value`() {
        assertEquals(1000.0, aggregateProgress(listOf(event(250.0, AggregationMode.ADD, 1), event(1000.0, AggregationMode.SET_TOTAL, 2))), 0.0)
    }

    @Test fun `replace then add is deterministic`() {
        assertEquals(1100.0, aggregateProgress(listOf(event(900.0, AggregationMode.REPLACE, 2), event(200.0, AggregationMode.ADD, 3), event(500.0, AggregationMode.ADD, 1))), 0.0)
    }
}
