# Phase 34 — Saderat card-to-card UI contract

- Added a semantic, version-tolerant UI contract for the Saderat transfer flow.
- Models: Transfer -> Source -> Destination -> Amount -> Payment -> OTP.
- Uses accessibility-discoverable labels/aliases rather than guessed private resource IDs.
- Payment and OTP steps are explicitly sensitive and must remain behind the central confirmation gate.
- Added unit tests for ordering and sensitivity.

This phase does not claim a live-money transfer. Final automation requires a real device, the installed bank app, granted Accessibility permission, and a current UI-node snapshot.
