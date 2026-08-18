# Phase 9 — File / Code Agent

Implemented real user-selected workspace support using Android Storage Access Framework (SAF).

## Added
- Persistent `ACTION_OPEN_DOCUMENT_TREE` workspace selection.
- Real read/write access to the selected tree URI using `DocumentsContract`.
- Recursive workspace indexing with bounded depth/item count.
- Agent tools:
  - `list_workspace`
  - `read_file`
  - `write_file` (confirmation required)
  - `zip_workspace` (confirmation required)
- Real ZIP creation written back into the selected workspace.
- Code screen now exposes a real workspace picker.
- Path traversal (`..`) is rejected.

The app deliberately does not request broad storage access for this feature. Android's Storage Access Framework gives the app access only to the directory the user selected and supports persistable URI permissions.
