# Phase 7 — Confirmation & Permission Engine

## Implemented
- Central `ConfirmationManager` with single-use UUID requests and 2-minute expiry.
- Risk classification: LOW / MEDIUM / HIGH / CRITICAL.
- Central `ToolRegistry.execute()` boundary now checks Android runtime permissions before tool execution.
- Runtime permission requests for contacts, SMS, calls and SMS sending are surfaced to the Android UI instead of bypassed.
- Approved tool requests are resumed with the original immutable argument snapshot.
- Rejected/expired requests cannot be replayed through the same confirmation token.
- Generic Compose confirmation dialog added for non-bank tool actions.
- Existing bank confirmation dialog remains separate; banking is not marked as implemented.
- Emergency stop clears pending confirmations and permission requests.

## Security design
- No Android permission is granted programmatically.
- No confirmation is silently accepted by the model.
- Tool arguments are preserved at approval time; later model output cannot mutate the pending request.
- Financial tools are classified CRITICAL even though real banking execution is not yet implemented.

## Not implemented yet
- Biometric authorization for CRITICAL actions.
- Real SMS sending/calling tools.
- Real bank execution.
- Background permission requests without an Activity.
