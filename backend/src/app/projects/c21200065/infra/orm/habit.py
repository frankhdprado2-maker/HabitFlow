from datetime import datetime

from sqlalchemy import (
    BigInteger,
    Boolean,
    DateTime,
    Double,
    ForeignKey,
    Integer,
    String,
    Text,
    func,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.projects.c21200065.infra.orm.base import Base


class HabitORM(Base):
    __tablename__ = "habits"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("auth_users.id", ondelete="CASCADE"), index=True, nullable=False
    )
    name: Mapped[str] = mapped_column(Text, nullable=False)
    icon: Mapped[str] = mapped_column(String(64), nullable=False)
    frequency: Mapped[str] = mapped_column(Text, nullable=False)
    reminder_time: Mapped[str] = mapped_column(String(32), nullable=False)
    category: Mapped[str] = mapped_column(String(80), nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    frequency_type: Mapped[str] = mapped_column(String(40), nullable=False)
    weekdays_csv: Mapped[str] = mapped_column(Text, default="", nullable=False)
    times_per_week: Mapped[int | None] = mapped_column(Integer, nullable=True)
    interval_days: Mapped[int | None] = mapped_column(Integer, nullable=True)
    monthly_days_csv: Mapped[str] = mapped_column(Text, default="", nullable=False)
    schedule_start_date: Mapped[str | None] = mapped_column(String(10), nullable=True)
    schedule_end_date: Mapped[str | None] = mapped_column(String(10), nullable=True)
    schedule_timezone: Mapped[str] = mapped_column(String(80), nullable=False)
    schedule_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    frequency_needs_review: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    frequency_original: Mapped[str] = mapped_column(Text, default="", nullable=False)
    schedule_effective_from: Mapped[str | None] = mapped_column(String(10), nullable=True)
    measurement_type: Mapped[str] = mapped_column(String(32), default="BOOLEAN", nullable=False)
    target_value: Mapped[float] = mapped_column(Double, default=1.0, nullable=False)
    measurement_unit: Mapped[str] = mapped_column(String(40), default="", nullable=False)
    allow_partial_progress: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    aggregation_mode: Mapped[str] = mapped_column(String(32), default="ADD", nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )


class HabitEventORM(Base):
    __tablename__ = "habit_events_remote"

    id: Mapped[str] = mapped_column(String(128), primary_key=True)
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("auth_users.id", ondelete="CASCADE"), index=True, nullable=False
    )
    habit_id: Mapped[str] = mapped_column(
        String(64), ForeignKey("habits.id", ondelete="CASCADE"), index=True, nullable=False
    )
    habit_name: Mapped[str] = mapped_column(Text, nullable=False)
    status: Mapped[str] = mapped_column(String(24), nullable=False)
    timestamp: Mapped[int] = mapped_column(BigInteger, nullable=False)
    note: Mapped[str] = mapped_column(Text, default="", nullable=False)
    value: Mapped[float | None] = mapped_column(Double, nullable=True)
    normalized_value: Mapped[float | None] = mapped_column(Double, nullable=True)
    unit: Mapped[str | None] = mapped_column(String(40), nullable=True)
    aggregation_mode: Mapped[str | None] = mapped_column(String(32), nullable=True)
    idempotency_key: Mapped[str | None] = mapped_column(String(128), nullable=True)
    source: Mapped[str] = mapped_column(String(32), default="MANUAL", nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
