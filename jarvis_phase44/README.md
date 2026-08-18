# Jarvis Local Assistant — Phase 27

Phase 8 adds real Android communication tools to the existing local-agent architecture.

### Tools
- `make_call`: resolve a contact locally or accept a phone number, require confirmation, then place an Android `ACTION_CALL`.
- `send_sms`: require confirmation and send via `SmsManager`.
- Existing SMS search, Contacts search, Memory, Accessibility, Browser, and Termux tools remain available.

### Safety model
All call/SMS actions require explicit user confirmation. Android runtime permissions are still mandatory. Confirmation approval injects `confirmed=true` only at the final tool boundary, preventing the agent from silently escalating an action.

### Not yet implemented
- Real bank transfer automation
- Multimodal Qwen/Gemma vision inference
- Full browser checkout/payment automation
- End-to-end autonomous financial workflows

## Phase 9: File/Code Agent
The Code Agent now uses Android Storage Access Framework for a user-selected workspace. The agent can list, read, write (with confirmation), and create a ZIP in that workspace. It does not silently scan arbitrary storage.


## Phase 17

Vision now uses a real llama.cpp/libmtmd native runtime with a user-downloaded GGUF + matching mmproj. The APK does not contain model weights. The first project build fetches llama.cpp through CMake, so Gradle/CMake build requires network access.


## Phase 27 — Build Reproducibility & Preflight
The project now includes a local preflight validator for Android/NDK/CMake prerequisites and a documented release-build path. The validator does not claim an APK was built; it reports exactly which host prerequisites are missing.

The native llama.cpp dependency remains intentionally external and model weights are never bundled. Because the native dependency is currently fetched by CMake, a network connection is required for a clean first native build unless the dependency is pre-populated in the build environment.


## Phase 28 CI build

A GitHub Actions workflow is provided at `.github/workflows/android-build.yml`. It installs the pinned Android SDK/NDK/CMake toolchain, runs preflight, builds the debug APK, and uploads it as an artifact.
