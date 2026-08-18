# Phase 46 — Transaction Recovery & Safe Resume

Adds a deterministic offline state machine for interrupted banking workflows.

Important:
- An interrupted/ambiguous transaction becomes `UNKNOWN`.
- `UNKNOWN` requires user review before any continuation.
- The recovery layer never assumes a transfer succeeded.
- No payment API or network service is introduced by this phase.
