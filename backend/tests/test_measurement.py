import pytest

from app.projects.c21200065.domain.measurement import normalize_measurement


def test_normalizes_liters_and_written_quantity_from_interpreter() -> None:
    result = normalize_measurement(0.5, "litros", "ml")
    assert result.value == 500
    assert result.unit == "ml"


def test_normalizes_hours() -> None:
    assert normalize_measurement(1, "hora", "min").value == 60


@pytest.mark.parametrize("value", [-1, float("nan"), float("inf")])
def test_rejects_invalid_values(value: float) -> None:
    with pytest.raises(ValueError):
        normalize_measurement(value, "pasos")


def test_rejects_incompatible_units() -> None:
    with pytest.raises(ValueError):
        normalize_measurement(10, "minutos", "ml")
