import json
import re
import time
import unicodedata
from dataclasses import dataclass, field
from typing import Literal

VoiceIntent = Literal[
    "registrar_habito",
    "consultar_habito",
    "plan_recomendacion",
    "aclaracion",
    "cancelar",
    "desconocido",
]
VoiceStatus = Literal["completed", "skipped", "failed"]


@dataclass
class HabitContext:
    id: str
    name: str
    category: str = ""


@dataclass
class VoiceEvent:
    habit_id: str | None
    habit_name: str
    status: VoiceStatus
    quantity: float | None = None
    unit: str | None = None


@dataclass
class HabitEventContext:
    habit_id: str
    habit_name: str
    status: str
    timestamp: int


@dataclass
class AchievementContext:
    id: str
    title: str
    description: str
    requirement: str
    unlocked: bool
    xp: int


@dataclass
class ConversationContext:
    events: list[HabitEventContext] = field(default_factory=list)
    achievements: list[AchievementContext] = field(default_factory=list)
    categories: list[str] = field(default_factory=list)


@dataclass
class VoicePlan:
    title: str
    summary: str
    category: str
    actions: list[str] = field(default_factory=list)


@dataclass
class VoiceSession:
    candidates: list[HabitContext] = field(default_factory=list)
    pending_status: VoiceStatus = "completed"
    pending_quantity: float | None = None
    pending_unit: str | None = None
    pending_habit_name: str | None = None
    awaiting_confirmation: bool = False

    def to_json(self) -> str:
        return json.dumps(
            {
                "candidates": [candidate.__dict__ for candidate in self.candidates],
                "pending_status": self.pending_status,
                "pending_quantity": self.pending_quantity,
                "pending_unit": self.pending_unit,
                "pending_habit_name": self.pending_habit_name,
                "awaiting_confirmation": self.awaiting_confirmation,
            }
        )

    @classmethod
    def from_json(cls, value: str | None) -> "VoiceSession":
        if not value:
            return cls()
        try:
            data = json.loads(value)
        except json.JSONDecodeError:
            return cls()
        return cls(
            candidates=[HabitContext(**item) for item in data.get("candidates", [])],
            pending_status=data.get("pending_status") or "completed",
            pending_quantity=data.get("pending_quantity"),
            pending_unit=data.get("pending_unit"),
            pending_habit_name=data.get("pending_habit_name"),
            awaiting_confirmation=bool(data.get("awaiting_confirmation")),
        )


@dataclass
class VoiceTurnResult:
    intent: VoiceIntent
    response: str
    events: list[VoiceEvent] = field(default_factory=list)
    plan: VoicePlan | None = None
    question: str | None = None
    quick_replies: list[str] = field(default_factory=list)
    session: VoiceSession = field(default_factory=VoiceSession)
    clear_session: bool = False


def handle_voice_turn(
    text: str,
    habits: list[HabitContext],
    session: VoiceSession | None = None,
    context: ConversationContext | None = None,
) -> VoiceTurnResult:
    clean_text = _normalize(text)
    session = session or VoiceSession()
    context = context or ConversationContext()

    if not clean_text:
        return VoiceTurnResult(intent="desconocido", response="No escuche ningun comando.")

    if any(word in clean_text for word in ["cancelar", "olvida", "dejalo"]):
        return VoiceTurnResult(
            intent="cancelar",
            response="Listo, cancele esta conversacion.",
            clear_session=True,
        )

    if _is_greeting(clean_text) or _is_social_response(clean_text):
        question = "Me alegra escucharte. Que habito hiciste hoy o cual quieres crear?"
        return VoiceTurnResult(
            intent="aclaracion",
            response=question,
            question=question,
            quick_replies=[habit.name for habit in habits[:4]] + ["Registrar uno nuevo"],
            session=session,
        )

    if _is_progress_query(clean_text):
        return VoiceTurnResult(
            intent="consultar_habito",
            response=_progress_summary(habits, context),
            quick_replies=["Que me falta hoy?", "Dame un plan", "Proximo logro"],
            session=session,
        )

    if _is_missing_today_query(clean_text):
        return VoiceTurnResult(
            intent="consultar_habito",
            response=_missing_today_summary(habits, context),
            quick_replies=["Dame un plan", "Registrar por voz"],
            session=session,
        )

    if _is_achievement_query(clean_text):
        return VoiceTurnResult(
            intent="consultar_habito",
            response=_achievement_summary(context),
            quick_replies=["Dame un plan", "Como voy esta semana?"],
            session=session,
        )

    if _is_plan_request(clean_text):
        plan = _build_plan(clean_text, habits, context)
        return VoiceTurnResult(
            intent="plan_recomendacion",
            response=f"{plan.title}. {plan.summary} " + " ".join(plan.actions),
            plan=plan,
            quick_replies=["Crear primer habito", "Ajustar plan", "Como voy esta semana?"],
            session=session,
        )

    status = _status_from_text(clean_text) or session.pending_status
    quantity, unit = _quantity_from_text(clean_text)
    parts = _split_compound(clean_text)

    if session.awaiting_confirmation and session.pending_habit_name:
        if _is_affirmative(clean_text):
            event = VoiceEvent(
                habit_id=None,
                habit_name=session.pending_habit_name,
                status=session.pending_status,
                quantity=quantity or session.pending_quantity,
                unit=unit or session.pending_unit,
            )
            return VoiceTurnResult(
                intent="registrar_habito",
                response=f"Perfecto, cree y registre el habito {event.habit_name}.",
                events=[event],
                clear_session=True,
            )
        if _is_negative(clean_text):
            question = "De acuerdo. Dime el nombre del habito como quieres guardarlo."
            return VoiceTurnResult(
                intent="aclaracion",
                response=question,
                question=question,
                quick_replies=["Cancelar"],
                session=VoiceSession(pending_status=session.pending_status),
            )

    events: list[VoiceEvent] = []
    unresolved: list[HabitContext] = []
    for part in parts:
        match = _match_habits(part, habits)
        if len(match) == 1:
            event_quantity, event_unit = _quantity_from_text(part)
            events.append(
                VoiceEvent(
                    habit_id=match[0].id,
                    habit_name=match[0].name,
                    status=status,
                    quantity=event_quantity or quantity,
                    unit=event_unit or unit,
                )
            )
        elif len(match) > 1:
            unresolved = match
            break

    if events and not unresolved:
        response = _confirmation(events)
        return VoiceTurnResult(
            intent="registrar_habito",
            response=response,
            events=events,
            clear_session=True,
        )

    if session.candidates:
        selected = _choose_from_candidates(clean_text, session.candidates)
        if selected:
            event = VoiceEvent(
                habit_id=selected.id,
                habit_name=selected.name,
                status=status,
                quantity=quantity or session.pending_quantity,
                unit=unit or session.pending_unit,
            )
            return VoiceTurnResult(
                intent="registrar_habito",
                response=_confirmation([event]),
                events=[event],
                clear_session=True,
            )

    candidates = unresolved or _match_habits(clean_text, habits)
    if len(candidates) > 1:
        next_session = VoiceSession(
            candidates=candidates[:4],
            pending_status=status,
            pending_quantity=quantity,
            pending_unit=unit,
        )
        options = [candidate.name for candidate in next_session.candidates]
        question = f"Que habito registro: {' o '.join(options)}?"
        return VoiceTurnResult(
            intent="aclaracion",
            response=question,
            question=question,
            quick_replies=options + ["Cancelar"],
            session=next_session,
        )

    inferred = _infer_generic_habit(clean_text, habits)
    if inferred:
        event = VoiceEvent(
            habit_id=inferred.id,
            habit_name=inferred.name,
            status=status,
            quantity=quantity,
            unit=unit,
        )
        return VoiceTurnResult(
            intent="registrar_habito",
            response=_confirmation([event]),
            events=[event],
            clear_session=True,
        )

    proposed_habit = _extract_new_habit_name(clean_text)
    if proposed_habit:
        next_session = VoiceSession(
            pending_status=status,
            pending_quantity=quantity,
            pending_unit=unit,
            pending_habit_name=proposed_habit,
            awaiting_confirmation=True,
        )
        question = f"Entendi que quieres crear y registrar el habito {proposed_habit}. Lo guardo?"
        return VoiceTurnResult(
            intent="aclaracion",
            response=question,
            question=question,
            quick_replies=["Si, guardalo", "No, cambiar", "Cancelar"],
            session=next_session,
        )

    question = "Que habito quieres registrar?"
    return VoiceTurnResult(
        intent="aclaracion",
        response=question,
        question=question,
        quick_replies=[habit.name for habit in habits[:4]] + ["Cancelar"],
        session=VoiceSession(pending_status=status, pending_quantity=quantity, pending_unit=unit),
    )


def _normalize(text: str) -> str:
    replacements = str.maketrans("áéíóúüñ", "aeiouun")
    normalized = unicodedata.normalize("NFD", text.lower().translate(replacements))
    without_marks = "".join(char for char in normalized if unicodedata.category(char) != "Mn")
    return re.sub(r"\s+", " ", without_marks).strip()


def _status_from_text(text: str) -> VoiceStatus | None:
    if any(word in text for word in ["salte", "omiti", "omitir"]):
        return "skipped"
    if any(word in text for word in ["falle", "no pude", "perdi"]):
        return "failed"
    if any(
        word in text
        for word in ["hice", "complete", "termine", "corri", "lei", "pude", "puede", "listo"]
    ):
        return "completed"
    return None


def _quantity_from_text(text: str) -> tuple[float | None, str | None]:
    match = re.search(r"(\d+(?:[.,]\d+)?)\s*(minutos?|mins?|horas?|paginas?|km|kilometros?)?", text)
    if not match:
        return None, None
    quantity = float(match.group(1).replace(",", "."))
    unit = match.group(2) or None
    return quantity, unit


def _split_compound(text: str) -> list[str]:
    parts = [part.strip() for part in re.split(r"\s+y\s+", text) if part.strip()]
    return parts if len(parts) > 1 else [text]


def _match_habits(text: str, habits: list[HabitContext]) -> list[HabitContext]:
    matches = []
    for habit in habits:
        name = _normalize(habit.name)
        category = _normalize(habit.category)
        tokens = [token for token in re.split(r"\W+", name) if len(token) > 3]
        if name in text or any(token in text for token in tokens):
            matches.append(habit)
        elif category and category in text:
            matches.append(habit)
    return matches


def _choose_from_candidates(text: str, candidates: list[HabitContext]) -> HabitContext | None:
    exact = [candidate for candidate in candidates if _normalize(candidate.name) == text]
    if len(exact) == 1:
        return exact[0]
    matches = _match_habits(text, candidates)
    if len(matches) == 1:
        return matches[0]
    return None


def _infer_generic_habit(text: str, habits: list[HabitContext]) -> HabitContext | None:
    hints = {
        "ejercicio": ["correr", "gimnasio", "entrenar"],
        "leer": ["leer", "lectura"],
        "estudiar": ["estudiar", "estudio"],
        "agua": ["agua"],
    }
    for hint, keywords in hints.items():
        if hint in text:
            matches = [
                habit
                for habit in habits
                if any(keyword in _normalize(habit.name) for keyword in keywords)
            ]
            if len(matches) == 1:
                return matches[0]
    return None


def _is_greeting(text: str) -> bool:
    return text in {"hola", "buenas", "buenos dias", "buenas tardes", "buenas noches"}


def _is_social_response(text: str) -> bool:
    if _extract_new_habit_name(text) or _quantity_from_text(text)[0] is not None:
        return False
    return any(
        phrase in text
        for phrase in ["estoy bien", "todo bien", "bien", "mas o menos", "mal"]
    )


def _is_progress_query(text: str) -> bool:
    return any(
        phrase in text
        for phrase in ["como voy", "progreso", "esta semana", "semana", "avance"]
    )


def _is_missing_today_query(text: str) -> bool:
    return any(
        phrase in text
        for phrase in ["que me falta", "faltan hoy", "falta hoy", "pendiente"]
    )


def _is_achievement_query(text: str) -> bool:
    return any(
        phrase in text
        for phrase in ["proximo logro", "siguiente logro", "cuanto me falta", "insignia"]
    )


def _is_plan_request(text: str) -> bool:
    return any(
        phrase in text
        for phrase in [
            "dame un plan",
            "recomiendame",
            "recomendacion",
            "quiero mejorar",
            "quiero ser mas constante",
            "plan",
            "meta",
        ]
    )


def _progress_summary(habits: list[HabitContext], context: ConversationContext) -> str:
    if not habits:
        return "Aun no tienes habitos activos. Podemos crear uno pequeno para empezar hoy."
    week_start = _now_ms() - 7 * 24 * 60 * 60 * 1000
    week_events = [
        event
        for event in context.events
        if event.timestamp >= week_start and event.status.lower() == "completed"
    ]
    counts: dict[str, int] = {}
    for event in week_events:
        counts[event.habit_name] = counts.get(event.habit_name, 0) + 1
    if not counts:
        names = ", ".join(habit.name for habit in habits[:3])
        return f"Esta semana aun no hay registros completados. Puedes empezar con {names}."
    top = sorted(counts.items(), key=lambda item: item[1], reverse=True)[:3]
    detail = ", ".join(
        f"{name}: {count} registro{'s' if count != 1 else ''}"
        for name, count in top
    )
    missing = _missing_today_names(habits, context)
    suffix = (
        f" Te falta hoy: {', '.join(missing[:3])}."
        if missing
        else " Hoy ya tienes registros activos."
    )
    return f"Esta semana vas asi: {detail}.{suffix}"


def _missing_today_summary(habits: list[HabitContext], context: ConversationContext) -> str:
    missing = _missing_today_names(habits, context)
    if not habits:
        return "No hay habitos activos todavia. Puedo ayudarte a crear el primero."
    if not missing:
        return "No veo pendientes por ahora: hoy ya registraste tus habitos activos."
    return f"Te falta registrar hoy: {', '.join(missing[:4])}."


def _achievement_summary(context: ConversationContext) -> str:
    locked = [achievement for achievement in context.achievements if not achievement.unlocked]
    if not locked:
        unlocked = [achievement for achievement in context.achievements if achievement.unlocked]
        if unlocked:
            return "Ya desbloqueaste todos los logros cargados localmente. Buen ritmo."
        return "Aun no tengo logros cargados para calcular el siguiente desbloqueo."
    next_achievement = sorted(locked, key=lambda item: item.xp)[0]
    completed = len([event for event in context.events if event.status.lower() == "completed"])
    return (
        f"Tu proximo logro es {next_achievement.title}: {next_achievement.requirement}. "
        f"Llevas {completed} registros completados; sigue con una accion pequena hoy."
    )


def _build_plan(text: str, habits: list[HabitContext], context: ConversationContext) -> VoicePlan:
    category = _plan_category(text, context)
    completed_week = len(
        [
            event
            for event in context.events
            if event.timestamp >= _now_ms() - 7 * 24 * 60 * 60 * 1000
            and event.status.lower() == "completed"
        ]
    )
    if not habits:
        return VoicePlan(
            title="Plan de arranque",
            summary=f"Empecemos con una meta de {category.lower()} muy facil de sostener.",
            category=category,
            actions=[
                "Crea un habito de 10 minutos tres veces por semana.",
                "Registralo por voz apenas termines.",
                "Revisamos el progreso en 7 dias.",
            ],
        )
    if completed_week <= 2:
        return VoicePlan(
            title=f"Plan suave de {category}",
            summary="Tu historial reciente sugiere bajar friccion antes de subir intensidad.",
            category=category,
            actions=[
                "Elige un solo habito principal esta semana.",
                "Reduce la meta a 10 o 15 minutos.",
                "Ponlo en un horario fijo y facil de recordar.",
            ],
        )
    return VoicePlan(
        title=f"Plan de constancia en {category}",
        summary="Ya hay movimiento esta semana, asi que conviene consolidar.",
        category=category,
        actions=[
            "Mantén dos habitos activos como prioridad.",
            "Agrega una revision corta el domingo.",
            "Si fallas dos dias seguidos, baja la frecuencia en vez de abandonar.",
        ],
    )


def _plan_category(text: str, context: ConversationContext) -> str:
    for category in context.categories:
        normalized = _normalize(category)
        if normalized and normalized in text:
            return category
    if "leer" in text or "estudi" in text:
        return "Estudio"
    if "mover" in text or "correr" in text or "ejercicio" in text:
        return "Salud"
    if context.categories:
        return context.categories[0]
    return "Productividad"


def _missing_today_names(habits: list[HabitContext], context: ConversationContext) -> list[str]:
    day_start = _day_start_ms()
    completed_today = {
        event.habit_id
        for event in context.events
        if event.timestamp >= day_start and event.status.lower() == "completed"
    }
    return [habit.name for habit in habits if habit.id not in completed_today]


def _now_ms() -> int:
    return int(time.time() * 1000)


def _day_start_ms() -> int:
    seconds = int(time.time())
    local = time.localtime(seconds)
    start = time.mktime(
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
    return int(start * 1000)


def _is_affirmative(text: str) -> bool:
    return any(
        re.search(rf"\b{re.escape(word)}\b", text)
        for word in ["si", "claro", "ok", "dale", "guardalo", "registralo", "correcto"]
    )


def _is_negative(text: str) -> bool:
    return any(
        re.search(rf"\b{re.escape(word)}\b", text)
        for word in ["no", "cambiar", "corrige", "otro"]
    )


def _extract_new_habit_name(text: str) -> str | None:
    working = re.sub(r"[?¿¡!.,;:]+", " ", text)
    working = re.sub(r"\s+", " ", working).strip()
    if not working:
        return None

    action_starts = [
        "leer",
        "estudiar",
        "tomar agua",
        "tomar",
        "correr",
        "caminar",
        "meditar",
        "hacer ejercicio",
        "ejercicio",
    ]
    prefix_actions = {
        "lei": "leer",
        "leí": "leer",
        "corri": "correr",
        "corrí": "correr",
        "estudie": "estudiar",
        "estudié": "estudiar",
        "tome": "tomar",
        "tomé": "tomar",
    }
    removable_prefixes = [
        "quiero registrar",
        "quiero crear",
        "crear",
        "crea",
        "registrar",
        "registra",
        "registre",
        "hice",
        "complete",
        "termine",
        "pude",
        "puede",
    ]

    for phrase, replacement in prefix_actions.items():
        match = re.search(rf"\b{re.escape(phrase)}\b", working)
        if match:
            candidate = f"{replacement} {working[match.end():]}".strip()
            return _format_habit_name(candidate)

    starts: list[tuple[int, str]] = []
    for phrase in action_starts:
        match = re.search(rf"\b{re.escape(phrase)}\b", working)
        if match:
            starts.append((match.start(), working[match.start():]))
    if starts:
        starts.sort(key=lambda item: item[0])
        return _format_habit_name(starts[0][1])

    for phrase in removable_prefixes:
        match = re.search(rf"\b{re.escape(phrase)}\b", working)
        if match:
            candidate = working[match.end():].strip()
            return _format_habit_name(candidate)

    return None


def _format_habit_name(value: str) -> str | None:
    previous = ""
    value = value.strip()
    while value != previous:
        previous = value
        value = re.sub(r"^(un|una|el|la|los|las|habito|hábito|de)\s+", "", value).strip()
    value = re.sub(r"\s+", " ", value).strip()
    if len(value) < 3:
        return None
    value = value[:70].strip()
    return value[:1].upper() + value[1:]


def _confirmation(events: list[VoiceEvent]) -> str:
    if len(events) > 1:
        names = ", ".join(event.habit_name for event in events)
        return f"Listo, registre {len(events)} habitos: {names}."
    event = events[0]
    quantity = f" {event.quantity:g} {event.unit}" if event.quantity and event.unit else ""
    return f"Listo, registre{quantity} de {event.habit_name}."
