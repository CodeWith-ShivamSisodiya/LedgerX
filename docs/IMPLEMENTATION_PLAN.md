# Implementation plan and dependency map

## Phase 1 — Foundation

1. Project and design system → required by every feature.
2. Keystore-wrapped SQLCipher database → required before real records.
3. Account and ledger schema → authoritative source for balances and analytics.
4. Onboarding, accounts, quick entry and dashboard → first usable daily loop.
5. Biometric session gate, balanced reversal/correction workflow, audit history, and database integrity tests are implemented.

## Phase 2 — Daily money

Implemented recurring templates and idempotent occurrences, explicit receipt confirmation for scheduled income, bills/subscriptions with price history, transfers, split/refund/correction entry, paged database search, CSV/XLSX mapping/preview/deduplication/atomic rollback, reconciliation, tags, merchant memory, and encrypted receipts with per-transaction counts and removal.

## Phase 3 — Wealth

Implemented instrument masters, dated valuations, FIFO lots/cost basis, purchases/sales/dividends/SIPs/corporate actions, allocation/return metrics and XIRR. FD/RD funding and lock-ins, retirement contributions/events/scenarios, physical assets with valuation/maintenance/debt links, amortized variable-rate loans whose persisted schedules advance when payments are recorded, card statements, manageable encrypted policy documents, and non-inflating goal allocations feed net worth.

## Phase 4 — Intelligence

Goals, transparent weighted health scoring/history, deterministic multi-horizon scenario forecasts, category/merchant/lifestyle/interest analytics, interactive accessible charts with exact-value text alternatives, local evidence-based insights, and the consolidated financial/tax calendar consume authoritative Phase 1–3 data. Outputs expose assumptions, completeness, monetary impact, and uncertainty.

## Phase 5 — Indian tax and advanced planning

Versioned TY 2026-27 old/new rule packs cover ordinary income, user-classified sections 111A/112A, standard deductions, regime-specific deductions, rebate, cess, TDS, advance tax, and CA export. Persistent essential-expense, protection-need, inflation and return assumptions drive baseline/optimistic/adverse retirement projections. Unsupported filing complexity is explicitly excluded and warned.

## Phase 6 — Production hardening

Encrypted portable backup/recovery (including attachment bytes), backward-compatible backup parsing, v1→v2→v3 migrations, accessibility semantics, notification resilience, 100k-transaction performance coverage, security documentation, and CI emulator/APK packaging are implemented. Production signing and the release version remain gated on a green CI run and verified APK installation.
