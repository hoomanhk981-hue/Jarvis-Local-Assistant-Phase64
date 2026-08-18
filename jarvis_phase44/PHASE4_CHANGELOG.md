# Phase 4 — Real image input + local vision preprocessing

## Implemented
- Gallery image picker using Android Activity Result API.
- Camera capture using Android `TakePicture` contract and FileProvider.
- Real local OCR using ML Kit Text Recognition.
- Vision screen now displays the selected/captured image instead of always showing a hard-coded demo image.
- Removed the previous hard-coded fake Qwen 2-VL response.
- Added an explicit boundary between deterministic local OCR and the separately downloadable multimodal vision model.
- The app never claims that OCR is a multimodal LLM response.

## Not falsely claimed
The downloadable multimodal model is still a separate model slot. A compatible multimodal inference runtime must be wired to it before the app can truthfully advertise full image understanding. Phase 4 therefore provides real image acquisition and a real local OCR fallback rather than fabricating visual reasoning.

## Reference
Android CameraX/Camera APIs support real image capture and image analysis; this phase uses the platform Activity Result camera contract for the simplest robust capture path. See Android's CameraX documentation for future direct camera preview/analysis integration.
