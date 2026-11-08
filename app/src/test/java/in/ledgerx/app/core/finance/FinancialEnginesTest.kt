package com.ledgerx.app.core.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import com.ledgerx.app.core.planning.RecurrenceEngine

class FinancialEnginesTest {
    @Test
    fun zeroInterestLoanClosesExactly() {
        val result = LoanCalculator.amortize(100_00L, 0, 3)
        assertEquals(0L, result.totalInterestMinor)
        assertEquals(0L, result.payments.last().closingMinor)
        assertEquals(100_00L, result.payments.sumOf { it.principalMinor })
    }

    @Test fun loanPrepaymentReducesInterest() {
        val impact = LoanCalculator.simulatePrepayment(10_00_000_00L, 850, 120, 1_00_000_00L)
        assertEquals(9_00_000_00L, impact.newPrincipalMinor)
        assertTrue(impact.interestSavedMinor > 0)
    }

    @Test
    fun xirrMatchesOneYearReturn() {
        val result = XirrCalculator.calculate(
            listOf(
                DatedCashFlow(LocalDate.of(2025, 1, 1), -100_00L),
                DatedCashFlow(LocalDate.of(2026, 1, 1), 110_00L),
            )
        )
        assertEquals(0.10, result, 0.000001)
    }

    @Test
    fun incompleteHealthDataNeverBecomesHealthyScore() {
        val result = HealthScoreCalculator.calculate(HealthInputs(savingsRatePercent = 25.0))
        assertNull(result.score)
        assertEquals(15, result.completenessPercent)
    }

    @Test
    fun criticalHealthWarningsRemainIndependent() {
        val result = HealthScoreCalculator.calculate(
            HealthInputs(0.5, 55.0, 0.4, 30.0, -1, 100.0, 35.0)
        )
        assertTrue(result.criticalWarnings.size >= 3)
    }

    @Test
    fun configurableTaxSlabsApplyRebateAndCess() {
        val rules = TaxRuleSet(
            "TEST", "TEST", 0, 500_00L, 20_00L, 400,
            listOf(TaxSlab(0, 300_00L, 0), TaxSlab(300_00L, null, 1_000)),
        )
        val estimate = IndianTaxEngine.estimate(600_00L, 0, rules)
        assertEquals(300_00L, estimate.taxableIncomeMinor - 300_00L)
        assertEquals(30_00L, estimate.slabTaxMinor)
        assertEquals(31_20L, estimate.totalMinor)
    }

    @Test
    fun taxYear2026DoesNotApplyOldRegimeDeductionsToNewRegime() {
        val gross = 15_00_000_00L
        val deductions = 1_50_000_00L
        val old = IndianTaxEngine.estimate(gross, deductions, IndianTaxRules.taxYear2026Old())
        val newWithClaim = IndianTaxEngine.estimate(gross, deductions, IndianTaxRules.taxYear2026New())
        val newWithoutClaim = IndianTaxEngine.estimate(gross, 0, IndianTaxRules.taxYear2026New())
        assertTrue(old.taxableIncomeMinor < newWithClaim.taxableIncomeMinor)
        assertEquals(newWithoutClaim, newWithClaim)
    }

    @Test
    fun forecastRetainsExplicitScenarioAndHorizon() {
        val assumptions = ForecastAssumptions(100_00L, 10_00L, 8_00L, 0, 0, 0)
        val result = ForecastEngine.project("baseline", assumptions, 12)
        assertEquals(124_00L, result.points.last().balanceMinor)
        assertEquals(13, result.points.size)
    }

    @Test
    fun monthlyRecurrenceHandlesShortMonths() {
        val dates = RecurrenceEngine.dueDates(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 4, 30), "MONTHLY", 1)
        assertEquals(listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 28), LocalDate.of(2026, 4, 28)), dates)
    }

    @Test fun quarterlyRecurrenceAdvancesThreeMonths() {
        assertEquals(LocalDate.of(2027, 2, 28), RecurrenceEngine.next(LocalDate.of(2026, 11, 30), "QUARTERLY", 1))
    }

    @Test fun fifoCostBasisHandlesPartialLotsAndRealizedGain() {
        val result = InvestmentCostBasisCalculator.fifo(
            listOf(CostBasisLot("first", 1_000_000, 100_00), CostBasisLot("second", 1_000_000, 140_00)),
            saleQuantityMicros = 1_500_000,
            grossProceedsMinor = 300_00,
        )
        assertEquals(170_00L, result.costBasisMinor)
        assertEquals(130_00L, result.realizedGainMinor)
        assertEquals(listOf(1_000_000L, 500_000L), result.allocations.map { it.quantityMicros })
    }

    @Test fun advancedTaxSeparatesSpecialRateCapitalGains() {
        val estimate = AdvancedIndianTaxEngine.estimate(
            TaxIncomeBreakdown(
                salaryMinor = 10_00_000_00L,
                interestMinor = 1_00_000_00L,
                dividendMinor = 1_00_000_00L,
                shortTermCapitalGain111AMinor = 1_00_000_00L,
                longTermCapitalGain112AMinor = 2_00_000_00L,
                eligibleDeductionsMinor = 0,
            ),
            IndianTaxRules.taxYear2026New(),
        )
        assertEquals(29_375_00L, estimate.specialRateTaxMinor)
        assertEquals(85_150_00L, estimate.totalMinor)
        assertTrue(estimate.warnings.isNotEmpty())
    }
}
