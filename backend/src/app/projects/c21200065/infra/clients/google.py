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
        if settings.GOOGLE_CLIENT_ID and data.get("aud") != settings.GOOGLE_CLIENT_ID:
            return None
        return data


google_oauth_client = GoogleOAuthClient()
