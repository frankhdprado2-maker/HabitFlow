from datetime import datetime
from types import SimpleNamespace
from typing import Any, Optional
from uuid import uuid4

from sqlalchemy import delete, select, update

from app.projects.c21200065.infra.db.postgres import async_session
from app.projects.c21200065.infra.orm.auth import AuthRefreshTokenORM, AuthUserORM


class UserORMRepository:
    async def find_by_id(self, user_id: str) -> Optional[dict[str, Any]]:
        async with async_session() as session:
            result = await session.execute(select(AuthUserORM).where(AuthUserORM.id == user_id))
            user = result.scalar_one_or_none()
            return _user_to_dict(user)

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
            name=user.get("name"),
            username=user.get("username"),
            goal=user.get("goal"),
            timezone=user.get("timezone"),
            avatar_url=user.get("avatar_url"),
        )
        async with async_session() as session:
            session.add(record)
            await session.commit()
        return SimpleNamespace(inserted_id=user_id)

    async def update_user(self, user_id: str, values: dict[str, Any]) -> Optional[dict[str, Any]]:
        clean_values = {key: value for key, value in values.items() if value is not None}
        if clean_values:
            async with async_session() as session:
                await session.execute(update(AuthUserORM).where(AuthUserORM.id == user_id).values(**clean_values))
                await session.commit()
        return await self.find_by_id(user_id)


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
        "name": user.name,
        "username": user.username,
        "goal": user.goal,
        "timezone": user.timezone,
        "avatar_url": user.avatar_url,
    }
