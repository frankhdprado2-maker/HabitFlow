from typing import Literal

from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field

from app.projects.c21200065.api.deps import get_current_user
from app.projects.c21200065.domain.voice_conversation_service import (
    HabitContext,
    VoiceSession,
    handle_voice_turn,
)
from app.projects.c21200065.infra.db.redis import redis_client

router = APIRouter(prefix="/ai", tags=["ai"])

_memory_sessions: dict[str, str] = {}


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
