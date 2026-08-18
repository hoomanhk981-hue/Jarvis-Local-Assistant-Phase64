# Jarvis build and validation

## 1. Open the project
Open the repository root in Android Studio with a compatible JDK and Android SDK.
The Android project lives in `jarvis_phase44/` (Gradle root with `settings.gradle.kts`).

## 2. Run preflight

```bash
python3 scripts/preflight.py
```

A failed preflight is not an application failure; it means the host is missing a build/device prerequisite.

## 3. Debug build

```bash
./gradlew :app:assembleDebug
```

The debug build is automatically signed with a debug key and is installable
directly on a phone (enable "install from unknown sources").

## 4. Release build

The release build expects these environment variables:

- `KEYSTORE_PATH` (defaults to `<project>/my-upload-key.jks`)
- `STORE_PASSWORD`
- `KEY_PASSWORD`

The release key alias is `upload`.

In CI (GitHub Actions) the keystore is provided automatically through the
repository secrets `KEYSTORE_BASE64`, `STORE_PASSWORD` and `KEY_PASSWORD`.
If these secrets are missing, the workflow falls back to a debug APK.

Never commit a keystore or plaintext release credentials.

## 5. Native/Vision dependency

The native Vision runtime fetches llama.cpp through CMake (pinned to a fixed
commit in `app/src/main/cpp/CMakeLists.txt`) and builds `libmtmd`. A clean
build therefore needs network access unless llama.cpp is already available to
the build environment. Model weights and the `mmproj` file remain external
downloads and are not packaged into the APK.

## 6. Building the APK in CI (GitHub Actions)

The workflow `.github/workflows/android-build.yml`:

1. Runs on every push to `main`/`master`, on pull requests, or manually via
   "Actions → Android Build & APK Generator → Run workflow".
2. Installs the pinned Android SDK / NDK / CMake toolchain.
3. Builds debug + release APKs (release only if the keystore secret exists).
4. Uploads the APK(s) as a build artifact.
5. Publishes them to the GitHub Release `v1.0.0` (rolling — always the newest APK).

### Installing the APK on a phone

- Download the APK from the **GitHub Release** page of the repository
  (stable link: `https://github.com/<owner>/Jarvis-Local-Assistant-Phase64/releases/tag/v1.0.0`)
  or from the workflow **Artifacts**.
- The APK is built for **arm64-v8a** (64-bit) and requires **Android 7.0 (API 24)+**.
- On the phone: allow "Install unknown apps" for your browser/download manager,
  then open the APK file and confirm installation.
- Because the release APK is signed with a stable key, installing a newer
  build over an older one works as a normal update (no uninstall needed).

## 7. Device validation

After installing the debug APK, validate permissions, Default Assistant selection, Termux integration, local model loading, Vision inference, and banking confirmation flows on a physical device. The repository does not claim those device tests were executed in this environment.
