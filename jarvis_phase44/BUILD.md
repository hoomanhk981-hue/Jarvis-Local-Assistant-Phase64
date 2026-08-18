# Jarvis build and validation

## 1. Open the project
Open the repository root in Android Studio with a compatible JDK and Android SDK.

## 2. Run preflight

```bash
python3 scripts/preflight.py
```

A failed preflight is not an application failure; it means the host is missing a build/device prerequisite.

## 3. Debug build

```bash
./gradlew :app:assembleDebug
```

The debug build uses the repository's debug signing configuration.

## 4. Release build

The release build expects these environment variables:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_PASSWORD`

The release key alias is `upload`.

Never commit a keystore or plaintext release credentials.

## 5. Native/Vision dependency

The native Vision runtime fetches llama.cpp through CMake and builds `libmtmd`. A clean build therefore needs network access unless llama.cpp is already available to the build environment. Model weights and the `mmproj` file remain external downloads and are not packaged into the APK.

## 6. Device validation

After installing the debug APK, validate permissions, Default Assistant selection, Termux integration, local model loading, Vision inference, and banking confirmation flows on a physical device. The repository does not claim those device tests were executed in this environment.
