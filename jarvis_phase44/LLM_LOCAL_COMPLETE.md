# Local LLM — Complete

The local text-model path is now treated as a complete subsystem.

## What is included

- GGUF model files are downloaded to the app's private `ai_models` directory.
- Downloads support pause/resume and SHA-256 verification when a checksum is supplied.
- Model readiness is checked before activation.
- Real on-device llama.cpp inference is used; the APK does not contain model weights.
- Only one text model is marked active at a time.
- Model activation/deactivation is serialized.
- Runtime profiles adapt to device RAM/CPU capacity.
- The model is unloaded under Android memory pressure.
- Inference is rejected when no local model is loaded.
- Model files remain local after download; inference does not require an API.
- The existing model manager UI can download, pause, delete, load, and switch models.

## Network boundary

Internet is required only for optional model acquisition (and for fetching the
native build dependency in a clean build environment). Once the model and
runtime are installed, text inference itself is local and does not call a
remote LLM API.

## Boundary

This subsystem does not promise that every arbitrary GGUF is compatible with
the bundled native runtime. Models must be compatible with the runtime and the
device's available resources.
