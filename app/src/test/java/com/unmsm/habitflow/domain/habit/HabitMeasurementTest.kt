package com.unmsm.habitflow.domain.habit

import org.junit.Assert.assertEquals
import org.junit.Test

class HabitMeasurementTest {
    @Test fun `liters normalize to milliliters`() {
        assertEquals(NormalizedMeasurement(500.0, "ml"), MeasurementNormalizer.normalize(0.5, "litros", "ml"))
    }

    @Test fun `hours normalize to minutes`() {
        assertEquals(NormalizedMeasurement(30.0, "min"), MeasurementNormalizer.normalize(0.5, "horas", "min"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative progress is rejected`() { MeasurementNormalizer.normalize(-1.0, "pasos") }

    @Test(expected = IllegalArgumentException::class)
    fun `incompatible unit is rejected`() { MeasurementNormalizer.normalize(10.0, "min", "ml") }
}
