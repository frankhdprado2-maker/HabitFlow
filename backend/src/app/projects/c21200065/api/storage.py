from fastapi import APIRouter, Depends, HTTPException

from app.projects.c21200065.api.deps import get_current_user, get_storage_service
from app.projects.c21200065.api.schemas import (
    ConfirmUploadRequest,
    FileResponse,
    UploadUrlRequest,
    UploadUrlResponse,
)
from app.projects.c21200065.domain.exceptions import StorageAccessError, StorageFileNotFoundError
from app.projects.c21200065.domain.storage_service import StorageService
from app.projects.c21200065.infra.settings import PROJECT_NAME

router = APIRouter(prefix="/storage", tags=["storage"])


@router.post("/upload-url", response_model=UploadUrlResponse)
async def get_upload_url(
    payload: UploadUrlRequest,
    current_user=Depends(get_current_user),
    service: StorageService = Depends(get_storage_service),
):
    result = await service.generate_upload_url(
        project_slug=PROJECT_NAME,
        user_id=current_user["user_id"],
        file_name=payload.file_name,
        content_type=payload.content_type,
        expires_in=payload.expires_in,
    )
    return UploadUrlResponse(**result)


@router.post("/confirm", response_model=FileResponse, status_code=201)
async def confirm_upload(
    payload: ConfirmUploadRequest,
    current_user=Depends(get_current_user),
    service: StorageService = Depends(get_storage_service),
):
    try:
        file = await service.confirm_upload(
            project_slug=PROJECT_NAME,
            user_id=current_user["user_id"],
            object_key=payload.object_key,
            file_name=payload.file_name,
            content_type=payload.content_type,
            size_bytes=payload.size_bytes,
            is_public=payload.is_public,
        )
    except StorageAccessError:
        raise HTTPException(status_code=403, detail="Invalid object_key for this user") from None
    return FileResponse.model_validate(file.model_dump())


@router.get("/files", response_model=list[FileResponse])
async def list_files(
    limit: int = 50,
    offset: int = 0,
    current_user=Depends(get_current_user),
    service: StorageService = Depends(get_storage_service),
):
    files = await service.list_files(
        project_slug=PROJECT_NAME,
        user_id=current_user["user_id"],
        limit=limit,
        offset=offset,
    )
    return [FileResponse.model_validate(f.model_dump()) for f in files]


@router.delete("/file/{file_id}", status_code=204)
async def delete_file(
    file_id: int,
    current_user=Depends(get_current_user),
    service: StorageService = Depends(get_storage_service),
):
    try:
        await service.delete_file(
            file_id=file_id,
            project_slug=PROJECT_NAME,
            user_id=current_user["user_id"],
        )
    except StorageFileNotFoundError:
        raise HTTPException(status_code=404, detail="File not found") from None
    except StorageAccessError:
        raise HTTPException(status_code=403, detail="Access denied") from None
