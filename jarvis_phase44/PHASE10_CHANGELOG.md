# Phase 10 — Real Termux execution results

- Replaced fire-and-forget `sendBroadcast()` behavior with the documented Termux `RUN_COMMAND` service flow.
- Added a one-shot mutable `PendingIntent` callback and `TermuxResultService`.
- Added execution correlation IDs and an in-process result broker.
- `run_termux` now waits for and returns real stdout, stderr and exit code.
- Background execution is used so stdout/stderr are returned separately.
- Added timeout handling and explicit failure reporting.
- Added Termux setup requirements to the implementation: Termux >= 0.109, RUN_COMMAND permission, and `allow-external-apps=true`.
- Kept destructive-command confirmation in place.
- Manifest now registers the non-exported result service.

Reference: Termux RUN_COMMAND documentation, May 30 2026.
