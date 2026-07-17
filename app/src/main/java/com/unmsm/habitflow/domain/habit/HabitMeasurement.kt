package com.unmsm.habitflow.domain.habit

import java.text.Normalizer
import java.util.Locale

enum class MeasurementType { BOOLEAN, COUNT, DURATION, QUANTITY }
enum class AggregationMode { ADD, SET_TOTAL, REPLACE }

data class HabitMeasurement(
    val type: MeasurementType = MeasurementType.BOOLEAN,
    val targetValue: Double = 1.0,
    val unit: String = "",
    val allowPartialProgress: Boolean = false,
    val aggregationMode: AggregationMode = AggregationMode.ADD
) {
    fun validationError(): String? = when {
        targetValue <= 0.0 || !targetValue.isFinite() -> "La meta debe ser mayor que cero."
        type != MeasurementType.BOOLEAN && unit.isBlank() -> "Selecciona una unidad para la meta."
        else -> null
    }
}

data class NormalizedMeasurement(val value: Double, val unit: String)

object MeasurementNormalizer {
    fun normalize(value: Double, unit: String, expectedUnit: String? = null): NormalizedMeasurement {
        require(value >= 0.0 && value.isFinite()) { "El progreso no puede ser negativo ni inválido." }
        val normalized = when (unit.normalized()) {
            "ml", "mililitro", "mililitros" -> NormalizedMeasurement(value, "ml")
            "l", "lt", "litro", "litros" -> NormalizedMeasurement(value * 1_000.0, "ml")
            "min", "mins", "minuto", "minutos" -> NormalizedMeasurement(value, "min")
            "h", "hr", "hrs", "hora", "horas" -> NormalizedMeasurement(value * 60.0, "min")
            "pagina", "paginas", "pag" -> NormalizedMeasurement(value, "páginas")
            "repeticion", "repeticiones", "rep", "reps" -> NormalizedMeasurement(value, "repeticiones")
            "paso", "pasos" -> NormalizedMeasurement(value, "pasos")
            "vez", "veces", "unidad", "unidades" -> NormalizedMeasurement(value, "unidades")
            else -> throw IllegalArgumentException("Unidad no compatible: $unit")
        }
        if (!expectedUnit.isNullOrBlank()) {
            val expected = normalize(1.0, expectedUnit).unit
            require(normalized.unit == expected) { "La unidad $unit no es compatible con $expectedUnit." }
        }
        return normalized
    }
}

private fun String.normalized(): String =
    Normalizer.normalize(lowercase(Locale.forLanguageTag("es-PE")).trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(".", "")
