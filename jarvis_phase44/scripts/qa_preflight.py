#!/usr/bin/env python3
from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
errors=[]
for p in ROOT.rglob('*'):
    if not p.is_file() or '.git' in p.parts or 'build' in p.parts: continue
    if p.stat().st_size > 25*1024*1024: errors.append(f'large source artifact: {p.relative_to(ROOT)}')
    if p.name.lower() in {'debug.keystore','my-upload-key.jks'}: errors.append(f'keystore committed: {p.relative_to(ROOT)}')
build=(ROOT/'app/build.gradle.kts').read_text()
if 'debug.keystore' in build: errors.append('custom debug.keystore reference remains')
if not (ROOT/'.github/workflows/android-build.yml').exists(): errors.append('CI workflow missing')
if errors:
 print('QA PREFLIGHT: FAIL'); print('\n'.join('- '+e for e in errors)); sys.exit(1)
print('QA PREFLIGHT: PASS')
