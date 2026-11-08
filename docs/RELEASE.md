# Build and release

Requirements: JDK 17, Android SDK Platform 35, Build Tools 35.0.0, and Gradle 8.9.

```text
gradle --no-daemon testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

The debug-signed installable APK is `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions executes the same gate, boots an API 35 emulator for connected migration/offline/onboarding tests, and uploads the APK, SHA-256 checksum, tests, and lint report.

For production, create a dedicated upload key outside the repository, inject signing values through protected CI secrets, restrict the release environment, enable Play App Signing, retain recovery records, and add a signed-release verification job (`apksigner verify --verbose`). Never commit a keystore or password.
