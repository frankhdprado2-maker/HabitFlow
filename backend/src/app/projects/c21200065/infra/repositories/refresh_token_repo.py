import datetime
from typing import Any, Optional

from app.projects.c21200065.infra.db.mongo import database


class RefreshTokenRepository:

    def __init__(self) -> None:
        self._collection = database["mobile_refresh_tokens"]

    async def create(
        self,
        token: str,
        user_id: str,
        email: str,
        expires_at: datetime.datetime,
        device_id: Optional[str],
    ) -> None:
        await self._collection.insert_one({
            "token": token,
            "user_id": user_id,
            "email": email,
            "expires_at": expires_at,
            "device_id": device_id,
            "created_at": datetime.datetime.now(datetime.UTC),
        })

    async def find(self, token: str) -> Optional[dict[str, Any]]:
        return await self._collection.find_one({"token": token})

    async def delete(self, token: str) -> None:
        await self._collection.delete_one({"token": token})
