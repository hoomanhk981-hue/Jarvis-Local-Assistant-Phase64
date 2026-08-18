# Phase 15 — Real Local Model Manager

- Model weights are never bundled in the APK.
- Added separate auxiliary model artifact support for multimodal models (`mmproj`).
- Vision entries now require both the main GGUF and the matching mmproj before being considered downloaded/ready.
- Added SHA-256 validation for both primary and auxiliary artifacts when checksums are supplied.
- Added resumable primary downloads and explicit readiness checks.
- Database schema version bumped to 2 with destructive migration for the prototype branch.
- The app must not claim Vision inference is available merely because an image was selected; the actual multimodal runtime remains a separate integration step.

Technical note: llama.cpp documents Qwen 2 VL/Qwen 2.5 VL support through its multimodal `libmtmd` path and a model-specific `mmproj` artifact.
