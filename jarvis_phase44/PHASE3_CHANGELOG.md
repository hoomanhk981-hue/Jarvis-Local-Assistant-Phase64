# Phase 3 — Local LLM Tool Agent

## Implemented
- Added `LocalAgentEngine` for a bounded local tool loop.
- Local model emits a strict JSON tool request or `none`.
- Tool calls are executed only through `ToolRegistry`.
- Tool results are fed back into the local model for the final response.
- Added fuzzy app-name resolution to `open_app`; package names are still accepted.
- Agent is only attempted for action-like requests; normal conversation remains conversational.
- Agent stops at three tool steps and never reports success without a successful tool result.

## Current tools
- `open_app`
- `run_termux`
- `accessibility_action` (`inspect`, `click_text`, `set_text`, `back`)

## Explicit limitations
- This phase does not implement banking automation.
- This phase does not implement browser automation.
- Vision inference is still a separate phase.
- Accessibility remains user-enabled and is not silently enabled by the app.
