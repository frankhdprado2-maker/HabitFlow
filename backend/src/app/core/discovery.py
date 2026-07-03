import importlib
import pkgutil
from typing import Any

import app.projects as projects
from fastapi import FastAPI

registry: list[dict[str, Any]] = []


def _import_project_module(project_name: str):
    module_path = f"app.projects.{project_name}.api.router"
    try:
        return importlib.import_module(module_path)
    except ModuleNotFoundError:
        return None


def _collect_routes(router) -> list[str]:
    paths = []
    for route in getattr(router, "routes", []):
        path = getattr(route, "path", None)
        methods = getattr(route, "methods", None)
        if path and methods:
            for method in sorted(methods):
                paths.append(f"{method} {path}")
    return paths


def load_projects(app: FastAPI):
    registry.clear()

    print("Scanning projects...")
    print("PATH:", projects.__path__)

    for m in pkgutil.iter_modules(projects.__path__):
        print("FOUND MODULE:", m.name)

        try:
            module = _import_project_module(m.name)
            if module is None:
                print(f"No router.py in {m.name}")
                continue

            router = getattr(module, "router", None)

            if router:
                prefix = f"/{m.name}"
                app.include_router(router, prefix=prefix)
                registry.append({
                    "name": m.name,
                    "prefix": prefix,
                    "routes": _collect_routes(router),
                })
                print(f"LOADED: {m.name}")
            else:
                print(f"No `router` object in {m.name}")

        except Exception as e:
            print(f"ERROR {m.name}: {e}")
