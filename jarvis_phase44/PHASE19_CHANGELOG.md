# Phase 19 — Runtime Lifecycle & Performance

- Added `LlmRuntimeManager` as the single lifecycle boundary around native llama inference.
- Serialized load/inference/unload with a coroutine Mutex to avoid native handle races.
- Added device capability detection: RAM, available RAM, CPU cores and low-RAM status.
- Added conservative Low/Medium/High clamping based on device capability.
- Added Android `onTrimMemory` handling; UI-hidden/critical memory pressure releases native model weights.
- Added safe runtime unload on ViewModel teardown.
- Added cancellation checks before/after native inference. The binding exposes no native cancellation API, so the native blocking call is never force-killed unsafely.
- Kept model files completely outside the APK.
