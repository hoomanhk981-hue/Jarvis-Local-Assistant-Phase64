# Phase 36 — Safe App Resolution

- Replaced naive package/label substring launch logic with deterministic ranking.
- Normalizes Persian/Arabic text and supports fuzzy matching and known aliases.
- Exact/high-confidence matches can launch automatically.
- Ambiguous or weak matches return the top candidates instead of opening a potentially wrong app.
- Internet availability is detected without a cloud/API dependency.
- When internet is available, a normal Google search URL is exposed for web-assisted resolution; this uses the user's browser rather than a paid search API.
- Offline mode remains fully usable for installed-app resolution.
- No external search API key is required.
