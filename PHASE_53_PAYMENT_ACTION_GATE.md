# Phase 53 — Payment Action Capability Gate

Adds a final deterministic capability gate between the Agent and a payment action.

The action is denied unless:
- the user explicitly approved it,
- the approval is still valid,
- the transaction data is unchanged,
- and no raw sensitive secret is being passed through the Agent layer.

This is a safety boundary, not a banking/payment API. It does not move money.
