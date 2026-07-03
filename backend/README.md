# Illarek Lab — Platform API

A multi-tenant FastAPI backend platform that hosts independent projects under a single application. Each project exposes its own endpoints, maintains isolated infrastructure, and follows a clean layered architecture.

---

## Overview

```
GET  /                          # landing page — lists all loaded projects
GET  /layout_example/health
POST /layout_example/auth/login
...
```

Projects are **automatically discovered** at startup — no manual registration required. Adding a new project is as simple as creating a folder under `src/app/projects/`.

---

## Architecture

Each project follows a strict three-layer architecture:

```
api/          HTTP layer — routers, schemas, dependency wiring
domain/       Business logic — services, models, ports (interfaces)
infra/        Infrastructure — databases, external clients, repositories
```

Dependencies flow in one direction: `api → domain ← infra`. The domain layer never imports from infrastructure — concrete implementations are injected via FastAPI `Depends`.

```
platform-api/
├── pyproject.toml
├── uv.lock
├── credentials/
│   └── {project_slug}.env
└── src/
    └── app/
        ├── main.py
        ├── core/
        │   └── discovery.py        # auto-loads projects at startup
        └── projects/
            └── layout_example/     # reference project — copy this to get started
                ├── api/
                ├── domain/
                └── infra/
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | FastAPI + Uvicorn |
| Package manager | uv |
| Validation | Pydantic v2 |
| PostgreSQL | SQLAlchemy 2.0 async + asyncpg |
| MongoDB | Motor (async) |
| Redis | redis-py async |
| Object storage | Cloudflare R2 / AWS S3 (aioboto3) |
| Authentication | JWT (PyJWT) + Google OAuth |
| GraphQL | Strawberry |
| Python | 3.12+ |

---

## Running Locally

### 1. Clone the repository

```bash
git clone https://github.com/illarek-lab/platform-api.git
cd platform-api
```

### 2. Install uv

```bash
# macOS / Linux
curl -LsSf https://astral.sh/uv/install.sh | sh
```

### 3. Install dependencies

```bash
uv sync
```

### 4. Configure environment variables

Each project reads from `credentials/{project_slug}.env`. Create the file for `layout_example`:

```bash
cp credentials/layout_example.env.example credentials/layout_example.env
```

Then open the file and fill in your values:

```env
APP_NAME=layout_example
APP_ENV=development
DEBUG=true

POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=platform
POSTGRES_USER=postgres
POSTGRES_PASSWORD=secret

MONGO_URI=mongodb://localhost:27017
MONGO_DATABASE=platform

REDIS_URL=redis://localhost:6379

OBJECT_STORAGE_PROVIDER=r2
OBJECT_STORAGE_BUCKET=my-bucket
OBJECT_STORAGE_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
OBJECT_STORAGE_ACCESS_KEY=...
OBJECT_STORAGE_SECRET_KEY=...
OBJECT_STORAGE_REGION=auto

SECRET_KEY=your-secret-key
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=120
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GOOGLE_REDIRECT_URI=...

LLM_URL=http://localhost:11434
```

### 5. Start the server

```bash
uv run uvicorn app.main:app --reload
```

| URL | Description |
|-----|-------------|
| `http://localhost:8000/` | Landing page |
| `http://localhost:8000/docs` | Swagger UI |
| `http://localhost:8000/redoc` | ReDoc |
| `http://localhost:8000/layout_example/health` | Health check |

---

## Contributing — Branch & PR Workflow

Direct commits to `main` are not allowed. All changes must go through a Pull Request.

### Branch naming convention

| Prefix | When to use | Example |
|--------|-------------|---------|
| `feat/` | New feature or endpoint | `feat/user-profile-endpoint` |
| `fix/` | Bug fix | `fix/uuid-validation-error` |
| `chore/` | Dependencies, config, tooling | `chore/update-sqlalchemy` |
| `docs/` | Documentation only | `docs/nginx-setup-guide` |
| `refactor/` | Code restructure, no behavior change | `refactor/storage-service` |

### Step-by-step

**1. Sync with main and create your branch**

```bash
git checkout main
git pull origin main
git checkout -b feat/your-feature-name
```

**2. Make your changes, commit using Conventional Commits**

```bash
git add <files>
git commit -m "feat: add user profile endpoint"
git push origin feat/your-feature-name
```

Commit message format:

```
feat: add something new
fix: resolve a bug
chore: update dependency
docs: add setup guide
refactor: restructure storage module
```

**3. Open a Pull Request**

```bash
gh pr create --title "feat: your feature name" --base main
```

Or go to GitHub — after pushing, click **"Compare & pull request"**.

**4. Wait for review and approval**

The PR requires approval from `@johnkbarrera` before it can be merged. You will not be able to merge without it.

> See [docs/branch-protection.md](docs/branch-protection.md) for the full branch protection setup.

---

## Adding a New Project

Use `layout_example` as your starting point — it contains a complete working implementation with auth, PostgreSQL (raw SQL + ORM), MongoDB, Redis, Cloudflare R2 storage, and GraphQL.

**1. Copy the reference project**

```bash
cp -r src/app/projects/layout_example src/app/projects/my_project
```

**2. Rename references** inside the new folder — replace every occurrence of `layout_example` with your project slug.

**3. Export a `router` object** from `api/router.py` — this is what the discovery system looks for:

```python
from fastapi import APIRouter

router = APIRouter()

@router.get("/health")
async def health():
    return {"project": "my_project", "status": "ok"}
```

**4. Add credentials** at `credentials/my_project.env`.

The platform will **automatically mount** the project at `/my_project/*` on the next startup — no changes to `main.py` required.

---

## Design Principles

- **Per-project isolation** — each project owns its configuration, databases, and clients
- **Dependency inversion** — domain defines interfaces (`Protocol`); infrastructure implements them
- **Constructor injection** — services receive dependencies as arguments, enabling testability
- **Soft deletes** — records are never physically removed; `deleted_at` is set instead
- **Presigned uploads** — files go directly from client to object storage; the API only handles metadata
- **JWT-bound ownership** — `user_id` is always derived from the token, never trusted from the request body

---

## Roadmap

- [x] Multi-project auto-discovery
- [x] JWT authentication (local + Google OAuth)
- [x] PostgreSQL async (raw SQL + ORM)
- [x] MongoDB async (Motor)
- [x] Cloudflare R2 / S3 presigned uploads
- [x] GraphQL (Strawberry)
- [x] Soft deletes
- [ ] Alembic migrations
- [ ] Redis caching
- [ ] Rate limiting
- [ ] Background jobs
- [ ] Observability (structured logging + tracing)
