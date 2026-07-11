package com.unmsm.habitflow.voice

import java.text.Normalizer
import java.util.Locale

internal fun isCoachRequest(text: String): Boolean {
    val normalized = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .trim()
    return COACH_QUERY_MARKERS.any(normalized::contains)
}

private val COACH_QUERY_MARKERS = listOf(
    "como voy",
    "progreso",
    "resumen semanal",
    "resumen de mi semana",
    "como fue mi semana",
    "plan de hoy",
    "dame un plan",
    "organiza mi dia",
    "que me falta",
    "cuantas veces",
    "que habito necesita",
    "que habito estoy",
    "que me recomiendas",
    "mejorar mi rutina",
    "recomendacion adaptativa",
    "estadisticas",
    "mejor racha"
)
