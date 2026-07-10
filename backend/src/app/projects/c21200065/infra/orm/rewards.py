from datetime import datetime

from sqlalchemy import Boolean, DateTime, Integer, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column

from app.projects.c21200065.infra.orm.base import Base


class CosmeticCatalogORM(Base):
    __tablename__ = "cosmetic_catalog"

    id: Mapped[str] = mapped_column(String(80), primary_key=True)
    name: Mapped[str] = mapped_column(Text, nullable=False)
    description: Mapped[str] = mapped_column(Text, nullable=False)
    kind: Mapped[str] = mapped_column(String(40), nullable=False)
    cost: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class UserCosmeticORM(Base):
    __tablename__ = "user_cosmetics"

    id: Mapped[str] = mapped_column(String(120), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(36), index=True, nullable=False)
    cosmetic_id: Mapped[str] = mapped_column(String(80), index=True, nullable=False)
    unlocked: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    equipped: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
