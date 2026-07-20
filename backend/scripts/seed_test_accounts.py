"""Idempotently seed the three HabitFlow demo accounts into the configured PostgreSQL DB."""

import argparse
import asyncio
import json
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import NAMESPACE_URL, uuid5

from sqlalchemy import delete, select

from app.projects.c21200065.domain.security import hash_password
from app.projects.c21200065.infra.db.postgres import async_session, engine
from app.projects.c21200065.infra.orm.auth import AuthUserORM
from app.projects.c21200065.infra.orm.base import Base
from app.projects.c21200065.infra.orm.habit import HabitEventORM, HabitORM

ACCOUNT_DATA = (
    {
        "key": "constancia",
        "name": "Valeria Constante",
        "goal": "Mantener rutinas de salud y lectura",
        "habits": (
            (
                "water",
                "Tomar 8 vasos de agua",
                "water_drop",
                "Diario",
                "09:00",
                "Salud",
                14,
                (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
            ),
            (
                "read",
                "Leer 20 páginas",
                "menu_book",
                "Diario",
                "21:00",
                "Crecimiento",
                9,
                (0, 1, 2, 3, 4, 5, 6, 8, 9, 10, 11),
            ),
            (
                "walk",
                "Caminar 30 minutos",
                "directions_walk",
                "Lun-Vie",
                "18:00",
                "Ejercicio",
                5,
                (0, 1, 2, 3, 4, 7, 8, 9),
            ),
        ),
    },
    {
        "key": "reinicio",
        "name": "Diego Reinicio",
        "goal": "Retomar hábitos después de una semana difícil",
        "habits": (
            (
                "study",
                "Estudiar Kotlin",
                "school",
                "Lun-Vie",
                "19:00",
                "Estudio",
                2,
                (0, 1, 5, 6, 10, 11),
            ),
            (
                "sleep",
                "Dormir antes de las 23:00",
                "bedtime",
                "Diario",
                "22:30",
                "Salud",
                3,
                (0, 1, 2, 6, 7, 8, 12),
            ),
            (
                "run",
                "Correr 5 km",
                "directions_run",
                "Mar-Jue-Sab",
                "06:30",
                "Ejercicio",
                1,
                (1, 4, 8, 11),
            ),
        ),
    },
    {
        "key": "medicion",
        "name": "Camila Progreso",
        "goal": "Mejorar con metas medibles",
        "habits": (
            (
                "meditate",
                "Meditar 10 minutos",
                "self_improvement",
                "Diario",
                "07:00",
                "Bienestar",
                7,
                (0, 1, 2, 3, 4, 5, 6, 9, 10),
            ),
            (
                "language",
                "Practicar inglés",
                "translate",
                "5 veces/semana",
                "20:00",
                "Aprendizaje",
                4,
                (0, 1, 2, 4, 5, 7, 8, 10),
            ),
            (
                "strength",
                "Entrenamiento de fuerza",
                "fitness_center",
                "3 veces/semana",
                "17:30",
                "Ejercicio",
                3,
                (0, 2, 4, 7, 9, 11),
            ),
        ),
    },
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--credentials-file", required=True, type=Path)
    return parser.parse_args()


async def seed(credentials_file: Path) -> None:
    credentials = json.loads(credentials_file.read_text(encoding="utf-8"))
    by_key = {item["key"]: item for item in credentials}
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)

    async with async_session() as session:
        for account in ACCOUNT_DATA:
            credential = by_key[account["key"]]
            result = await session.execute(
                select(AuthUserORM).where(AuthUserORM.email == credential["email"].lower())
            )
            user = result.scalar_one_or_none()
            user_id = str(uuid5(NAMESPACE_URL, f"habitflow-test:{account['key']}"))
            if user is None:
                user = AuthUserORM(
                    id=user_id, email=credential["email"].lower(), auth_provider="local"
                )
                session.add(user)
            else:
                user_id = user.id
            user.password = hash_password(credential["password"])
            user.name = account["name"]
            user.username = account["key"]
            user.goal = account["goal"]
            user.primary_goal = account["goal"]
            user.timezone = "America/Lima"
            user.onboarding_completed = True
            user.locale = "es-PE"
            await session.flush()

            await session.execute(delete(HabitEventORM).where(HabitEventORM.user_id == user_id))
            await session.execute(delete(HabitORM).where(HabitORM.user_id == user_id))
            now = datetime.now(UTC)
            for habit_definition in account["habits"]:
                (
                    slug,
                    name,
                    icon,
                    frequency,
                    time,
                    category,
                    _best_streak,
                    completed_days,
                ) = habit_definition
                habit_id = f"test-{account['key']}-{slug}"
                frequency_type = (
                    "DAILY"
                    if frequency == "Diario"
                    else ("SPECIFIC_WEEKDAYS" if "-" in frequency else "TIMES_PER_WEEK")
                )
                habit = HabitORM(
                    id=habit_id,
                    user_id=user_id,
                    name=name,
                    icon=icon,
                    frequency=frequency,
                    reminder_time=time,
                    category=category,
                    is_active=True,
                    frequency_type=frequency_type,
                    weekdays_csv="",
                    monthly_days_csv="",
                    schedule_timezone="America/Lima",
                    schedule_active=True,
                    frequency_needs_review=False,
                    frequency_original=frequency,
                    measurement_type="BOOLEAN",
                    target_value=1.0,
                    measurement_unit="",
                    allow_partial_progress=False,
                    aggregation_mode="ADD",
                )
                session.add(habit)
                await session.flush()
                for days_ago in completed_days:
                    timestamp = int((now - timedelta(days=days_ago)).timestamp() * 1000)
                    session.add(
                        HabitEventORM(
                            id=f"{habit_id}-{days_ago}",
                            user_id=user_id,
                            habit_id=habit_id,
                            habit_name=name,
                            status="Completed",
                            timestamp=timestamp,
                            note="Dato demostrativo",
                            source="SEED",
                        )
                    )
        await session.commit()

    print("Seeded 3 HabitFlow test accounts")


if __name__ == "__main__":
    asyncio.run(seed(parse_args().credentials_file))
