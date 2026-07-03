from typing import Optional

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from app.projects.c21200065.domain.models.geo_event import GeoEvent


class GeoEventRepository:

    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def create(self, data: dict) -> GeoEvent:
        result = await self._session.execute(
            text("""
                INSERT INTO geo_events (
                    user_id, latitude, longitude, altitude, accuracy,
                    speed, heading, event_type, device_id, platform,
                    app_version, device_model, recorded_at
                ) VALUES (
                    :user_id, :latitude, :longitude, :altitude, :accuracy,
                    :speed, :heading, :event_type, :device_id, :platform,
                    :app_version, :device_model, COALESCE(:recorded_at, NOW())
                ) RETURNING *
            """),
            data,
        )
        await self._session.commit()
        return GeoEvent.model_validate(dict(result.mappings().one()))

    async def find_by_id(self, id: int) -> Optional[GeoEvent]:
        result = await self._session.execute(
            text("SELECT * FROM geo_events WHERE id = :id"),
            {"id": id},
        )
        row = result.mappings().one_or_none()
        return GeoEvent.model_validate(dict(row)) if row else None

    async def find_all(
        self,
        user_id: Optional[str] = None,
        event_type: Optional[str] = None,
        limit: int = 50,
        offset: int = 0,
    ) -> list[GeoEvent]:
        filters = []
        params: dict = {"limit": limit, "offset": offset}

        if user_id:
            filters.append("user_id = :user_id")
            params["user_id"] = user_id
        if event_type:
            filters.append("event_type = :event_type")
            params["event_type"] = event_type

        where = f"WHERE {' AND '.join(filters)}" if filters else ""

        result = await self._session.execute(
            text(f"SELECT * FROM geo_events {where} ORDER BY recorded_at DESC LIMIT :limit OFFSET :offset"),
            params,
        )
        return [GeoEvent.model_validate(dict(row)) for row in result.mappings()]

    async def delete(self, id: int) -> bool:
        result = await self._session.execute(
            text("DELETE FROM geo_events WHERE id = :id"),
            {"id": id},
        )
        await self._session.commit()
        return result.rowcount > 0
