package com.unmsm.habitflow.domain.habit

import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

enum class HabitFrequencyType {
    DAILY,
    SPECIFIC_WEEKDAYS,
    TIMES_PER_WEEK,
    INTERVAL_DAYS,
    MONTHLY_DATES,
    ONE_TIME,
    LEGACY_REVIEW
}

data class HabitFrequency(
    val type: HabitFrequencyType = HabitFrequencyType.DAILY,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val timesPerWeek: Int? = null,
    val intervalDays: Int? = null,
    val monthlyDays: Set<Int> = emptySet(),
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val timezone: String = "America/Lima",
    val active: Boolean = true,
    val needsReview: Boolean = false,
    val originalText: String = "",
    val effectiveFrom: LocalDate? = null,
    val effectiveTo: LocalDate? = null
) {
    fun isEffectiveOn(date: LocalDate): Boolean {
        val lowerBound = startDate ?: effectiveFrom
        return active && lowerBound?.let(date::isBefore) != true && endDate?.let(date::isAfter) != true &&
            effectiveTo?.let(date::isAfter) != true
    }

    fun isScheduled(date: LocalDate): Boolean {
        if (!isEffectiveOn(date)) return false
        return when (type) {
            HabitFrequencyType.DAILY -> true
            HabitFrequencyType.SPECIFIC_WEEKDAYS -> date.dayOfWeek in weekdays
            HabitFrequencyType.TIMES_PER_WEEK -> true
            HabitFrequencyType.INTERVAL_DAYS -> startDate?.let { start ->
                val interval = intervalDays?.takeIf { it > 0 } ?: return false
                !date.isBefore(start) && java.time.temporal.ChronoUnit.DAYS.between(start, date) % interval == 0L
            } ?: false
            HabitFrequencyType.MONTHLY_DATES -> date.dayOfMonth in monthlyDays
            HabitFrequencyType.ONE_TIME -> date == startDate
            HabitFrequencyType.LEGACY_REVIEW -> false
        }
    }

    fun validationError(): String? = when {
        endDate != null && startDate != null && endDate.isBefore(startDate) -> "La fecha final no puede ser anterior a la inicial."
        type == HabitFrequencyType.SPECIFIC_WEEKDAYS && weekdays.isEmpty() -> "Selecciona al menos un día de la semana."
        type == HabitFrequencyType.TIMES_PER_WEEK && timesPerWeek !in 1..7 -> "Las veces por semana deben estar entre 1 y 7."
        type == HabitFrequencyType.INTERVAL_DAYS && (intervalDays == null || intervalDays <= 0) -> "El intervalo debe ser mayor que cero."
        type == HabitFrequencyType.INTERVAL_DAYS && startDate == null -> "Selecciona una fecha inicial para el intervalo."
        type == HabitFrequencyType.MONTHLY_DATES && (monthlyDays.isEmpty() || monthlyDays.any { it !in 1..31 }) -> "Selecciona fechas mensuales entre 1 y 31."
        type == HabitFrequencyType.ONE_TIME && startDate == null -> "Selecciona la fecha del hábito."
        type == HabitFrequencyType.LEGACY_REVIEW -> "La frecuencia heredada requiere revisión."
        else -> null
    }

    fun displayText(): String = when (type) {
        HabitFrequencyType.DAILY -> "Diario"
        HabitFrequencyType.SPECIFIC_WEEKDAYS -> weekdays.sortedBy { it.value }.joinToString("-") { it.spanishShort() }
        HabitFrequencyType.TIMES_PER_WEEK -> "${timesPerWeek ?: 1} veces por semana"
        HabitFrequencyType.INTERVAL_DAYS -> "Cada ${intervalDays ?: 1} días"
        HabitFrequencyType.MONTHLY_DATES -> "Días ${monthlyDays.sorted().joinToString(", ")} de cada mes"
        HabitFrequencyType.ONE_TIME -> "Una vez${startDate?.let { ": $it" }.orEmpty()}"
        HabitFrequencyType.LEGACY_REVIEW -> originalText.ifBlank { "Revisar frecuencia" }
    }

    companion object {
        fun fromLegacy(value: String, timezone: String = "America/Lima"): HabitFrequency {
            val normalized = value.normalized()
            if (normalized in setOf("diario", "todos los dias", "cada dia")) {
                return HabitFrequency(timezone = timezone, originalText = value)
            }
            Regex("^(\\d+) veces?( por)? semana$").matchEntire(normalized)?.let { match ->
                val times = match.groupValues[1].toIntOrNull()
                if (times in 1..7) return HabitFrequency(
                    type = HabitFrequencyType.TIMES_PER_WEEK,
                    timesPerWeek = times,
                    timezone = timezone,
                    originalText = value
                )
            }
            Regex("^cada (\\d+) dias$").matchEntire(normalized)?.let { match ->
                return HabitFrequency(
                    type = HabitFrequencyType.INTERVAL_DAYS,
                    intervalDays = match.groupValues[1].toIntOrNull(),
                    timezone = timezone,
                    needsReview = true,
                    originalText = value
                )
            }
            val tokens = normalized.split("-").map(String::trim).filter(String::isNotEmpty)
            val days = if (tokens.size == 2) {
                val start = tokens[0].toDayOfWeek()
                val end = tokens[1].toDayOfWeek()
                if (start != null && end != null) generateSequence(start) { day ->
                    if (day == end) null else DayOfWeek.of(day.value % 7 + 1)
                }.toSet() else emptySet()
            } else {
                tokens.mapNotNull(String::toDayOfWeek).toSet().takeIf { it.size == tokens.size }.orEmpty()
            }
            if (days.isNotEmpty()) return HabitFrequency(
                type = HabitFrequencyType.SPECIFIC_WEEKDAYS,
                weekdays = days,
                timezone = timezone,
                originalText = value
            )
            return HabitFrequency(
                type = HabitFrequencyType.LEGACY_REVIEW,
                timezone = timezone,
                needsReview = true,
                originalText = value
            )
        }
    }
}

private fun DayOfWeek.spanishShort() = when (this) {
    DayOfWeek.MONDAY -> "Lun"
    DayOfWeek.TUESDAY -> "Mar"
    DayOfWeek.WEDNESDAY -> "Mié"
    DayOfWeek.THURSDAY -> "Jue"
    DayOfWeek.FRIDAY -> "Vie"
    DayOfWeek.SATURDAY -> "Sáb"
    DayOfWeek.SUNDAY -> "Dom"
}

private fun String.normalized(): String =
    Normalizer.normalize(lowercase(Locale.forLanguageTag("es-PE")).trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("\\s+"), " ")

private fun String.toDayOfWeek(): DayOfWeek? = when (normalized().take(3)) {
    "lun" -> DayOfWeek.MONDAY
    "mar" -> DayOfWeek.TUESDAY
    "mie" -> DayOfWeek.WEDNESDAY
    "jue" -> DayOfWeek.THURSDAY
    "vie" -> DayOfWeek.FRIDAY
    "sab" -> DayOfWeek.SATURDAY
    "dom" -> DayOfWeek.SUNDAY
    else -> null
}
