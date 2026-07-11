from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

PROJECT_NAME = Path(__file__).resolve().parents[1].name
BASE_DIR = Path(__file__).resolve().parents[5]  # repo root (â€¦/platform-api)


class Settings(BaseSettings):

    # App
    APP_NAME: str
    APP_ENV: str
    DEBUG: bool = False
    LOG_LEVEL: str = "INFO"
    CORS_ALLOWED_ORIGINS: str = ""

    # PostgreSQL
    POSTGRES_HOST: str
    POSTGRES_PORT: int
    POSTGRES_DB: str
    POSTGRES_USER: str
    POSTGRES_PASSWORD: str
    POSTGRES_SSLMODE: str = "disable"
    POSTGRES_POOL_RECYCLE_SECONDS: int = 300

    # Mongo
    MONGO_URI: str
    MONGO_DATABASE: str

    # Redis
    REDIS_URL: str

    # Storage
    OBJECT_STORAGE_PROVIDER: str = "r2"
    OBJECT_STORAGE_BUCKET: str
    OBJECT_STORAGE_ENDPOINT: str
    OBJECT_STORAGE_ACCESS_KEY: str
    OBJECT_STORAGE_SECRET_KEY: str
    OBJECT_STORAGE_REGION: str

    # Auth
    SECRET_KEY: str
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 120
    REFRESH_TOKEN_EXPIRE_DAYS: int = 30

    GOOGLE_WEB_CLIENT_ID: str = ""
    GOOGLE_CLIENT_ID: str = ""
    GOOGLE_CLIENT_SECRET: str = ""
    GOOGLE_REDIRECT_URI: str = ""

    # AI
    LLM_TYPE: str = "local"
    LLM_PROVIDER: str = "openai"
    LLM_URL: str
    LLM_BASE_URL: str = "https://api.openai.com/v1"
    LLM_API_KEY: str = ""
    LLM_MODEL: str = "gpt-5.5"
    STT_BASE_URL: str = "https://api.openai.com/v1"
    STT_API_KEY: str = ""
    STT_MODEL: str = "whisper-1"
    GEMINI_API_KEY: str = ""
    GEMINI_MODEL: str = "gemini-3.1-flash-lite"

    # Seed
    SEED_ADMIN_EMAIL: str
    SEED_ADMIN_PASSWORD: str
    SEED_ADMIN_DOCUMENT_ID: str

    model_config = SettingsConfigDict(
        env_file=BASE_DIR / "credentials" / f"{PROJECT_NAME}.env",
        extra="ignore",
    )


settings = Settings()
