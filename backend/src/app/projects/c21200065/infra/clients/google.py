import httpx

from app.projects.c21200065.infra.settings import settings


class GoogleOAuthClient:
    TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo"

    async def verify_id_token(self, id_token: str) -> dict | None:
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.get(self.TOKENINFO_URL, params={"id_token": id_token})
        except httpx.HTTPError:
            return None

        if response.status_code != 200:
            return None

        data = response.json()
        expected_audience = settings.GOOGLE_WEB_CLIENT_ID or settings.GOOGLE_CLIENT_ID
        if expected_audience and data.get("aud") != expected_audience:
            return None
        issuer = data.get("iss")
        if issuer and issuer not in {"accounts.google.com", "https://accounts.google.com"}:
            return None
        email_verified = str(data.get("email_verified", "true")).lower()
        if email_verified not in {"true", "1"}:
            return None
        if not data.get("sub") or not data.get("email"):
            return None
        return data


google_oauth_client = GoogleOAuthClient()
