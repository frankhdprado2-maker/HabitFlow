# HabitFlow implementation plan

## Audit findings

- Android is a single Gradle module: `:app`.
- Kotlin version: `2.2.10`.
- Android Gradle Plugin: `9.2.1`.
- Compose BOM: `2026.02.01`.
- Material 3 dependency is `androidx.compose.material3:material3` through the Compose BOM.
- Navigation is centralized in `app/src/main/java/com/unmsm/habitflow/ui/navigation/AppNavigation.kt`.
- Current bottom navigation has five destinations: Home, Stats, History, Profile, Settings.
- Theme is in `ui/theme`. The current default is a purple-heavy "Clay" palette and default settings use `darkMode=true` and `accentColor=violet`.
- Screens are grouped in two large files: `AuthScreens.kt` and `MainScreens.kt`.
- Reusable UI is currently concentrated in `HabitFlowComponents.kt`, with repeated Material components and hardcoded visible text across screens.
- Room database version is `4`; entities include habits, habit events, achievements, notifications, user profile, plan recommendations, and cosmetic rewards.
- DAOs are scoped and UI observes Room through repositories/ViewModels, which matches the offline-first direction.
- Auth uses Retrofit/OkHttp/Moshi, JWT access and refresh tokens, `TokenAuthenticator`, and encrypted token storage.
- Google login originally used legacy `GoogleSignIn`; it is now migrated to Credential Manager with Google ID token.
- Voice currently records audio with `MediaRecorder`, sends it to backend transcription, then sends text to `/ai/voice-command`; it does not use `SpeechRecognizer` partial results.
- Voice UI state is represented by loose booleans (`listening`, `recording`, `transcribing`) instead of a typed state machine.
- Backend FastAPI project is mounted under `/c21200065`.
- Backend AI routes include `/ai/transcribe`, `/ai/transcription-status`, and `/ai/voice-command`.
- Backend voice domain already has a local parser, Redis/memory conversation session, ambiguity handling, and tests.
- Backend profile model has name, username, goal, timezone, avatar fields, and categories, but lacks onboarding/theme/accent/voice/locale fields.
- Tests present: Android default skeleton tests and backend `tests/test_voice_conversation.py`.

## Duplicates and risk areas

- Many visible Spanish strings are hardcoded in composables and ViewModels, several without accents.
- Purple/violet defaults are repeated in theme and settings.
- Screen files are large, making comprehensive redesign risky without incremental extraction.
- README says SpeechRecognizer is used, but code uses `MediaRecorder`; this mismatch is a regression risk for voice expectations.
- `AuthRepository.login/register/googleLogin` clears all local tables after token save, so offline user data can be lost on auth boundary.
- Habit completion currently creates events but does not update habit streak locally, so today's completion detection is approximate.
- `applyVoiceCommand` executes `registrar_habito` immediately; it does not require Android-side visual confirmation.

## Files to modify in this phase

- `app/src/main/java/com/unmsm/habitflow/ui/theme/Color.kt`
- `app/src/main/java/com/unmsm/habitflow/ui/theme/Theme.kt`
- `app/src/main/java/com/unmsm/habitflow/ui/theme/ClayTokens.kt`
- `app/src/main/java/com/unmsm/habitflow/ui/components/HabitFlowComponents.kt`
- `app/src/main/java/com/unmsm/habitflow/ui/state/UiStates.kt`
- `app/src/main/java/com/unmsm/habitflow/ui/viewmodel/AuthViewModels.kt`
- `app/src/main/java/com/unmsm/habitflow/ui/viewmodel/MainViewModels.kt`
- `app/src/main/java/com/unmsm/habitflow/ui/screens/AuthScreens.kt`
- `app/src/main/java/com/unmsm/habitflow/ui/screens/MainScreens.kt`
- `app/src/main/java/com/unmsm/habitflow/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/unmsm/habitflow/data/repository/SettingsRepository.kt`
- `app/src/main/java/com/unmsm/habitflow/data/repository/VoiceRepository.kt`
- `app/src/main/java/com/unmsm/habitflow/data/repository/HabitRepository.kt`
- `app/src/main/java/com/unmsm/habitflow/data/remote/api/VoiceApi.kt`
- `app/src/main/java/com/unmsm/habitflow/data/remote/dto/VoiceDtos.kt`
- `app/src/main/java/com/unmsm/habitflow/data/Mappers.kt`
- `app/src/main/java/com/unmsm/habitflow/voice/VoiceController.kt`
- `app/src/main/res/values/strings.xml`
- `backend/src/app/main.py`
- `backend/src/app/projects/c21200065/api/ai.py`
- `backend/src/app/projects/c21200065/api/auth.py`
- `backend/src/app/projects/c21200065/api/schemas.py`
- `backend/src/app/projects/c21200065/domain/auth_service.py`
- `backend/src/app/projects/c21200065/domain/voice_conversation_service.py`
- `backend/src/app/projects/c21200065/infra/orm/auth.py`
- `backend/src/app/projects/c21200065/infra/repositories/auth_orm_repo.py`
- `backend/tests/test_voice_conversation.py`
- focused Android unit tests under `app/src/test/...`

## Migration needs

- Android Room migration `4 -> 5` for user profile preference fields if stored locally in Room.
- Backend startup compatibility columns for:
  - `bio`
  - `primary_goal`
  - `preferred_categories`
  - `onboarding_completed`
  - `theme_mode`
  - `accent_theme`
  - `voice_response_enabled`
  - `locale`
- All new backend columns should be nullable or defaulted to avoid forcing old users through destructive migration.

## Implementation phases

1. Design system foundation:
   - Replace purple default with warm HabitFlow palette.
   - Rename/alias tokens to HabitFlow tokens while keeping old symbols compatible where needed.
   - Add reusable buttons, fields, cards, empty/error/loading states, voice orb, message bubbles, metric cards, section headers, and navigation bar.

2. Product flow:
   - Change default settings to light/system-friendly mint accent.
   - Route first login to profile/onboarding when profile is incomplete.
   - Expand profile setup into multi-step onboarding using existing profile update contract.
   - Improve login/register fields, password visibility, errors, and copy.

3. Main UI:
   - Redesign Home with greeting, date, progress, metrics, next activity, empty state, habit cards, and voice FAB.
   - Improve Progress and History with useful empty states, filters, summaries, and simple charts with Compose primitives.
   - Move Settings access into Profile top action while keeping Settings route for compatibility.
   - Redesign Profile and Settings with clearer sections and real empty states.

4. Voice:
   - Add typed `VoiceAssistantPhase`.
   - Introduce local text parser fallback for common Spanish phrases and confirmation requirements.
   - Avoid mixing first name with transcript; first name remains presentation-only.
   - Add a conversational UI with partial transcript field, suggestions, retry/cancel/manual actions.
   - Keep backend compatibility and add new `/ai/conversation` contract.

5. Backend profile and conversation:
   - Extend profile fields with safe defaults and validation.
   - Add conceptual `/ai/conversation` endpoint with typed intents and confirmation metadata.
   - Keep `/ai/voice-command` response stable for older Android clients.

6. Tests and verification:
   - Add unit tests for name normalization, local voice parsing, and state transitions where feasible.
   - Extend backend tests for conversation endpoint mapping, ambiguous actions, user-without-name, and personal-name phrases.
   - Run Gradle tests/lint/assembleDebug and backend pytest/ruff.

## Compatibility strategy

- Preserve existing routes and endpoint contracts.
- Add new backend fields as optional/defaulted fields.
- Keep `/ai/voice-command` operational while Android can move toward `/ai/conversation`.
- Google Sign-In now uses Credential Manager + Google ID token. Legacy `GoogleSignIn` code and `play-services-auth` dependency were removed.
- Keep Room as source of truth for visible habit and event data.
- Add local voice fallback rather than making the voice experience depend entirely on STT/AI provider availability.

## Risks

- Full Credential Manager migration can break Google login if OAuth client setup differs, so this phase improves current flow and keeps the door open.
- Android instrumentation tests may require emulator/device availability; local unit tests are safer in this environment.
- Backend tests may require environment defaults for database settings; parser-level tests avoid external infrastructure.
- Room profile migration must not lose old user profile rows.
- Voice confirmation is behavior-changing; old endpoint remains immediate, while new conversation endpoint exposes confirmation metadata.

## Verification commands

- Android unit tests: `.\gradlew.bat test`
- Android lint: `.\gradlew.bat lint`
- Android debug build: `.\gradlew.bat assembleDebug`
- Backend tests: from `backend`, `uv run pytest` or `python -m pytest`
- Backend lint: from `backend`, `uv run ruff check .` or `ruff check .`

## Implementation status after first pass

- Done: HabitFlow visual tokens, warm light default, mint default accent, typography and component library.
- Done: four-item main navigation with Profile-owned Settings and a prominent voice FAB.
- Done: redesigned Login, Register, post-login six-step onboarding, Home, Progress, History, Profile, Settings, Voice, Manual Habit, and account deletion confirmation screens.
- Done: Home and Progress now derive completion/progress from Room events instead of only from habit streaks.
- Done: habit completion is offline-first; local event is kept even when sync fails, and Home exposes an undo path.
- Done: voice UI uses a typed phase, `SpeechRecognizer` partial results, confirmation before applying events, and local deterministic fallback.
- Done: local voice parser protects personal-name phrases such as "Hola, soy Frank" from becoming habit names.
- Done: backend profile columns and schemas were extended for onboarding, theme, accent, voice response, locale, bio, and preferred categories.
- Done: backend `/ai/conversation` was added while keeping `/ai/voice-command` compatible.
- Done: focused Android and backend regression tests were added for voice parsing and name handling.
- Done: Android Google Sign-In was migrated from legacy `GoogleSignIn` to Credential Manager using `GOOGLE_WEB_CLIENT_ID`, `GetGoogleIdOption`, `GoogleIdTokenCredential`, cancellation/no-credential/token parsing handling, and the existing FastAPI token exchange.
- Pending: full extraction of every visible literal to string resources; key corrected terms are in `strings.xml`, but the large Compose files still contain inline copy.
- Done: Room database was migrated from version 4 to 5 with non-destructive profile columns for `primaryGoal`, `preferredCategoriesCsv`, `onboardingCompleted`, `themeMode`, `accentTheme`, `voiceResponseEnabled`, and `locale`.
- Done: backend Google verification now checks expected audience through `GOOGLE_WEB_CLIENT_ID`/`GOOGLE_CLIENT_ID`, issuer, verified email, subject, and email; Google login normalizes email and does not overwrite existing onboarding/theme/accent/voice preferences.
- Done: backend `/health` root endpoint, Render `healthCheckPath`, safe env example updates, and Render deployment notes were added.
- Done: Ruff passes on all modified backend Python files.
- Pending: full backend Ruff cleanup across the entire existing codebase; full `uv run ruff check .` still reports pre-existing debt outside modified files.

## Google authentication requirement

Required: Google Sign-In is an obligatory academic requirement. Email/password authentication is complementary and does not replace Google authentication. The project cannot be considered complete until Google Sign-In is verified end-to-end.

Status:

- Done: `Continuar con Google` remains visible in Login.
- Done: Android uses Credential Manager and Google ID token APIs.
- Done: Android sends only the Google ID token to FastAPI and stores only HabitFlow JWTs returned by the backend.
- Done: FastAPI verifies token data through Google tokeninfo, checks audience/issuer/email verification, creates or links users by Google subject then normalized email, and returns HabitFlow access/refresh tokens.
- Done: navigation after token exchange checks `/auth/me`; incomplete users go to onboarding and completed users go to Home.
- Blocked: end-to-end Google selector/token/JWT/navigation verification on emulator or device was not executed in this environment. Missing evidence: selector screenshot/open event, real Google ID token received, FastAPI verification of that real token, and device navigation proof.

## Final implementation report

Date: 2026-07-10.

Done:

- Credential Manager integration replaced legacy Google Sign-In code.
- Room migration 4 -> 5 preserves existing profile rows and copies `profileComplete` into `onboardingCompleted`.
- Profile preferences now persist through DTO/domain/entity/repository/onboarding for bio, goal, categories, theme, accent, voice response, locale, and onboarding completion.
- Backend profile validation was tightened with Pydantic constraints and category cleanup.
- Backend Google login avoids duplicate users by normalizing email and linking by Google subject/email without overwriting existing preferences.
- `/health`, `/docs`, `/openapi.json`, `/c21200065/health`, `/c21200065/ai/conversation`, and `/c21200065/ai/voice-command` were verified through FastAPI TestClient.
- Render config uses `healthCheckPath: /health`; secrets remain `sync: false`; `backend/docs/render-deployment.md` documents env vars and Postgres migration strategy.
- Key Spanish strings and plurals were corrected in `app/src/main/res/values/strings.xml`; critical Google/FAB strings are wired through resources.

Blocked:

- Google Sign-In end-to-end device/emulator verification: no emulator/device OAuth run was available.
- Render production deployment: no Render credentials/project deployment action was available, so no public URL was verified.

Pending:

- Full extraction of every visible Compose/ViewModel literal to string resources.
- Full backend Ruff cleanup outside modified files. `uv run ruff check .` still reports 113 issues in pre-existing untouched areas such as `deps`, geo events, GraphQL, storage, and ports.
- Convert startup Postgres compatibility SQL into a formal Alembic migration before production hardening.

Verification evidence:

- Passed: `.\gradlew.bat test lint assembleDebug`.
- Passed: `uv run pytest` (`13 passed`).
- Passed: Ruff on modified backend Python files.
- Passed: production command smoke test `uv run uvicorn app.main:app --host 127.0.0.1 --port 8765`, followed by `GET /health` -> 200; process was stopped after verification.
- Passed: TestClient route check for `/health`, `/docs`, `/openapi.json`, `/c21200065/health`, `/c21200065/ai/conversation`, and `/c21200065/ai/voice-command`.
- Failed by known global debt: `uv run ruff check .` across all backend files.

Files added in this pass:

- `backend/docs/render-deployment.md`
- `backend/tests/test_google_auth.py`

Operational notes:

- Android local config uses `GOOGLE_WEB_CLIENT_ID` from `local.properties` through `BuildConfig`.
- Backend accepts `GOOGLE_WEB_CLIENT_ID` and keeps `GOOGLE_CLIENT_ID` as a backward-compatible alias; during deployment both should be the same Web client ID.
- The real backend credential file remains ignored by Git; only example/documentation files were changed.

## Preparation phases 1 and 2 evidence

Date: 2026-07-10.

Repository status:

- Root path: `C:\Users\Frankie\Desktop\HabitFlow`.
- Branch: `main`.
- Remote: `origin` points to GitHub repository `frankhdprado2-maker/HabitFlow`.
- Render config checked at `backend/render.yaml`; no root-level `render.yaml` is present.
- Commit: prepared from this working tree after tests and OAuth evidence were recorded; exact short hash is reported in the final Codex response.
- Push: performed after commit to the current branch only; final push status is reported in the final Codex response.

Verification:

- Passed: `.\gradlew.bat test` (`BUILD SUCCESSFUL`).
- Passed: `.\gradlew.bat assembleDebug` (`BUILD SUCCESSFUL`).
- Passed: from `backend`, `uv run pytest` (`13 passed`).

Local and ignored files:

- `local.properties` remains ignored and was not staged; it contains local-only Android configuration values.
- `GOOGLE_WEB_CLIENT_ID` is configured through `local.properties` and `BuildConfig`; the actual value is intentionally not recorded here.
- Real backend credential env files under `backend/credentials/*.env` remain ignored.
- IDE files under `.idea/`, `.env*` files except `.env.example`, keystores, private keys, and credential material are ignored.

Security review:

- Diff path scan found no sensitive paths such as `local.properties`, real `.env`, keystores, private keys, or credential files in the versioned changes.
- Token-related scan hits were code variable assignments, not secret values.

Blocked or intentionally not executed:

- Google Sign-In end-to-end device/emulator OAuth verification remains blocked because no device/emulator OAuth run was performed in this preparation step.
- No Google Cloud Console changes, Render deployment, PostgreSQL provisioning, or production environment variable configuration was executed.

## Google Android OAuth preparation

Android identity values:

- Base `applicationId`: `com.unmsm.habitflow`.
- Effective debug `applicationId`: `com.unmsm.habitflow`.
- `namespace`: `com.unmsm.habitflow`.
- Product flavors: none.
- `applicationIdSuffix`: none.
- Explicit build types: `release`; debug uses the implicit Android debug build type.
- Explicit signing configs: none; debug uses the standard local debug signing config.
- `GOOGLE_WEB_CLIENT_ID`: wired through `BuildConfig` from `local.properties`; value intentionally omitted.

Debug signing report:

- Variant: `debug`.
- Config: `debug`.
- Store: `C:\Users\Frankie\.android\debug.keystore`.
- Alias: `AndroidDebugKey`.
- SHA-1: `DD:A1:3D:8E:1B:D3:BC:F7:4D:05:EA:F0:27:5E:D9:95:62:E8:A2:64`.
- SHA-256: `8C:70:3C:EE:3D:43:09:6B:3E:22:C0:54:BD:20:6C:63:33:BB:33:78:B3:B0:52:80:99:91:37:0C:83:C3:AB:C5`.

Status:

- Ready to create the Android OAuth client with package `com.unmsm.habitflow` and the debug SHA-1 above when the workflow is allowed to continue.
- Release SHA was not inferred or generated; a release signing config/keystore would produce a different OAuth fingerprint.
- Debug keystore was not changed.

## AI, Google login, voice, and insets stabilization

Date: 2026-07-10.

Phase 1 audit and initial validation:

- Initial Git status before this pass was not clean: `.idea/misc.xml` and `app/src/main/java/com/unmsm/habitflow/MainActivity.kt` were already modified.
- Initial commands executed: `git status`, `git diff`, `git log -1 --oneline`.
- Initial validation passed: `.\gradlew.bat test`, `.\gradlew.bat assembleDebug`, and from `backend`, `uv run pytest`.
- Root cause: Google login used a broad `loading` boolean and split Credential Manager from backend/profile loading, so errors and cancellations were not represented as typed terminal states.
- Root cause: bottom navigation was rendered without applying Scaffold `innerPadding` to `NavHost`, and the custom navigation bar forced a fixed height instead of using Material navigation bar insets.
- Root cause: SpeechRecognizer always requested offline preference when API allowed it, even when on-device recognition was not actually available; errors were string-only and rendered more than once.
- Root cause: the conversational endpoint existed but Android still used `/ai/voice-command` as the primary assistant path.

Phase 2 Google login:

- Added typed `GoogleLoginState`: `Idle`, `OpeningGoogle`, `ContactingBackend`, `LoadingProfile`, `Success`, and `Error`.
- Login starts only from the Google button click; duplicate clicks are ignored while Google flow is active.
- Credential Manager cancellation, missing credentials, parsing failure, generic credential failure, blank token, backend failures, timeout, empty response, and profile-loading failure all return to a recoverable UI state.
- Google button is disabled during the flow and exposes `Reintentar` after an error.
- Non-blocking `GET /health` warm-up runs when Login opens.
- OkHttp timeouts are now connect 20s, read 30s, write 30s, call 45s.
- Debug logs are limited to flow milestones and HTTP basics; tokens and secrets are not logged.

Phase 3 WindowInsets and FAB:

- Main navigation now uses one `Scaffold` with `contentWindowInsets = WindowInsets.safeDrawing`.
- `NavHost` consumes `innerPadding`; screens receive zero local Scaffold padding to avoid double padding.
- Bottom navigation remains in `bottomBar`, mic remains in `floatingActionButton`.
- Custom navigation bar no longer forces a fixed height and uses `NavigationBarDefaults.windowInsets`.
- Existing list bottom content padding remains to keep content clear of bottom bar and FAB.

Phase 4 and 11 voice:

- Replaced enum-like voice phase with sealed `VoiceAssistantPhase`, including typed `PartialResult(text)` and `Error(type, message)`.
- Added `VoiceErrorType` and `VoiceRecognitionError`.
- SpeechRecognizer now checks service availability and on-device availability separately.
- Offline recognizer is used only when on-device recognition is available; otherwise system recognition is used without forcing offline preference.
- Recognizer errors map to user-safe messages for audio, client, permission, network, timeout, no match, busy, server, speech timeout, too many requests, service unavailable, and unknown.
- Voice UI displays a single error component with `Reintentar`, `Escribir`, and `Manual`.
- Manual text input remains always available.
- Text sent to parser/backend is only the recognized or typed text; user name is not concatenated with transcript.

Phases 5 to 10 AI product functionality:

- Android primary assistant call now uses `/ai/conversation`; `/ai/voice-command` remains implemented for compatibility.
- Android sends structured context: habits, recent events, achievements, categories, preferred time, and duration when locally available.
- Backend `/ai/conversation` now supports typed intents for daily plan, weekly summary, adaptive recommendation, rescheduling, confirm/reject/cancel extensions, and the existing create/complete/skip/query flow.
- Added typed backend response models for `daily_plan`, `weekly_summary`, and `adaptive_recommendation`.
- `Plan de hoy` uses real pending habits, completed/skipped events for today, preferred time, and duration when available.
- Weekly summary calculates deterministic metrics before producing text; no LLM computes numbers.
- Adaptive recommendations use rules for repeated skips and strong recent completion.
- Rescheduling distinguishes one-time options from permanent changes through suggestions; no habit is modified without confirmation.
- Offline fallback remains local parser based and announces: `Estoy usando el modo sin conexion.`

Security and privacy:

- Android continues to send Google ID token only to FastAPI auth, never to the AI endpoint.
- Assistant context does not include access token, refresh token, Google ID token, passwords, secrets, or raw audio.
- Diff scan found no sensitive paths in versioned changes and no high-risk added secret lines.
- AI actions are mapped into typed models and still require explicit Android confirmation before local execution.

Files modified in this pass:

- Android: `AuthScreens.kt`, `AuthViewModels.kt`, `UiStates.kt`, `AppNavigation.kt`, `HabitFlowComponents.kt`, `MainScreens.kt`, `MainViewModels.kt`, `VoiceController.kt`, `VoiceRecognitionError.kt`, `AuthApi.kt`, `AuthRepository.kt`, `VoiceRepository.kt`, `VoiceDtos.kt`, `Mappers.kt`, `AppModule.kt`, `UiStateTest.kt`.
- Backend: `backend/src/app/projects/c21200065/api/ai.py`, `backend/tests/test_ai_conversation_features.py`.
- Pre-existing local changes still present: `.idea/misc.xml`, `MainActivity.kt`.

Automated verification after fixes:

- Passed: `.\gradlew.bat test`.
- Passed: `.\gradlew.bat lint`.
- Passed: `.\gradlew.bat assembleDebug`.
- Passed: from `backend`, `uv run pytest` (`16 passed`).
- Passed: from `backend`, `uv run ruff check src/app/projects/c21200065/api/ai.py tests/test_ai_conversation_features.py`.
- Passed: TestClient `GET /health` -> 200.
- Passed: TestClient `GET /docs` -> 200.
- Passed: TestClient `POST /c21200065/ai/conversation` with auth override -> 200 and intent `GENERATE_DAILY_PLAN`.
- Expected security check: `POST /c21200065/ai/conversation` without auth returned 401.

Manual verification matrix:

- Login Google cancelado: Not executed; requires device/emulator Credential Manager UI.
- Login Google exitoso: Blocked; requires real configured OAuth client and device/emulator.
- Render despertando: Not executed against live Render; timeout and warm-up handling implemented locally.
- Timeout backend: Not executed against live network; OkHttp and repository timeout mapping implemented.
- Usuario nuevo to onboarding: Not executed on device.
- Usuario recurrente to Home: Not executed on device.
- Barra de navegacion con tres botones: Not executed on device.
- Barra con navegacion por gestos: Not executed on device.
- FAB sin tapar contenido: Not executed visually on device.
- Permiso de microfono rechazado: Not executed on device.
- Permiso aceptado: Not executed on device.
- SpeechRecognizer disponible: Not executed on device.
- SpeechRecognizer no disponible: Not executed on device.
- Entrada manual: Not executed manually; code path remains present and compiles.
- Conversacion para crear habito: Backend/domain coverage passed; Android manual flow not executed.
- Confirmacion: Android confirmation path compiles and previous parser tests cover confirmation data; manual UI not executed.
- Plan diario: Backend smoke and tests passed.
- Resumen semanal: Backend tests passed.
- Recomendacion adaptativa: Backend tests passed.
- Modo offline: Local parser tests passed; network-off manual device test not executed.

Pending debt:

- Full Google sign-in must still be verified on a real emulator/device with the configured Web OAuth client.
- Visual insets/FAB behavior must still be checked on small phone, gesture navigation, and three-button navigation.
- SpeechRecognizer behavior must still be checked on devices with and without recognition services.
- Adaptive recommendation accept/edit/reject persistence is not yet tracked as a durable user preference.
- Release OAuth SHA remains separate from debug and was not generated here.
