import uuid
from pathlib import Path
from typing import Optional

from app.projects.c21200065.domain.exceptions import StorageAccessError, StorageFileNotFoundError
from app.projects.c21200065.domain.models.file import File
from app.projects.c21200065.domain.ports import IFileRepository, IStorageClient
from app.projects.c21200065.infra.settings import settings


class StorageService:

    def __init__(self, file_repo: IFileRepository, storage_client: IStorageClient) -> None:
        self._repo = file_repo
        self._storage = storage_client

    async def generate_upload_url(
        self,
        project_slug: str,
        user_id: str,
        file_name: str,
        content_type: str,
        expires_in: int = 3600,
    ) -> dict:
        ext = Path(file_name).suffix
        object_key = f"{project_slug}/{user_id}/{uuid.uuid4()}{ext}"
        upload_url = await self._storage.get_upload_url(object_key, content_type, expires_in)
        return {"upload_url": upload_url, "object_key": object_key, "expires_in": expires_in}

    async def confirm_upload(
        self,
        project_slug: str,
        user_id: str,
        object_key: str,
        file_name: str,
        content_type: Optional[str],
        size_bytes: Optional[int],
        is_public: bool,
    ) -> File:
        if not object_key.startswith(f"{project_slug}/{user_id}/"):
            raise StorageAccessError

        return await self._repo.create({
            "project_slug": project_slug,
            "user_id": user_id,
            "storage_provider": settings.OBJECT_STORAGE_PROVIDER,
            "bucket": settings.OBJECT_STORAGE_BUCKET,
            "object_key": object_key,
            "url": None,
            "file_name": file_name,
            "content_type": content_type,
            "size_bytes": size_bytes,
            "is_public": is_public,
        })

    async def list_files(
        self,
        project_slug: str,
        user_id: str,
        limit: int = 50,
        offset: int = 0,
    ) -> list[File]:
        return await self._repo.find_by_project_and_user(project_slug, user_id, limit, offset)

    async def delete_file(self, file_id: int, project_slug: str, user_id: str) -> None:
        file = await self._repo.find_by_id(file_id)

        if not file:
            raise StorageFileNotFoundError

        if str(file.user_id) != user_id or file.project_slug != project_slug:
            raise StorageAccessError

        await self._storage.delete(file.object_key)
        await self._repo.delete(file_id)
