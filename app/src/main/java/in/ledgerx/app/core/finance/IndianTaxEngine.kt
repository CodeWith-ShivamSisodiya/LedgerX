package com.ledgerx.app.core.finance

import java.math.BigDecimal
import java.math.RoundingMode

data class TaxSlab(val lowerMinor: Long, val upperMinorExclusive: Long?, val rateBasisPoints: Int)
data class TaxRuleSet(
    val financialYear: String,
    val regime: String,
    val standardDeductionMinor: Long,
    val rebateTaxableIncomeLimitMinor: Long,
    val rebateMaximumMinor: Long,
    val cessBasisPoints: Int,
    val slabs: List<TaxSlab>,
    val allowEligibleDeductions: Boolean = true,
)
data class TaxEstimate(val taxableIncomeMinor: Long, val slabTaxMinor: Long, val rebateMinor: Long, val cessMinor: Long, val totalMinor: Long)

object IndianTaxEngine {
    fun estimate(grossIncomeMinor: Long, eligibleDeductionsMinor: Long, rules: TaxRuleSet): TaxEstimate {
        require(grossIncomeMinor >= 0 && eligibleDeductionsMinor >= 0)
        require(rules.slabs.zipWithNext().all { (a, b) -> a.lowerMinor <= b.lowerMinor }) { "Tax slabs must be ordered" }
        val applicableDeductions = if (rules.allowEligibleDeductions) eligibleDeductionsMinor else 0L
        val taxable = (grossIncomeMinor - rules.standardDeductionMinor - applicableDeductions).coerceAtLeast(0)
        var tax = 0L
        for (slab in rules.slabs) {
            val upper = slab.upperMinorExclusive ?: taxable
            val inSlab = (minOf(taxable, upper) - slab.lowerMinor).coerceAtLeast(0)
            val slabTax = BigDecimal(inSlab).multiply(BigDecimal(slab.rateBasisPoints))
                .divide(BigDecimal(10_000), 0, RoundingMode.DOWN).longValueExact()
            tax = Math.addExact(tax, slabTax)
        }
        val rebate = if (taxable <= rules.rebateTaxableIncomeLimitMinor) minOf(tax, rules.rebateMaximumMinor) else 0
        val afterRebate = tax - rebate
        val cess = BigDecimal(afterRebate).multiply(BigDecimal(rules.cessBasisPoints))
            .divide(BigDecimal(10_000), 0, RoundingMode.DOWN).longValueExact()
        return TaxEstimate(taxable, tax, rebate, cess, afterRebate + cess)
    }
}

object IndianTaxRules {
    private const val LAKH = 10_000_000L

    /** Official central slab structure for Tax Year 2026-27. Surcharge, marginal relief,
     * special-rate income, and individual deduction eligibility require professional review. */
    fun taxYear2026New() = TaxRuleSet(
        financialYear = "2026-27",
        regime = "NEW",
        standardDeductionMinor = 75_000_00L,
        rebateTaxableIncomeLimitMinor = 12 * LAKH,
        rebateMaximumMinor = 60_000_00L,
        cessBasisPoints = 400,
        allowEligibleDeductions = false,
        slabs = listOf(
            TaxSlab(0, 4 * LAKH, 0), TaxSlab(4 * LAKH, 8 * LAKH, 500),
            TaxSlab(8 * LAKH, 12 * LAKH, 1_000), TaxSlab(12 * LAKH, 16 * LAKH, 1_500),
            TaxSlab(16 * LAKH, 20 * LAKH, 2_000), TaxSlab(20 * LAKH, 24 * LAKH, 2_500),
            TaxSlab(24 * LAKH, null, 3_000),
        ),
    )

    fun taxYear2026Old() = TaxRuleSet(
        financialYear = "2026-27",
        regime = "OLD",
        standardDeductionMinor = 50_000_00L,
        rebateTaxableIncomeLimitMinor = 5 * LAKH,
        rebateMaximumMinor = 12_500_00L,
        cessBasisPoints = 400,
        allowEligibleDeductions = true,
        slabs = listOf(
            TaxSlab(0, 250_000_00L, 0), TaxSlab(250_000_00L, 5 * LAKH, 500),
            TaxSlab(5 * LAKH, 10 * LAKH, 2_000), TaxSlab(10 * LAKH, null, 3_000),
        ),
    )
}
