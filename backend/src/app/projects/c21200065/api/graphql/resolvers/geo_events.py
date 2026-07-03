from typing import Optional

import strawberry

from app.projects.c21200065.api.graphql.types import GeoEventInput, GeoEventType
from app.projects.c21200065.infra.repositories.geo_event_repo import GeoEventRepository


def _to_type(event) -> GeoEventType:
    return GeoEventType(**event.model_dump())


@strawberry.type
class GeoEventQuery:

    @strawberry.field
    async def geo_event(self, id: int, info: strawberry.types.Info) -> Optional[GeoEventType]:
        repo: GeoEventRepository = info.context["repo"]
        event = await repo.find_by_id(id)
        return _to_type(event) if event else None

    @strawberry.field
    async def geo_events(
        self,
        info: strawberry.types.Info,
        user_id: Optional[str] = None,
        event_type: Optional[str] = None,
        limit: int = 50,
        offset: int = 0,
    ) -> list[GeoEventType]:
        repo: GeoEventRepository = info.context["repo"]
        events = await repo.find_all(user_id=user_id, event_type=event_type, limit=limit, offset=offset)
        return [_to_type(e) for e in events]


@strawberry.type
class GeoEventMutation:

    @strawberry.mutation
    async def create_geo_event(self, input: GeoEventInput, info: strawberry.types.Info) -> GeoEventType:
        repo: GeoEventRepository = info.context["repo"]
        event = await repo.create(strawberry.asdict(input))
        return _to_type(event)

    @strawberry.mutation
    async def delete_geo_event(self, id: int, info: strawberry.types.Info) -> bool:
        repo: GeoEventRepository = info.context["repo"]
        return await repo.delete(id)
