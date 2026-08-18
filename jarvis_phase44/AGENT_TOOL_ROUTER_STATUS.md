# Agent / Tool Router — Implementation Status

Implemented:
- Central ToolId registry.
- Deterministic first-pass intent routing.
- Routing for local LLM, Vision, SMS, Contacts, App Launcher,
  File/Document, Memory, Web Search and Banking.
- Image input routes to Vision.
- Internet is selected only when explicitly requested.
- Banking is marked as confirmation-required.
- General requests default to the local LLM.

Important boundary:
This is the deterministic routing layer. Final natural-language intent
resolution, multilingual/fuzzy understanding, and execution-result feedback
still need to be wired to the local LLM/Agent loop and tested on Android.

Current status: 85%.
