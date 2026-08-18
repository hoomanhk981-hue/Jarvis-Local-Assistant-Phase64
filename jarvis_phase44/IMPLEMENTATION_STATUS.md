Phase 29 complete: automated unit-test coverage and repository QA checks were added, and debug signing was corrected to use the AGP-managed debug keystore. Dependency-free QA passes in this environment; Android compilation/device tests require the pinned CI environment.

## Phase 30
Offline voice pack management was added. TTS uses Android's official install-data flow; Speech-to-Text opens the system voice-input/language management UI because Android does not expose a universal public third-party speech-model installation API.

## Phase 31
Offline capability boundaries were documented and audited. Core local inference/tools remain independent of paid remote inference APIs; network-only operations are explicitly identified.
