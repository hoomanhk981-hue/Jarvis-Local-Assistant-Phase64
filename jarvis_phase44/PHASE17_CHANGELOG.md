# Phase 17 — Real multimodal Vision Runtime (llama.cpp + libmtmd)

This phase replaces the previous OCR-only Vision path with a real multimodal inference path.

## Runtime

- Android native CMake integration fetches llama.cpp at build time.
- `LLAMA_BUILD_MTMD=ON` builds the current `libmtmd` multimodal library.
- JNI bridge loads a user-downloaded GGUF language/vision model plus its matching `mmproj` GGUF.
- Images are read from Android `Uri`, converted through `mtmd_helper_bitmap_init_from_buf`, tokenized with `mtmd_tokenize`, encoded/decoded through `mtmd_helper_eval_chunk_single`, and generated with llama.cpp sampling.
- Chat template metadata from the loaded model is used when available.
- No model weights or mmproj files are bundled in the APK/source archive.

## Model source correction

The Qwen2-VL preset now points to the currently published `ggml-org/Qwen2-VL-2B-Instruct-GGUF` files:
- `Qwen2-VL-2B-Instruct-Q4_K_M.gguf`
- `mmproj-Qwen2-VL-2B-Instruct-f16.gguf`

The repository currently lists these as separate files; the multimodal projector is architecture-specific and is required for image inference.

## Safety / correctness

- If either file is missing, Vision refuses to claim that multimodal inference is available.
- OCR remains a separate local fallback capability, not a fake Vision-LLM response.
- CPU-first native configuration is used in this phase for predictable Android compatibility.
- GPU/offload optimization is intentionally deferred to a dedicated performance phase.

## Build note

The native dependency is fetched during Gradle/CMake build, so the first build requires network access. Runtime/model downloads remain separate: the end user still downloads the model and mmproj from the Model Manager.
