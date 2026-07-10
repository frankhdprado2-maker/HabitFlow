from app.projects.c21200065.domain.voice_conversation_service import (
    AchievementContext,
    ConversationContext,
    HabitContext,
    HabitEventContext,
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


def test_proposes_new_habit_from_spoken_conversation_and_confirms_it() -> None:
    first_turn = handle_voice_turn("puede leer 20 paginas de un libro", [])

    assert first_turn.intent == "aclaracion"
    assert first_turn.session.awaiting_confirmation is True
    assert first_turn.session.pending_habit_name == "Leer 20 paginas de un libro"

    confirmed = handle_voice_turn("si guardalo", [], first_turn.session)

    assert confirmed.intent == "registrar_habito"
    assert confirmed.clear_session is True
    assert confirmed.events[0].habit_id is None
    assert confirmed.events[0].habit_name == "Leer 20 paginas de un libro"
    assert confirmed.events[0].status == "completed"
    assert confirmed.events[0].quantity == 20


def test_social_answer_keeps_the_conversation_open() -> None:
    result = handle_voice_turn("estoy bien", [])

    assert result.intent == "aclaracion"
    assert result.events == []
    assert "Que habito" in result.response


def test_answers_weekly_progress_from_real_events() -> None:
    habits = [
        HabitContext(id="read", name="Leer", category="Estudio"),
        HabitContext(id="water", name="Tomar agua", category="Salud"),
    ]
    context = ConversationContext(
        events=[
            HabitEventContext(habit_id="read", habit_name="Leer", status="Completed", timestamp=9_999_999_999_999),
            HabitEventContext(habit_id="read", habit_name="Leer", status="Completed", timestamp=9_999_999_999_998),
        ]
    )

    result = handle_voice_turn("como voy esta semana", habits, context=context)

    assert result.intent == "consultar_habito"
    assert "Leer: 2" in result.response
    assert "Tomar agua" in result.response


def test_builds_safe_plan_recommendation() -> None:
    context = ConversationContext(
        categories=["Estudio"],
        achievements=[
            AchievementContext("week", "Semana solida", "", "Racha de 7 dias", False, 250),
        ],
    )

    result = handle_voice_turn("quiero ser mas constante leyendo", [], context=context)

    assert result.intent == "plan_recomendacion"
    assert result.plan is not None
    assert result.plan.category == "Estudio"
    assert result.plan.actions
