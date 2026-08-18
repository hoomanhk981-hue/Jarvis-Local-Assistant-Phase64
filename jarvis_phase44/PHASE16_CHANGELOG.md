# Phase 16 — Real LLM Runtime Profiles (Low / Medium / High)

The Low / Medium / High setting is now an actual llama.cpp runtime profile instead of a UI-only token-count switch.

## Runtime profiles

| Mode | Context | Max generation | CPU threads |
|---|---:|---:|---:|
| LOW | 2048 | 160 tokens | up to 2 |
| MEDIUM | 4096 | 320 tokens | up to 4 |
| HIGH | 8192 | 768 tokens | up to 6 |

The requested thread count is capped by the device's available processor count.

## Behavior

- The selected profile is used when loading a local GGUF model.
- If the user changes Low/Medium/High after a model is already loaded, the next generation detects the profile change and reloads the same GGUF with the new context/thread configuration.
- Token budget changes per generation as well.
- System instructions differ slightly by mode: Low prioritizes concise speed, Medium balances speed/quality, High asks for more careful reasoning.
- No cloud inference or API fallback was added.
- Model weights remain user-downloaded; they are not bundled in the APK.

## Important limitation

The three profiles change runtime configuration and generation budget, but they do not magically change the underlying model's parameter count. Selecting HIGH on a 0.5B GGUF still runs that same 0.5B model. A future model-selection policy can automatically recommend/switch to a larger downloaded model when the device has enough RAM.

## Files changed

- `app/src/main/java/com/example/data/models/LocalLlmEngine.kt`
- `app/src/main/java/com/example/data/models/RealModelDownloadManager.kt`
- `app/src/main/java/com/example/ui/viewmodel/AssistantViewModel.kt`
