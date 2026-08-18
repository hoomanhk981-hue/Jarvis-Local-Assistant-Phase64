# Phase 27 — Build Reproducibility & Preflight

- Added `scripts/preflight.py` to report Android SDK/NDK/CMake/ADB and native-source prerequisites without pretending to build an APK.
- Added `BUILD.md` with explicit debug/release build and signing requirements.
- Updated the README phase header and documented the external llama.cpp native dependency.
- Preserved the local-first model policy: model weights and Vision `mmproj` remain user downloads and are not bundled.
- No release keystore or credentials were added.
