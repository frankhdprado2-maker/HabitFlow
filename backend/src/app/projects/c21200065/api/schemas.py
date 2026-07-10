from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, EmailStr, Field, field_validator


class LoginRequest(BaseModel):
    email: EmailStr
    password: str
    device_id: str | None = None


class RegisterRequest(LoginRequest):
    name: str | None = None
    username: str | None = None
    goal: str | None = None
    avatar_key: str | None = None
    categories: list[str] = Field(default_factory=list)


class GoogleLoginRequest(BaseModel):
    token: str
    device_id: str | None = None


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class RefreshTokenRequest(BaseModel):
    refresh_token: str
    device_id: str | None = None


class RegisterResponse(BaseModel):
    user_id: str


class ProfileUpdateRequest(BaseModel):
    name: str = Field(min_length=1, max_length=80)
    username: str | None = Field(default=None, max_length=32, pattern=r"^[A-Za-z0-9_.-]+$")
    bio: str | None = Field(default=None, max_length=280)
    goal: str | None = Field(default=None, max_length=120)
    primary_goal: str | None = Field(default=None, max_length=120)
    timezone: str | None = Field(default=None, max_length=64)
    avatar_key: str | None = Field(default=None, max_length=64)
    categories: list[str] = Field(default_factory=list)
    preferred_categories: list[str] = Field(default_factory=list)
    onboarding_completed: bool | None = None
    theme_mode: str | None = Field(default=None, pattern=r"^(light|dark|system)$")
    accent_theme: str | None = Field(default=None, pattern=r"^(mint|coral|amber|ocean|violet)$")
    voice_response_enabled: bool | None = None
    locale: str | None = Field(default=None, pattern=r"^(es-PE|es-ES|en-US)$")

    @field_validator("categories", "preferred_categories")
    @classmethod
    def clean_categories(cls, value: list[str]) -> list[str]:
        clean: list[str] = []
        for item in value:
            category = item.strip()[:40]
            if category and category not in clean:
                clean.append(category)
        return clean[:8]


class UserProfileResponse(BaseModel):
    id: str
    name: str = ""
    username: str | None = None
    email: EmailStr
    bio: str = ""
    goal: str = ""
    primary_goal: str = ""
    timezone: str = "America/Lima"
    avatar_url: str | None = None
    avatar_key: str | None = None
    categories: list[str] = Field(default_factory=list)
    preferred_categories: list[str] = Field(default_factory=list)
    onboarding_completed: bool = False
    theme_mode: str = "system"
    accent_theme: str = "mint"
    voice_response_enabled: bool = True
    locale: str = "es-PE"
    profile_complete: bool = False


# Storage

class UploadUrlRequest(BaseModel):
    file_name: str
    content_type: str
    size_bytes: int | None = None
    is_public: bool = False
    expires_in: int = 3600


class UploadUrlResponse(BaseModel):
    upload_url: str
    object_key: str
    expires_in: int


class ConfirmUploadRequest(BaseModel):
    object_key: str
    file_name: str
    content_type: str | None = None
    size_bytes: int | None = None
    is_public: bool = False


class FileResponse(BaseModel):
    id: int
    project_slug: str
    user_id: str
    storage_provider: str
    bucket: str
    object_key: str
    url: str | None = None
    file_name: str
    content_type: str | None = None
    size_bytes: int | None = None
    is_public: bool
    uploaded_at: datetime


class GeoEventCreate(BaseModel):
    user_id: UUID | None = None
    latitude: float
    longitude: float
    altitude: float | None = None
    accuracy: float | None = None
    speed: float | None = None
    heading: float | None = None
    event_type: str = "gps_ping"
    device_id: str | None = None
    platform: str | None = None
    app_version: str | None = None
    device_model: str | None = None
    recorded_at: datetime | None = None


class GeoEventResponse(BaseModel):
    id: int
    user_id: UUID | None = None
    latitude: float
    longitude: float
    altitude: float | None = None
    accuracy: float | None = None
    speed: float | None = None
    heading: float | None = None
    event_type: str
    device_id: str | None = None
    platform: str | None = None
    app_version: str | None = None
    device_model: str | None = None
    recorded_at: datetime
    created_at: datetime
