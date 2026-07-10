import json
import re
from dataclasses import dataclass, field
from typing import Literal

VoiceIntent = Literal["registrar_habito", "consultar_habito", "aclaracion", "cancelar", "desconocido"]
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
class VoiceSession:
    candidates: list[HabitContext] = field(default_factory=list)
    pending_status: VoiceStatus = "completed"
    pending_quantity: float | None = None
    pending_unit: str | None = None

    def to_json(self) -> str:
        return json.dumps(
            {
                "candidates": [candidate.__dict__ for candidate in self.candidates],
                "pending_status": self.pending_status,
                "pending_quantity": self.pending_quantity,
                "pending_unit": self.pending_unit,
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
        )


@dataclass
class VoiceTurnResult:
    intent: VoiceIntent
    response: str
    events: list[VoiceEvent] = field(default_factory=list)
    question: str | None = None
    quick_replies: list[str] = field(default_factory=list)
    session: VoiceSession = field(default_factory=VoiceSession)
    clear_session: bool = False


def handle_voice_turn(text: str, habits: list[HabitContext], session: VoiceSession | None = None) -> VoiceTurnResult:
    clean_text = _normalize(text)
    session = session or VoiceSession()

    if not clean_text:
        return VoiceTurnResult(intent="desconocido", response="No escuche ningun comando.")

    if any(word in clean_text for word in ["cancelar", "olvida", "dejalo"]):
        return VoiceTurnResult(intent="cancelar", response="Listo, cancele esta conversacion.", clear_session=True)

    status = _status_from_text(clean_text) or session.pending_status
    quantity, unit = _quantity_from_text(clean_text)
    parts = _split_compound(clean_text)

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
    return text.lower().translate(replacements).strip()


def _status_from_text(text: str) -> VoiceStatus | None:
    if any(word in text for word in ["salte", "omiti", "omitir"]):
        return "skipped"
    if any(word in text for word in ["falle", "no pude", "perdi"]):
        return "failed"
    if any(word in text for word in ["hice", "complete", "termine", "corri", "lei", "listo"]):
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
            matches = [habit for habit in habits if any(keyword in _normalize(habit.name) for keyword in keywords)]
            if len(matches) == 1:
                return matches[0]
    return None


def _confirmation(events: list[VoiceEvent]) -> str:
    if len(events) > 1:
        names = ", ".join(event.habit_name for event in events)
        return f"Listo, registre {len(events)} habitos: {names}."
    event = events[0]
    quantity = f" {event.quantity:g} {event.unit}" if event.quantity and event.unit else ""
    return f"Listo, registre{quantity} de {event.habit_name}."
