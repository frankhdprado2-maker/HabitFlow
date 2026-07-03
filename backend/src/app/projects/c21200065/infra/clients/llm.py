import httpx

from app.projects.c21200065.infra.settings import settings


class LLMClient:

    def __init__(self) -> None:
        self._base_url = settings.LLM_URL

    async def complete(self, prompt: str, model: str = "default", **kwargs) -> str:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self._base_url}/completions",
                json={"model": model, "prompt": prompt, **kwargs},
            )
            response.raise_for_status()
        return response.json()["choices"][0]["text"]

    async def chat(self, messages: list[dict], model: str = "default", **kwargs) -> str:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self._base_url}/chat/completions",
                json={"model": model, "messages": messages, **kwargs},
            )
            response.raise_for_status()
        return response.json()["choices"][0]["message"]["content"]


llm_client = LLMClient()
