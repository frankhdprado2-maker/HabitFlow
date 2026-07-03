from typing import Optional

from fastapi import APIRouter, Depends, HTTPException

from app.projects.c21200065.api.deps import get_geo_event_orm_repo
from app.projects.c21200065.api.schemas import GeoEventCreate, GeoEventResponse
from app.projects.c21200065.infra.repositories.geo_event_orm_repo import GeoEventORMRepository

router = APIRouter(prefix="/geo-events-orm", tags=["geo-events-orm"])


@router.post("/", response_model=GeoEventResponse, status_code=201)
async def create(payload: GeoEventCreate, repo: GeoEventORMRepository = Depends(get_geo_event_orm_repo)):
    return await repo.create(payload.model_dump())


@router.get("/{id}", response_model=GeoEventResponse)
async def get_by_id(id: int, repo: GeoEventORMRepository = Depends(get_geo_event_orm_repo)):
    event = await repo.find_by_id(id)
    if not event:
        raise HTTPException(status_code=404, detail="Geo event not found")
    return event


@router.get("/", response_model=list[GeoEventResponse])
async def get_all(
    user_id: Optional[str] = None,
    event_type: Optional[str] = None,
    limit: int = 50,
    offset: int = 0,
    repo: GeoEventORMRepository = Depends(get_geo_event_orm_repo),
):
    return await repo.find_all(user_id=user_id, event_type=event_type, limit=limit, offset=offset)


@router.delete("/{id}", status_code=204)
async def delete(id: int, repo: GeoEventORMRepository = Depends(get_geo_event_orm_repo)):
    deleted = await repo.delete(id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Geo event not found")
