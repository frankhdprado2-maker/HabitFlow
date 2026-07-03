from typing import Literal, Optional

from pydantic import BaseModel, EmailStr


class UserDocument(BaseModel):
    email: EmailStr
    auth_provider: Literal["local", "google"]
    password: Optional[str] = None
    google_id: Optional[str] = None

    model_config = {"extra": "ignore"}
