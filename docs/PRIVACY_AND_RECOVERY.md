# Privacy, backup and recovery design

## Implemented

- SQLCipher database with randomly generated key material.
- Android Keystore AES-GCM wrapping key.
- Wrapped key ciphertext and IV only in private preferences.
- Screenshot protection on the main activity.
- Android automatic backup and device transfer disabled.
- No network permission or telemetry dependency.

## Required before backup is enabled

Portable backups cannot rely on an Android Keystore key because that key is intentionally non-exportable and device-bound. The planned format therefore uses a new random backup key, authenticated encryption, a versioned manifest, database checksum, KDF parameters, and either a user password or separately displayed recovery key. Restore must authenticate and validate into a temporary database before atomically replacing live data. Wrong passwords, corrupted archives, schema incompatibility, low storage and interrupted restore all require tests.

Until that workflow is implemented and verified, LedgerX intentionally does not advertise a working backup feature.

