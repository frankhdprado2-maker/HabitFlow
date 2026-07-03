from fastapi import APIRouter

from app.projects.c21200065.api.ai import router as ai_router
from app.projects.c21200065.api.auth import router as auth_router
from app.projects.c21200065.api.geo_events import router as geo_events_router
from app.projects.c21200065.api.geo_events_orm import router as geo_events_orm_router
from app.projects.c21200065.api.graphql.router import router as graphql_router
from app.projects.c21200065.api.storage import router as storage_router

router = APIRouter()


@router.get("/health")
async def health():
    return {"project": "c21200065", "status": "ok"}


router.include_router(auth_router)
router.include_router(geo_events_router)
router.include_router(geo_events_orm_router)
router.include_router(graphql_router, prefix="/graphql")
router.include_router(storage_router)
router.include_router(ai_router)
