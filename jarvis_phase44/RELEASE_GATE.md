# Jarvis Release Gate — Phase 33

This gate is intentionally strict: a source ZIP is not considered a release until the Android project has been built and exercised on a physical device.

## Required gates

1. Gradle Wrapper present and executable.
2. JDK 17 available (required by AGP 9.1.x).
3. Android SDK with the project's compile/build tools installed.
4. NDK `29.0.13113456` installed, or the project NDK version deliberately changed and tested.
5. `:app:assembleDebug` succeeds from a clean checkout.
6. Native llama.cpp/mtmd configuration compiles for `arm64-v8a`.
7. APK installs on a physical Android device.
8. Local text-model download/load/inference works with networking disabled after download.
9. Local Vision model + matching `mmproj` download/load/inference works offline after download.
10. Offline STT/TTS voice assets work without a network fallback.
11. Assistant role can be selected by the user and invokes the app correctly.
12. SMS, Contacts, Files, Accessibility and Tool permissions work on-device.
13. Banking workflow remains confirmation-gated and is tested only with a non-production/test flow before any real transaction.

## Current environment result

The repository preflight currently reports 7/10 checks because this environment does not provide the Gradle wrapper, Android SDK environment, or ADB. Therefore **no APK build is claimed by this phase**.

## Important product rule

Offline inference means downloaded local models continue to run without network access. It does not mean web search, app-store downloads, or websites can work without the Internet.
