from typing import Optional

from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.projects.c21200065.domain.models.geo_event import GeoEvent
from app.projects.c21200065.infra.orm.geo_event import GeoEventORM


class GeoEventORMRepository:

    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def create(self, data: dict) -> GeoEvent:
        event = GeoEventORM(**data)
        self._session.add(event)
        await self._session.commit()
        await self._session.refresh(event)
        return GeoEvent.model_validate(event)

    async def find_by_id(self, id: int) -> Optional[GeoEvent]:
        result = await self._session.get(GeoEventORM, id)
        return GeoEvent.model_validate(result) if result else None

    async def find_all(
        self,
        user_id: Optional[str] = None,
        event_type: Optional[str] = None,
        limit: int = 50,
        offset: int = 0,
    ) -> list[GeoEvent]:
        stmt = select(GeoEventORM).order_by(GeoEventORM.recorded_at.desc()).limit(limit).offset(offset)

        if user_id:
            stmt = stmt.where(GeoEventORM.user_id == user_id)
        if event_type:
            stmt = stmt.where(GeoEventORM.event_type == event_type)

        result = await self._session.execute(stmt)
        return [GeoEvent.model_validate(row) for row in result.scalars()]

    async def delete(self, id: int) -> bool:
        result = await self._session.execute(
            delete(GeoEventORM).where(GeoEventORM.id == id)
        )
        await self._session.commit()
        return result.rowcount > 0
