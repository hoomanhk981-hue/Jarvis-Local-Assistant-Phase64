# Phase 13 — Android Default Assistant + VoiceInteractionService

- Added a real `VoiceInteractionService` implementation with `BIND_VOICE_INTERACTION`.
- Added `VoiceInteractionSessionService` and a lightweight session bridge.
- Added `android.voice_interaction` metadata and `voice-interaction-service` XML.
- System ASSISTANT role remains explicitly user-controlled through `RoleManager.createRequestRoleIntent(ROLE_ASSISTANT)`.
- Existing `ACTION_ASSIST` and `ACTION_VOICE_COMMAND` MainActivity entry points are retained.
- Keyguard voice-assist entry launches Jarvis through the normal Android activity path.
- No hidden settings writes, root access, or privileged APIs are used.

References:
- Android RoleManager / ROLE_ASSISTANT documentation.
- Android VoiceInteractionService documentation.
