import datetime
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

    async def register(self, email: str, password: str) -> str:
        if await self._user_repo.find_by_email(email):
            raise UserAlreadyExistsError

        result = await self._user_repo.create_user(
            {
                "email": email,
                "password": hash_password(password),
                "auth_provider": "local",
            }
        )
        return str(result.inserted_id)

    async def google_login(self, id_token: str) -> tuple[str, str]:
        data = await self._google_client.verify_id_token(id_token)

        if not data:
            raise InvalidGoogleTokenError

        email = data["email"]
        google_id = data["sub"]

        if not await self._user_repo.find_by_email(email):
            await self._user_repo.create_user(
                {
                    "email": email,
                    "google_id": google_id,
                    "auth_provider": "google",
                }
            )

        return google_id, email

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
