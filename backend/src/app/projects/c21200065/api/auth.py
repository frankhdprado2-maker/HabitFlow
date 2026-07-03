from fastapi import APIRouter, Depends, HTTPException

from app.projects.c21200065.api.deps import get_auth_service, get_current_user
from app.projects.c21200065.api.schemas import GoogleLoginRequest, LoginRequest, RefreshTokenRequest, TokenResponse
from app.projects.c21200065.domain.auth_service import AuthService
from app.projects.c21200065.domain.exceptions import (
    InvalidCredentialsError,
    InvalidGoogleTokenError,
    InvalidRefreshTokenError,
    UserAlreadyExistsError,
)
from app.projects.c21200065.infra.token import create_token

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/login", response_model=TokenResponse)
async def login(payload: LoginRequest, service: AuthService = Depends(get_auth_service)):
    try:
        user_id, email = await service.login(payload.email, payload.password)
        refresh_token = await service.create_refresh_token(user_id, email, payload.device_id)
        return TokenResponse(access_token=create_token(user_id, email), refresh_token=refresh_token)
    except InvalidCredentialsError:
        raise HTTPException(status_code=401, detail="Invalid credentials") from None


@router.post("/register", response_model=TokenResponse)
async def register(payload: LoginRequest, service: AuthService = Depends(get_auth_service)):
    try:
        user_id = await service.register(payload.email, payload.password)
        refresh_token = await service.create_refresh_token(user_id, payload.email, payload.device_id)
        return TokenResponse(access_token=create_token(user_id, payload.email), refresh_token=refresh_token)
    except UserAlreadyExistsError:
        raise HTTPException(status_code=400, detail="User already exists") from None


@router.post("/google", response_model=TokenResponse)
async def google_login(payload: GoogleLoginRequest, service: AuthService = Depends(get_auth_service)):
    try:
        google_id, email = await service.google_login(payload.token)
        refresh_token = await service.create_refresh_token(google_id, email, payload.device_id)
        return TokenResponse(access_token=create_token(google_id, email), refresh_token=refresh_token)
    except InvalidGoogleTokenError:
        raise HTTPException(status_code=401, detail="Invalid Google token") from None


@router.post("/refresh-token", response_model=TokenResponse)
async def refresh_token(payload: RefreshTokenRequest, service: AuthService = Depends(get_auth_service)):
    try:
        user_id, email, new_refresh_token = await service.rotate_refresh_token(payload.refresh_token, payload.device_id)
        return TokenResponse(access_token=create_token(user_id, email), refresh_token=new_refresh_token)
    except InvalidRefreshTokenError:
        raise HTTPException(status_code=401, detail="Invalid or expired refresh token") from None


@router.get("/me")
async def me(user=Depends(get_current_user)):
    return {"user": user}
