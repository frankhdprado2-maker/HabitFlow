import httpx

from app.projects.c21200065.infra.settings import settings


class LLMClient:
    def __init__(self) -> None:
        self._base_url = settings.LLM_BASE_URL if settings.LLM_TYPE == "cloud" else settings.LLM_URL

    def _headers(self) -> dict[str, str]:
        headers = {"Content-Type": "application/json"}
        if settings.LLM_TYPE == "cloud" and settings.LLM_API_KEY:
            headers["Authorization"] = f"Bearer {settings.LLM_API_KEY}"
        return headers

    async def complete(self, prompt: str, model: str | None = None, **kwargs) -> str:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self._base_url}/completions",
                json={"model": model or settings.LLM_MODEL, "prompt": prompt, **kwargs},
                headers=self._headers(),
                timeout=30,
            )
            response.raise_for_status()
        return response.json()["choices"][0]["text"]

    async def chat(self, messages: list[dict], model: str | None = None, **kwargs) -> str:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self._base_url}/chat/completions",
                json={"model": model or settings.LLM_MODEL, "messages": messages, **kwargs},
                headers=self._headers(),
                timeout=30,
            )
            response.raise_for_status()
        return response.json()["choices"][0]["message"]["content"]


llm_client = LLMClient()
