# Changelog

## 0.9.0 — Six-phase release candidate

- Added complete daily-money flows: dated/refund/split/reversal entry, transfers, recurring execution, bills, search/filtering, CSV/XLSX import, deduplication, preview, rollback, and reconciliation.
- Added funded investments and deposits, dated valuations, XIRR, fixed income, retirement history, physical assets, debt amortization/prepayment analysis, insurance, goals, and non-inflating allocations.
- Added FIFO cost-basis lots, realized/unrealized gains, dividends/TDS, SIP schedules, RD contributions, loan schedules, credit-card statements/payments, retirement events, archived accounts, tags, and encrypted receipt attachments.
- Added detailed gross/take-home income, audit-preserving import rollback, account rename, buy-more investing, deposit lock-ins, variable-rate loan rescheduling, policy documents, asset maintenance/valuations/debt links, and configurable planning assumptions.
- Added evidence-based findings, multi-horizon baseline/optimistic/adverse forecasts, transparent health history, financial calendar, and offline notification scheduling.
- Added database-backed category, merchant, lifestyle-inflation, spending-composition, interest-leakage and ledger net-worth-movement analytics plus retirement stress scenarios and accessible chart summaries.
- Added interactive accessible charts for cashflow composition, categories, merchants, assets, debt schedules, goals, investment valuations, health history, forecasts, and retirement scenarios.
- Loan payments now mark the next amortization row paid and advance the due date; encrypted receipt and policy-document metadata is visible with local removal controls.
- Added versioned TY 2026-27 tax comparison including user-classified 111A/112A gains, expanded CA/transaction CSV reports, theme settings, biometric/device-credential locking, screenshot protection, and no-network manifest policy.
- Added portable PBKDF2/AES-256-GCM backup and atomic verified restore plus crypto tests.
- Added explicit Room v1→v2→v3 migrations, migration and 100,000-row performance instrumentation coverage, end-to-end encrypted backup restore coverage, recurring/SIP/dividend/loan-schedule accounting tests, expanded calculation/import tests, emulator execution in CI, and APK checksums.
- Added security, recovery, calculation, and release documentation.

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
- Added independently tested loan amortization, XIRR, deterministic forecasting, transparent health scoring, configurable tax estimation, CSV parsing, and import deduplication engines.
- Added normalized Room storage for recurring money, bills, imports, reconciliation, investments, valuations, deposits, retirement, assets, loans, insurance, goals, tax, health, insights, forecasts, notifications, and backup metadata.
- Added live bills, loans, policies and goals UI flows, balanced transfers, evidence-driven health dimensions, and explicit 12-month forecast scenarios.
- Added optional biometric/device-credential app locking with DataStore persistence and foreground-session invalidation.
- Added idempotent recurring ledger execution on startup and through unique periodic WorkManager scheduling.
