from copy import deepcopy

import pytest

from app.projects.c21200065.api.habits import (
    list_habit_events,
    list_habits,
    upsert_habit,
    upsert_habit_event,
)
from app.projects.c21200065.api.schemas import HabitEventSync, HabitSync


class InMemoryHabitRepository:
    def __init__(self) -> None:
        self.habits: dict[tuple[str, str], dict] = {}
        self.events: dict[tuple[str, str], dict] = {}

    async def upsert_habit(self, user_id: str, data: dict):
        self.habits[(user_id, data["id"])] = deepcopy(data)
        return data

    async def list_habits(self, user_id: str):
        return [deepcopy(value) for (owner, _), value in self.habits.items() if owner == user_id]

    async def upsert_event(self, user_id: str, data: dict):
        if (user_id, data["habit_id"]) not in self.habits:
            raise LookupError
        self.events[(user_id, data["id"])] = deepcopy(data)
        return data

    async def list_events(self, user_id: str):
        return [deepcopy(value) for (owner, _), value in self.events.items() if owner == user_id]


@pytest.mark.asyncio
async def test_habit_survives_logout_and_login_on_another_device() -> None:
    repo = InMemoryHabitRepository()
    first_session = {"user_id": "user-1", "email": "demo@example.com"}
    habit = HabitSync(
        id="habit-1",
        name="Leer 20 páginas",
        icon="menu_book",
        frequency="Diario",
        reminder_time="21:00",
        category="Crecimiento",
        frequency_type="DAILY",
    )
    event = HabitEventSync(
        id="event-1",
        habit_id=habit.id,
        habit_name=habit.name,
        status="Completed",
        timestamp=1_721_340_000_000,
    )

    await upsert_habit(habit.id, habit, first_session, repo)
    await upsert_habit_event(event.id, event, first_session, repo)

    # A second session/device has no Room state and rebuilds it exclusively from the API.
    second_session = {"user_id": "user-1", "email": "demo@example.com"}
    restored_habits = await list_habits(second_session, repo)
    restored_events = await list_habit_events(second_session, repo)

    assert [item["id"] for item in restored_habits] == [habit.id]
    assert [item["habit_id"] for item in restored_events] == [habit.id]
    assert restored_events[0]["status"] == "Completed"


@pytest.mark.asyncio
async def test_remote_habits_are_isolated_by_authenticated_user() -> None:
    repo = InMemoryHabitRepository()
    owner = {"user_id": "owner", "email": "owner@example.com"}
    other = {"user_id": "other", "email": "other@example.com"}
    habit = HabitSync(
        id="private-habit",
        name="Meditar",
        icon="self_improvement",
        frequency="Diario",
        reminder_time="07:00",
        category="Bienestar",
        frequency_type="DAILY",
    )

    await upsert_habit(habit.id, habit, owner, repo)

    assert await list_habits(other, repo) == []
