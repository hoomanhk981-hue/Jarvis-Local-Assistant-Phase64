# Phase 14 — Strict Local Voice Pipeline

Implemented a real local-only voice boundary for Jarvis.

## Speech-to-Text
- Uses `SpeechRecognizer.createOnDeviceSpeechRecognizer()` on Android 12+.
- Does not silently fall back to a network recognizer.
- Supports Persian (`fa-IR`) and English (`en-US`) when the device recognizer has the corresponding on-device language model installed.
- Partial and final recognition results are exposed to the existing AssistantViewModel.
- Reports a clear error when an on-device recognizer/language is unavailable.

## Text-to-Speech
- Prefers installed voices whose `isNetworkConnectionRequired` is false.
- Refuses to silently use an online voice.
- Persian and English are selected independently according to the app language.
- Reports when an offline voice for the selected language is not installed.

## Assistant integration
- Existing `ACTION_ASSIST` / `ACTION_VOICE_COMMAND` entry point continues to open the live voice UI.
- Existing STT → `processUserMessage()` → local Agent → TTS path now uses the strict local STT/TTS boundary.
- Added lifecycle cleanup to prevent recognizer/TTS leaks.

## Important limitation
Android's platform TTS engine is only as local as the installed voice data. Jarvis therefore verifies the selected voice is not marked as network-required. A dedicated Persian offline TTS model/engine can be added later without changing the Agent layer.
