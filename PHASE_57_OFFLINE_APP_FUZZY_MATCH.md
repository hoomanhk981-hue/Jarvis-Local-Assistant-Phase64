# Phase 57 — Offline Fuzzy App Matching

Adds a local fuzzy matcher on top of app-name normalization.

It:
- ranks installed apps by edit distance,
- tolerates small spelling mistakes,
- works without internet,
- does not invent app names,
- leaves the final confidence/confirmation decision to the launcher layer.

The web-search fallback remains a separate optional path for online mode.
