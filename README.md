# LedgerX — Private Financial Command Center

LedgerX is a native, offline-first Android personal finance application for Indian households. Financial records are stored locally in an encrypted SQLCipher database; the application manifest requests no internet permission.

## Current implementation: Phase 1 foundation

- Native Kotlin, Jetpack Compose, Material 3, Hilt, Room, Coroutines and Flow.
- Progressive privacy-first onboarding and first-account creation.
- Savings, cash and credit-card accounts with exact paise balances.
- Double-entry-inspired postings with a zero-sum invariant checked before and after persistence.
- Correct initial liability sign for credit-card balances.
- Confirmed income and expense entry from the floating quick-entry action.
- Dashboard, account balances, recent activity, evidence-limited insights, and honest health-score incompleteness.
- Database passphrase generated randomly, wrapped by an Android Keystore AES-GCM key, and never stored in plaintext.
- Sensitive-window screenshot protection (`FLAG_SECURE`).
- No internet permission, analytics, ads, backend, or registration.

This is an actively developed Phase 1 slice, not the completed multi-phase product described in the master brief. Transfers, editing/reversal, biometric unlock, backup/restore, imports, reconciliation, investments, loans, insurance, goals, forecasting, tax, calendar, and production hardening remain tracked work.

## Build requirements

- Android Studio Ladybug or newer
- JDK 17
- Android SDK Platform 35 and Build Tools 35.x
- Gradle 8.9 (or generate a wrapper with a compatible installed Gradle)

The current machine audit found only Java 8, no Gradle installation, and no Android SDK platform/build-tools directory. Therefore an APK has not been claimed or fabricated.

After installing the requirements:

```text
gradle wrapper --gradle-version 8.9
./gradlew testDebugUnitTest assembleDebug
```

The debug APK will then be produced by Gradle at `app/build/outputs/apk/debug/app-debug.apk` only after a successful build.

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

