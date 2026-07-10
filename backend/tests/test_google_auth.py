import pytest

from app.projects.c21200065.domain.auth_service import AuthService
from app.projects.c21200065.infra.clients.google import GoogleOAuthClient
from app.projects.c21200065.infra.settings import settings


class FakeGoogleClient:
    async def verify_id_token(self, id_token: str) -> dict:
        assert id_token == "google-token"
        return {
            "sub": "google-sub",
            "email": "FRANK@example.com",
            "name": "Frank Google",
            "picture": "https://example.com/frank.png",
        }


class FakeRefreshTokenRepo:
    pass


class FakeUserRepo:
    def __init__(self) -> None:
        self.user = {
            "_id": "existing-user",
            "email": "frank@example.com",
            "auth_provider": "local",
            "name": "Frank Local",
            "username": "frank",
            "onboarding_completed": True,
            "theme_mode": "dark",
            "accent_theme": "coral",
            "voice_response_enabled": False,
            "google_id": None,
            "avatar_url": None,
        }
        self.updated_values: dict | None = None

    async def find_by_email(self, email: str) -> dict | None:
        return self.user if email == self.user["email"] else None

    async def find_by_google_id(self, google_id: str) -> dict | None:
        return self.user if google_id == self.user.get("google_id") else None

    async def find_by_id(self, user_id: str) -> dict | None:
        return self.user if user_id == self.user["_id"] else None

    async def update_user(self, user_id: str, values: dict) -> dict:
        assert user_id == self.user["_id"]
        self.updated_values = values
        self.user.update(values)
        return self.user


@pytest.mark.asyncio
async def test_google_login_links_existing_email_without_overwriting_preferences() -> None:
    repo = FakeUserRepo()
    service = AuthService(repo, FakeGoogleClient(), FakeRefreshTokenRepo())

    user_id, email = await service.google_login("google-token")

    assert user_id == "existing-user"
    assert email == "frank@example.com"
    assert repo.updated_values == {
        "google_id": "google-sub",
        "avatar_url": "https://example.com/frank.png",
    }
    assert repo.user["onboarding_completed"] is True
    assert repo.user["theme_mode"] == "dark"
    assert repo.user["accent_theme"] == "coral"
    assert repo.user["voice_response_enabled"] is False


class FakeResponse:
    def __init__(self, status_code: int, payload: dict) -> None:
        self.status_code = status_code
        self._payload = payload

    def json(self) -> dict:
        return self._payload


class FakeAsyncClient:
    payload: dict = {}
    status_code = 200

    def __init__(self, timeout: float) -> None:
        self.timeout = timeout

    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc, tb) -> None:
        return None

    async def get(self, url: str, params: dict) -> FakeResponse:
        assert url == GoogleOAuthClient.TOKENINFO_URL
        assert params == {"id_token": "token"}
        return FakeResponse(self.status_code, self.payload)


@pytest.mark.asyncio
async def test_google_oauth_client_rejects_wrong_audience(monkeypatch) -> None:
    import app.projects.c21200065.infra.clients.google as google_module

    monkeypatch.setattr(google_module.httpx, "AsyncClient", FakeAsyncClient)
    monkeypatch.setattr(settings, "GOOGLE_WEB_CLIENT_ID", "expected-client")
    FakeAsyncClient.payload = {
        "aud": "other-client",
        "iss": "https://accounts.google.com",
        "email_verified": "true",
        "sub": "google-sub",
        "email": "frank@example.com",
    }

    assert await GoogleOAuthClient().verify_id_token("token") is None


@pytest.mark.asyncio
async def test_google_oauth_client_rejects_unverified_email(monkeypatch) -> None:
    import app.projects.c21200065.infra.clients.google as google_module

    monkeypatch.setattr(google_module.httpx, "AsyncClient", FakeAsyncClient)
    monkeypatch.setattr(settings, "GOOGLE_WEB_CLIENT_ID", "expected-client")
    FakeAsyncClient.payload = {
        "aud": "expected-client",
        "iss": "accounts.google.com",
        "email_verified": "false",
        "sub": "google-sub",
        "email": "frank@example.com",
    }

    assert await GoogleOAuthClient().verify_id_token("token") is None
