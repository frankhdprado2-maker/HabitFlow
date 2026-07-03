# HabitFlow

HabitFlow es una app Android nativa para gestionar habitos universitarios con registro manual, dictado de voz, autenticacion local y login con Google. El proyecto esta unificado en un solo repositorio: app Android + backend FastAPI desplegable en Render.

## Estado Actual

- App Android en Kotlin + Jetpack Compose.
- Backend FastAPI dentro de `backend/`.
- Backend publico actual: `https://habitflow-e353.onrender.com/c21200065/`.
- Autenticacion por email/password funcionando.
- Login con Google integrado en Android, pendiente de que Google Cloud tenga el OAuth Android/Web correctamente configurado.
- Cuentas nuevas empiezan en cero: sin habitos, logros, amigos, rachas ni notificaciones falsas.
- Registro manual de habitos disponible.
- Dictado de voz con tutorial y ejemplos.
- El dictado puede registrar frases como `ya corri 30 minutos` o `complete leer 20 paginas`.
- Room se usa como almacenamiento local offline-first.
- Render no necesita cambios para pantallas Android; solo para variables de backend.

## Estructura

```text
HabitFlow/
├─ app/                 # App Android Kotlin/Compose
├─ backend/             # Backend FastAPI para Render
├─ gradle/              # Gradle wrapper/config
├─ README.md            # Este archivo
└─ local.properties     # Configuracion local Android, no se sube a Git
```

## Stack Android

- Kotlin
- Jetpack Compose
- Navigation Compose
- MVVM + ViewModel
- Hilt
- Room
- Retrofit + OkHttp
- Moshi
- DataStore
- EncryptedSharedPreferences
- WorkManager
- SpeechRecognizer + TextToSpeech
- Google Sign-In

## Stack Backend

- FastAPI
- Uvicorn
- Pydantic v2
- SQLAlchemy async + asyncpg
- PostgreSQL/Neon
- JWT con PyJWT
- Google token verification
- Cloudflare R2/S3 compatible storage
- GraphQL con Strawberry
- Endpoint AI/voz con cliente LLM configurable

## Funcionalidades Android

### Autenticacion

- Crear cuenta con email y contrasena.
- Iniciar sesion con email y contrasena.
- Login con Google.
- Cerrar sesion desde Ajustes.
- Al crear una cuenta o iniciar sesion se limpia la base local para evitar mezclar datos de otra sesion.

### Habitos

- Lista de habitos del dia.
- Crear habito manualmente.
- Marcar habito como completado.
- Ver detalle de habito.
- Historial local.
- Estadisticas basicas.

### Voz

La pantalla de voz incluye un tutorial con ejemplos. El flujo actual es:

1. El usuario toca el microfono.
2. Android convierte voz a texto con `SpeechRecognizer`.
3. La frase se envia al backend en `/ai/voice-command`.
4. El backend interpreta la intencion.
5. Si detecta una accion de registro, Android crea o actualiza el habito localmente.

Ejemplos soportados:

```text
Ya corri 30 minutos
Complete leer 20 paginas
Salte tomar agua
No pude estudiar algoritmos
```

### Datos Iniciales

La app ya no carga datos demo automaticamente. Una cuenta nueva debe aparecer vacia.

## Backend Principal

El proyecto FastAPI montado es `c21200065`, por eso las rutas publicas usan este prefijo:

```text
https://habitflow-e353.onrender.com/c21200065/
```

Endpoints relevantes:

```text
GET  /health
POST /auth/register
POST /auth/login
POST /auth/google
POST /auth/refresh-token
GET  /auth/me
POST /geo-events/
GET  /geo-events/
GET  /geo-events/{id}
DELETE /geo-events/{id}
POST /graphql
POST /storage/upload-url
POST /storage/confirm
GET  /storage/files
DELETE /storage/file/{id}
POST /ai/voice-command
```

## Configuracion Android

Configura `local.properties` en tu maquina:

```properties
sdk.dir=C\:\\Users\\Frankie\\AppData\\Local\\Android\\Sdk
BASE_URL=https://habitflow-e353.onrender.com/c21200065/
GOOGLE_WEB_CLIENT_ID=TU_CLIENTE_WEB.apps.googleusercontent.com
```

Notas:

- `BASE_URL` debe terminar en `/c21200065/`.
- `GOOGLE_WEB_CLIENT_ID` debe ser el Client ID de tipo `Aplicacion web`, no el Android.
- El cliente Android de Google Cloud no se pega en `local.properties`; solo registra package + SHA-1.

## Configuracion Google Cloud

Para que Google Sign-In funcione se necesitan dos clientes OAuth para HabitFlow:

### Cliente Android

Tipo: `Android`

```text
Package name: com.unmsm.habitflow
SHA-1 debug: DD:A1:3D:8E:1B:D3:BC:F7:4D:05:EA:F0:27:5E:D9:95:62:E8:A2:64
```

### Cliente Web

Tipo: `Aplicacion web`

Este es el que se usa en:

```properties
GOOGLE_WEB_CLIENT_ID=...
```

Y en Render:

```env
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GOOGLE_REDIRECT_URI=https://habitflow-e353.onrender.com/c21200065/auth/google/callback
```

## Configuracion Render

Render debe apuntar al repo de GitHub con `Root Directory`:

```text
backend
```

Build command:

```bash
pip install -r requirements.txt
```

Start command:

```bash
uvicorn app.main:app --host 0.0.0.0 --port $PORT
```

Variable obligatoria:

```env
PYTHONPATH=src
```

Las demas variables deben configurarse en Render Environment. Usa como guia:

```text
backend/credentials/c21200065.env.example
```

No subas archivos `.env` reales ni claves privadas al repositorio.

## Compilar APK

Desde la raiz del proyecto:

```powershell
.\gradlew.bat assembleDebug
```

APK generado:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Instalar por ADB:

```powershell
C:\Users\Frankie\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r C:\Users\Frankie\Desktop\HabitFlow\app\build\outputs\apk\debug\app-debug.apk
```

Si `adb devices` no muestra el celular, activa Depuracion USB y acepta el permiso en el telefono.

## Ejecutar Backend Local

Desde `backend/`:

```powershell
pip install -r requirements.txt
$env:PYTHONPATH="src"
uvicorn app.main:app --reload
```

URL local:

```text
http://localhost:8000/c21200065/health
```

Si usas el emulador Android contra backend local, `BASE_URL` debe ser:

```properties
BASE_URL=http://10.0.2.2:8000/c21200065/
```

Si usas celular real, no uses `10.0.2.2`; usa una URL publica como Render.

## Pendientes

- Mejorar Google Sign-In cuando Google Cloud quede totalmente configurado.
- Sincronizar habitos como entidad propia en backend, no solo eventos.
- Editar perfil real.
- Crear/borrar cuenta real en backend desde la app.
- Mejorar estadisticas con datos historicos reales.
- Agregar pruebas automatizadas Android/backend.
- Preparar build release firmado.
- Publicacion por Google Play testing interno.

## Seguridad

- No commitear `local.properties`.
- No commitear `backend/credentials/*.env`.
- Rotar cualquier API key que haya sido compartida accidentalmente.
- Mantener secretos solo en Render Environment o en archivos locales ignorados por Git.
