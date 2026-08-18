# Phase 44 — Transaction Confirmation Binding

Adds a real confirmation gate for sensitive bank-transfer workflows.

## What changed
- Binds confirmation to an exact canonical `TransferRequest` fingerprint.
- Confirmation expires after 120 seconds by default.
- A confirmation cannot be reused twice.
- Any change to destination, amount, source card, expiry, CVV2, or recipient invalidates the confirmation.
- Uses SHA-256 only as an integrity fingerprint; it is not used as a secret store.
- Does not submit money to a bank and does not bypass bank security controls.

This layer is intended to sit between the UI confirmation and any future device-side accessibility execution.
