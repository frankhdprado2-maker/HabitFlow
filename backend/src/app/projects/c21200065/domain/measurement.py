from __future__ import annotations

import math
import unicodedata
from dataclasses import dataclass


@dataclass(frozen=True)
class NormalizedMeasurement:
    value: float
    unit: str


def _clean(unit: str) -> str:
    text = unicodedata.normalize("NFD", unit.strip().lower().replace(".", ""))
    return "".join(char for char in text if unicodedata.category(char) != "Mn")


def normalize_measurement(value: float, unit: str, expected_unit: str | None = None) -> NormalizedMeasurement:
    if not math.isfinite(value) or value < 0:
        raise ValueError("El progreso no puede ser negativo ni inválido.")
    conversions = {
        "ml": (1.0, "ml"), "mililitro": (1.0, "ml"), "mililitros": (1.0, "ml"),
        "l": (1000.0, "ml"), "lt": (1000.0, "ml"), "litro": (1000.0, "ml"), "litros": (1000.0, "ml"),
        "min": (1.0, "min"), "minuto": (1.0, "min"), "minutos": (1.0, "min"),
        "h": (60.0, "min"), "hora": (60.0, "min"), "horas": (60.0, "min"),
        "pagina": (1.0, "páginas"), "paginas": (1.0, "páginas"),
        "repeticion": (1.0, "repeticiones"), "repeticiones": (1.0, "repeticiones"),
        "paso": (1.0, "pasos"), "pasos": (1.0, "pasos"),
        "vez": (1.0, "unidades"), "veces": (1.0, "unidades"), "unidad": (1.0, "unidades"), "unidades": (1.0, "unidades"),
    }
    try:
        factor, normalized_unit = conversions[_clean(unit)]
    except KeyError as exc:
        raise ValueError(f"Unidad no compatible: {unit}") from exc
    if expected_unit:
        expected = normalize_measurement(1, expected_unit).unit
        if normalized_unit != expected:
            raise ValueError(f"La unidad {unit} no es compatible con {expected_unit}.")
    return NormalizedMeasurement(value * factor, normalized_unit)
