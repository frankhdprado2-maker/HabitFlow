# HabitFlow

HabitFlow es una app Android nativa en Kotlin para gestionar hábitos y tareas universitarias. Usa Jetpack Compose, MVVM, Repository, Room offline-first, Retrofit/OkHttp, Hilt, WorkManager, DataStore, EncryptedSharedPreferences y un módulo de voz con `SpeechRecognizer` + TextToSpeech.

## Estructura unificada

```text
HabitFlow/
├─ app/        # App Android Kotlin/Compose
├─ backend/    # Backend FastAPI para Render
├─ gradle/
└─ local.properties
```

El backend antes estaba en `C:\Users\Frankie\Desktop\platform-api`; ahora también tienes una copia dentro de este proyecto en `backend/`.

## Configuración

Edita `local.properties` en tu máquina:

```properties
BASE_URL=https://TU-BACKEND.onrender.com/c21200065/
AI_API_KEY=
GOOGLE_WEB_CLIENT_ID=tu-web-client-id.apps.googleusercontent.com
```

`BASE_URL` debe terminar apuntando al proyecto FastAPI `c21200065`. En emulador local, el valor por defecto es `http://10.0.2.2:8000/c21200065/`.

## Backend esperado

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/google`
- `POST /auth/refresh-token`
- `GET /auth/me`
- `POST /geo-events/`
- `GET /geo-events/`
- `GET /geo-events/{id}`
- `DELETE /geo-events/{id}`
- `POST /graphql`
- `POST /storage/upload-url`
- `POST /storage/confirm`
- `GET /storage/files`
- `DELETE /storage/file/{id}`
- `POST /ai/voice-command`

Los hábitos se guardan en Room. Al marcarlos como completados/saltados/fallados, la app crea un registro local y lo sincroniza con `/geo-events/` usando `device_id` como `habit_id`, `event_type` como nombre del hábito y `metadata.status` como estado.

## Ejecutar

```bash
./gradlew build
./gradlew installDebug
```

El primer arranque carga datos demo para que Home, Estadísticas, Historial, Perfil y Logros se puedan revisar aunque el backend todavía no esté listo.

## Desplegar Backend En Render

Sube este proyecto a GitHub y en Render usa la carpeta `backend` como raíz del servicio, o crea el Web Service apuntando a ese directorio.

Render debe usar:

```bash
Build Command: pip install -r requirements.txt
Start Command: uvicorn app.main:app --host 0.0.0.0 --port $PORT
```

Variable requerida:

```text
PYTHONPATH=src
```

Las demás variables están documentadas en `backend/credentials/c21200065.env.example`. No subas archivos `.env` reales con claves.
