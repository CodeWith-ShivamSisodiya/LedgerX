# LedgerX security and privacy model

LedgerX requests no internet permission and includes no advertising, analytics, account, or telemetry SDK. Android system backup and device transfer are disabled. Sensitive windows use `FLAG_SECURE`.

## Live database

Room uses SQLCipher. `SecureDatabaseKeyManager` creates 256 random bits, wraps them with a non-exportable Android Keystore AES-GCM key, and stores only the IV and ciphertext in app-private preferences. The key is never logged or embedded in the APK. Device credential/strong biometric locking is optional and the in-memory session is invalidated whenever the activity stops.

## Portable backup

Portable `.lxb` files are logical, device-independent backups. The user password is processed with PBKDF2-HMAC-SHA256 (210,000 iterations, random 128-bit salt) to produce an AES-256 key. Data is authenticated and encrypted with AES-GCM using a random 96-bit IV. The header is versioned; GCM authentication, format, required-table, and record-count checks occur before a restore transaction changes live data. Restore is all-or-nothing. Receipt images/PDFs are size-limited and stored as encrypted database blobs, so they receive the same live-database and portable-backup protection instead of leaking through a shared filesystem.

Passwords must be at least 12 characters. LedgerX cannot recover a forgotten password. Keep the backup and password separately. Password character buffers used by the ViewModel are overwritten after each operation.

## Threat boundaries

LedgerX protects data at rest and prevents routine screenshots. It cannot protect a compromised/rooted OS, malicious accessibility service, unlocked-device physical access while app lock is disabled, or a password disclosed outside the app. Exported CSV is intentionally plaintext and should be handled as sensitive data.

## Review checklist

- Keep `INTERNET` absent from the manifest for the offline edition.
- Never log transaction, account, tax, backup, or key material.
- Re-run unit tests, lint, migration compilation, and APK build for every release.
- Manage production signing outside the repository; use Play App Signing or an equivalently protected HSM-backed process.
- Re-verify stored tax rules against official sources before changing their verification date or version.
