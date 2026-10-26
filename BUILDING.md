# Build Tuhkar

This is a small native Java Android project using Android's command-line tools—no Node.js, web runtime, Gradle plugin or downloaded app dependencies.

## Tools

Use Linux/macOS (or WSL), JDK 11 or newer, Python 3, `zip`, OpenSSL and the Android SDK command-line tools. The release in this repository was host-tested with JDK 11; CI uses JDK 17.

Install the SDK components and accept their licenses:

```bash
sdkmanager "platforms;android-35" "build-tools;35.0.0"
export ANDROID_HOME=/path/to/your/android-sdk
```

## Local development

```bash
bash build.sh debug
bash test.sh
```

The debug build creates a local development key in ignored `.signing/`. Output: `output/Tuhkar-v1.1.0-debug.apk`. **This is not the official release signature**, and cannot replace an officially signed app. Use a test device/profile; uninstalling an existing build can erase app settings/favorites (not media).

`bash build.sh unsigned` produces an aligned unsigned APK for independent signing. Unsigned APKs are not directly installable.

Tests cover pure logic and actual Java actions/views with deterministic Android doubles; they do not establish real GPU appearance, native audio/Visualizer/EQ support, OS prompt/lifecycle dispatch, power or frame-time behavior.

## Official release signing — KnightWin only

Keep the owner-only signing kit **outside this repository**. Never upload it to GitHub, Actions artifacts or a release. Its README identifies the two encrypted keystores and private password files.

```bash
export RELEASE_KEYSTORE=/private/path/knightwin-release.p12
export RELEASE_PASSWORD_FILE=/private/path/release-password.txt
export LEGACY_KEYSTORE=/private/path/legacy-fallback.p12
export LEGACY_PASSWORD_FILE=/private/path/legacy-password.txt
bash build.sh release
```

Output: `output/Tuhkar-v1.1.0.apk`. The script uses the checked-in **public** `signing/upgrade-lineage.bin`, rotates to the private KnightWin certificate on Android 9+, and keeps the previous certificate for Android 8 compatibility. Do not replace/recreate those keys or lineage for normal updates. Do not use a public debug key as a release key.

The release build rejects debuggable APK signing and fails if private inputs are missing; it does not silently fall back to debug signing.

## Versions & creator

`app.properties` records version **1.1.0**, creator **KnightWin**, **GPL-3.0-only** and application ID **com.luma.media**. The manifest uses **versionName 1.1.0 / versionCode 49**. Code 49 is intentionally higher than the internal preview builds (up to 48), so the final release installs as an update even though its public version label is normalized to 1.1.0. Android uses versionCode for update ordering.

Future releases must increase versionCode and update the visible version and HTTP User-Agent strings consistently. `tests/ReleaseMetadataTest.py` checks this release's identity. Keep the same application ID, SharedPreferences namespace and signing history for compatible upgrades. Do not remove one-time migration code merely because it refers to old preference names.

Original application code is GPL-3.0-only. Font licenses are included separately. A signed binary distribution must be accompanied by compliant corresponding source; tagging and publishing this complete source alongside the APK is the intended workflow.
