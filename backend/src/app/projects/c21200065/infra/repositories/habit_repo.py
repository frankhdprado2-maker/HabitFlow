from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.projects.c21200065.infra.orm.habit import HabitEventORM, HabitORM


class HabitRepository:
    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def upsert_habit(self, user_id: str, data: dict) -> HabitORM:
        habit = await self._session.get(HabitORM, data["id"])
        if habit is not None and habit.user_id != user_id:
            raise PermissionError("Habit belongs to another user")
        if habit is None:
            habit = HabitORM(user_id=user_id, **data)
            self._session.add(habit)
        else:
            for key, value in data.items():
                setattr(habit, key, value)
        await self._session.commit()
        await self._session.refresh(habit)
        return habit

    async def list_habits(self, user_id: str) -> list[HabitORM]:
        result = await self._session.execute(
            select(HabitORM).where(HabitORM.user_id == user_id).order_by(HabitORM.created_at)
        )
        return list(result.scalars())

    async def upsert_event(self, user_id: str, data: dict) -> HabitEventORM:
        habit = await self._session.get(HabitORM, data["habit_id"])
        if habit is None or habit.user_id != user_id:
            raise LookupError("Habit not found")
        event = await self._session.get(HabitEventORM, data["id"])
        if event is not None and event.user_id != user_id:
            raise PermissionError("Event belongs to another user")
        if event is None:
            event = HabitEventORM(user_id=user_id, **data)
            self._session.add(event)
        else:
            for key, value in data.items():
                setattr(event, key, value)
        await self._session.commit()
        await self._session.refresh(event)
        return event

    async def list_events(self, user_id: str) -> list[HabitEventORM]:
        result = await self._session.execute(
            select(HabitEventORM)
            .where(HabitEventORM.user_id == user_id)
            .order_by(HabitEventORM.timestamp)
        )
        return list(result.scalars())
