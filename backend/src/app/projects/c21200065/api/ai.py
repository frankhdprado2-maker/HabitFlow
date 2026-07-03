import json
import re
from typing import Literal

from fastapi import APIRouter
from pydantic import BaseModel

from app.projects.c21200065.infra.clients.llm import llm_client

router = APIRouter(prefix="/ai", tags=["ai"])


class VoiceCommandRequest(BaseModel):
    text: str
    locale: str = "es-PE"


class VoiceCommandResponse(BaseModel):
    intent: Literal["registrar_habito", "consultar_habito", "desconocido"]
    response: str
    habit_id: str | None = None
    habit_name: str | None = None
    status: Literal["completed", "skipped", "failed"] | None = None


@router.post("/voice-command", response_model=VoiceCommandResponse)
async def voice_command(request: VoiceCommandRequest) -> VoiceCommandResponse:
    text = request.text.strip()
    if not text:
        return VoiceCommandResponse(intent="desconocido", response="No escuche ningun comando.")

    try:
        content = await llm_client.chat(
            messages=[
                {
                    "role": "system",
                    "content": (
                        "Eres el parser de comandos de voz de HabitFlow. "
                        "Devuelve solo JSON valido con estas claves: "
                        "intent, response, habit_id, habit_name, status. "
                        "intent debe ser registrar_habito, consultar_habito o desconocido. "
                        "status debe ser completed, skipped, failed o null. "
                        "Si el usuario dice que ya hizo algo, usa registrar_habito y completed. "
                        "Responde en espanol peruano breve."
                    ),
                },
                {"role": "user", "content": text},
            ],
            temperature=0.2,
        )
        return _parse_llm_response(content)
    except Exception:
        return _fallback_parse(text)


def _parse_llm_response(content: str) -> VoiceCommandResponse:
    cleaned = content.strip()
    match = re.search(r"\{.*\}", cleaned, flags=re.DOTALL)
    if match:
        cleaned = match.group(0)
    data = json.loads(cleaned)
    return VoiceCommandResponse(
        intent=data.get("intent") or "desconocido",
        response=data.get("response") or "Listo, entendi tu comando.",
        habit_id=data.get("habit_id"),
        habit_name=data.get("habit_name"),
        status=data.get("status"),
    )


def _fallback_parse(text: str) -> VoiceCommandResponse:
    lower = text.lower()
    if any(word in lower for word in ["faltan", "pendiente", "cuantos", "que habitos"]):
        return VoiceCommandResponse(
            intent="consultar_habito",
            response="Todavia no tengo una consulta completa conectada, pero puedo registrar habitos por voz.",
        )

    status: Literal["completed", "skipped", "failed"] = "completed"
    if any(word in lower for word in ["salte", "omitir", "omiti"]):
        status = "skipped"
    if any(word in lower for word in ["falle", "no pude", "perdi"]):
        status = "failed"

    habit_name = _extract_habit_name(lower)
    return VoiceCommandResponse(
        intent="registrar_habito",
        response=f"Listo, registre {habit_name} como {status}.",
        habit_id=habit_name.replace(" ", "_"),
        habit_name=habit_name,
        status=status,
    )


def _extract_habit_name(text: str) -> str:
    replacements = [
        "ya ",
        "hice ",
        "complete ",
        "registrar ",
        "registra ",
        "termine ",
    ]
    cleaned = text
    for value in replacements:
        cleaned = cleaned.replace(value, "")
    cleaned = cleaned.strip(" .,!¡¿?")
    return cleaned or "habito"
