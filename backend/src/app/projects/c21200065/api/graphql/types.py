from datetime import datetime
from typing import Optional
from uuid import UUID

import strawberry


@strawberry.type
class GeoEventType:
    id: int
    user_id: Optional[UUID]
    latitude: float
    longitude: float
    altitude: Optional[float]
    accuracy: Optional[float]
    speed: Optional[float]
    heading: Optional[float]
    event_type: str
    device_id: Optional[str]
    platform: Optional[str]
    app_version: Optional[str]
    device_model: Optional[str]
    recorded_at: datetime
    created_at: datetime


@strawberry.input
class GeoEventInput:
    latitude: float
    longitude: float
    user_id: Optional[UUID] = None
    altitude: Optional[float] = None
    accuracy: Optional[float] = None
    speed: Optional[float] = None
    heading: Optional[float] = None
    event_type: str = "gps_ping"
    device_id: Optional[str] = None
    platform: Optional[str] = None
    app_version: Optional[str] = None
    device_model: Optional[str] = None
    recorded_at: Optional[datetime] = None
