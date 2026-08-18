# Phase 6 — SMS + Contacts + Memory Agent Tools

Implemented real local Agent tools:

- `search_contacts`: reads Android Contacts Provider with READ_CONTACTS, normalizes Persian/Latin variants and phone numbers, ranks up to 3 close matches.
- `search_sms`: reads real `Telephony.Sms` messages with READ_SMS, filters by query/sender/category, includes OTP/TICKET/BANK categories and lightweight fuzzy token matching.
- `search_memory`: queries the existing Room private-memory database locally.
- `remember`: writes a user-provided fact to Room only when the tool receives explicit `confirmed=true`; otherwise it returns a confirmation request.

No network access is used by these tools. The tools are registered in the existing ToolRegistry and are therefore available to the local agent loop.

Important Android constraint: SMS access is permission/role sensitive. The app must have READ_SMS at runtime, and Android may apply additional restrictions depending on the device/role configuration. This phase does not bypass Android permissions.
