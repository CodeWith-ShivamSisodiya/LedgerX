# Changelog

## 0.1.0 — Phase 1 foundation

- Initialized native Android project and dependency catalog.
- Added premium AMOLED/light Compose design tokens and five-tab application shell.
- Added privacy onboarding, account creation, dashboard, activity, wealth, insights and health views.
- Added exact minor-unit money type, balanced ledger postings, account balance projections and audit log.
- Added encrypted Room database key management with Android Keystore wrapping.
- Added quick confirmed income/expense entry and honest empty/incomplete-data states.
- Added core money, overflow, balancing and savings-rate unit tests.
- Documented toolchain limits, accounting rules, security, recovery and phased delivery plan.
- Added GitHub Actions CI for unit testing, debug APK creation and artifact retention.
- Updated Android SDK setup to `setup-android@v4` without the removed legacy `tools` package.
- Added the LedgerX logo as the adaptive launcher, round launcher, and onboarding brand asset.
- Fixed Kotlin compilation by replacing the reserved-word package prefix `in.ledgerx.app` with `com.ledgerx.app` and enabled Room schema export.
