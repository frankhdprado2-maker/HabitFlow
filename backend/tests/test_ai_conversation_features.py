import time

from app.projects.c21200065.api.ai import (
    ConversationUserContext,
    VoiceConversationRequest,
    VoiceEventContext,
    VoiceHabitContext,
    _special_conversation_response,
)


def test_daily_plan_uses_pending_real_habits() -> None:
    request = VoiceConversationRequest(
        text="Plan de hoy",
        user_context=ConversationUserContext(
            existing_habits=[
                VoiceHabitContext(
                    id="study",
                    name="Estudiar 25 minutos",
                    category="Estudio",
                    preferred_time="19:00",
                    duration_minutes=25,
                )
            ]
        ),
    )

    response = _special_conversation_response(request, "session")

    assert response is not None
    assert response.intent == "GENERATE_DAILY_PLAN"
    assert response.daily_plan is not None
    assert response.daily_plan.items[0].habit_id == "study"
    assert response.requires_confirmation is True


def test_weekly_summary_reports_insufficient_data() -> None:
    request = VoiceConversationRequest(text="Resumen semanal")

    response = _special_conversation_response(request, "session")

    assert response is not None
    assert response.intent == "GENERATE_WEEKLY_SUMMARY"
    assert response.weekly_summary is not None
    assert response.weekly_summary.data_sufficient is False


def test_adaptive_recommendation_for_three_skips() -> None:
    now = int(time.time() * 1000)
    request = VoiceConversationRequest(
        text="Que me recomiendas para mejorar mi rutina",
        user_context=ConversationUserContext(
            existing_habits=[
                VoiceHabitContext(
                    id="study",
                    name="Estudiar 60 minutos",
                    category="Estudio",
                    duration_minutes=60,
                )
            ],
            recent_events=[
                VoiceEventContext(
                    habit_id="study",
                    habit_name="Estudiar 60 minutos",
                    status="Skipped",
                    timestamp=now - offset,
                )
                for offset in (1_000, 2_000, 3_000)
            ],
        ),
    )

    response = _special_conversation_response(request, "session")

    assert response is not None
    assert response.intent == "GET_ADAPTIVE_RECOMMENDATION"
    assert response.adaptive_recommendation is not None
    assert response.adaptive_recommendation.type == "REDUCE_DURATION"
    assert response.requires_confirmation is True
