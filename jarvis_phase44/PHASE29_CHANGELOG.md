# Phase 29 — Automated QA and Build Hardening

- Added unit tests for fuzzy matching, app matching, single-use confirmation, dangerous Termux classification, and banking validation/state transitions.
- Added a dependency-free repository QA preflight.
- Removed the missing custom debug.keystore dependency; debug builds now use the Android Gradle Plugin's managed debug signing.
- Release signing remains external through environment variables; no credentials or keystores are committed.
- Android compilation/device tests still require the pinned CI environment.
