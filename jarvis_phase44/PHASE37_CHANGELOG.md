# Phase 37 — File → App Routing

- Added `open_file` Agent Tool.
- Resolves files inside the user-selected Storage Access Framework workspace.
- Uses Android MIME metadata and `ACTION_VIEW`.
- If one compatible app exists, opens it directly.
- If several compatible apps exist, delegates final selection to Android's chooser.
- Supports an explicit preferred package when the user/agent has already selected an app.
- Uses only local Android APIs; no network/API is required.
- Does not bypass app permissions or sandbox boundaries.
