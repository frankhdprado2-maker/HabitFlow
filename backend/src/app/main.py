from fastapi import FastAPI
from fastapi.responses import HTMLResponse
from sqlalchemy import text

from app.core.discovery import load_projects, registry
from app.projects.c21200065.infra.db.postgres import engine
from app.projects.c21200065.infra.orm import AuthRefreshTokenORM, AuthUserORM, FileORM, GeoEventORM
from app.projects.c21200065.infra.orm.base import Base

app = FastAPI(title="Platform API", version="1.0.0")

print("STARTING APP")
load_projects(app)


@app.on_event("startup")
async def create_database_tables():
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)
        await _ensure_auth_profile_columns(connection)


async def _ensure_auth_profile_columns(connection):
    for statement in (
        "ALTER TABLE auth_users ADD COLUMN IF NOT EXISTS name TEXT",
        "ALTER TABLE auth_users ADD COLUMN IF NOT EXISTS username TEXT",
        "ALTER TABLE auth_users ADD COLUMN IF NOT EXISTS goal TEXT",
        "ALTER TABLE auth_users ADD COLUMN IF NOT EXISTS timezone TEXT",
        "ALTER TABLE auth_users ADD COLUMN IF NOT EXISTS avatar_url TEXT",
        "ALTER TABLE auth_users ADD COLUMN IF NOT EXISTS avatar_key TEXT",
        "ALTER TABLE auth_users ADD COLUMN IF NOT EXISTS categories TEXT",
    ):
        await connection.execute(text(statement))


def _routes_for_prefix(prefix: str) -> list[str]:
    routes = []
    for route in app.routes:
        path = getattr(route, "path", None)
        methods = getattr(route, "methods", None)
        if not path or not methods or not path.startswith(prefix):
            continue

        relative_path = path.removeprefix(prefix) or "/"
        for method in sorted(methods):
            if method in {"HEAD", "OPTIONS"}:
                continue
            routes.append(f"{method} {relative_path}")
    return routes


@app.get("/", response_class=HTMLResponse, include_in_schema=False)
async def landing():
    cards = ""
    project_routes = []
    for project in registry:
        routes = _routes_for_prefix(project["prefix"])
        project_routes.append(routes)
        route_rows = "".join(
            f'<tr><td class="method {r.split()[0]}">{r.split()[0]}</td><td class="path">{project["prefix"]}{r.split(" ", 1)[1]}</td></tr>'
            for r in routes
        )
        cards += f"""
        <div class="card">
            <div class="card-header">
                <div class="card-icon">
                    <svg viewBox="0 0 24 24" stroke="#fff" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/></svg>
                </div>
                <h2>{project["name"]}</h2>
                <span class="badge">project</span>
                <div class="links">
                    <a href="{project["prefix"]}/health">health</a>
                    <a href="/docs">docs</a>
                </div>
            </div>
            <table>{route_rows}</table>
        </div>"""

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Illarek Lab — Platform API</title>
<style>
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #f0f2f5; color: #1a202c; min-height: 100vh; }}

  /* Top bar */
  .topbar {{ background: #fff; border-bottom: 1px solid #e2e8f0; padding: 0 40px; display: flex; align-items: center; justify-content: space-between; height: 62px; box-shadow: 0 1px 4px rgba(0,0,0,.04); }}
  .topbar-brand {{ display: flex; align-items: center; gap: 10px; text-decoration: none; }}
  .brand-logo {{ width: 30px; height: 30px; background: linear-gradient(135deg, #0ea5e9 0%, #0369a1 100%); border-radius: 8px; display: flex; align-items: center; justify-content: center; }}
  .brand-logo svg {{ width: 16px; height: 16px; fill: #fff; }}
  .brand-text {{ display: flex; flex-direction: column; line-height: 1; }}
  .brand-name {{ font-size: 0.92rem; font-weight: 800; color: #0f172a; letter-spacing: -.02em; }}
  .brand-sub {{ font-size: 0.65rem; font-weight: 500; color: #94a3b8; letter-spacing: .04em; text-transform: uppercase; margin-top: 1px; }}
  .topbar-links {{ display: flex; gap: 8px; }}
  .topbar-links a {{ font-size: 0.78rem; font-weight: 600; color: #0369a1; text-decoration: none; border: 1px solid #bae6fd; background: #f0f9ff; border-radius: 7px; padding: 5px 14px; transition: background .15s; }}
  .topbar-links a:hover {{ background: #e0f2fe; }}

  /* Hero */
  .hero {{ max-width: 860px; margin: 48px auto 0; padding: 0 24px 44px; }}
  .hero-tag {{ display: inline-flex; align-items: center; gap: 6px; font-size: 0.72rem; font-weight: 600; text-transform: uppercase; letter-spacing: .08em; color: #0369a1; background: #f0f9ff; border: 1px solid #bae6fd; border-radius: 99px; padding: 4px 12px; margin-bottom: 18px; }}
  .hero-tag-dot {{ width: 6px; height: 6px; background: #0ea5e9; border-radius: 50%; animation: pulse 2s infinite; }}
  @keyframes pulse {{ 0%, 100% {{ opacity: 1; }} 50% {{ opacity: .4; }} }}
  .hero h1 {{ font-size: 2.1rem; font-weight: 800; color: #0f172a; line-height: 1.15; letter-spacing: -.03em; }}
  .hero h1 span {{ color: #0ea5e9; }}
  .hero p {{ margin-top: 10px; color: #64748b; font-size: 0.93rem; }}
  .hero-stats {{ display: flex; gap: 24px; margin-top: 24px; }}
  .stat {{ display: flex; flex-direction: column; }}
  .stat-value {{ font-size: 1.4rem; font-weight: 800; color: #0f172a; letter-spacing: -.02em; }}
  .stat-label {{ font-size: 0.72rem; color: #94a3b8; text-transform: uppercase; letter-spacing: .05em; margin-top: 1px; }}
  .stat-divider {{ width: 1px; background: #e2e8f0; }}

  /* Grid */
  .grid {{ max-width: 860px; margin: 0 auto; padding: 0 24px 60px; display: grid; gap: 16px; }}

  /* Card */
  .card {{ background: #fff; border: 1px solid #e2e8f0; border-radius: 14px; overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,.05); }}
  .card-header {{ padding: 16px 22px; border-bottom: 1px solid #f1f5f9; display: flex; align-items: center; gap: 12px; background: #fafbfc; }}
  .card-icon {{ width: 32px; height: 32px; background: linear-gradient(135deg, #0ea5e9, #0369a1); border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }}
  .card-icon svg {{ width: 15px; height: 15px; fill: #fff; }}
  .card-header h2 {{ font-size: 0.92rem; font-weight: 700; color: #0f172a; flex: 1; }}
  .badge {{ font-size: 0.67rem; font-weight: 700; background: #f0f9ff; color: #0369a1; padding: 2px 9px; border-radius: 99px; text-transform: uppercase; letter-spacing: .06em; border: 1px solid #bae6fd; }}
  .links {{ display: flex; gap: 8px; }}
  .links a {{ font-size: 0.75rem; font-weight: 600; color: #059669; text-decoration: none; background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 6px; padding: 3px 10px; }}
  .links a:hover {{ background: #dcfce7; }}

  /* Table */
  table {{ width: 100%; border-collapse: collapse; }}
  tr {{ transition: background .1s; }}
  tr:hover {{ background: #f8fafc; }}
  tr:not(:last-child) {{ border-bottom: 1px solid #f1f5f9; }}
  td {{ padding: 9px 22px; font-size: 0.81rem; font-family: "SF Mono", "Fira Code", ui-monospace, monospace; }}
  .method {{ width: 66px; font-weight: 700; }}
  .method.GET {{ color: #059669; }}
  .method.POST {{ color: #2563eb; }}
  .method.PUT {{ color: #d97706; }}
  .method.DELETE {{ color: #dc2626; }}
  .method.PATCH {{ color: #7c3aed; }}
  .path {{ color: #475569; }}

  /* Footer */
  footer {{ text-align: center; padding: 28px; font-size: 0.76rem; color: #94a3b8; border-top: 1px solid #e2e8f0; background: #fff; margin-top: 8px; }}
  footer strong {{ color: #0369a1; }}
</style>
</head>
<body>

<div class="topbar">
  <a class="topbar-brand" href="/">
    <div class="brand-logo">
      <svg viewBox="0 0 24 24"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>
    </div>
    <div class="brand-text">
      <span class="brand-name">illarek-lab</span>
      <span class="brand-sub">Platform API</span>
    </div>
  </a>
  <div class="topbar-links">
    <a href="/docs">Swagger UI</a>
    <a href="/redoc">ReDoc</a>
  </div>
</div>

<div class="hero">
  <div class="hero-tag">
    <div class="hero-tag-dot"></div>
    Live &mdash; REST API
  </div>
  <h1>Illarek Lab<br><span>Platform API</span></h1>
  <p>Multi-project FastAPI platform. Projects are auto-discovered on startup.</p>
  <div class="hero-stats">
    <div class="stat">
      <span class="stat-value">{len(registry)}</span>
      <span class="stat-label">Project{"s" if len(registry) != 1 else ""}</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat">
      <span class="stat-value">{sum(len(routes) for routes in project_routes)}</span>
      <span class="stat-label">Endpoints</span>
    </div>
  </div>
</div>

<div class="grid">{cards}</div>

<footer>
  <strong>illarek-lab</strong> &mdash; Platform API &middot; FastAPI &middot; Python
</footer>
</body>
</html>"""
