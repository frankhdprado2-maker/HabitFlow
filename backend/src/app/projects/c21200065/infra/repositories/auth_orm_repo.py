from datetime import datetime
from types import SimpleNamespace
from typing import Any, Optional
from uuid import uuid4

from sqlalchemy import delete, select

from app.projects.c21200065.infra.db.postgres import async_session
from app.projects.c21200065.infra.orm.auth import AuthRefreshTokenORM, AuthUserORM


class UserORMRepository:
    async def find_by_email(self, email: str) -> Optional[dict[str, Any]]:
        async with async_session() as session:
            result = await session.execute(select(AuthUserORM).where(AuthUserORM.email == email))
            user = result.scalar_one_or_none()
            return _user_to_dict(user)

    async def find_by_google_id(self, google_id: str) -> Optional[dict[str, Any]]:
        async with async_session() as session:
            result = await session.execute(select(AuthUserORM).where(AuthUserORM.google_id == google_id))
            user = result.scalar_one_or_none()
            return _user_to_dict(user)

    async def create_user(self, user: dict[str, Any]):
        user_id = str(uuid4())
        record = AuthUserORM(
            id=user_id,
            email=user["email"],
            password=user.get("password"),
            google_id=user.get("google_id"),
            auth_provider=user["auth_provider"],
        )
        async with async_session() as session:
            session.add(record)
            await session.commit()
        return SimpleNamespace(inserted_id=user_id)


class RefreshTokenORMRepository:
    async def create(
        self,
        token: str,
        user_id: str,
        email: str,
        expires_at: datetime,
        device_id: Optional[str],
    ) -> None:
        record = AuthRefreshTokenORM(
            token=token,
            user_id=user_id,
            email=email,
            expires_at=expires_at,
            device_id=device_id,
        )
        async with async_session() as session:
            session.add(record)
            await session.commit()

    async def find(self, token: str) -> Optional[dict[str, Any]]:
        async with async_session() as session:
            result = await session.execute(select(AuthRefreshTokenORM).where(AuthRefreshTokenORM.token == token))
            record = result.scalar_one_or_none()
            if record is None:
                return None
            return {
                "token": record.token,
                "user_id": record.user_id,
                "email": record.email,
                "expires_at": record.expires_at,
                "device_id": record.device_id,
            }

    async def delete(self, token: str) -> None:
        async with async_session() as session:
            await session.execute(delete(AuthRefreshTokenORM).where(AuthRefreshTokenORM.token == token))
            await session.commit()


def _user_to_dict(user: AuthUserORM | None) -> Optional[dict[str, Any]]:
    if user is None:
        return None
    return {
        "_id": user.id,
        "email": user.email,
        "password": user.password,
        "google_id": user.google_id,
        "auth_provider": user.auth_provider,
    }
