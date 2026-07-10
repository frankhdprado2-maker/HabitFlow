from app.projects.c21200065.domain.voice_conversation_service import (
    HabitContext,
    VoiceSession,
    handle_voice_turn,
)


def test_registers_high_confidence_habit_with_quantity() -> None:
    habits = [HabitContext(id="read", name="Leer 20 paginas", category="Estudio")]

    result = handle_voice_turn("Complete leer 20 paginas", habits)

    assert result.intent == "registrar_habito"
    assert result.clear_session is True
    assert len(result.events) == 1
    assert result.events[0].habit_id == "read"
    assert result.events[0].status == "completed"
    assert result.events[0].quantity == 20
    assert result.events[0].unit == "paginas"


def test_asks_for_clarification_when_match_is_ambiguous() -> None:
    habits = [
        HabitContext(id="algorithms", name="Estudiar algoritmos", category="Universidad"),
        HabitContext(id="english", name="Estudiar ingles", category="Idiomas"),
    ]

    result = handle_voice_turn("Complete estudiar", habits)

    assert result.intent == "aclaracion"
    assert result.events == []
    assert result.quick_replies == ["Estudiar algoritmos", "Estudiar ingles", "Cancelar"]


def test_resolves_pending_clarification_with_selected_habit() -> None:
    session = VoiceSession(
        candidates=[
            HabitContext(id="algorithms", name="Estudiar algoritmos", category="Universidad"),
            HabitContext(id="english", name="Estudiar ingles", category="Idiomas"),
        ],
        pending_status="completed",
    )

    result = handle_voice_turn("Estudiar ingles", [], session)

    assert result.intent == "registrar_habito"
    assert result.clear_session is True
    assert result.events[0].habit_id == "english"


def test_registers_failed_status_and_compound_events() -> None:
    habits = [
        HabitContext(id="study", name="Estudiar algoritmos", category="Universidad"),
        HabitContext(id="water", name="Tomar agua", category="Salud"),
    ]

    failed = handle_voice_turn("No pude estudiar algoritmos", habits)
    compound = handle_voice_turn("Complete estudiar algoritmos y tomar agua", habits)

    assert failed.events[0].status == "failed"
    assert [event.habit_id for event in compound.events] == ["study", "water"]
