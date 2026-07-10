import datetime
import re
from typing import Optional

from app.projects.c21200065.domain.exceptions import (
    InvalidCredentialsError,
    InvalidGoogleTokenError,
    InvalidRefreshTokenError,
    UserAlreadyExistsError,
)
from app.projects.c21200065.domain.ports import IGoogleOAuthClient, IRefreshTokenRepository, IUserRepository
from app.projects.c21200065.domain.security import generate_refresh_token_value, hash_password, verify_password


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
        name: Optional[str] = None,
        username: Optional[str] = None,
        goal: Optional[str] = None,
    ) -> str:
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
                "timezone": "America/Lima",
            }
        )
        return str(result.inserted_id)

    async def google_login(self, id_token: str) -> tuple[str, str]:
        data = await self._google_client.verify_id_token(id_token)

        if not data:
            raise InvalidGoogleTokenError

        email = data.get("email")
        google_id = data.get("sub")
        if not email or not google_id:
            raise InvalidGoogleTokenError

        user = await self._user_repo.find_by_google_id(google_id)
        if not user:
            user = await self._user_repo.find_by_email(email)

        profile_values = {
            "google_id": google_id,
            "name": _clean_text(data.get("name")),
            "username": _username_or_email(None, email),
            "avatar_url": _clean_text(data.get("picture")),
            "timezone": "America/Lima",
        }

        if not user:
            result = await self._user_repo.create_user(
                {
                    "email": email,
                    "auth_provider": "google",
                    **profile_values,
                }
            )
            return str(result.inserted_id), email

        missing_profile_values = {
            key: value
            for key, value in profile_values.items()
            if value and not user.get(key)
        }
        if missing_profile_values:
            user = await self._user_repo.update_user(str(user["_id"]), missing_profile_values) or user

        return str(user["_id"]), email

    async def get_profile(self, user_id: str, email: str) -> dict:
        user = await self._user_repo.find_by_id(user_id) or await self._user_repo.find_by_email(email)
        if not user:
            return _profile_from_email(user_id, email)
        return _profile_from_user(user)

    async def update_profile(
        self,
        user_id: str,
        name: str,
        username: Optional[str] = None,
        goal: Optional[str] = None,
        timezone: Optional[str] = None,
    ) -> dict:
        current = await self._user_repo.find_by_id(user_id)
        if not current:
            raise InvalidCredentialsError

        values = {
            "name": _clean_text(name),
            "username": _username_or_email(username, current["email"]),
            "goal": _clean_text(goal),
            "timezone": _clean_text(timezone) or "America/Lima",
        }
        user = await self._user_repo.update_user(user_id, values) or current
        return _profile_from_user(user)

    async def create_refresh_token(self, user_id: str, email: str, device_id: Optional[str] = None) -> str:
        token, expires_at = generate_refresh_token_value(self._refresh_token_expire_days)
        await self._refresh_token_repo.create(token, user_id, email, expires_at, device_id)
        return token

    async def rotate_refresh_token(self, old_token: str, device_id: Optional[str] = None) -> tuple[str, str, str]:
        doc = await self._refresh_token_repo.find(old_token)

        if not doc:
            raise InvalidRefreshTokenError

        expires_at: datetime.datetime = doc["expires_at"]
        if expires_at.tzinfo is None:
            expires_at = expires_at.replace(tzinfo=datetime.timezone.utc)

        if expires_at < datetime.datetime.now(datetime.UTC):
            await self._refresh_token_repo.delete(old_token)
            raise InvalidRefreshTokenError

        stored_device_id = doc.get("device_id")
        if stored_device_id and stored_device_id != device_id:
            raise InvalidRefreshTokenError

        await self._refresh_token_repo.delete(old_token)

        new_token, new_expires_at = generate_refresh_token_value(self._refresh_token_expire_days)
        await self._refresh_token_repo.create(new_token, doc["user_id"], doc["email"], new_expires_at, device_id)

        return doc["user_id"], doc["email"], new_token


def _clean_text(value: Optional[str]) -> Optional[str]:
    if value is None:
        return None
    clean = value.strip()
    return clean or None


def _username_or_email(username: Optional[str], email: str) -> str:
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
        "timezone": "America/Lima",
        "avatar_url": None,
        "profile_complete": False,
    }


def _profile_from_user(user: dict) -> dict:
    email = user["email"]
    username = user.get("username") or email.split("@", 1)[0]
    name = user.get("name") or ""
    goal = user.get("goal") or ""
    return {
        "id": str(user["_id"]),
        "name": name,
        "username": username,
        "email": email,
        "bio": "",
        "goal": goal,
        "timezone": user.get("timezone") or "America/Lima",
        "avatar_url": user.get("avatar_url"),
        "profile_complete": bool(name and username),
    }
