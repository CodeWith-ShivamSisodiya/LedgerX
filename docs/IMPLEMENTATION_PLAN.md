# Implementation plan and dependency map

## Phase 1 — Foundation (in progress)

1. Project and design system → required by every feature.
2. Keystore-wrapped SQLCipher database → required before real records.
3. Account and ledger schema → authoritative source for balances and analytics.
4. Onboarding, accounts, quick entry and dashboard → first usable daily loop.
5. Biometric session gate, reversal/edit workflow and database integrity tests → remaining Phase 1 hardening.

## Phase 2 — Daily money

Recurring templates and occurrences depend on the ledger. Import adapters depend on stable account/category models and add preview, mapping, deduplication, reconciliation and atomic rollback. Search, filters, splits, refunds and subscriptions follow.

## Phase 3 — Wealth

Instrument masters, dated valuations, holdings/cost basis and corporate actions precede performance metrics. Fixed income, EPF/EPS/PPF/NPS, physical assets, loans and insurance then feed net worth without double counting.

## Phase 4 — Intelligence

Goals, health scoring, deterministic analytics, scenario forecasts, local insights and calendar consume authoritative Phase 1–3 data. Every output carries inputs, assumptions, completeness and uncertainty.

## Phase 5 — Indian tax and advanced planning

Versioned financial-year rule packs, regime comparison, capital gains, TDS, advance tax, retirement scenarios and CA export. Rule packs require independent examples and effective-date tests.

## Phase 6 — Production hardening

Encrypted portable backup/recovery, migrations, accessibility, notification resilience, 100k-transaction performance tests, security review, release signing and verified APK production.

