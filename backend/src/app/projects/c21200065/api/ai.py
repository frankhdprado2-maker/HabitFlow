import re
import time
from collections import Counter
from typing import Annotated, Any, Literal

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field

from app.projects.c21200065.api.deps import get_current_user
from app.projects.c21200065.domain.habit_interpretation_service import (
    GeminiHabitInterpreter,
    HabitInterpretationError,
    HabitInterpretationRequest,
    HabitInterpretationResponse,
)
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

_memory_sessions: dict[str, str] = {}
habit_interpreter = GeminiHabitInterpreter()


class VoiceHabitContext(BaseModel):
    id: str
    name: str
    category: str = ""
    preferred_time: str | None = None
    duration_minutes: int | None = None
    priority: str | None = None


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
    recent_events: list[VoiceEventContext] = Field(default_factory=list)
    achievements: list[VoiceAchievementContext] = Field(default_factory=list)
    categories: list[str] = Field(default_factory=list)


class ConversationAction(BaseModel):
    type: Literal[
        "CREATE_HABIT",
        "COMPLETE_HABIT",
        "SKIP_HABIT",
        "UPDATE_HABIT",
        "DELETE_HABIT",
        "QUERY_TODAY",
        "QUERY_PROGRESS",
        "GENERATE_DAILY_PLAN",
        "GENERATE_WEEKLY_SUMMARY",
        "GET_ADAPTIVE_RECOMMENDATION",
        "RESCHEDULE_HABIT",
        "CANCEL",
        "CONFIRM",
        "REJECT",
        "UNKNOWN",
    ]
    payload: dict[str, Any] = Field(default_factory=dict)


class DailyPlanItem(BaseModel):
    habit_id: str
    habit_name: str
    suggested_time: str
    duration_minutes: int
    reason: str


class DailyPlanResponse(BaseModel):
    summary: str
    items: list[DailyPlanItem] = Field(default_factory=list)
    requires_confirmation: bool = True


class WeeklySummaryResponse(BaseModel):
    headline: str = ""
    summary: str = ""
    highlights: list[str] = Field(default_factory=list)
    recommendation: str = ""
    data_sufficient: bool = False


class AdaptiveRecommendationResponse(BaseModel):
    habit_id: str | None = None
    type: str = ""
    title: str = ""
    message: str = ""
    reason: str = ""
    proposed_change: dict[str, Any] = Field(default_factory=dict)
    requires_confirmation: bool = True


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
        "DELETE_HABIT",
        "QUERY_TODAY",
        "QUERY_PROGRESS",
        "GENERATE_DAILY_PLAN",
        "GENERATE_WEEKLY_SUMMARY",
        "GET_ADAPTIVE_RECOMMENDATION",
        "RESCHEDULE_HABIT",
        "CANCEL",
        "CONFIRM",
        "REJECT",
        "UNKNOWN",
    ]
    confidence: float = 0.0
    action: ConversationAction | None = None
    missing_fields: list[str] = Field(default_factory=list)
    requires_confirmation: bool = False
    suggestions: list[str] = Field(default_factory=list)
    daily_plan: DailyPlanResponse | None = None
    weekly_summary: WeeklySummaryResponse | None = None
    adaptive_recommendation: AdaptiveRecommendationResponse | None = None


@router.post("/interpret-habit", response_model=HabitInterpretationResponse)
async def interpret_habit(
    request: HabitInterpretationRequest,
    current_user: CurrentUserDep,
) -> HabitInterpretationResponse:
    del current_user
    try:
        return await habit_interpreter.interpret(request.text, request.timezone)
    except HabitInterpretationError as error:
        status_code = {
            "missing_api_key": 503,
            "quota": 429,
            "timeout": 504,
            "invalid_response": 502,
            "provider_error": 502,
        }.get(error.kind, 502)
        raise HTTPException(status_code=status_code, detail=error.message) from error


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
    context = _conversation_context_from_request(request)
    special_response = _special_conversation_response(request, session_id)
    if special_response is not None:
        return special_response

    habit_contexts = [
        HabitContext(id=item.id, name=item.name, category=item.category)
        for item in request.user_context.existing_habits
    ]
    result = handle_voice_turn(
        request.text,
        habit_contexts,
        session,
        context,
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


def _conversation_context_from_request(request: VoiceConversationRequest) -> ConversationContext:
    return ConversationContext(
        events=[
            HabitEventContext(
                habit_id=item.habit_id,
                habit_name=item.habit_name,
                status=item.status,
                timestamp=item.timestamp,
            )
            for item in request.user_context.recent_events
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
            for item in request.user_context.achievements
        ],
        categories=request.user_context.categories,
    )


def _special_conversation_response(
    request: VoiceConversationRequest,
    session_id: str,
) -> VoiceConversationResponse | None:
    text = _normalize_text(request.text)
    if _wants_daily_plan(text):
        return _daily_plan_response(request, session_id)
    if _wants_weekly_summary(text):
        return _weekly_summary_response(request, session_id)
    if _wants_adaptive_recommendation(text):
        return _adaptive_recommendation_response(request, session_id)
    if _wants_reschedule(text):
        return VoiceConversationResponse(
            session_id=session_id,
            assistant_message=(
                "Quieres reprogramarlo para hoy, moverlo a manana, "
                "saltarlo solo por hoy o cancelar?"
            ),
            intent="RESCHEDULE_HABIT",
            confidence=0.82,
            missing_fields=["habit_id", "reschedule_option"],
            suggestions=["Reprogramar hoy", "Mover a manana", "Saltar solo hoy", "Cancelar"],
        )
    if _is_reject(text):
        return VoiceConversationResponse(
            session_id=session_id,
            assistant_message="De acuerdo, no hare ningun cambio.",
            intent="REJECT",
            confidence=0.9,
            suggestions=["Intentar otra vez", "Plan de hoy", "Cancelar"],
        )
    return None


def _daily_plan_response(
    request: VoiceConversationRequest,
    session_id: str,
) -> VoiceConversationResponse:
    habits = request.user_context.existing_habits
    if not habits:
        message = "Agrega habitos con hora o duracion para crear un plan mas preciso."
        return VoiceConversationResponse(
            session_id=session_id,
            assistant_message=message,
            intent="GENERATE_DAILY_PLAN",
            confidence=0.92,
            daily_plan=DailyPlanResponse(summary=message, items=[], requires_confirmation=False),
            suggestions=["Crear un habito", "Registrar manualmente"],
        )

    day_start = _day_start_ms()
    completed_today = {
        event.habit_id
        for event in request.user_context.recent_events
        if event.timestamp >= day_start and event.status.lower() == "completed"
    }
    skipped_today = {
        event.habit_id
        for event in request.user_context.recent_events
        if event.timestamp >= day_start and event.status.lower() == "skipped"
    }
    pending = [
        habit
        for habit in habits
        if habit.id not in completed_today and habit.id not in skipped_today
    ]
    if not pending:
        message = "Ya completaste tu rutina de hoy."
        return VoiceConversationResponse(
            session_id=session_id,
            assistant_message=message,
            intent="GENERATE_DAILY_PLAN",
            confidence=0.95,
            daily_plan=DailyPlanResponse(summary=message, items=[], requires_confirmation=False),
            suggestions=["Ver resumen semanal", "Registrar otro habito"],
        )

    items: list[DailyPlanItem] = []
    for habit in sorted(pending, key=lambda item: item.preferred_time or "23:59"):
        duration = habit.duration_minutes or _duration_from_name(habit.name) or 0
        suggested_time = habit.preferred_time or "Sin hora"
        reason = (
            "Es tu horario habitual y aun no lo completaste."
            if habit.preferred_time
            else "Aun no tiene hora fija; puedes ubicarlo en un bloque libre."
        )
        items.append(
            DailyPlanItem(
                habit_id=habit.id,
                habit_name=habit.name,
                suggested_time=suggested_time,
                duration_minutes=duration,
                reason=reason,
            )
        )
    if all(item.duration_minutes == 0 and item.suggested_time == "Sin hora" for item in items):
        summary = "Agrega habitos con hora o duracion para crear un plan mas preciso."
    elif len(items) == 1:
        summary = "Tienes una actividad pendiente."
    else:
        summary = f"Tienes {len(items)} actividades pendientes para hoy."
    return VoiceConversationResponse(
        session_id=session_id,
        assistant_message=summary,
        intent="GENERATE_DAILY_PLAN",
        confidence=0.9,
        daily_plan=DailyPlanResponse(summary=summary, items=items, requires_confirmation=True),
        requires_confirmation=True,
        suggestions=["Aceptar plan", "Editar", "Rechazar"],
    )


def _weekly_summary_response(
    request: VoiceConversationRequest,
    session_id: str,
) -> VoiceConversationResponse:
    now = _now_ms()
    week_start = now - 7 * 24 * 60 * 60 * 1000
    previous_week_start = now - 14 * 24 * 60 * 60 * 1000
    week_events = [
        event for event in request.user_context.recent_events if event.timestamp >= week_start
    ]
    previous_events = [
        event
        for event in request.user_context.recent_events
        if previous_week_start <= event.timestamp < week_start
    ]
    completed = [event for event in week_events if event.status.lower() == "completed"]
    skipped = [event for event in week_events if event.status.lower() == "skipped"]
    scheduled = len(completed) + len(skipped)
    if scheduled == 0:
        summary = "Completa algunos habitos para generar tu primer resumen semanal."
        weekly = WeeklySummaryResponse(data_sufficient=False, summary=summary)
    else:
        rate = round(len(completed) * 100 / scheduled)
        previous_scheduled = len(previous_events)
        previous_completed = len(
            [event for event in previous_events if event.status.lower() == "completed"]
        )
        previous_rate = (
            round(previous_completed * 100 / previous_scheduled)
            if previous_scheduled
            else 0
        )
        most_consistent = Counter(event.habit_name for event in completed).most_common(1)
        most_skipped = Counter(event.habit_name for event in skipped).most_common(1)
        delta = rate - previous_rate
        highlights = [
            f"Completaste {len(completed)} de {scheduled} actividades.",
            f"Tu tasa semanal fue {rate}%.",
        ]
        if most_consistent:
            highlights.append(f"Tu habito mas constante fue {most_consistent[0][0]}.")
        if most_skipped:
            highlights.append(f"El habito con mayor dificultad fue {most_skipped[0][0]}.")
        weekly = WeeklySummaryResponse(
            headline="Una semana en progreso",
            summary=(
                f"Completaste {len(completed)} de {scheduled} actividades. "
                f"Variacion frente a la semana anterior: {delta:+d} puntos."
            ),
            highlights=highlights,
            recommendation=(
                "Manten una meta pequena y revisa horarios si una actividad "
                "se repite como omitida."
            ),
            data_sufficient=True,
        )
        summary = weekly.summary
    return VoiceConversationResponse(
        session_id=session_id,
        assistant_message=summary,
        intent="GENERATE_WEEKLY_SUMMARY",
        confidence=0.9,
        weekly_summary=weekly,
        suggestions=["Plan de hoy", "Recomendacion adaptativa"],
    )


def _adaptive_recommendation_response(
    request: VoiceConversationRequest,
    session_id: str,
) -> VoiceConversationResponse:
    recommendation = _build_adaptive_recommendation(request)
    if recommendation is None:
        message = "Necesito mas datos reales para darte una recomendacion adaptativa."
        return VoiceConversationResponse(
            session_id=session_id,
            assistant_message=message,
            intent="GET_ADAPTIVE_RECOMMENDATION",
            confidence=0.72,
            adaptive_recommendation=AdaptiveRecommendationResponse(
                title="Aun faltan datos",
                message=message,
                requires_confirmation=False,
            ),
            suggestions=["Plan de hoy", "Registrar progreso"],
        )
    return VoiceConversationResponse(
        session_id=session_id,
        assistant_message=recommendation.message,
        intent="GET_ADAPTIVE_RECOMMENDATION",
        confidence=0.88,
        adaptive_recommendation=recommendation,
        requires_confirmation=True,
        suggestions=["Aceptar", "Editar", "Rechazar"],
    )


def _build_adaptive_recommendation(
    request: VoiceConversationRequest,
) -> AdaptiveRecommendationResponse | None:
    now = _now_ms()
    week_start = now - 7 * 24 * 60 * 60 * 1000
    week_events = [
        event for event in request.user_context.recent_events if event.timestamp >= week_start
    ]
    by_habit = {habit.id: habit for habit in request.user_context.existing_habits}
    skipped_counts = Counter(
        event.habit_id for event in week_events if event.status.lower() == "skipped"
    )
    for habit_id, count in skipped_counts.items():
        if count >= 3 and habit_id in by_habit:
            habit = by_habit[habit_id]
            return AdaptiveRecommendationResponse(
                habit_id=habit_id,
                type="REDUCE_DURATION",
                title="Hazlo mas facil de mantener",
                message=f"Podrias reducir temporalmente {habit.name} para sostener la rutina.",
                reason=f"Lo omitiste {count} veces en los ultimos 7 dias.",
                proposed_change={"duration_minutes": max(10, (habit.duration_minutes or 25) // 2)},
            )
    completed_counts = Counter(
        event.habit_id for event in week_events if event.status.lower() == "completed"
    )
    for habit_id, count in completed_counts.items():
        if count >= 6 and habit_id in by_habit:
            habit = by_habit[habit_id]
            return AdaptiveRecommendationResponse(
                habit_id=habit_id,
                type="MAINTAIN_OR_INCREASE",
                title="Buen ritmo sostenido",
                message=f"{habit.name} va muy bien. Mantenerlo estable es una buena opcion.",
                reason=f"Lo completaste {count} de los ultimos 7 dias.",
                proposed_change={},
            )
    return None


def _normalize_text(text: str) -> str:
    return text.lower().strip()


def _wants_daily_plan(text: str) -> bool:
    return "plan de hoy" in text or "dame un plan" in text or "organiza mi dia" in text


def _wants_weekly_summary(text: str) -> bool:
    return (
        "resumen semanal" in text
        or "resumen de mi semana" in text
        or "como fue mi semana" in text
    )


def _wants_adaptive_recommendation(text: str) -> bool:
    return (
        "recomendacion adaptativa" in text
        or "que me recomiendas" in text
        or "mejorar mi rutina" in text
    )


def _wants_reschedule(text: str) -> bool:
    return "hoy no puedo" in text or "reprogram" in text or "mover a manana" in text


def _is_reject(text: str) -> bool:
    return text in {"no", "rechazar", "cancelar recomendacion"}


def _duration_from_name(name: str) -> int | None:
    match = re.search(r"(\d{1,3})\s*(min|minutos?)", name.lower())
    return int(match.group(1)) if match else None


def _now_ms() -> int:
    return int(time.time() * 1000)


def _day_start_ms() -> int:
    now = int(time.time())
    local = time.localtime(now)
    return int(
        time.mktime(
            (
                local.tm_year,
                local.tm_mon,
                local.tm_mday,
                0,
                0,
                0,
                local.tm_wday,
                local.tm_yday,
                local.tm_isdst,
            )
        )
        * 1000
    )
