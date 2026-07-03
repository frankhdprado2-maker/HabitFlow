import httpx


class GoogleOAuthClient:
    TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo"

    async def verify_id_token(self, id_token: str) -> dict | None:
        async with httpx.AsyncClient() as client:
            response = await client.get(f"{self.TOKENINFO_URL}?id_token={id_token}")
        if response.status_code != 200:
            return None
        return response.json()


google_oauth_client = GoogleOAuthClient()
