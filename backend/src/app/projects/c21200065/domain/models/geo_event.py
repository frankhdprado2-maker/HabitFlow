from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel


class GeoEvent(BaseModel):
    id: int
    user_id: Optional[UUID] = None
    latitude: float
    longitude: float
    altitude: Optional[float] = None
    accuracy: Optional[float] = None
    speed: Optional[float] = None
    heading: Optional[float] = None
    event_type: str
    device_id: Optional[str] = None
    platform: Optional[str] = None
    app_version: Optional[str] = None
    device_model: Optional[str] = None
    recorded_at: datetime
    created_at: datetime

    model_config = {"from_attributes": True}
