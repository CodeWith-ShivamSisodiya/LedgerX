# LedgerX — Private Financial Command Center

LedgerX is a native, offline-first Android personal finance application for Indian households. Financial records are stored locally in an encrypted SQLCipher database; the application manifest requests no internet permission.

## Current implementation: six-phase release candidate

- Native Kotlin, Jetpack Compose, Material 3, Hilt, Room, Coroutines and Flow.
- Progressive privacy-first onboarding and first-account creation.
- Savings, current, cash, wallet and credit-card accounts with exact paise balances, rename/archive, transfers and reconciliation.
- Double-entry-inspired postings with a zero-sum invariant checked before and after persistence.
- Correct initial liability sign for credit-card balances.
- Dated gross/take-home income, expenses, refunds, reimbursements, split expenses, reversible corrections, tags, encrypted receipt attachment counts/removal, merchant memory, recurring confirmation, bills and subscriptions.
- Offline CSV/XLSX statement parsing with column mapping, validation preview, duplicate fingerprints, atomic rollback, and account reconciliation.
- FIFO investment lots, purchases/sales/dividends/SIPs/corporate actions, dated valuations, realized/unrealized return and XIRR; FD/RD lock-ins; EPF/EPS/PPF/NPS history and retirement scenarios; physical assets/maintenance/debt links; insurance documents; variable-rate loans and credit-card statements.
- Goals and non-inflating allocations, category/merchant/lifestyle/interest analytics, evidence-based savings findings, multi-horizon scenarios, consolidated financial/tax calendar, configurable local reminders, transparent health score history, and interactive accessible financial charts.
- Versioned Tax Year 2026-27 old/new regime comparison, tax profile, plaintext transaction export, and CA working export with explicit scope warnings.
- Database passphrase generated randomly, wrapped by an Android Keystore AES-GCM key, and never stored in plaintext.
- Sensitive-window screenshot protection (`FLAG_SECURE`).
- Optional biometric/device-credential app locking that resets whenever the activity leaves the foreground.
- Password-encrypted, device-independent backup/restore with AES-256-GCM authentication and transactional verification.
- Non-destructive Room migrations from the shipped v1 ledger through the v3 planning/security schema, with instrumented migration coverage.
- No internet permission, analytics, ads, backend, or registration.

Version `0.9.0` is intentionally marked as a release candidate until the expanded branch passes the GitHub Actions unit-test/lint/APK gate and the resulting APK is installed on an Android device. No APK path or passing build is claimed before that evidence exists.

## Build requirements

- Android Studio Ladybug or newer
- JDK 17
- Android SDK Platform 35 and Build Tools 35.x
- Gradle 8.9 (or generate a wrapper with a compatible installed Gradle)

The current machine audit found only Java 8, no Gradle installation, and no Android SDK platform/build-tools directory. Therefore an APK has not been claimed or fabricated.

After installing the requirements:

```text
gradle wrapper --gradle-version 8.9
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

The debug APK will then be produced by Gradle at `app/build/outputs/apk/debug/app-debug.apk` only after a successful build.

## GitHub Actions build

The `Android build` workflow runs on pushes and pull requests targeting `main` or `master`, version tags matching `v*`, and manual dispatches. It installs JDK 17, Gradle 8.9, Android Platform 35 and Build Tools 35.0.0, then runs:

```text
testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

Successful runs also boot an API 35 emulator, install the app, execute migration/offline/onboarding instrumentation tests, and expose an installable `LedgerX-debug-<run number>` APK plus SHA-256 artifact. Test reports are uploaded even when the test/build step fails. The workflow deliberately creates a debug-signed development APK; production release signing must use protected GitHub secrets and a separately managed release key.

See [financial calculations](docs/FINANCIAL-CALCULATIONS.md), [security](docs/SECURITY.md), [backup recovery](docs/BACKUP-RECOVERY.md), and [release instructions](docs/RELEASE.md).

## Accounting conventions

Amounts are stored as signed 64-bit integer minor units (paise for INR). A business transaction is authoritative only with at least two postings whose signed total is exactly zero. Arithmetic overflow is rejected.

- Expense: owned account decreases; expense ledger increases.
- Income: owned account increases; income ledger decreases.
- Transfer: source account decreases; destination account increases; no expense is created.
- Credit-card opening balance: liability is negative in net worth.
- Credit-card purchase: liability becomes more negative and consumption expense is recognized once.
- Credit-card settlement (transfer): cash decreases and card liability moves toward zero; no second expense.

Room foreign keys protect transaction/posting ownership. Audit rows are written alongside created accounts and transactions in the same database transaction.

## Security notes

`SecureDatabaseKeyManager` creates 256 bits of random database key material. It encrypts that material with an Android Keystore AES-GCM key and stores only ciphertext and IV in private app preferences. Android system cloud backup and device transfer are disabled until the explicit encrypted recovery workflow exists.

Release signing keys must never be committed. For production, create and protect a dedicated upload key outside the repository, configure signing through local/CI secrets, enable shrinking after validation, and retain the Play App Signing recovery records.
