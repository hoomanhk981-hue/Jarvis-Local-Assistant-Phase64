# Phase 61 — Transfer Amount Validation

Adds a local validation layer for transfer amounts.

- Accepts Persian/Arabic numerals and normalizes them.
- Removes common grouping separators.
- Uses integer amounts to avoid floating-point rounding.
- Supports minimum/maximum boundaries supplied by the banking flow.
- Does not contact a bank or execute a transfer.
