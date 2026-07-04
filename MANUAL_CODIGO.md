# Manual de explicación del código - HabitFlow

Este documento explica la estructura y el funcionamiento del proyecto **HabitFlow** por capas: Android, persistencia local, sincronización, autenticación, voz/IA y backend FastAPI.

> Nota: este manual describe la lógica principal del código y sirve como guía para exposición, revisión académica o mantenimiento del proyecto.

## Índice

- [Resumen general](#resumen-general)
- [Inicio de la app](#inicio-de-la-app)
- [Inyección de dependencias](#inyección-de-dependencias)
- [Modelo de datos](#modelo-de-datos)
- [Base local Room](#base-local-room)
- [Repositorio de hábitos](#repositorio-de-hábitos)
- [Autenticación](#autenticación)
- [Login y registro en UI](#login-y-registro-en-ui)
- [Navegación](#navegación)
- [Pantalla principal](#pantalla-principal)
- [Registro manual](#registro-manual)
- [Detalle de hábito](#detalle-de-hábito)
- [Historial](#historial)
- [Estadísticas](#estadísticas)
- [Voz](#voz)
- [Ajustes](#ajustes)
- [Perfil, logros y notificaciones](#perfil-logros-y-notificaciones)
- [Sincronización en segundo plano](#sincronización-en-segundo-plano)
- [Backend FastAPI](#backend-fastapi)
- [Router del proyecto](#router-del-proyecto)
- [Auth en backend](#auth-en-backend)
- [Eventos / historial](#eventos--historial)
- [IA / voz en backend](#ia--voz-en-backend)
- [Storage](#storage)
- [GraphQL](#graphql)
- [Estado actual y pendientes](#estado-actual-y-pendientes)

## Resumen General
HabitFlow es una app Android nativa hecha con Kotlin + Jetpack Compose, conectada a un backend FastAPI en Python.

El repositorio tiene dos partes principales:

- [app](app): aplicación Android.
- [backend](backend): API FastAPI desplegable en Render.

La idea principal es una app de hábitos universitarios con:

- autenticación por email/password;
- login con Google;
- creación manual de hábitos;
- registro de hábitos por voz;
- almacenamiento local offline-first con Room;
- sincronización de eventos con backend;
- historial, estadísticas, perfil, logros, notificaciones y ajustes.

## Inicio De La App
El punto de entrada Android está en [MainActivity.kt](app/src/main/java/com/unmsm/habitflow/MainActivity.kt).

Ahí se hace algo muy simple:

```kotlin
setContent {
    HabitFlowTheme {
        HabitFlowApp()
    }
}
```

Eso significa que `MainActivity` no contiene lógica de negocio. Solo activa pantalla completa con `enableEdgeToEdge()`, aplica el tema visual y carga la navegación principal.

La clase [HabitFlowApplication.kt](app/src/main/java/com/unmsm/habitflow/HabitFlowApplication.kt) inicializa Hilt con `@HiltAndroidApp` y configura WorkManager para poder inyectar dependencias en workers.

## Inyección De Dependencias
La configuración central está en [AppModule.kt](app/src/main/java/com/unmsm/habitflow/di/AppModule.kt).

Ahí se crean:

- `Moshi`, para convertir JSON a objetos Kotlin.
- `OkHttpClient`, para llamadas HTTP.
- `Retrofit`, para consumir el backend.
- APIs como `AuthApi`, `HabitEventApi`, `VoiceApi`, `StorageApi`.
- Base de datos Room `HabitFlowDatabase`.
- DAOs para hábitos, eventos, logros y notificaciones.

También lee `BASE_URL`, `AI_API_KEY` y `GOOGLE_WEB_CLIENT_ID` desde `local.properties`, definidos como `BuildConfig`.

## Modelo De Datos
Los modelos principales están en [Models.kt](app/src/main/java/com/unmsm/habitflow/domain/model/Models.kt).

Ahí se definen objetos de dominio:

- `User`: usuario de la app.
- `Habit`: hábito.
- `HabitEvent`: registro de cumplimiento de un hábito.
- `HabitStatus`: `Completed`, `Skipped`, `Failed`, `Pending`.
- `Achievement`: logro.
- `AppNotification`: notificación.
- `VoiceCommandResult`: resultado interpretado por voz.

Room usa entidades separadas en [Entities.kt](app/src/main/java/com/unmsm/habitflow/data/local/entity/Entities.kt). Por ejemplo, `HabitEntity` representa cómo se guarda el hábito en SQLite, mientras que `Habit` representa cómo lo usa la app.

Los mappers están en [Mappers.kt](app/src/main/java/com/unmsm/habitflow/data/Mappers.kt). Sirven para convertir:

- DTO remoto → dominio.
- entidad Room → dominio.
- dominio → entidad Room.

Eso evita mezclar UI, base de datos y API en una sola clase.

## Base Local Room
La base está en [HabitFlowDatabase.kt](app/src/main/java/com/unmsm/habitflow/data/local/HabitFlowDatabase.kt).

Tiene cuatro tablas:

- `habits`
- `habit_events`
- `achievements`
- `notifications`

Los DAOs controlan consultas:

- [HabitDao.kt](app/src/main/java/com/unmsm/habitflow/data/local/dao/HabitDao.kt): observar hábitos activos, buscar por nombre, insertar o actualizar.
- [HabitEventDao.kt](app/src/main/java/com/unmsm/habitflow/data/local/dao/HabitEventDao.kt): historial, eventos por hábito, eventos no sincronizados.
- [AchievementDao.kt](app/src/main/java/com/unmsm/habitflow/data/local/dao/AchievementDao.kt): logros.
- [NotificationDao.kt](app/src/main/java/com/unmsm/habitflow/data/local/dao/NotificationDao.kt): notificaciones.

## Repositorio De Hábitos
La lógica principal de hábitos está en [HabitRepository.kt](app/src/main/java/com/unmsm/habitflow/data/repository/HabitRepository.kt).

Implementa:

- observar hábitos con `observeHabits()`;
- observar historial con `observeEvents()`;
- crear hábitos manualmente con `createHabit()`;
- marcar hábitos con `markHabit()`;
- registrar notas con `addNote()`;
- aplicar comandos de voz con `applyVoiceCommand()`;
- sincronizar eventos con `syncEvent()`;
- sincronizar pendientes con `syncPending()`;
- descargar historial remoto con `pullRemoteHistory()`.

El flujo más importante es este:

1. El usuario marca un hábito.
2. La app crea un `HabitEvent` local con `synced = false`.
3. Se guarda en Room.
4. Se intenta enviar al backend usando `HabitEventApi`.
5. Si el backend responde bien, el evento local se marca como sincronizado.

Eso es offline-first: primero se protege el dato local, luego se sincroniza.

## Autenticación
La autenticación Android está en [AuthRepository.kt](app/src/main/java/com/unmsm/habitflow/data/repository/AuthRepository.kt).

Implementa:

- `login(email, password)`;
- `register(...)`;
- `googleLogin(idToken)`;
- `me()`;
- `logout()`;
- `isLoggedIn()`.

Cuando el usuario inicia sesión o se registra, la app limpia la base local con `database.clearAllTables()`. Eso evita mezclar datos de una sesión anterior con otra cuenta.

Los tokens se guardan en [TokenManager.kt](app/src/main/java/com/unmsm/habitflow/data/auth/TokenManager.kt) usando `EncryptedSharedPreferences`, o sea almacenamiento cifrado.

El header `Authorization: Bearer token` se agrega automáticamente en [AuthInterceptor.kt](app/src/main/java/com/unmsm/habitflow/data/remote/AuthInterceptor.kt).

Si el access token vence, [TokenAuthenticator.kt](app/src/main/java/com/unmsm/habitflow/data/remote/TokenAuthenticator.kt) intenta pedir un nuevo token con `/auth/refresh-token`.

## Login Y Registro En UI
Las pantallas están en [AuthScreens.kt](app/src/main/java/com/unmsm/habitflow/ui/screens/AuthScreens.kt).

`LoginScreen` permite:

- ingresar email;
- ingresar contraseña;
- iniciar sesión;
- login con Google;
- ir a registro;
- ir a recuperación.

La lógica está en [AuthViewModels.kt](app/src/main/java/com/unmsm/habitflow/ui/viewmodel/AuthViewModels.kt).

`LoginViewModel` valida email y contraseña, llama a `AuthRepository.login()` y actualiza `LoginUiState`.

`RegisterViewModel` maneja un registro de 3 pasos:

1. nombre y username;
2. email y contraseña;
3. objetivo principal.

Valida email y contraseña antes de crear la cuenta.

## Navegación
La navegación está en [AppNavigation.kt](app/src/main/java/com/unmsm/habitflow/ui/navigation/AppNavigation.kt).

Define rutas como:

- `splash`
- `onboarding`
- `login`
- `register`
- `home`
- `stats`
- `history`
- `profile`
- `settings`
- `voice`
- `manual_habit`
- `habit/{habitId}`

También tiene una barra inferior con:

- Inicio
- Stats
- Historial
- Perfil
- Ajustes

Cada pantalla se registra en un `NavHost`.

## Pantalla Principal
La pantalla Home está en [MainScreens.kt](app/src/main/java/com/unmsm/habitflow/ui/screens/MainScreens.kt).

`HomeScreen` muestra:

- saludo del usuario;
- progreso del día;
- barra de progreso;
- racha;
- cantidad de hábitos completados;
- lista de hábitos;
- botón para crear hábito manual;
- botón de notificaciones;
- botón de voz.

Su lógica está en `HomeViewModel`, dentro de [MainViewModels.kt](app/src/main/java/com/unmsm/habitflow/ui/viewmodel/MainViewModels.kt).

Ese ViewModel combina los hábitos locales con el estado de voz usando `combine()`. Cuando el usuario marca un hábito, llama a:

```kotlin
habitRepository.markHabit(habit, HabitStatus.Completed)
```

## Registro Manual
`ManualHabitScreen` permite crear un hábito con:

- nombre;
- categoría;
- frecuencia;
- hora o nota.

El ViewModel es `ManualHabitViewModel`.

Valida que el nombre tenga al menos 2 caracteres. Luego llama a:

```kotlin
habitRepository.createHabit(...)
```

El hábito se guarda solo localmente en Room.

## Detalle De Hábito
`HabitDetailScreen` muestra:

- nombre del hábito;
- categoría;
- frecuencia;
- racha;
- mejor racha;
- porcentaje mensual;
- heatmap visual;
- campo de nota;
- notas recientes.

`HabitDetailViewModel` recibe `habitId` desde la navegación usando `SavedStateHandle`.

Luego observa:

- el hábito por ID;
- los eventos de ese hábito;
- la nota escrita por el usuario.

Cuando se marca el día o se agrega nota, también termina creando un `HabitEvent`.

## Historial
`HistoryScreen` muestra eventos registrados.

`HistoryViewModel` permite filtrar por:

- Todos;
- Completados;
- Saltados.

El filtro se aplica sobre los eventos locales observados desde Room.

## Estadísticas
`StatsScreen` muestra:

- racha actual;
- porcentaje del mes;
- barras semanales;
- cumplimiento por hábito.

Actualmente las estadísticas son básicas. `currentStreak` se calcula con la mayor racha de los hábitos. `monthPercent` está en `0`, y `weekly` usa una lista fija de siete valores en cero. Es decir: la estructura está preparada, pero la estadística histórica real todavía está pendiente.

## Voz
La voz tiene tres piezas:

- [VoiceController.kt](app/src/main/java/com/unmsm/habitflow/voice/VoiceController.kt)
- [VoiceRepository.kt](app/src/main/java/com/unmsm/habitflow/data/repository/VoiceRepository.kt)
- `VoiceViewModel` en [MainViewModels.kt](app/src/main/java/com/unmsm/habitflow/ui/viewmodel/MainViewModels.kt)

El flujo es:

1. El usuario toca el micrófono.
2. `SpeechRecognizer` escucha en español Perú (`es-PE`).
3. Android convierte voz a texto.
4. `VoiceRepository` envía el texto al backend en `/ai/voice-command`.
5. El backend responde con intención, hábito y estado.
6. `HabitRepository.applyVoiceCommand()` crea o busca el hábito.
7. Se registra el evento localmente.
8. `TextToSpeech` lee la respuesta al usuario.

Ejemplo: si el usuario dice “Ya corrí 30 minutos”, el backend puede devolver algo como:

```json
{
  "intent": "registrar_habito",
  "habit_name": "corri 30 minutos",
  "status": "completed"
}
```

Luego Android crea o actualiza ese hábito.

## Ajustes
`SettingsScreen` permite cambiar:

- notificaciones;
- modo oscuro;
- biometría;
- perfil público;
- cerrar sesión;
- ir a eliminar cuenta.

La persistencia de ajustes está en [SettingsRepository.kt](app/src/main/java/com/unmsm/habitflow/data/repository/SettingsRepository.kt), usando DataStore.

Cerrar sesión llama a `authRepository.logout()`, que elimina tokens y limpia la base local.

## Perfil, Logros Y Notificaciones
`ProfileScreen` pide el usuario actual con `authRepository.me()` y muestra logros desbloqueados.

`AchievementsScreen` lista logros desde Room.

`NotificationsScreen` lista notificaciones desde Room.

Hay una función `ensureSeedData()` en `HabitRepository`, pero en el flujo revisado no vi que se llame automáticamente. Por eso, tal como dice el README, las cuentas nuevas deberían empezar vacías.

## Sincronización En Segundo Plano
Existe [SyncHabitEventsWorker.kt](app/src/main/java/com/unmsm/habitflow/work/SyncHabitEventsWorker.kt).

Ese worker llama:

```kotlin
habitRepository.syncPending()
```

Si funciona, devuelve `Result.success()`. Si falla, devuelve `Result.retry()`.

Eso permite reintentar eventos no sincronizados. Ojo: el worker está implementado, pero no encontré en el código leído una programación periódica explícita de WorkManager.

## Backend FastAPI
El backend empieza en [main.py](backend/src/app/main.py).

Crea la app FastAPI, carga proyectos automáticamente y en startup crea tablas con SQLAlchemy:

```python
await connection.run_sync(Base.metadata.create_all)
```

El sistema de descubrimiento está en [discovery.py](backend/src/app/core/discovery.py). Busca carpetas dentro de `app/projects`, importa su `api/router.py` y monta cada proyecto con prefijo.

En este caso el proyecto es `c21200065`, así que las rutas quedan bajo:

```text
/c21200065/
```

## Router Del Proyecto
El router principal está en [router.py](backend/src/app/projects/c21200065/api/router.py).

Incluye:

- `/health`
- `/auth`
- `/geo-events`
- `/geo-events-orm`
- `/graphql`
- `/storage`
- `/ai`

## Auth En Backend
La API de auth está en [auth.py](backend/src/app/projects/c21200065/api/auth.py).

Tiene:

- `POST /auth/login`
- `POST /auth/register`
- `POST /auth/google`
- `POST /auth/refresh-token`
- `GET /auth/me`

La lógica de negocio está en [auth_service.py](backend/src/app/projects/c21200065/domain/auth_service.py).

El backend:

- busca usuarios por email;
- valida password;
- crea usuarios;
- verifica login con Google;
- genera refresh tokens;
- rota refresh tokens.

Las contraseñas se hashean en [security.py](backend/src/app/projects/c21200065/domain/security.py) usando PBKDF2 con SHA-256 y 600,000 iteraciones.

Los access tokens JWT se crean en [token.py](backend/src/app/projects/c21200065/infra/token.py).

## Eventos / Historial
Los eventos están en:

- [geo_events.py](backend/src/app/projects/c21200065/api/geo_events.py)
- [geo_event_repo.py](backend/src/app/projects/c21200065/infra/repositories/geo_event_repo.py)
- [geo_event.py](backend/src/app/projects/c21200065/infra/orm/geo_event.py)

Aunque el nombre sea `geo-events`, Android lo usa como historial remoto de hábitos. Guarda datos como:

- `event_type`: nombre del hábito;
- `device_id`: id del hábito;
- `latitude` y `longitude`: van como `0.0`;
- `recorded_at`: fecha de registro.

Un detalle importante: Android envía `notes` y `metadata`, pero el esquema backend `GeoEventCreate` actual no declara esos campos. Entonces parte de esa metadata puede no persistirse en la tabla actual.

## IA / Voz En Backend
El endpoint está en [ai.py](backend/src/app/projects/c21200065/api/ai.py).

Recibe texto y devuelve una estructura:

- `intent`
- `response`
- `habit_id`
- `habit_name`
- `status`

Primero intenta usar el cliente LLM de [llm.py](backend/src/app/projects/c21200065/infra/clients/llm.py). Le pide devolver JSON válido.

Si falla, usa `_fallback_parse()`, que detecta palabras como:

- `salte`, `omitir`, `omiti` → `skipped`
- `falle`, `no pude`, `perdi` → `failed`
- por defecto → `completed`

Así la funcionalidad de voz no depende completamente de la IA externa.

## Storage
El backend tiene endpoints de archivos en [storage.py](backend/src/app/projects/c21200065/api/storage.py).

Sirven para:

- generar URL de subida;
- confirmar subida;
- listar archivos;
- eliminar archivo.

La lógica está en [storage_service.py](backend/src/app/projects/c21200065/domain/storage_service.py).

Usa un cliente compatible S3/R2 en [storage.py](backend/src/app/projects/c21200065/infra/clients/storage.py).

La seguridad consiste en que el `object_key` debe empezar con:

```text
{project_slug}/{user_id}/
```

Así un usuario no puede confirmar archivos en la carpeta de otro.

## GraphQL
GraphQL está en:

- [router.py](backend/src/app/projects/c21200065/api/graphql/router.py)
- [types.py](backend/src/app/projects/c21200065/api/graphql/types.py)
- [geo_events.py](backend/src/app/projects/c21200065/api/graphql/resolvers/geo_events.py)

Permite consultar, crear y borrar `geo_events` usando Strawberry GraphQL.

## Estado Actual Y Pendientes
La app ya tiene la arquitectura principal armada y varias funcionalidades conectadas. Pero hay algunas partes todavía básicas o incompletas:

- Recuperar contraseña es pantalla visual, no llama backend real.
- Verificar email es pantalla visual, no verifica código real.
- Editar perfil muestra campos, pero no guarda backend.
- Eliminar cuenta navega, pero no elimina realmente en backend.
- Estadísticas mensuales/semanales aún están incompletas.
- El heatmap es visual/demostrativo.
- El worker existe, pero no vi programación periódica.
- La sincronización usa `geo-events`, no una entidad backend propia de hábitos.
- Hay textos con caracteres mal codificados como `hÃ¡bito`, señal de encoding mojibake en algunos archivos.

En resumen: HabitFlow está construido con una arquitectura bastante clara: Compose para UI, ViewModels para estado, Repositories para lógica, Room para offline, Retrofit para backend, Hilt para inyección, y FastAPI/Postgres para servidor. La funcionalidad más interesante es el registro por voz, porque cruza Android SpeechRecognizer, backend IA, creación automática de hábitos y sincronización offline-first.
