# Jarvis Offline Capability Contract

## Works without Internet after required local assets are installed
- Local LLM inference
- Local Vision inference (model + matching mmproj installed)
- Local memory/database
- Contacts/SMS access through Android framework APIs, subject to permissions
- File/workspace operations through Android storage APIs
- Agent routing and confirmation logic
- Accessibility-driven UI automation where the target app permits it
- TTS when an offline voice is installed
- On-device STT when the selected recognizer/provider has the requested offline language installed

## Requires Internet
- Initial model/voice downloads
- Web search and web browsing
- Websites and online purchases
- Google Play / third-party app-store downloads
- Any explicitly configured remote API

## Important
The app does not require a paid inference API for its local LLM/Vision core. Android framework APIs are local platform APIs, not remote inference services.
