# Phase 28 — Reproducible CI Build Pipeline

- Added GitHub Actions workflow for a clean Android debug build.
- Pins JDK 21, Android API 36, Build Tools 36.0.0, CMake 3.31.6 and NDK 29.0.13113456.
- Uses Gradle 9.3.1 to match the project's Android Gradle Plugin 9.1.1 line.
- Runs the repository preflight before compilation.
- Builds `:app:assembleDebug` and uploads the resulting APK as a workflow artifact.
- Does not package model weights or secrets.
- This workflow is a build recipe; it has not been executed on GitHub from this environment.
