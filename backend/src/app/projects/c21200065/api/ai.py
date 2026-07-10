from typing import Annotated, Any, Literal

import httpx
from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile
from pydantic import BaseModel, Field

from app.projects.c21200065.api.deps import get_current_user
from app.projects.c21200065.domain.voice_conversation_service import (
    AchievementContext,
    ConversationContext,
    HabitContext,
    HabitEventContext,
    VoiceSession,
    handle_voice_turn,
)
from app.projects.c21200065.infra.db.redis import redis_client
from app.projects.c21200065.infra.settings import settings

router = APIRouter(prefix="/ai", tags=["ai"])
CurrentUserDep = Annotated[dict, Depends(get_current_user)]
AudioFile = Annotated[UploadFile, File()]
LanguageForm = Annotated[str, Form()]

_memory_sessions: dict[str, str] = {}
_MAX_AUDIO_BYTES = 10 * 1024 * 1024


class VoiceHabitContext(BaseModel):
    id: str
    name: str
    category: str = ""


class VoiceEventContext(BaseModel):
    habit_id: str
    habit_name: str
    status: str
    timestamp: int


class VoiceAchievementContext(BaseModel):
    id: str
    title: str
    description: str = ""
    requirement: str = ""
    unlocked: bool = False
    xp: int = 0


class VoiceCommandRequest(BaseModel):
    text: str
    locale: str = "es-PE"
    habits: list[VoiceHabitContext] = Field(default_factory=list)
    recent_events: list[VoiceEventContext] = Field(default_factory=list)
    achievements: list[VoiceAchievementContext] = Field(default_factory=list)
    categories: list[str] = Field(default_factory=list)
    conversation_id: str | None = None


class VoiceEventResponse(BaseModel):
    habit_id: str | None = None
    habit_name: str
    status: Literal["completed", "skipped", "failed"]
    quantity: float | None = None
    unit: str | None = None


class VoicePlanResponse(BaseModel):
    title: str
    summary: str
    category: str
    actions: list[str] = Field(default_factory=list)


class VoiceCommandResponse(BaseModel):
    intent: Literal[
        "registrar_habito",
        "consultar_habito",
        "plan_recomendacion",
        "aclaracion",
        "cancelar",
        "desconocido",
    ]
    response: str
    habit_id: str | None = None
    habit_name: str | None = None
    status: Literal["completed", "skipped", "failed"] | None = None
    question: str | None = None
    quick_replies: list[str] = Field(default_factory=list)
    events: list[VoiceEventResponse] = Field(default_factory=list)
    plan: VoicePlanResponse | None = None
    conversation_id: str


class ConversationUserContext(BaseModel):
    first_name: str | None = None
    existing_habits: list[VoiceHabitContext] = Field(default_factory=list)


class ConversationAction(BaseModel):
    type: Literal[
        "CREATE_HABIT",
        "COMPLETE_HABIT",
        "SKIP_HABIT",
        "UPDATE_HABIT",
        "QUERY_TODAY",
        "QUERY_PROGRESS",
        "CANCEL",
        "UNKNOWN",
    ]
    payload: dict[str, Any] = Field(default_factory=dict)


class VoiceConversationRequest(BaseModel):
    session_id: str | None = None
    text: str
    locale: str = "es-PE"
    timezone: str = "America/Lima"
    user_context: ConversationUserContext = Field(default_factory=ConversationUserContext)
    pending_action: ConversationAction | None = None


class VoiceConversationResponse(BaseModel):
    session_id: str
    assistant_message: str
    intent: Literal[
        "CREATE_HABIT",
        "COMPLETE_HABIT",
        "SKIP_HABIT",
        "UPDATE_HABIT",
        "QUERY_TODAY",
        "QUERY_PROGRESS",
        "CANCEL",
        "UNKNOWN",
    ]
    confidence: float = 0.0
    action: ConversationAction | None = None
    missing_fields: list[str] = Field(default_factory=list)
    requires_confirmation: bool = False
    suggestions: list[str] = Field(default_factory=list)


class VoiceTranscriptionResponse(BaseModel):
    transcript: str
    language: str = "es"


class VoiceTranscriptionStatusResponse(BaseModel):
    configured: bool
    provider: str
    model: str


@router.get("/transcription-status", response_model=VoiceTranscriptionStatusResponse)
async def transcription_status(current_user: CurrentUserDep) -> VoiceTranscriptionStatusResponse:
    del current_user
    return VoiceTranscriptionStatusResponse(
        configured=bool(settings.STT_API_KEY),
        provider=settings.STT_BASE_URL.rstrip("/"),
        model=settings.STT_MODEL,
    )


@router.post("/transcribe", response_model=VoiceTranscriptionResponse)
async def transcribe_audio(
    audio: AudioFile,
    current_user: CurrentUserDep,
    language: LanguageForm = "es",
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
    current_user: CurrentUserDep,
) -> VoiceCommandResponse:
    conversation_id = request.conversation_id or current_user["user_id"]
    session = VoiceSession.from_json(await _get_session(conversation_id))
    habit_contexts = [
        HabitContext(id=item.id, name=item.name, category=item.category)
        for item in request.habits
    ]
    result = handle_voice_turn(
        request.text,
        habit_contexts,
        session,
        ConversationContext(
            events=[
                HabitEventContext(
                    habit_id=item.habit_id,
                    habit_name=item.habit_name,
                    status=item.status,
                    timestamp=item.timestamp,
                )
                for item in request.recent_events
            ],
            achievements=[
                AchievementContext(
                    id=item.id,
                    title=item.title,
                    description=item.description,
                    requirement=item.requirement,
                    unlocked=item.unlocked,
                    xp=item.xp,
                )
                for item in request.achievements
            ],
            categories=request.categories,
        ),
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
    plan = (
        VoicePlanResponse(
            title=result.plan.title,
            summary=result.plan.summary,
            category=result.plan.category,
            actions=result.plan.actions,
        )
        if result.plan
        else None
    )
    return VoiceCommandResponse(
        intent=result.intent,
        response=result.response,
        habit_id=first.habit_id if first else None,
        habit_name=first.habit_name if first else None,
        status=first.status if first else None,
        question=result.question,
        quick_replies=result.quick_replies,
        events=events,
        plan=plan,
        conversation_id=conversation_id,
    )


@router.post("/conversation", response_model=VoiceConversationResponse)
async def voice_conversation(
    request: VoiceConversationRequest,
    current_user: CurrentUserDep,
) -> VoiceConversationResponse:
    session_id = request.session_id or current_user["user_id"]
    session = VoiceSession.from_json(await _get_session(session_id))
    habit_contexts = [
        HabitContext(id=item.id, name=item.name, category=item.category)
        for item in request.user_context.existing_habits
    ]
    result = handle_voice_turn(
        request.text,
        habit_contexts,
        session,
        ConversationContext(),
    )

    if result.clear_session:
        await _delete_session(session_id)
    else:
        await _set_session(session_id, result.session.to_json())

    action = _conversation_action(result)
    return VoiceConversationResponse(
        session_id=session_id,
        assistant_message=result.response,
        intent=action.type if action else _conversation_intent(result.intent, request.text),
        confidence=_conversation_confidence(result.intent, action),
        action=action,
        missing_fields=[] if action else _missing_fields(result),
        requires_confirmation=action is not None,
        suggestions=result.quick_replies,
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


def _conversation_action(result) -> ConversationAction | None:
    if not result.events:
        return None
    event = result.events[0]
    if event.status == "skipped":
        action_type = "SKIP_HABIT"
    elif event.habit_id:
        action_type = "COMPLETE_HABIT"
    else:
        action_type = "CREATE_HABIT"
    payload: dict[str, Any] = {
        "habit_id": event.habit_id,
        "name": event.habit_name,
        "status": event.status,
    }
    if event.quantity is not None:
        payload["quantity"] = event.quantity
    if event.unit:
        payload["unit"] = event.unit
    return ConversationAction(type=action_type, payload=payload)


def _conversation_intent(legacy_intent: str, text: str) -> str:
    lowered = text.lower()
    if legacy_intent == "cancelar":
        return "CANCEL"
    if legacy_intent == "consultar_habito" and any(
        word in lowered for word in ["progreso", "semana", "avance"]
    ):
        return "QUERY_PROGRESS"
    if legacy_intent == "consultar_habito":
        return "QUERY_TODAY"
    return "UNKNOWN"


def _conversation_confidence(legacy_intent: str, action: ConversationAction | None) -> float:
    if action is not None:
        return 0.88
    if legacy_intent in {"consultar_habito", "cancelar"}:
        return 0.78
    if legacy_intent == "aclaracion":
        return 0.56
    return 0.35


def _missing_fields(result) -> list[str]:
    if result.intent == "aclaracion":
        return ["habit_name"]
    return []


async def _transcribe_with_stt_provider(
    content: bytes,
    filename: str,
    content_type: str,
    language: str,
) -> str:
    api_key = settings.STT_API_KEY
    if not api_key:
        raise HTTPException(
            status_code=503,
            detail="Missing STT_API_KEY for audio transcription",
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
        raise HTTPException(
            status_code=502,
            detail=f"Transcription provider failed: {detail}",
        ) from error
    except httpx.HTTPError as error:
        raise HTTPException(
            status_code=502,
            detail="Could not reach transcription provider",
        ) from error

    data = response.json()
    transcript = str(data.get("text") or data.get("transcript") or "").strip()
    if not transcript:
        raise HTTPException(status_code=422, detail="Transcription provider returned empty text")
    return transcript
