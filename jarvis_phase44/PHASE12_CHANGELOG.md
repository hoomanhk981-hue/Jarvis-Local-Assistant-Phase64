# Phase 12 — Memory + Personal JSON + Encrypted Password Vault

Implemented on top of Phase 11.

## Added
- Android Keystore-backed AES-GCM encryption for saved passwords.
- `save_password` Agent tool with explicit confirmation.
- `get_password` Agent tool with explicit confirmation; plaintext is returned only to the confirmed tool call.
- `export_memory_json` Agent tool for user-selected workspace export.
- Memory export deliberately excludes password/OTP/CVV2 secrets.
- Registered the new tools in `ToolRegistry`.

## Security changes
- Removed deterministic package-name-derived AES key.
- Removed plaintext fallback when encryption/decryption fails.
- Sensitive values remain out of normal audit strings.

## Design note
The personal JSON export is intentionally a portable memory document, not a plaintext password backup. Device-bound secrets stay in Android Keystore + Room. This follows Android's separation between private app data/databases and user-selected shared documents.
