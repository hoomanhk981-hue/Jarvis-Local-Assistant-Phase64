# Speech-to-Text

Implemented in this phase:

- Offline STT model lifecycle.
- Local model readiness validation.
- Serialized model load/unload/transcription.
- 16-bit PCM, mono, 16 kHz runtime contract.
- Persian language selection (`fa`).
- No speech API is required by the controller.
- Model remains local after installation.

Important boundary:
The Android microphone/audio-capture UI and a concrete native STT backend/model
still need to be bound and tested on a real Android device. The controller
cannot honestly claim 100% end-to-end Persian transcription until a concrete
offline model/backend is selected and a real recording is transcribed on-device.

Current status: 80%.
