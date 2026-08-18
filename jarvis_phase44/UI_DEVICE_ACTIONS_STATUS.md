# UI / Device Actions — Implementation Status

Implemented:
- Open an installed Android application by package name.
- Open a local/content URI with the appropriate Android application.
- Open Android system Settings.
- Open the settings page for a specific application.
- Explicit intent/result/error handling.

The tool intentionally does not provide arbitrary shell execution, root
operations, security bypasses, or hidden interaction with protected screens.

Current status: 90%.
Remaining work is wiring the Agent/Tool Router to these actions and testing
the exact Android device flows.
