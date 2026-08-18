# Phase 5 — Private Browser Agent

Implemented a real in-app browser control layer.

## Added
- `BrowserActivity` with a private WebView.
- `BrowserController` with URL normalization, DOM inspection, click and text-entry actions.
- Agent tools: `open_url`, `browser_inspect`, `browser_click`, `browser_type`, `browser_back`.
- Tool results are returned through the existing `LocalAgentEngine` instead of being assumed successful.
- Only HTTP/HTTPS URLs are accepted by the URL normalizer.

## Important limitation
The current `llama-android:0.1.1` dependency exposes text completion/embeddings, not llama.cpp's modern `libmtmd` multimodal API. Therefore Phase 5 does NOT falsely claim that Qwen2-VL image inference is implemented. The existing ML Kit OCR remains the real local image fallback.

Modern llama.cpp multimodal support uses `libmtmd` and a model plus model-specific `mmproj` file. A future native bridge must be built against a llama.cpp revision exposing that API before the vision LLM can be honestly enabled.
