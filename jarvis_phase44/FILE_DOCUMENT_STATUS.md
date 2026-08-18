# File / Document Handling — Implementation Status

Implemented:
- Android Storage Access Framework document picker.
- MIME-type aware file opening.
- Opening files through ACTION_VIEW with read grants.
- Persistable read permission for user-selected documents.
- Single- and multi-MIME document selection.
- No unrestricted filesystem access is assumed.

Current status: 90%.
Remaining work is connecting file intents to the Agent/Tool Router and testing
common file types on the target Android device.
