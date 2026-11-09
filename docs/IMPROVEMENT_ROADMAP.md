# LedgerX — Wealth Manager Roadmap (scope reset)

**Product decision (9 November 2026): LedgerX is now a wealth manager, not a daily expense tracker.** Manual expense logging is too high-friction. Do not build recurring expense-entry, budgeting, expense forecasting, or daily expense reminders. Retain existing accounting data and import/export support for compatibility; do not delete user data or destructive-migrate historical records.

## Primary daily experience
- Open app → understand *assets, liabilities, and estimated net worth*.
- Add an existing holding once; revise the balance or valuation only when it changes.
- Record optional notes with holdings and important dates, without requiring manual expense transactions.
- Keep the first screen quiet and visually clear; move specialist tools out of the main journey.

## P0 — Financial correctness
- [ ] Audit all net-worth calculations for correct cash/deposit/investment and liability treatment; especially cash moved into FD/RD/stock/MF and loan-linked property.
- [ ] Show recorded valuation date, distinguish stale or missing price from true real-time value.
- [ ] Track initial contribution/cost, current value, total gain/loss separately; avoid counting contributions as profit.
- [ ] Keep insurance sum-assured out of net worth; optional surrender value must be explicitly entered.
- [ ] Ensure any import or holding update cannot silently double-count or discard balance entries.
- [ ] Verify backup/restore and migrations using real legacy sample data; preserve user files offline.

## P1 — Simple portfolio and notes
- [x] Wealth-first Home screen with net worth and holding categories; no daily-expense CTA.
- [x] Simple asset shortcuts and advanced tools collapsed in Wealth.
- [ ] Portfolio detail cards: account balance, FD/RD principal and maturity, stocks/MF quantity/cost/latest manual valuation, retirement, gold/property, and loan outstanding.
- [ ] Add or update an entire holding through a short guided flow rather than a wall of forms.
- [x] General-purpose **notes per holding** (title, text, updated timestamp), encrypted in the existing SQLCipher Room database, with create/edit/delete. Audit/history of prior note revisions remains future work.
- [ ] Asset and portfolio attachments (policy, receipts/certificates); keep encrypted storage and easy restore.
- [ ] Portfolio groups and filters: bank/deposit, market investments, metals/property, retirement, loans and protection.
- [ ] Loan-specific views: outstanding vs original debt, optional linked asset, paid-off status.
- [ ] Insurance policy inventory, premium/renewal reminder, nominee; coverage separate from asset value.

## P2 — Useful wealth intelligence
- [ ] Snapshot history and net-worth trend, with timestamps and editable valuations.
- [ ] Allocation charts (asset category / liquidity / invested capital vs market growth).
- [ ] FD/RD maturity calendar and renewal reminders; interest projections must state rate and assumptions.
- [ ] Investment return calculations (realized/unrealized, XIRR where complete dated flows exist).
- [ ] Asset notes timeline, important documents, tags and searchable history.
- [ ] Concentration and liquidity warnings, emergency cash coverage when known; avoid inventing missing expense inputs.
- [ ] Exports of portfolio, valuations, and notes; cross-check totals for reconciliation.

## P3 — UX and test quality
- [ ] Four clear areas: Overview, Assets, Trends, More (legacy records accessible from More).
- [ ] Consistent simple visual design, type scale, accessible contrast, touch targets and rupee formatting.
- [ ] Test no-data, single-holding, many-holdings, stale valuation and loan-heavy scenarios.
- [ ] Keep fast APK CI; verify migration/instrumentation separately before a release; optional Maestro UI flows still require a device.
- [ ] Prefer intuitive language over accounting jargon; only ask for needed fields.

## Current delivery / limitations
- Phase 1 Home/Wealth changes are in PR #2 (not yet automatically merged).
- Existing Room database, expenses and transactions are retained for data compatibility; the app is **not yet a pure wealth-only build**.
- Generic editable notes storage and the simplified holding wizard are **not yet implemented**.
- No personal example amounts are inserted into the user's actual accounts.
