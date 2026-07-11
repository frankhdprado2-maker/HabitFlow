import json
from datetime import date

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.projects.c21200065.api import ai
from app.projects.c21200065.api.deps import get_current_user
from app.projects.c21200065.domain.habit_interpretation_service import (
    GeminiHabitInterpreter,
    HabitInterpretationError,
    HabitInterpretationRequest,
)


def _response(**overrides):
    payload = {
        "intent": "register_habit",
        "habits": [
            {
                "name": "Tomar agua",
                "action": "completed",
                "quantity": 2,
                "unit": "litros",
                "date": "2026-07-10",
                "notes": None,
                "existing_habit_id": None,
            }
        ],
        "confidence": 0.95,
        "needs_confirmation": False,
        "confirmation_message": "¿Deseas registrar este hábito?",
    }
    payload.update(overrides)
    return json.dumps(payload)


@pytest.mark.asyncio
async def test_interprets_single_habit_without_real_gemini() -> None:
    interpreter = GeminiHabitInterpreter(generate_json=lambda _: _response())

    result = await interpreter.interpret("Hoy tomé dos litros de agua")

    assert result.intent == "register_habit"
    assert result.habits[0].name == "Tomar agua"
    assert result.habits[0].quantity == 2


@pytest.mark.asyncio
async def test_interprets_multiple_habits_without_real_gemini() -> None:
    interpreter = GeminiHabitInterpreter(
        generate_json=lambda _: _response(
            habits=[
                {
                    "name": "Meditar",
                    "action": "completed",
                    "quantity": 10,
                    "unit": "minutos",
                    "date": "2026-07-10",
                    "notes": None,
                    "existing_habit_id": None,
                },
                {
                    "name": "Leer",
                    "action": "completed",
                    "quantity": 20,
                    "unit": "páginas",
                    "date": "2026-07-10",
                    "notes": None,
                    "existing_habit_id": None,
                },
            ],
            confirmation_message="¿Deseas registrar estos dos hábitos?",
        )
    )

    result = await interpreter.interpret("Hoy medité diez minutos y leí veinte páginas")

    assert [habit.name for habit in result.habits] == ["Meditar", "Leer"]


@pytest.mark.asyncio
async def test_prompt_uses_backend_date_and_falls_back_for_invalid_timezone() -> None:
    prompts: list[str] = []
    interpreter = GeminiHabitInterpreter(
        generate_json=lambda prompt: prompts.append(prompt) or _response(),
        clock=lambda _: date(2026, 7, 10),
    )

    await interpreter.interpret("Ayer estudié una hora", timezone="Invalid/Zone")

    assert "Fecha actual calculada por el backend: 2026-07-10" in prompts[0]
    assert '"ayer"' in prompts[0]


def test_text_validation_rejects_empty_and_too_long_text() -> None:
    with pytest.raises(ValueError):
        HabitInterpretationRequest(text=" ")
    with pytest.raises(ValueError):
        HabitInterpretationRequest(text="x" * 1001)


@pytest.mark.asyncio
async def test_invalid_gemini_response_is_rejected() -> None:
    interpreter = GeminiHabitInterpreter(
        generate_json=lambda _: _response(intent="register_habit", habits=[])
    )

    with pytest.raises(HabitInterpretationError) as error:
        await interpreter.interpret("Hoy estudié")

    assert error.value.kind == "invalid_response"


@pytest.mark.asyncio
async def test_quota_timeout_and_missing_key_errors_are_classified() -> None:
    quota = GeminiHabitInterpreter(
        generate_json=lambda _: (_ for _ in ()).throw(RuntimeError("429 quota"))
    )
    timeout = GeminiHabitInterpreter(generate_json=lambda _: (_ for _ in ()).throw(TimeoutError()))
    missing_key = GeminiHabitInterpreter(api_key="", generate_json=None)

    with pytest.raises(HabitInterpretationError) as quota_error:
        await quota.interpret("Hoy leí")
    with pytest.raises(HabitInterpretationError) as timeout_error:
        await timeout.interpret("Hoy leí")
    with pytest.raises(HabitInterpretationError) as key_error:
        await missing_key.interpret("Hoy leí")

    assert quota_error.value.kind == "quota"
    assert timeout_error.value.kind == "timeout"
    assert key_error.value.kind == "missing_api_key"


def test_interpret_habit_endpoint_requires_auth() -> None:
    app = FastAPI()
    app.include_router(ai.router)
    client = TestClient(app)

    response = client.post(
        "/ai/interpret-habit",
        json={"text": "Hoy leí", "timezone": "America/Lima"},
    )

    assert response.status_code == 401


def test_interpret_habit_endpoint_uses_authenticated_interpreter(monkeypatch) -> None:
    class FakeInterpreter:
        async def interpret(self, text: str, timezone: str):
            assert text == "Hoy leí"
            assert timezone == "America/Lima"
            return await GeminiHabitInterpreter(generate_json=lambda _: _response()).interpret(
                text,
                timezone,
            )

    app = FastAPI()
    app.dependency_overrides[get_current_user] = lambda: {
        "user_id": "user",
        "email": "user@example.com",
    }
    app.include_router(ai.router)
    monkeypatch.setattr(ai, "habit_interpreter", FakeInterpreter())
    client = TestClient(app)

    response = client.post(
        "/ai/interpret-habit",
        json={"text": "Hoy leí", "timezone": "America/Lima"},
    )

    assert response.status_code == 200
    assert response.json()["habits"][0]["name"] == "Tomar agua"
