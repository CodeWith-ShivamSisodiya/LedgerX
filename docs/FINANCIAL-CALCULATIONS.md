# Financial calculations and invariants

- Money is signed 64-bit integer minor units. Parsing uses `BigDecimal`; binary floating-point is not used for stored balances, taxes, EMIs, costs, or postings.
- Every authoritative transaction is inserted with postings that sum to exactly zero. Overflow throws instead of wrapping.
- Transfers and credit-card settlements move assets/liabilities without creating expenses. Refunds reverse the original expense ledger. Reversals create equal/opposite postings and never mutate a reconciled transaction.
- Detailed income requires gross income to equal take-home cash, other deductions, and tax withheld. Only take-home cash credits the selected bank account; the gross income and non-cash components remain balanced and separately identifiable.
- Investment and deposit purchases reduce the selected cash account and create an asset ledger posting atomically. FIFO lots retain exact remaining cost basis. A sale removes cost basis, records fees separately, and recognizes the balancing realized gain/loss; manual dated valuations produce unrealized gain/loss without fabricating income. Dividends split received cash and TDS receivable against gross dividend income.
- Goal allocations label existing account balances and never create assets.
- EMI schedules use monthly reducing-balance arithmetic with final-installment closure. Prepayment comparisons recompute both schedules.
- XIRR uses dated cash flows and is shown only when a numerical solution exists.
- Forecasts are deterministic scenarios with explicit income growth, expense inflation, and return assumptions. They are not promises.
- Retirement projections compound the recorded corpus and the trailing-12-month average retirement contribution at a user-entered annual return, inflate the target at a user-entered rate, and show baseline/optimistic/adverse shortfalls. EPS pension contributions are excluded from liquid retirement wealth.
- The health score uses the documented 20/15/15/15/15/10/10 weights. Missing input contributes to incompleteness, never a healthy default; critical warnings are independent of the overall score.
- Tax Year 2026-27 comparison uses versioned old/new central slab metadata verified from official Income Tax Department/India Budget sources. Old-regime deductions are not applied to the new regime. User-classified section 111A gains use 20%; section 112A gains above the ₹1.25 lakh threshold use 12.5%. Rebate eligibility is tested against combined taxable ordinary and supported special-rate income, while the rebate is not applied to special-rate tax. Surcharge, marginal relief, loss set-off, grandfathering, residential/age rules, and classification eligibility require professional verification.
