# Phase 31 — Offline Capability Audit & Network Boundary

- Documented the offline/online boundary for core features.
- Added an explicit network-capability policy so local inference is not conceptually tied to web/API access.
- Clarified that Android framework APIs (SMS, Contacts, PackageManager, Accessibility, Storage) are device-local APIs, not paid cloud APIs.
- Added release notes for voice packs: TTS can use Android's official offline voice-data installer; STT availability/language packs remain provider/device dependent.
- Audited the native Vision dependency: llama.cpp is fetched at build time, while model weights remain external downloads and are not bundled in the APK.
