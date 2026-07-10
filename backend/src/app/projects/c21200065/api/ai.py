from typing import Literal

import httpx
from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile
from pydantic import BaseModel, Field

from app.projects.c21200065.api.deps import get_current_user
from app.projects.c21200065.domain.voice_conversation_service import (
    HabitContext,
    VoiceSession,
    handle_voice_turn,
)
from app.projects.c21200065.infra.db.redis import redis_client
from app.projects.c21200065.infra.settings import settings

router = APIRouter(prefix="/ai", tags=["ai"])

_memory_sessions: dict[str, str] = {}
_MAX_AUDIO_BYTES = 10 * 1024 * 1024


class VoiceHabitContext(BaseModel):
    id: str
    name: str
    category: str = ""


class VoiceCommandRequest(BaseModel):
    text: str
    locale: str = "es-PE"
    habits: list[VoiceHabitContext] = Field(default_factory=list)
    conversation_id: str | None = None


class VoiceEventResponse(BaseModel):
    habit_id: str | None = None
    habit_name: str
    status: Literal["completed", "skipped", "failed"]
    quantity: float | None = None
    unit: str | None = None


class VoiceCommandResponse(BaseModel):
    intent: Literal["registrar_habito", "consultar_habito", "aclaracion", "cancelar", "desconocido"]
    response: str
    habit_id: str | None = None
    habit_name: str | None = None
    status: Literal["completed", "skipped", "failed"] | None = None
    question: str | None = None
    quick_replies: list[str] = Field(default_factory=list)
    events: list[VoiceEventResponse] = Field(default_factory=list)
    conversation_id: str


class VoiceTranscriptionResponse(BaseModel):
    transcript: str
    language: str = "es"


@router.post("/transcribe", response_model=VoiceTranscriptionResponse)
async def transcribe_audio(
    audio: UploadFile = File(...),
    language: str = Form("es"),
    current_user=Depends(get_current_user),
) -> VoiceTranscriptionResponse:
    del current_user
    content = await audio.read()
    if not content:
        raise HTTPException(status_code=400, detail="Audio file is empty")
    if len(content) > _MAX_AUDIO_BYTES:
        raise HTTPException(status_code=413, detail="Audio file is too large")

    transcript = await _transcribe_with_stt_provider(
        content=content,
        filename=audio.filename or "habitflow-voice.m4a",
        content_type=audio.content_type or "audio/mp4",
        language=language,
    )
    return VoiceTranscriptionResponse(transcript=transcript, language=language)


@router.post("/voice-command", response_model=VoiceCommandResponse)
async def voice_command(
    request: VoiceCommandRequest,
    current_user=Depends(get_current_user),
) -> VoiceCommandResponse:
    conversation_id = request.conversation_id or current_user["user_id"]
    session = VoiceSession.from_json(await _get_session(conversation_id))
    result = handle_voice_turn(
        request.text,
        [HabitContext(id=item.id, name=item.name, category=item.category) for item in request.habits],
        session,
    )

    if result.clear_session:
        await _delete_session(conversation_id)
    else:
        await _set_session(conversation_id, result.session.to_json())

    events = [
        VoiceEventResponse(
            habit_id=event.habit_id,
            habit_name=event.habit_name,
            status=event.status,
            quantity=event.quantity,
            unit=event.unit,
        )
        for event in result.events
    ]
    first = events[0] if events else None
    return VoiceCommandResponse(
        intent=result.intent,
        response=result.response,
        habit_id=first.habit_id if first else None,
        habit_name=first.habit_name if first else None,
        status=first.status if first else None,
        question=result.question,
        quick_replies=result.quick_replies,
        events=events,
        conversation_id=conversation_id,
    )


async def _get_session(conversation_id: str) -> str | None:
    key = _session_key(conversation_id)
    try:
        return await redis_client.get(key)
    except Exception:
        return _memory_sessions.get(key)


async def _set_session(conversation_id: str, value: str) -> None:
    key = _session_key(conversation_id)
    try:
        await redis_client.set(key, value, ex=900)
    except Exception:
        _memory_sessions[key] = value


async def _delete_session(conversation_id: str) -> None:
    key = _session_key(conversation_id)
    try:
        await redis_client.delete(key)
    except Exception:
        _memory_sessions.pop(key, None)


def _session_key(conversation_id: str) -> str:
    return f"voice:conversation:{conversation_id}"


async def _transcribe_with_stt_provider(
    content: bytes,
    filename: str,
    content_type: str,
    language: str,
) -> str:
    api_key = settings.STT_API_KEY or settings.LLM_API_KEY
    if not api_key:
        raise HTTPException(
            status_code=503,
            detail="Missing STT_API_KEY or LLM_API_KEY for audio transcription",
        )

    base_url = settings.STT_BASE_URL.rstrip("/")
    try:
        async with httpx.AsyncClient(timeout=60) as client:
            response = await client.post(
                f"{base_url}/audio/transcriptions",
                headers={"Authorization": f"Bearer {api_key}"},
                data={
                    "model": settings.STT_MODEL,
                    "language": language,
                    "response_format": "json",
                },
                files={"file": (filename, content, content_type)},
            )
            response.raise_for_status()
    except httpx.HTTPStatusError as error:
        detail = error.response.text[:300] if error.response is not None else str(error)
        raise HTTPException(status_code=502, detail=f"Transcription provider failed: {detail}") from error
    except httpx.HTTPError as error:
        raise HTTPException(status_code=502, detail="Could not reach transcription provider") from error

    data = response.json()
    transcript = str(data.get("text") or data.get("transcript") or "").strip()
    if not transcript:
        raise HTTPException(status_code=422, detail="Transcription provider returned empty text")
    return transcript
