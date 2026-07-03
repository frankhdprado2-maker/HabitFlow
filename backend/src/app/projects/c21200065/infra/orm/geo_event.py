from datetime import datetime
from typing import Optional
from uuid import UUID

from sqlalchemy import BigInteger, DateTime, Double, String, Text, func
from sqlalchemy.dialects.postgresql import UUID as PG_UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.projects.c21200065.infra.orm.base import Base


class GeoEventORM(Base):
    __tablename__ = "geo_events"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)

    user_id: Mapped[Optional[UUID]] = mapped_column(PG_UUID(as_uuid=True), nullable=True)

    latitude: Mapped[float] = mapped_column(Double)
    longitude: Mapped[float] = mapped_column(Double)
    altitude: Mapped[Optional[float]] = mapped_column(Double, nullable=True)

    accuracy: Mapped[Optional[float]] = mapped_column(Double, nullable=True)
    speed: Mapped[Optional[float]] = mapped_column(Double, nullable=True)
    heading: Mapped[Optional[float]] = mapped_column(Double, nullable=True)

    event_type: Mapped[str] = mapped_column(Text, default="gps_ping")

    device_id: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    platform: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    app_version: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    device_model: Mapped[Optional[str]] = mapped_column(String, nullable=True)

    recorded_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
