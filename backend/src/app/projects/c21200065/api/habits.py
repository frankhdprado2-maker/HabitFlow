from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException

from app.projects.c21200065.api.deps import get_current_user, get_habit_repo
from app.projects.c21200065.api.schemas import HabitEventSync, HabitSync
from app.projects.c21200065.infra.repositories.habit_repo import HabitRepository

router = APIRouter(tags=["habits"])
CurrentUserDep = Annotated[dict, Depends(get_current_user)]
HabitRepoDep = Annotated[HabitRepository, Depends(get_habit_repo)]


@router.put("/habits/{habit_id}", response_model=HabitSync)
async def upsert_habit(
    habit_id: str, payload: HabitSync, user: CurrentUserDep, repo: HabitRepoDep
):
    if habit_id != payload.id:
        raise HTTPException(status_code=400, detail="Habit id mismatch")
    try:
        return await repo.upsert_habit(user["user_id"], payload.model_dump())
    except PermissionError:
        raise HTTPException(status_code=403, detail="Habit belongs to another user") from None


@router.get("/habits", response_model=list[HabitSync])
async def list_habits(user: CurrentUserDep, repo: HabitRepoDep):
    return await repo.list_habits(user["user_id"])


@router.put("/habit-events/{event_id}", response_model=HabitEventSync)
async def upsert_habit_event(
    event_id: str, payload: HabitEventSync, user: CurrentUserDep, repo: HabitRepoDep
):
    if event_id != payload.id:
        raise HTTPException(status_code=400, detail="Event id mismatch")
    try:
        return await repo.upsert_event(user["user_id"], payload.model_dump())
    except LookupError:
        raise HTTPException(status_code=404, detail="Habit not found") from None
    except PermissionError:
        raise HTTPException(status_code=403, detail="Event belongs to another user") from None


@router.get("/habit-events", response_model=list[HabitEventSync])
async def list_habit_events(user: CurrentUserDep, repo: HabitRepoDep):
    return await repo.list_events(user["user_id"])
