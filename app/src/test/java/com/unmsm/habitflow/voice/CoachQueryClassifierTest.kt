package com.unmsm.habitflow.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachQueryClassifierTest {
    @Test
    fun recognizesProgressAndPlanningQuestionsWithOrWithoutAccents() {
        assertTrue(isCoachRequest("¿Cómo voy esta semana?"))
        assertTrue(isCoachRequest("Que habito necesita atencion"))
        assertTrue(isCoachRequest("Dame un plan de hoy"))
        assertTrue(isCoachRequest("¿Qué me recomiendas para mejorar mi rutina?"))
    }

    @Test
    fun leavesHabitRegistrationsInTheStructuredRegistrationFlow() {
        assertFalse(isCoachRequest("Ya corrí treinta minutos"))
        assertFalse(isCoachRequest("Completa tomar agua"))
        assertFalse(isCoachRequest("Ayer leí veinte páginas"))
    }
}
