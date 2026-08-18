# Phase 24 — Banking OTP + Result Verification Foundation

- Added local-only `BankOtpDetector` for Persian/English OTP extraction.
- Added `BankTransferResultVerifier` so a transfer is never considered successful merely because a screen/button was reached.
- Success requires an explicit success signal and a reference/transaction identifier; otherwise result remains UNKNOWN.
- OTP detection is candidate-based and does not automatically submit a code.
- No banking credentials or OTP values are persisted by these components.
