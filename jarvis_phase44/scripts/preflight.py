#!/usr/bin/env python3
"""Static host/build preflight for Jarvis. Does not build the APK."""
from pathlib import Path
import os, re, shutil, subprocess, sys

root = Path(__file__).resolve().parents[1]
checks = []
def check(name, ok, detail):
    checks.append((name, ok, detail))

check('Gradle wrapper', (root/'gradlew').exists() or (root/'gradlew.bat').exists(),
      'gradlew/gradlew.bat present' if (root/'gradlew').exists() or (root/'gradlew.bat').exists() else 'missing Gradle wrapper; open in Android Studio or add wrapper')
check('Android project files', (root/'settings.gradle.kts').exists() and (root/'app/build.gradle.kts').exists(), 'settings.gradle.kts + app/build.gradle.kts')
check('CMake project', (root/'app/src/main/cpp/CMakeLists.txt').exists(), 'native CMakeLists.txt present')
check('Vision JNI', (root/'app/src/main/cpp/jarvis_vision_jni.cpp').exists(), 'JNI source present')
check('Java', shutil.which('java') is not None, shutil.which('java') or 'java not found')
check('CMake', shutil.which('cmake') is not None, shutil.which('cmake') or 'cmake not found')
check('ADB', shutil.which('adb') is not None, shutil.which('adb') or 'adb not found (needed for device validation)')

sdk = os.environ.get('ANDROID_SDK_ROOT') or os.environ.get('ANDROID_HOME')
check('Android SDK env', bool(sdk), sdk or 'ANDROID_SDK_ROOT/ANDROID_HOME not set')
if sdk:
    sdkp=Path(sdk)
    check('SDK path exists', sdkp.is_dir(), str(sdkp))
    ndk_version='29.0.13113456'
    ndk=sdkp/'ndk'/ndk_version
    check('Required NDK', ndk.is_dir(), str(ndk))

text=(root/'app/build.gradle.kts').read_text()
m=re.search(r'ndkVersion\s*=\s*"([^"]+)"', text)
check('Pinned NDK version', bool(m), m.group(1) if m else 'ndkVersion not found')
cmake=(root/'app/src/main/cpp/CMakeLists.txt').read_text()
check('Native dependency declared', 'FetchContent_Declare' in cmake and 'llama_cpp' in cmake, 'llama.cpp FetchContent declaration present')

failed=sum(not ok for _,ok,_ in checks)
for name,ok,detail in checks:
    print(f"[{'OK' if ok else 'MISSING'}] {name}: {detail}")
print(f"\nPreflight: {len(checks)-failed}/{len(checks)} checks satisfied")
sys.exit(1 if failed else 0)
