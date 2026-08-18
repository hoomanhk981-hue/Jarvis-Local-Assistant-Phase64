# Phase 62 — Safe Card Input Policy

Adds a UI-facing local policy for card-number entry.

- Separators are normalized.
- Only the expected 16 digits are accepted for validation.
- Display is masked.
- CVV2 is deliberately not represented or persisted by this component.
- No bank API or network dependency is introduced.
