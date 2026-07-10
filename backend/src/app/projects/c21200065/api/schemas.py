from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel, EmailStr, Field


class LoginRequest(BaseModel):
    email: EmailStr
    password: str
    device_id: Optional[str] = None


class RegisterRequest(LoginRequest):
    name: Optional[str] = None
    username: Optional[str] = None
    goal: Optional[str] = None
    avatar_key: Optional[str] = None
    categories: list[str] = Field(default_factory=list)


class GoogleLoginRequest(BaseModel):
    token: str
    device_id: Optional[str] = None


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class RefreshTokenRequest(BaseModel):
    refresh_token: str
    device_id: Optional[str] = None


class RegisterResponse(BaseModel):
    user_id: str


class ProfileUpdateRequest(BaseModel):
    name: str
    username: Optional[str] = None
    goal: Optional[str] = None
    timezone: Optional[str] = None
    avatar_key: Optional[str] = None
    categories: list[str] = Field(default_factory=list)


class UserProfileResponse(BaseModel):
    id: str
    name: str = ""
    username: Optional[str] = None
    email: EmailStr
    bio: str = ""
    goal: str = ""
    timezone: str = "America/Lima"
    avatar_url: Optional[str] = None
    avatar_key: Optional[str] = None
    categories: list[str] = Field(default_factory=list)
    profile_complete: bool = False


# â”€â”€ Storage â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

class UploadUrlRequest(BaseModel):
    file_name: str
    content_type: str
    size_bytes: Optional[int] = None
    is_public: bool = False
    expires_in: int = 3600


class UploadUrlResponse(BaseModel):
    upload_url: str
    object_key: str
    expires_in: int


class ConfirmUploadRequest(BaseModel):
    object_key: str
    file_name: str
    content_type: Optional[str] = None
    size_bytes: Optional[int] = None
    is_public: bool = False


class FileResponse(BaseModel):
    id: int
    project_slug: str
    user_id: str
    storage_provider: str
    bucket: str
    object_key: str
    url: Optional[str] = None
    file_name: str
    content_type: Optional[str] = None
    size_bytes: Optional[int] = None
    is_public: bool
    uploaded_at: datetime


class GeoEventCreate(BaseModel):
    user_id: Optional[UUID] = None
    latitude: float
    longitude: float
    altitude: Optional[float] = None
    accuracy: Optional[float] = None
    speed: Optional[float] = None
    heading: Optional[float] = None
    event_type: str = "gps_ping"
    device_id: Optional[str] = None
    platform: Optional[str] = None
    app_version: Optional[str] = None
    device_model: Optional[str] = None
    recorded_at: Optional[datetime] = None


class GeoEventResponse(BaseModel):
    id: int
    user_id: Optional[UUID] = None
    latitude: float
    longitude: float
    altitude: Optional[float] = None
    accuracy: Optional[float] = None
    speed: Optional[float] = None
    heading: Optional[float] = None
    event_type: str
    device_id: Optional[str] = None
    platform: Optional[str] = None
    app_version: Optional[str] = None
    device_model: Optional[str] = None
    recorded_at: datetime
    created_at: datetime
