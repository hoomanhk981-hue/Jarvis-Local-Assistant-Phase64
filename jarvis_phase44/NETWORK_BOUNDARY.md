# Jarvis network boundary — Phase 32

Jarvis does not require a remote inference API for its local AI path.

## Local/offline after installation
- GGUF text-model inference runs locally through the native runtime.
- Multimodal inference uses the locally downloaded model + matching `mmproj`.
- Memory, contacts, SMS, files, accessibility actions, Termux integration and confirmation logic use Android/local components.
- TTS/STT can be offline when an offline voice/model is installed.

## Network-required by nature
- Initial model/voice downloads.
- Web search and website interaction.
- Opening online banking/web pages.
- Any future optional cloud integration explicitly added by the product.

## Important
`INTERNET` permission is present because the app can download models and access websites. It does **not** mean ordinary chat is sent to a remote LLM.

The offline smoke test in `scripts/offline_smoke.py` checks that the repository does not contain known remote-inference SDK/API markers in application source.
