from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from app.projects.c21200065.infra.settings import settings

DATABASE_URL = (
    f"postgresql+asyncpg://"
    f"{settings.POSTGRES_USER}:{settings.POSTGRES_PASSWORD}@"
    f"{settings.POSTGRES_HOST}:{settings.POSTGRES_PORT}/{settings.POSTGRES_DB}"
)

connect_args = {"statement_cache_size": 0}
if settings.POSTGRES_SSLMODE == "require":
    connect_args["ssl"] = True

engine = create_async_engine(
    DATABASE_URL,
    echo=False,
    connect_args=connect_args,
    pool_pre_ping=True,
    pool_recycle=settings.POSTGRES_POOL_RECYCLE_SECONDS,
)
async_session = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
