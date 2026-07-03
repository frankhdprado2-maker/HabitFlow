import strawberry
from fastapi import Depends
from sqlalchemy.ext.asyncio import AsyncSession
from strawberry.fastapi import GraphQLRouter

from app.projects.c21200065.api.deps import get_db_session
from app.projects.c21200065.api.graphql.resolvers.geo_events import GeoEventMutation, GeoEventQuery
from app.projects.c21200065.infra.repositories.geo_event_repo import GeoEventRepository


async def get_context(session: AsyncSession = Depends(get_db_session)):
    return {"repo": GeoEventRepository(session)}


schema = strawberry.Schema(query=GeoEventQuery, mutation=GeoEventMutation)

router = GraphQLRouter(schema, context_getter=get_context)
