from typing import AsyncGenerator

import jwt
from fastapi import Depends, HTTPException
from fastapi.security import HTTPBearer
from sqlalchemy.ext.asyncio import AsyncSession

from app.projects.c21200065.domain.auth_service import AuthService
from app.projects.c21200065.infra.clients.google import GoogleOAuthClient
from app.projects.c21200065.infra.db.postgres import async_session
from app.projects.c21200065.domain.storage_service import StorageService
from app.projects.c21200065.infra.clients.storage import storage_client
from app.projects.c21200065.infra.repositories.file_orm_repo import FileORMRepository
from app.projects.c21200065.infra.repositories.geo_event_orm_repo import GeoEventORMRepository
from app.projects.c21200065.infra.repositories.geo_event_repo import GeoEventRepository
from app.projects.c21200065.infra.repositories.refresh_token_repo import RefreshTokenRepository
from app.projects.c21200065.infra.repositories.user_repo import UserRepository
from app.projects.c21200065.infra.settings import settings

security = HTTPBearer()

_user_repo = UserRepository()
_google_client = GoogleOAuthClient()
_refresh_token_repo = RefreshTokenRepository()
_auth_service = AuthService(_user_repo, _google_client, _refresh_token_repo, settings.REFRESH_TOKEN_EXPIRE_DAYS)


def get_auth_service() -> AuthService:
    return _auth_service


async def get_db_session() -> AsyncGenerator[AsyncSession, None]:
    async with async_session() as session:
        yield session


def get_geo_event_repo(session: AsyncSession = Depends(get_db_session)) -> GeoEventRepository:
    return GeoEventRepository(session)


def get_geo_event_orm_repo(session: AsyncSession = Depends(get_db_session)) -> GeoEventORMRepository:
    return GeoEventORMRepository(session)


def get_storage_service(session: AsyncSession = Depends(get_db_session)) -> StorageService:
    return StorageService(FileORMRepository(session), storage_client)


def get_current_user(token=Depends(security)):
    try:
        payload = jwt.decode(
            token.credentials,
            settings.SECRET_KEY,
            algorithms=[settings.ALGORITHM],
        )
        return {"user_id": payload["sub"], "email": payload["email"]}
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid token") from None
