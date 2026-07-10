# HabitFlow Render deployment

## Commands

- Build command: `pip install -r requirements.txt`
- Start command: `uvicorn app.main:app --host 0.0.0.0 --port $PORT`
- Health check: `GET /health`
- Project health check: `GET /c21200065/health`

## Required environment

Set secrets in the Render dashboard or through `sync: false` Blueprint variables. Do not commit real values.

| Variable | Purpose |
| --- | --- |
| `PYTHONPATH=src` | Allows `app.main:app` imports. |
| `APP_NAME`, `APP_ENV`, `DEBUG`, `LOG_LEVEL` | Runtime metadata. |
| `CORS_ALLOWED_ORIGINS` | Comma-separated Android/web origins when needed. |
| `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_SSLMODE=require` | PostgreSQL connection. |
| `REDIS_URL` | Voice conversation session cache, with in-memory fallback only for local/test failures. |
| `MONGO_URI`, `MONGO_DATABASE` | Legacy MongoDB dependencies. |
| `SECRET_KEY`, `ALGORITHM`, `ACCESS_TOKEN_EXPIRE_MINUTES`, `REFRESH_TOKEN_EXPIRE_DAYS` | HabitFlow JWT and refresh tokens. |
| `GOOGLE_WEB_CLIENT_ID` | Audience expected for Google ID tokens from Android Credential Manager. |
| `GOOGLE_CLIENT_ID` | Backward-compatible alias; keep equal to `GOOGLE_WEB_CLIENT_ID` during transition. |
| `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI` | OAuth console metadata; Android must not receive the secret. |
| `OBJECT_STORAGE_*` | R2/S3-compatible storage. |
| `LLM_*`, `STT_*` | AI and speech-to-text providers. |
| `SEED_ADMIN_*` | Optional seed/admin values used by existing project setup. |

## PostgreSQL migration strategy

The current app startup calls `Base.metadata.create_all` and then adds missing `auth_users` profile columns with `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`. This is safe for existing Render databases and preserves users, refresh tokens, habits, and profile values.

Before production handoff, convert the startup compatibility SQL into an Alembic revision so schema history is explicit:

1. Generate a revision for `auth_users` profile columns.
2. Use nullable/default-compatible columns for `bio`, `primary_goal`, `preferred_categories`, `onboarding_completed`, `theme_mode`, `accent_theme`, `voice_response_enabled`, and `locale`.
3. Backfill existing users with non-destructive defaults.
4. Deploy, run `alembic upgrade head`, then remove the startup compatibility helper only after all environments are migrated.

## Google login checklist

1. Android `local.properties` has `GOOGLE_WEB_CLIENT_ID`.
2. Render has `GOOGLE_WEB_CLIENT_ID` with the same Web client ID.
3. Credential Manager returns a Google ID token.
4. `POST /c21200065/auth/google` verifies `aud`, issuer, email verification, subject, and email through Google tokeninfo.
5. HabitFlow JWT access and refresh tokens are returned and stored by Android.
6. New or incomplete users route to onboarding; completed users route to Home.
