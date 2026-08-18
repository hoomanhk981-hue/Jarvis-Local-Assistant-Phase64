# Phase 63 — Transfer Parties Validation

Local validation for transfer parties:
- source and destination card identifiers must be structurally valid;
- source and destination cards cannot be identical;
- destination Sheba can be checked with the existing local IBAN validator;
- no bank lookup, API, credential storage, or transfer execution is performed.
