from typing import Any, Optional

from app.projects.c21200065.infra.db.mongo import database


class UserRepository:

    def __init__(self) -> None:
        self._collection = database["mobile_users"]

    async def find_by_email(self, email: str) -> Optional[dict[str, Any]]:
        return await self._collection.find_one({"email": email})

    async def find_by_google_id(self, google_id: str) -> Optional[dict[str, Any]]:
        return await self._collection.find_one({"google_id": google_id})

    async def create_user(self, user: dict[str, Any]):
        return await self._collection.insert_one(user)
