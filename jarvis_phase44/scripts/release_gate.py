#!/usr/bin/env python3
from pathlib import Path
import re, sys

root = Path(__file__).resolve().parents[1]
checks = []
checks.append(("Gradle wrapper", (root / "gradlew").exists() or (root / "gradlew.bat").exists()))
checks.append(("settings.gradle.kts", (root / "settings.gradle.kts").exists()))
checks.append(("app module", (root / "app" / "build.gradle.kts").exists()))
checks.append(("native CMake", (root / "app" / "src" / "main" / "cpp" / "CMakeLists.txt").exists()))
checks.append(("release gate document", (root / "RELEASE_GATE.md").exists()))

for name, ok in checks:
    print(f"[{'PASS' if ok else 'FAIL'}] {name}")

failed = [name for name, ok in checks if not ok]
print(f"Release gate: {len(checks)-len(failed)}/{len(checks)} repository checks passed")
if failed:
    print("Remaining repository blockers:")
    for name in failed:
        print(f" - {name}")
    sys.exit(1)
