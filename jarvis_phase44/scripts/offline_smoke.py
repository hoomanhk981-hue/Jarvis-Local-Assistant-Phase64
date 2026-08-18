#!/usr/bin/env python3
"""Static offline-boundary smoke test. It does not require Android SDK or Gradle."""
from pathlib import Path
import re, sys

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app" / "src"

# These are remote inference/provider markers, not ordinary URLs used for model
# downloads or opening websites. Keep the list conservative to avoid false positives.
PATTERNS = [
    r"com\.google\.ai\.client",
    r"generativelanguage\.googleapis\.com",
    r"api\.openai\.com",
    r"api\.anthropic\.com",
    r"api\.cohere\.ai",
    r"huggingface_hub\.InferenceClient",
]

blob = "\n".join(
    p.read_text(errors="ignore")
    for p in SRC.rglob("*") if p.is_file() and p.suffix in {".kt", ".java", ".cpp", ".h", ".xml"}
)
failed = []
for pat in PATTERNS:
    if re.search(pat, blob, re.IGNORECASE):
        failed.append(pat)

if failed:
    print("OFFLINE SMOKE: FAIL")
    print("Remote inference markers found:")
    for item in failed:
        print(f"- {item}")
    sys.exit(1)

print("OFFLINE SMOKE: PASS")
print("No known remote-inference SDK/API markers found in app source.")
print("Network access remains available for downloads and web features.")
