# Phase 56 — App Name Normalization

Small-but-important launcher hardening.

The launcher now has a deterministic normalization layer for:
- Persian/Arabic character variants
- extra whitespace
- punctuation differences
- case differences

This reduces cases where a spoken or typed app name is interpreted as an unrelated application. It is fully offline and adds no API dependency.
