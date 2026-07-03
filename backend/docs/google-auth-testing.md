# Testing Google Authentication

This guide explains how to obtain a Google ID token and use it to test the `POST /auth/google` endpoint.

## How it works

The endpoint `POST /{projectSlug}/auth/google` accepts a **Google ID token** (not an access token).  
The server verifies it by calling `https://oauth2.googleapis.com/tokeninfo?id_token=<token>`, then creates or retrieves the user in MongoDB and returns a JWT.

```
Mobile/Web app
  └─ Google Sign-In SDK ──► Google ID token
                                 │
                          POST /auth/google
                          { "token": "<id_token>" }
                                 │
                         FastAPI verifies token
                         via tokeninfo endpoint
                                 │
                         Returns JWT access_token
```

## Postman collection

All required endpoints are already in `docs/mobile-api.postman_collection.json` under the **Auth** folder:

| Request | Method | URL |
|---------|--------|-----|
| Register | POST | `{{baseUrl}}/{{projectSlug}}/auth/register` |
| Login | POST | `{{baseUrl}}/{{projectSlug}}/auth/login` |
| **Google login** | **POST** | **`{{baseUrl}}/{{projectSlug}}/auth/google`** |
| Me (current user) | GET | `{{baseUrl}}/{{projectSlug}}/auth/me` |

The `googleIdToken` collection variable holds the token. After a successful Google login the `accessToken` variable is set automatically by the test script.

## Getting a Google ID token for testing

### Option 1 — Google OAuth 2.0 Playground (recommended, no code needed)

1. Open [https://developers.google.com/oauthplayground/](https://developers.google.com/oauthplayground/)
2. Click the gear icon (top right) and check **"Use your own OAuth credentials"**
3. Enter your `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` from `credentials/layout_example.env`
4. In the left panel, scroll to **"Google OAuth2 API v2"** and select `https://www.googleapis.com/auth/userinfo.email`
5. Click **Authorize APIs** — sign in with your Google account
6. Click **Exchange authorization code for tokens**
7. Copy the `id_token` value from the JSON response

> The `id_token` is a JWT string starting with `eyJ...` and is typically valid for **1 hour**.

### Option 2 — Python script (requires `google-auth` library)

```bash
pip install google-auth requests
```

```python
import google.oauth2.credentials
import google.auth.transport.requests
import requests

# After obtaining credentials via Google Sign-In flow:
# credentials = google.oauth2.credentials.Credentials(token=ACCESS_TOKEN, ...)
# request = google.auth.transport.requests.Request()
# credentials.refresh(request)
# id_token = credentials.id_token

# Quick test with tokeninfo (verify a token you already have):
token = "YOUR_ID_TOKEN_HERE"
r = requests.get(f"https://oauth2.googleapis.com/tokeninfo?id_token={token}")
print(r.json())
```

### Option 3 — From a real mobile / web app

If the project has a frontend or mobile client that already implements Google Sign-In, capture the `id_token` from the sign-in callback and paste it into the `googleIdToken` Postman variable.

## Step-by-step test in Postman

1. **Start the server**
   ```bash
   uv sync
   uv run uvicorn app.main:app --reload
   ```
   Confirm the log shows `✔ LOADED: layout_example`.

2. **Import the collection** — open Postman and import `docs/mobile-api.postman_collection.json`.

3. **Set the `googleIdToken` variable**
   - Open the collection, go to **Variables**
   - Paste your Google ID token in the `googleIdToken` row (Current Value column)

4. **Run "Google login"**
   - Open `layout_example → Auth → Google login`
   - Click **Send**
   - Expected response (`200 OK`):
     ```json
     {
       "access_token": "eyJhbGci...",
       "token_type": "bearer"
     }
     ```
   - The test script automatically saves the JWT to `accessToken`.

5. **Verify with "Me (current user)"**
   - Open `layout_example → Auth → Me (current user)`
   - Click **Send**
   - Expected response (`200 OK`):
     ```json
     {
       "user": {
         "user_id": "<google_sub_id>",
         "email": "your@gmail.com"
       }
     }
     ```

## Error responses

| Status | Detail | Cause |
|--------|--------|-------|
| `401` | `Invalid Google token` | Token expired, malformed, or `GOOGLE_CLIENT_ID` mismatch |
| `422` | Validation error | Missing `token` field in request body |

## Environment variables required

Ensure `credentials/layout_example.env` has:

```env
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret
```

> **Note:** `GOOGLE_CLIENT_ID` is used by the mobile/web client to initiate the sign-in flow. The server itself only calls the public `tokeninfo` endpoint — it does **not** use `GOOGLE_CLIENT_SECRET` during token verification.
