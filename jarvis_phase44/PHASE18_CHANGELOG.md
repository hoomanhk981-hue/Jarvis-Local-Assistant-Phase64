# Phase 18 — Hardened Assistant Tool Routing

- Added a single confirmation boundary in `ToolRegistry` for state-changing, external-communication, secret, code-execution and sensitive UI actions.
- Added single-use confirmation IDs and an approval execution path.
- Preserved Android permission checks before tool execution.
- Added confirmation summaries and risk levels.
- Hardened the local agent planner against fabricated tool names, arguments, success claims and self-issued `confirmed=true`.
- Kept low-risk read-only tools directly executable so normal search/memory/browser inspection is not unnecessarily blocked.
- Termux confirmation is restricted to dangerous commands rather than every harmless command.
