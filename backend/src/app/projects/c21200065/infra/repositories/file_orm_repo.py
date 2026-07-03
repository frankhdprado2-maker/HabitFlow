from datetime import UTC, datetime
from typing import Optional

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.projects.c21200065.domain.models.file import File
from app.projects.c21200065.infra.orm.file import FileORM


class FileORMRepository:

    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def create(self, data: dict) -> File:
        record = FileORM(**data)
        self._session.add(record)
        await self._session.commit()
        await self._session.refresh(record)
        return File.model_validate(record)

    async def find_by_id(self, id: int) -> Optional[File]:
        stmt = select(FileORM).where(FileORM.id == id, FileORM.deleted_at.is_(None))
        result = await self._session.execute(stmt)
        row = result.scalar_one_or_none()
        return File.model_validate(row) if row else None

    async def find_by_project_and_user(
        self,
        project_slug: str,
        user_id: str,
        limit: int = 50,
        offset: int = 0,
    ) -> list[File]:
        stmt = (
            select(FileORM)
            .where(
                FileORM.project_slug == project_slug,
                FileORM.user_id == user_id,
                FileORM.deleted_at.is_(None),
            )
            .order_by(FileORM.uploaded_at.desc())
            .limit(limit)
            .offset(offset)
        )
        result = await self._session.execute(stmt)
        return [File.model_validate(row) for row in result.scalars()]

    async def delete(self, id: int) -> bool:
        result = await self._session.execute(
            update(FileORM)
            .where(FileORM.id == id, FileORM.deleted_at.is_(None))
            .values(deleted_at=datetime.now(UTC))
        )
        await self._session.commit()
        return result.rowcount > 0
