# Phase 8 — Calls + SMS Tools

- Added `make_call` tool using Android `ACTION_CALL` with explicit confirmation and `CALL_PHONE` runtime permission.
- Added `send_sms` tool using `SmsManager` with explicit confirmation and `SEND_SMS` runtime permission.
- Contact-name calls resolve against the local Contacts Provider; ambiguous matches are returned instead of guessing.
- Confirmation approval now replays the tool with `confirmed=true`, preventing the confirmation loop that existed in the previous architecture.
- Dangerous Termux commands now honor the same confirmed replay flag.
- No automatic sending/calling happens without the confirmation gate.

Android references: ACTION_CALL/CALL_PHONE and SmsManager/SEND_SMS are platform APIs; actual availability and permissions remain device/OS dependent.
