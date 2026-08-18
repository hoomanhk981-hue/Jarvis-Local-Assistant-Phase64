# Jarvis Phase 2 — Real Tool Boundary + Accessibility Bridge

## Implemented
- Added a central `ToolRegistry` and typed `JarvisTool` / `ToolResult` contracts.
- Added `open_app` tool with real Android package launching.
- Added `run_termux` tool with the existing Termux RUN_COMMAND bridge and a confirmation gate for dangerous commands.
- Added a user-enabled `AccessibilityService`.
- Accessibility service can inspect the active accessibility tree, click an exact text/content-description node, set text in an editable field, and perform Back.
- Added `AssistantViewModel.executeTool()` so UI/agent layers have one explicit tool boundary.
- Added Android accessibility-service declaration and configuration.
- Fixed a duplicate local variable that prevented compilation in `loadCodeFilesFromDisk()`.

## Important limitation
This phase does NOT claim autonomous screen reasoning or banking automation. The accessibility bridge is an explicit primitive. A later agent layer must decide which tool to call, and sensitive actions must remain confirmation-gated.

## Android policy note
Android documents AccessibilityService as a specialized accessibility mechanism. Users must explicitly enable the service; distribution through app stores may impose additional policy requirements.
