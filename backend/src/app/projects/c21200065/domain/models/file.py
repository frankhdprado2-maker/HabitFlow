from datetime import datetime
from typing import Optional

from pydantic import BaseModel


class File(BaseModel):
    id: int
    project_slug: str
    user_id: str
    storage_provider: str
    bucket: str
    object_key: str
    url: Optional[str] = None
    file_name: str
    content_type: Optional[str] = None
    size_bytes: Optional[int] = None
    is_public: bool = False
    uploaded_at: datetime
    deleted_at: Optional[datetime] = None

    model_config = {"from_attributes": True}
