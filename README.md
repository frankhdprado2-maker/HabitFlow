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
├─ README.md            # Resumen principal del proyecto
├─ MANUAL_CODIGO.md     # Explicacion detallada del codigo
└─ local.properties     # Configuracion local Android, no se sube a Git
```

## Manual de Explicacion del Codigo

Para entender la arquitectura interna del proyecto, revisa el archivo:

[MANUAL_CODIGO.md](MANUAL_CODIGO.md)

Ese manual explica el codigo por capas: Android, navegacion, ViewModels, repositorios, Room, autenticacion, voz/IA, sincronizacion, backend FastAPI, endpoints, storage, GraphQL y pendientes actuales.

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

### Voz e interpretacion inteligente

La pantalla de voz esta en la navegacion principal como `Asistente de voz` desde Home. El proyecto detectado es Android nativo con Kotlin + Jetpack Compose; por eso el reconocimiento usa APIs nativas de Android: `SpeechRecognizer`, `RecognizerIntent` y `TextToSpeech`. No se usa Web Speech API.

Flujo actual:

1. El usuario toca el microfono en `Registrar con voz`.
2. Android solicita `RECORD_AUDIO` en tiempo de ejecucion si hace falta.
3. Android convierte voz a texto con `SpeechRecognizer` configurado para `es-PE`.
4. La transcripcion aparece en pantalla y puede editarse manualmente.
5. La app envia solo texto autenticado a `POST /ai/interpret-habit`.
6. FastAPI llama a Gemini con `google-genai`, valida la respuesta con Pydantic y responde con habitos estructurados.
7. Android muestra una confirmacion editable con nombre, estado, cantidad, unidad, fecha, notas y asociacion con habitos existentes.
8. Solo al tocar `Confirmar y registrar`, Android reutiliza `HabitRepository.applyVoiceCommand`, Room y `geo-events/`.

La IA nunca guarda automaticamente un habito. Si detecta `query_habit`, la app no guarda nada y muestra que las consultas inteligentes quedan para una version posterior.

Ejemplos soportados:

```text
Ya corri 30 minutos
Complete leer 20 paginas
Salte tomar agua
No pude estudiar algoritmos
Hoy medite diez minutos y lei veinte paginas
Manana quiero correr treinta minutos
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
POST /ai/interpret-habit
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
GEMINI_API_KEY=tu_clave_de_gemini
GEMINI_MODEL=gemini-3.1-flash-lite
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

## Instalar y ejecutar

Backend local:

```powershell
cd backend
pip install -r requirements.txt
$env:PYTHONPATH="src"
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Android debug:

```powershell
.\gradlew.bat assembleDebug
```

Pruebas:

```powershell
.\gradlew.bat testDebugUnitTest
cd backend
$env:PYTHONPATH="src"
pytest
```

## Probar voz en emulador Android

1. En `local.properties`, usa `BASE_URL=http://10.0.2.2:8000/c21200065/` para backend local o la URL de Render para produccion.
2. Inicia FastAPI con las variables de entorno y `GEMINI_API_KEY`.
3. Abre la app en el emulador, inicia sesion y entra a `Asistente de voz`.
4. Toca el microfono, concede `RECORD_AUDIO`, dicta una frase y revisa la transcripcion.
5. Edita el texto si hace falta, envia, corrige las tarjetas y toca `Confirmar y registrar`.

## Probar en celular fisico

1. Usa una URL accesible desde el telefono: Render o la IP LAN de tu PC, por ejemplo `http://192.168.1.20:8000/c21200065/`.
2. Si usas IP local, ejecuta FastAPI con `--host 0.0.0.0` y permite el puerto en el firewall.
3. Configura `BASE_URL` en `local.properties`, recompila e instala el APK.
4. Concede microfono, dicta una frase y confirma manualmente antes de guardar.

## Limitaciones conocidas

- El reconocimiento de voz depende del dispositivo/servicios Android; el modo offline solo funciona si Android lo soporta.
- Las consultas tipo `Cuantas veces medite esta semana?` se detectan como `query_habit`, pero todavia no generan respuestas estadisticas inteligentes.
- La asociacion con habitos existentes se infiere localmente por nombre y puede corregirse con chips antes de guardar.
- No se guarda audio ni se envia audio a FastAPI; solo se envia texto.
