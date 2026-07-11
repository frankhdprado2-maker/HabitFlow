import asyncio
import json
import re
from collections.abc import Callable
from datetime import date, datetime, timedelta, tzinfo
from datetime import timezone as fixed_timezone
from typing import Literal, NoReturn
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.projects.c21200065.infra.settings import settings

Intent = Literal["register_habit", "create_habit", "plan_habit", "query_habit", "unknown"]
Action = Literal["completed", "planned", "created", "unknown"]


class HabitInterpretationError(Exception):
    def __init__(self, kind: str, message: str) -> None:
        super().__init__(message)
        self.kind = kind
        self.message = message


class HabitInterpretationRequest(BaseModel):
    text: str = Field(min_length=2, max_length=1000)
    timezone: str = "America/Lima"

    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    @field_validator("text")
    @classmethod
    def validate_text(cls, value: str) -> str:
        clean = " ".join(value.split())
        if len(clean) < 2:
            raise ValueError("text must contain at least 2 characters")
        return clean

    @field_validator("timezone")
    @classmethod
    def validate_timezone(cls, value: str) -> str:
        return value.strip() or "America/Lima"


class InterpretedHabit(BaseModel):
    name: str = Field(min_length=1, max_length=80)
    action: Action
    quantity: float | None = Field(default=None, gt=0)
    unit: str | None = Field(default=None, max_length=40)
    date: date
    notes: str | None = Field(default=None, max_length=220)
    existing_habit_id: str | None = Field(default=None, max_length=120)

    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    @field_validator("name")
    @classmethod
    def validate_name(cls, value: str) -> str:
        clean = " ".join(value.split())
        if not clean:
            raise ValueError("habit name is required")
        return clean

    @field_validator("date", mode="before")
    @classmethod
    def validate_iso_date(cls, value: object) -> object:
        if isinstance(value, str) and not re.fullmatch(r"\d{4}-\d{2}-\d{2}", value):
            raise ValueError("date must use YYYY-MM-DD format")
        return value

    @field_validator("unit", "notes", "existing_habit_id")
    @classmethod
    def blank_to_none(cls, value: str | None) -> str | None:
        if value is None:
            return None
        clean = " ".join(value.split())
        return clean or None


class HabitInterpretationResponse(BaseModel):
    intent: Intent
    habits: list[InterpretedHabit] = Field(default_factory=list)
    confidence: float = Field(ge=0, le=1)
    needs_confirmation: bool = True
    confirmation_message: str = Field(min_length=1, max_length=220)

    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    @model_validator(mode="after")
    def validate_habits_for_registration(self) -> "HabitInterpretationResponse":
        if self.intent in {"register_habit", "create_habit", "plan_habit"} and not self.habits:
            raise ValueError("registration intents require at least one habit")
        return self


GenerateJson = Callable[[str], str]
Clock = Callable[[tzinfo], date]


class GeminiHabitInterpreter:
    def __init__(
        self,
        api_key: str | None = None,
        model: str | None = None,
        generate_json: GenerateJson | None = None,
        clock: Clock | None = None,
        timeout_seconds: int = 30,
    ) -> None:
        self.api_key = api_key if api_key is not None else settings.GEMINI_API_KEY
        self.model = model or settings.GEMINI_MODEL
        self.generate_json = generate_json
        self.clock = clock or (lambda tz: datetime.now(tz).date())
        self.timeout_seconds = timeout_seconds

    async def interpret(
        self,
        text: str,
        timezone: str = "America/Lima",
    ) -> HabitInterpretationResponse:
        request = HabitInterpretationRequest(text=text, timezone=timezone)
        tz = self._resolve_timezone(request.timezone)
        current_date = self.clock(tz)
        prompt = self._build_prompt(request.text, request.timezone, current_date)
        raw_text = await self._generate(prompt)
        payload = self._parse_json(raw_text)
        try:
            return HabitInterpretationResponse.model_validate(payload)
        except Exception as error:
            raise HabitInterpretationError(
                "invalid_response",
                "Gemini returned a response that does not match HabitFlow schema",
            ) from error

    async def _generate(self, prompt: str) -> str:
        if self.generate_json is not None:
            try:
                return self.generate_json(prompt)
            except TimeoutError as error:
                raise HabitInterpretationError(
                    "timeout",
                    "Gemini habit interpretation timed out",
                ) from error
            except HabitInterpretationError:
                raise
            except Exception as error:
                self._raise_provider_error(error)
        if not self.api_key:
            raise HabitInterpretationError(
                "missing_api_key",
                "Missing GEMINI_API_KEY for habit interpretation",
            )
        try:
            async with asyncio.timeout(self.timeout_seconds):
                return await asyncio.to_thread(self._generate_with_gemini, prompt)
        except TimeoutError as error:
            raise HabitInterpretationError(
                "timeout",
                "Gemini habit interpretation timed out",
            ) from error
        except HabitInterpretationError:
            raise
        except Exception as error:
            self._raise_provider_error(error)

    def _raise_provider_error(self, error: Exception) -> NoReturn:
        message = str(error)
        lowered = message.lower()
        if "quota" in lowered or "429" in lowered or "resource_exhausted" in lowered:
            raise HabitInterpretationError(
                "quota",
                "Gemini quota was exhausted",
            ) from error
        raise HabitInterpretationError(
            "provider_error",
            "Could not reach Gemini for habit interpretation",
        ) from error

    def _generate_with_gemini(self, prompt: str) -> str:
        from google import genai
        from google.genai import types

        client = genai.Client(api_key=self.api_key)
        response = client.models.generate_content(
            model=self.model,
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                temperature=0.1,
            ),
        )
        text = (response.text or "").strip()
        if not text:
            raise HabitInterpretationError(
                "invalid_response",
                "Gemini returned an empty response",
            )
        return text

    def _build_prompt(self, text: str, timezone: str, current_date: date) -> str:
        return f"""
Eres el asistente de HabitFlow, una aplicación móvil para registrar hábitos.

Interpreta mensajes escritos o transcritos por voz y conviértelos en información estructurada.

Fecha actual calculada por el backend: {current_date.isoformat()}
Zona horaria solicitada por el backend: {timezone}

Reglas:
1. Detecta si el usuario completó, planeó o quiere crear un hábito.
2. Puede existir más de un hábito en una misma frase.
3. Usa nombres breves y claros en español.
4. Convierte "hoy", "ayer" y "mañana" a fechas ISO usando la fecha actual proporcionada.
5. Gemini no debe decidir cuál es la fecha actual.
6. No inventes cantidades, unidades, fechas ni notas.
   Si infieres fecha de una acción inmediata, marca needs_confirmation=true.
7. Si existe ambigüedad, establece needs_confirmation=true.
8. No afirmes que el hábito fue guardado.
9. Solo interpreta la intención.
10. Devuelve exclusivamente JSON compatible con el esquema solicitado.

Valores permitidos para intent: register_habit, create_habit, plan_habit, query_habit, unknown.
Valores permitidos para action: completed, planned, created, unknown.

Esquema JSON:
{{
  "intent": "register_habit",
  "habits": [
    {{
      "name": "Tomar agua",
      "action": "completed",
      "quantity": 2,
      "unit": "litros",
      "date": "{current_date.isoformat()}",
      "notes": null,
      "existing_habit_id": null
    }}
  ],
  "confidence": 0.95,
  "needs_confirmation": true,
  "confirmation_message": "¿Deseas registrar este hábito?"
}}

Texto del usuario:
{text}
""".strip()

    def _parse_json(self, raw_text: str) -> dict:
        clean = raw_text.strip()
        if clean.startswith("```"):
            clean = clean.strip("`").strip()
            if clean.startswith("json"):
                clean = clean[4:].strip()
        if not clean.startswith("{"):
            start = clean.find("{")
            end = clean.rfind("}")
            if start >= 0 and end > start:
                clean = clean[start : end + 1]
        try:
            payload = json.loads(clean)
        except json.JSONDecodeError as error:
            raise HabitInterpretationError(
                "invalid_response",
                "Gemini returned invalid JSON",
            ) from error
        if not isinstance(payload, dict):
            raise HabitInterpretationError(
                "invalid_response",
                "Gemini returned a non-object JSON response",
            )
        return payload

    def _resolve_timezone(self, timezone: str) -> tzinfo:
        try:
            return ZoneInfo(timezone)
        except ZoneInfoNotFoundError:
            try:
                return ZoneInfo("America/Lima")
            except ZoneInfoNotFoundError:
                return fixed_timezone(timedelta(hours=-5), "America/Lima")
