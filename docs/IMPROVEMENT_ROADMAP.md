# LedgerX — Simplification and Correctness Roadmap

Status: Active. Scope: offline personal finance Android app. Keep user data private, preserve existing records and migrations, and favor understandable flows over new features.

## User's monthly example (illustrative plan, NOT seeded into the app)
- Rent ₹10,000; car EMI ₹13,000; parents ₹15,000; plot EMI ₹33,000; wife/household ₹4,000.
- RD ₹10,000; mutual-fund SIP ₹30,000; stock SIP ₹10,000; FD ₹50,000.
- Total scheduled cash outflow ₹1,75,000/month: ₹29,000 living/family, ₹46,000 loan installments, ₹1,00,000 saved/invested.
- EMI principal is debt reduction, not consumption; interest/fees are costs. FD/RD/SIP/stock buys transfer cash into assets, not spending.
- Insurance premium is a cost for protection, not automatically an investment; only independently verified surrender/investment value should be included in net worth.

## P0 — Correctness before analytics
- [ ] Establish explicit classification: consumption, interest/fees, loan-principal repayment, asset transfer/contribution, income, internal transfer, refund.
- [ ] Prevent double-counting money moved to FD/RD/SIP/investment and investment purchases when calculating monthly surplus and net worth.
- [ ] Distinguish **planned**, **paid**, **invested**, **overdue**, and **cleared**. Never label a schedule as executed until confirmed.
- [ ] Test EMI split and transfer balances; cover partial prepayment, interest rate change, foreclosure, SIP cancellation, FD maturity, RD installment and early withdrawal.
- [ ] Audit credit-card debt, cash account balance, outstanding loans, owned property and loan-linked property in net-worth calculation.
- [ ] Verify Room migrations/backup/restore with saved historical data and explicit rollback for import failures.
- [ ] Show missing valuations and last-update dates rather than implying offline prices are live.
- [ ] Do not create wealth from merely adding savings/investment schedules; wealth only follows real balance or valuation entries.

## P1 — Core everyday use
- [x] Simplify Home to a money overview, recent activity and wealth breakdown (first iteration).
- [x] Simplify Wealth entry shortcuts and collapse advanced actions (first iteration).
- [ ] Replace misleading 'Invested' monthly label with 'Recorded investment' and clearly qualify cashflow availability.
- [ ] Add **Monthly Commitments**: name, category, amount, due day, recurrence, source account, optional end date, active/paused, mark paid; configure once, confirm monthly.
- [ ] Add simple setup assistant for recurring obligations; allow edits, pauses and exceptions without rewriting history.
- [ ] Add streamlined **Add money / Add expense / Transfer / Add asset** pathways with sensible defaults and fewer required fields.
- [ ] Wealth timeline: cost/contributions vs current value vs gain and reliable snapshots over time.
- [ ] Central inventory of FD, RD, MF, stocks, gold, property, bank cash, EPF/PPF/NPS, insurance coverage and loans.
- [ ] Clear zero-data states and partial-data explanations rather than pretend financial precision.
- [ ] Make Home navigation manageable with 3–4 top-level destinations and nontechnical labels.

## P2 — Insight without overload
- [ ] Display monthly allocation breakdown: living, debt service, investments/savings, remainder (vs income).
- [ ] Cashflow calendar with due-date alerts; no transaction should auto-post without confirmation.
- [ ] Net worth trend, debt principal trend, savings/investment contribution trend, liquidity, and emergency-fund coverage.
- [ ] Personalized savings suggestions grounded only in recorded data, show assumptions and uncertainty.
- [ ] Insurance coverage/renewal overview; do not misrepresent policy face value as assets.
- [ ] Monthly review summary and searchable year-to-date reporting.
- [ ] Import bank/broker data with duplicate detection, explicit mapping, privacy-preserving offline processing.

## P3 — Design, accessibility, quality
- [ ] Visual system: calmer cards, consistent spacing and typography, lighter density, accessible contrast, large touch targets.
- [ ] Separate overview and advanced details using progressive disclosure; avoid long rows of technical buttons.
- [ ] Create a tested screenshot gallery for common devices, dark mode, empty/large-data states.
- [ ] Preserve fast emulator-free APK CI and a separate instrumentation suite for migrations; gradually grow Maestro UI smoke paths, noting Maestro still requires a device.
- [ ] Test small phones, screen readers, Indian rupee/lakh formatting, large font settings, offline startup and responsiveness.

## Delivery gates
1. Each increment on a PR branch; fast build (unit/lint/APK) must pass before merge.
2. Add tests for accounting behavior before modifying balances, schemas or auto-post logic.
3. No personal monthly amounts hardcoded; examples belong only in documentation.
4. Before a production release, run separate device and migration tests and verify a restore.
5. Treat net worth and surplus as incomplete when critical accounts or liabilities are missing.
