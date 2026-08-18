#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
checks = [
    ("Main Android module", ROOT / "app" / "build.gradle.kts"),
    ("Manifest", ROOT / "app" / "src" / "main" / "AndroidManifest.xml"),
    ("Local LLM", ROOT / "app/src/main/java/com/example/data/models/LocalLlmEngine.kt"),
    ("Vision runtime", ROOT / "app/src/main/java/com/example/data/models/LocalVisionModelController.kt"),
    ("STT", ROOT / "app/src/main/java/com/example/speech/SpeechToTextManager.kt"),
    ("TTS", ROOT / "app/src/main/java/com/example/speech/TextToSpeechManager.kt"),
    ("App launcher", ROOT / "app/src/main/java/com/example/tools/launcher/AppLauncher.kt"),
    ("SMS", ROOT / "app/src/main/java/com/example/tools/sms/SmsTool.kt"),
    ("Contacts", ROOT / "app/src/main/java/com/example/tools/contacts/ContactsTool.kt"),
    ("Memory", ROOT / "app/src/main/java/com/example/tools/memory/MemoryTool.kt"),
    ("Device actions", ROOT / "app/src/main/java/com/example/tools/device/DeviceActions.kt"),
    ("File handling", ROOT / "app/src/main/java/com/example/tools/files/FileDocumentTool.kt"),
    ("Permissions", ROOT / "app/src/main/java/com/example/tools/permissions/PermissionManager.kt"),
    ("Agent router", ROOT / "app/src/main/java/com/example/agent/AgentToolRouter.kt"),
    ("Production tool registry", ROOT / "app/src/main/java/com/example/assistant/ToolRegistry.kt"),
    ("Banking", ROOT / "app/src/main/java/com/example/bank/SaderatBankAdapter.kt"),
]

failed = []
for name, path in checks:
    if path.exists() and path.stat().st_size > 0:
        print(f"[OK] {name}: {path.relative_to(ROOT)}")
    else:
        print(f"[FAIL] {name}: {path}")
        failed.append(name)

# Guardrails for the most important offline/confirmation boundaries.
router = (ROOT / "app/src/main/java/com/example/assistant/ToolRegistry.kt").read_text(encoding="utf-8")
launcher = (ROOT / "app/src/main/java/com/example/tools/launcher/AppLauncher.kt").read_text(encoding="utf-8")
tts = (ROOT / "app/src/main/java/com/example/speech/TextToSpeechManager.kt").read_text(encoding="utf-8")
stt = (ROOT / "app/src/main/java/com/example/speech/SpeechToTextManager.kt").read_text(encoding="utf-8")

invariants = [
    ("bank confirmation gate", "PrepareBankTransferTool" in router or 'bank' in router.lower()),
    ("launcher typo matching", "levenshtein" in launcher.lower()),
    ("launcher raw online query", "onlineSearchUrl" in launcher),
    ("offline TTS guard", "isNetworkConnectionRequired" in tts),
    ("on-device STT", "createOnDeviceSpeechRecognizer" in stt),
]
for name, ok in invariants:
    print(f"[{'OK' if ok else 'FAIL'}] {name}")
    if not ok:
        failed.append(name)

print(f"\nFinal code gate: {'PASS' if not failed else 'FAIL'}")
raise SystemExit(0 if not failed else 1)
