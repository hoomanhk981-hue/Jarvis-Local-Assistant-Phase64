# App Launcher — Implementation Status

Implemented:
- Installed-app discovery.
- Exact, prefix, substring and package-name matching.
- Persian character normalization.
- Typo tolerance with Levenshtein distance.
- Ranked candidate results.
- Confidence-aware local launch planning.
- Explicit online-resolution decision boundary.
- The raw user query is preserved for online search.
- Offline mode never requires network access.
- App launch through Android package manager.

Important architecture:
The launcher itself does NOT silently perform web access. When local matching
is ambiguous and internet is available, it returns NEEDS_ONLINE_RESOLUTION so
the Agent/Web Search layer can search the exact raw user query, interpret the
result, map it back to an installed package, and then call open().

Current status: 100% of the App Launcher tool layer.
Real-device and End-to-End verification remain required before release.
