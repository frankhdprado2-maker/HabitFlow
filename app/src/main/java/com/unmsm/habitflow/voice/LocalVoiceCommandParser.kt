package com.unmsm.habitflow.voice

import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitStatus
import com.unmsm.habitflow.domain.model.VoiceCommandResult
import com.unmsm.habitflow.domain.model.VoiceEventResult
import java.text.Normalizer
import java.util.Locale

object LocalVoiceCommandParser {
    fun parse(text: String, habits: List<Habit>): VoiceCommandResult {
        val clean = normalize(text)
        if (clean.isBlank()) {
            return clarification("No pude entender el audio. Intenta nuevamente o escribe el hábito.")
        }
        if (isPersonalNameStatement(clean)) {
            return clarification("Gracias. Para crear o registrar un hábito, dime la acción que quieres guardar.")
        }
        if (containsAny(clean, "cancelar", "cancela", "olvida", "dejalo", "déjalo")) {
            return VoiceCommandResult(intent = "cancelar", response = "Listo, cancelé la acción.")
        }
        if (containsAny(clean, "que me falta", "pendiente", "faltan hoy")) {
            val names = habits.take(4).joinToString(", ") { it.name }
            return VoiceCommandResult(
                intent = "consultar_habito",
                response = if (names.isBlank()) {
                    "Aún no tienes hábitos activos. Podemos crear uno pequeño para hoy."
                } else {
                    "Para hoy revisa: $names."
                },
                quickReplies = listOf("Crear hábito", "Registrar manualmente")
            )
        }
        if (containsAny(clean, "progreso", "como voy", "cómo voy", "estadisticas", "estadísticas")) {
            val best = habits.maxOfOrNull { it.bestStreak } ?: 0
            return VoiceCommandResult(
                intent = "consultar_habito",
                response = "Tu mejor racha local es de $best días. Completa un hábito para actualizar el progreso.",
                quickReplies = listOf("Qué me falta hoy", "Crear hábito")
            )
        }

        val status = statusFrom(clean)
        val matchedHabit = matchHabit(clean, habits)
        val quantity = quantityFrom(clean)
        if (matchedHabit != null) {
            return VoiceCommandResult(
                intent = "registrar_habito",
                response = "Voy a registrar ${matchedHabit.name}. ¿Lo confirmas?",
                habitId = matchedHabit.id,
                habitName = matchedHabit.name,
                status = status,
                quickReplies = listOf("Confirmar", "Corregir", "Cancelar"),
                events = listOf(
                    VoiceEventResult(
                        habitId = matchedHabit.id,
                        habitName = matchedHabit.name,
                        status = status,
                        quantity = quantity?.first,
                        unit = quantity?.second
                    )
                )
            )
        }

        val proposed = proposedHabitName(clean)
        if (proposed != null) {
            return VoiceCommandResult(
                intent = "registrar_habito",
                response = "Crearé el hábito $proposed. ¿Lo confirmas?",
                habitName = proposed,
                status = status,
                quickReplies = listOf("Confirmar", "Corregir", "Cancelar"),
                events = listOf(
                    VoiceEventResult(
                        habitId = null,
                        habitName = proposed,
                        status = status,
                        quantity = quantity?.first,
                        unit = quantity?.second
                    )
                )
            )
        }

        return clarification("No estoy seguro de qué hábito quieres registrar. Puedes decir: “Ya corrí treinta minutos”.")
    }

    fun isAffirmative(text: String): Boolean =
        containsAny(normalize(text), "si", "sí", "confirmar", "confirmo", "claro", "ok", "dale", "correcto")

    fun isNegativeOrCorrection(text: String): Boolean =
        containsAny(normalize(text), "no", "corrige", "corregir", "cambiar", "cancelar", "cancela")

    fun normalize(text: String): String {
        val withoutMarks = Normalizer.normalize(text.trim().lowercase(Locale("es", "PE")), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return withoutMarks
            .replace(Regex("[¡!¿?;:,.]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun clarification(message: String): VoiceCommandResult =
        VoiceCommandResult(
            intent = "aclaracion",
            response = message,
            quickReplies = listOf("Crear hábito", "Qué me falta hoy", "Cancelar")
        )

    private fun isPersonalNameStatement(text: String): Boolean =
        text.startsWith("hola soy ") ||
            text.startsWith("soy ") ||
            text.startsWith("mi nombre es ") ||
            text.startsWith("me llamo ")

    private fun statusFrom(text: String): HabitStatus =
        when {
            containsAny(text, "salta", "salte", "saltar", "omiti", "omití") -> HabitStatus.Skipped
            containsAny(text, "no pude", "falle", "fallé", "perdi", "perdí") -> HabitStatus.Failed
            containsAny(text, "crear", "crea", "quiero", "recuerdame", "recuérdame") && !containsAny(text, "complete", "ya ", "termine", "terminé") -> HabitStatus.Pending
            else -> HabitStatus.Completed
        }

    private fun matchHabit(text: String, habits: List<Habit>): Habit? {
        val withoutAddress = text.substringAfter(", ", text)
        val matches = habits.filter { habit ->
            val name = normalize(habit.name)
            val category = normalize(habit.category)
            val tokens = name.split(" ").filter { it.length >= 4 }
            name in withoutAddress || tokens.any { token -> token in withoutAddress } || (category.isNotBlank() && category in withoutAddress)
        }
        return matches.singleOrNull()
    }

    private fun proposedHabitName(text: String): String? {
        val cleaned = text.substringAfter(", ", text)
        val actionStart = listOf(
            "leer",
            "estudiar",
            "beber agua",
            "tomar agua",
            "correr",
            "caminar",
            "meditar",
            "dormir",
            "preparar tareas"
        ).mapNotNull { action ->
            val index = cleaned.indexOf(action)
            if (index >= 0) index to cleaned.substring(index) else null
        }.minByOrNull { it.first }?.second

        val candidate = actionStart
            ?: cleaned.removePrefix("quiero crear")
                .removePrefix("crear")
                .removePrefix("crea")
                .removePrefix("quiero")
                .removePrefix("recuerdame")
                .trim()

        val withoutFillers = candidate
            .replace(Regex("^(el|la|un|una|habito|hábito|de)\\s+"), "")
            .replace(Regex("\\btodos los dias\\b.*"), "")
            .replace(Regex("\\btodas las noches\\b.*"), "")
            .replace(Regex("\\ba las?\\b.*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (withoutFillers.length < 3) return null
        return withoutFillers.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale("es", "PE")) else char.toString()
        }.take(70)
    }

    private fun quantityFrom(text: String): Pair<Double, String?>? {
        val digitMatch = Regex("(\\d+(?:[.,]\\d+)?)\\s*(minutos?|horas?|paginas?|páginas?|vasos?|km|kilometros?)?").find(text)
        if (digitMatch != null) {
            return digitMatch.groupValues[1].replace(",", ".").toDouble() to digitMatch.groupValues.getOrNull(2)?.ifBlank { null }
        }
        val words = mapOf(
            "diez" to 10.0,
            "quince" to 15.0,
            "veinte" to 20.0,
            "veinticinco" to 25.0,
            "treinta" to 30.0,
            "cuarenta" to 40.0,
            "cincuenta" to 50.0,
            "sesenta" to 60.0
        )
        for ((word, value) in words) {
            val match = Regex("\\b$word\\b\\s*(minutos?|horas?|paginas?|páginas?|vasos?|km|kilometros?)?").find(text)
            if (match != null) {
                return value to match.groupValues.getOrNull(1)?.ifBlank { null }
            }
        }
        return null
    }

    private fun containsAny(text: String, vararg values: String): Boolean =
        values.any { normalize(it) in text }
}
