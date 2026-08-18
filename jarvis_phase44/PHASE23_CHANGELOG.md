# Phase 23 — Banking Transaction State Machine

Implemented a strict state machine around the existing banking workflow.

## Changes
- Added `BankTransactionStateMachine` with explicit states:
  DRAFT, VALIDATED, AWAITING_CONFIRMATION, CONFIRMED, TEST_EXECUTED, FAILED, CANCELLED.
- Validation and confirmation are now represented as distinct states.
- Cancellation cannot silently turn into confirmation.
- The state machine intentionally does **not** submit a real financial transaction.
- Existing `SaderatTestAdapter` remains the only execution path suitable for automated tests.

## Security boundary
The production adapter is still an app-launch/UI-automation adapter, not a private banking API. No attempt is made to bypass the bank application's authentication, OTP controls, or security mechanisms.
