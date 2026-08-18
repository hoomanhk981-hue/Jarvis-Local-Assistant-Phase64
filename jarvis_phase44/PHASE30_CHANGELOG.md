# Phase 30 — Offline Voice Pack Management

- Added `VoicePackManager`.
- Added Settings UI for offline STT/TTS readiness and official Android installation flows.
- Added Persian and English TTS install actions using `TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA`.
- Added Speech-to-Text language management via Android's official voice-input settings.
- Kept strict no-hidden-network-fallback behavior.
- Important platform limitation: Android does not expose a universal public API for an app to silently download arbitrary third-party SpeechRecognizer language packs. Jarvis therefore opens the selected recognizer's official language-management UI instead of pretending it can install an arbitrary pack itself.
