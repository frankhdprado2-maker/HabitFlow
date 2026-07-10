import datetime
import json
import re

from app.projects.c21200065.domain.exceptions import (
    InvalidCredentialsError,
    InvalidGoogleTokenError,
    InvalidRefreshTokenError,
    UserAlreadyExistsError,
)
from app.projects.c21200065.domain.ports import (
    IGoogleOAuthClient,
    IRefreshTokenRepository,
    IUserRepository,
)
from app.projects.c21200065.domain.security import (
    generate_refresh_token_value,
    hash_password,
    verify_password,
)


class AuthService:

    def __init__(
        self,
        user_repo: IUserRepository,
        google_client: IGoogleOAuthClient,
        refresh_token_repo: IRefreshTokenRepository,
        refresh_token_expire_days: int = 30,
    ) -> None:
        self._user_repo = user_repo
        self._google_client = google_client
        self._refresh_token_repo = refresh_token_repo
        self._refresh_token_expire_days = refresh_token_expire_days

    async def login(self, email: str, password: str) -> tuple[str, str]:
        email = _normalize_email(email)
        user = await self._user_repo.find_by_email(email)

        if not user or not user.get("password"):
            raise InvalidCredentialsError

        if not verify_password(password, user["password"]):
            raise InvalidCredentialsError

        return str(user["_id"]), email

    async def register(
        self,
        email: str,
        password: str,
        name: str | None = None,
        username: str | None = None,
        goal: str | None = None,
        avatar_key: str | None = None,
        categories: list[str] | None = None,
    ) -> str:
        email = _normalize_email(email)
        if await self._user_repo.find_by_email(email):
            raise UserAlreadyExistsError

        result = await self._user_repo.create_user(
            {
                "email": email,
                "password": hash_password(password),
                "auth_provider": "local",
                "name": _clean_text(name),
                "username": _username_or_email(username, email),
                "goal": _clean_text(goal),
                "primary_goal": _clean_text(goal),
                "timezone": "America/Lima",
                "avatar_key": _clean_text(avatar_key),
                "categories": _categories_to_json(categories or []),
                "preferred_categories": _categories_to_json(categories or []),
                "onboarding_completed": False,
                "theme_mode": "system",
                "accent_theme": "mint",
                "voice_response_enabled": True,
                "locale": "es-PE",
            }
        )
        return str(result.inserted_id)

    async def google_login(self, id_token: str) -> tuple[str, str]:
        data = await self._google_client.verify_id_token(id_token)

        if not data:
            raise InvalidGoogleTokenError

        email = _normalize_email(data.get("email", ""))
        google_id = data.get("sub")
        if not email or not google_id:
            raise InvalidGoogleTokenError

        user = await self._user_repo.find_by_google_id(google_id)
        if not user:
            user = await self._user_repo.find_by_email(email)

        new_profile_values = {
            "google_id": google_id,
            "name": _clean_text(data.get("name")),
            "username": _username_or_email(None, email),
            "avatar_url": _clean_text(data.get("picture")),
            "timezone": "America/Lima",
            "onboarding_completed": False,
            "theme_mode": "system",
            "accent_theme": "mint",
            "voice_response_enabled": True,
            "locale": "es-PE",
        }

        if not user:
            result = await self._user_repo.create_user(
                {
                    "email": email,
                    "auth_provider": "google",
                    **new_profile_values,
                }
            )
            return str(result.inserted_id), email

        existing_profile_values = {
            "google_id": google_id,
            "name": _clean_text(data.get("name")),
            "avatar_url": _clean_text(data.get("picture")),
        }
        missing_profile_values = {
            key: value
            for key, value in existing_profile_values.items()
            if value and not user.get(key)
        }
        if missing_profile_values:
            user = (
                await self._user_repo.update_user(str(user["_id"]), missing_profile_values)
                or user
            )

        return str(user["_id"]), email

    async def get_profile(self, user_id: str, email: str) -> dict:
        user = await self._user_repo.find_by_id(user_id)
        user = user or await self._user_repo.find_by_email(email)
        if not user:
            return _profile_from_email(user_id, email)
        return _profile_from_user(user)

    async def update_profile(
        self,
        user_id: str,
        name: str,
        username: str | None = None,
        bio: str | None = None,
        goal: str | None = None,
        primary_goal: str | None = None,
        timezone: str | None = None,
        avatar_key: str | None = None,
        categories: list[str] | None = None,
        preferred_categories: list[str] | None = None,
        onboarding_completed: bool | None = None,
        theme_mode: str | None = None,
        accent_theme: str | None = None,
        voice_response_enabled: bool | None = None,
        locale: str | None = None,
    ) -> dict:
        current = await self._user_repo.find_by_id(user_id)
        if not current:
            raise InvalidCredentialsError

        values = {
            "name": _clean_text(name),
            "username": _username_or_email(username, current["email"]),
            "bio": _limited_text(bio, 280),
            "goal": _clean_text(goal),
            "primary_goal": _clean_text(primary_goal) or _clean_text(goal),
            "timezone": _clean_text(timezone) or "America/Lima",
            "avatar_key": _clean_text(avatar_key),
            "categories": _categories_to_json(categories or []),
            "preferred_categories": _categories_to_json(preferred_categories or categories or []),
            "onboarding_completed": onboarding_completed,
            "theme_mode": _enum_or_default(theme_mode, {"light", "dark", "system"}, "system"),
            "accent_theme": _enum_or_default(
                accent_theme,
                {"mint", "coral", "amber", "ocean", "violet"},
                "mint",
            ),
            "voice_response_enabled": voice_response_enabled,
            "locale": _enum_or_default(locale, {"es-PE", "es-ES", "en-US"}, "es-PE"),
        }
        user = await self._user_repo.update_user(user_id, values) or current
        return _profile_from_user(user)

    async def create_refresh_token(
        self,
        user_id: str,
        email: str,
        device_id: str | None = None,
    ) -> str:
        token, expires_at = generate_refresh_token_value(self._refresh_token_expire_days)
        await self._refresh_token_repo.create(token, user_id, email, expires_at, device_id)
        return token

    async def rotate_refresh_token(
        self,
        old_token: str,
        device_id: str | None = None,
    ) -> tuple[str, str, str]:
        doc = await self._refresh_token_repo.find(old_token)

        if not doc:
            raise InvalidRefreshTokenError

        expires_at: datetime.datetime = doc["expires_at"]
        if expires_at.tzinfo is None:
            expires_at = expires_at.replace(tzinfo=datetime.UTC)

        if expires_at < datetime.datetime.now(datetime.UTC):
            await self._refresh_token_repo.delete(old_token)
            raise InvalidRefreshTokenError

        stored_device_id = doc.get("device_id")
        if stored_device_id and stored_device_id != device_id:
            raise InvalidRefreshTokenError

        await self._refresh_token_repo.delete(old_token)

        new_token, new_expires_at = generate_refresh_token_value(self._refresh_token_expire_days)
        await self._refresh_token_repo.create(
            new_token,
            doc["user_id"],
            doc["email"],
            new_expires_at,
            device_id,
        )

        return doc["user_id"], doc["email"], new_token


def _clean_text(value: str | None) -> str | None:
    if value is None:
        return None
    clean = value.strip()
    return clean or None


def _normalize_email(value: str) -> str:
    return value.strip().lower()


def _limited_text(value: str | None, max_length: int) -> str | None:
    clean = _clean_text(value)
    return clean[:max_length] if clean else None


def _enum_or_default(value: str | None, allowed: set[str], default: str) -> str:
    clean = _clean_text(value)
    if clean in allowed:
        return clean
    return default


def _username_or_email(username: str | None, email: str) -> str:
    source = _clean_text(username) or email.split("@", 1)[0]
    normalized = re.sub(r"[^A-Za-z0-9_.-]+", "", source).strip("._-")
    return normalized[:32] or email.split("@", 1)[0]


def _profile_from_email(user_id: str, email: str) -> dict:
    username = email.split("@", 1)[0]
    return {
        "id": user_id,
        "name": "",
        "username": username,
        "email": email,
        "bio": "",
        "goal": "",
        "primary_goal": "",
        "timezone": "America/Lima",
        "avatar_url": None,
        "avatar_key": None,
        "categories": [],
        "preferred_categories": [],
        "onboarding_completed": False,
        "theme_mode": "system",
        "accent_theme": "mint",
        "voice_response_enabled": True,
        "locale": "es-PE",
        "profile_complete": False,
    }


def _profile_from_user(user: dict) -> dict:
    email = user["email"]
    username = user.get("username") or email.split("@", 1)[0]
    name = user.get("name") or ""
    goal = user.get("goal") or ""
    onboarding_completed = user.get("onboarding_completed")
    profile_complete = bool(name and username and onboarding_completed is not False)
    return {
        "id": str(user["_id"]),
        "name": name,
        "username": username,
        "email": email,
        "bio": user.get("bio") or "",
        "goal": goal,
        "primary_goal": user.get("primary_goal") or goal,
        "timezone": user.get("timezone") or "America/Lima",
        "avatar_url": user.get("avatar_url"),
        "avatar_key": user.get("avatar_key"),
        "categories": _categories_from_json(user.get("categories")),
        "preferred_categories": _categories_from_json(user.get("preferred_categories"))
        or _categories_from_json(user.get("categories")),
        "onboarding_completed": bool(onboarding_completed is not False and name and username),
        "theme_mode": user.get("theme_mode") or "system",
        "accent_theme": user.get("accent_theme") or "mint",
        "voice_response_enabled": user.get("voice_response_enabled") is not False,
        "locale": user.get("locale") or "es-PE",
        "profile_complete": profile_complete,
    }


def _categories_to_json(categories: list[str]) -> str:
    clean = []
    for category in categories:
        value = _clean_text(category)
        if value and value not in clean:
            clean.append(value[:40])
    return json.dumps(clean[:8])


def _categories_from_json(value: str | None) -> list[str]:
    if not value:
        return []
    try:
        data = json.loads(value)
    except json.JSONDecodeError:
        data = value.split(",")
    return [str(item).strip() for item in data if str(item).strip()]
