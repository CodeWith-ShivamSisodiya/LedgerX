package com.ledgerx.app.core.finance

import java.math.BigDecimal
import java.math.RoundingMode

data class TaxIncomeBreakdown(
    val salaryMinor: Long,
    val interestMinor: Long,
    val dividendMinor: Long,
    val shortTermCapitalGain111AMinor: Long,
    val longTermCapitalGain112AMinor: Long,
    val eligibleDeductionsMinor: Long,
)

data class AdvancedTaxEstimate(
    val ordinaryTaxableIncomeMinor: Long,
    val ordinaryTaxAfterRebateMinor: Long,
    val specialRateTaxMinor: Long,
    val cessMinor: Long,
    val totalMinor: Long,
    val warnings: List<String>,
)

object AdvancedIndianTaxEngine {
    private const val LTCG_112A_EXEMPTION_MINOR = 125_000_00L

    fun estimate(input: TaxIncomeBreakdown, rules: TaxRuleSet): AdvancedTaxEstimate {
        require(listOf(input.salaryMinor, input.interestMinor, input.dividendMinor, input.shortTermCapitalGain111AMinor, input.longTermCapitalGain112AMinor, input.eligibleDeductionsMinor).all { it >= 0 })
        val ordinaryGross = listOf(input.salaryMinor, input.interestMinor, input.dividendMinor).fold(0L) { total, value -> Math.addExact(total, value) }
        val salaryLimitedRules = rules.copy(
            standardDeductionMinor = minOf(rules.standardDeductionMinor, input.salaryMinor),
            rebateTaxableIncomeLimitMinor = -1,
            rebateMaximumMinor = 0,
            cessBasisPoints = 0,
        )
        val ordinary = IndianTaxEngine.estimate(ordinaryGross, input.eligibleDeductionsMinor, salaryLimitedRules)
        val stcgTax = percentage(input.shortTermCapitalGain111AMinor, 2_000)
        val taxableLtcg = (input.longTermCapitalGain112AMinor - LTCG_112A_EXEMPTION_MINOR).coerceAtLeast(0)
        val ltcgTax = percentage(taxableLtcg, 1_250)
        val special = Math.addExact(stcgTax, ltcgTax)
        val totalTaxableIncome = Math.addExact(ordinary.taxableIncomeMinor, Math.addExact(input.shortTermCapitalGain111AMinor, taxableLtcg))
        val ordinaryRebate = if (totalTaxableIncome <= rules.rebateTaxableIncomeLimitMinor) minOf(ordinary.slabTaxMinor, rules.rebateMaximumMinor) else 0L
        val ordinaryAfterRebate = ordinary.slabTaxMinor - ordinaryRebate
        val beforeCess = Math.addExact(ordinaryAfterRebate, special)
        val cess = percentage(beforeCess, rules.cessBasisPoints)
        return AdvancedTaxEstimate(
            ordinaryTaxableIncomeMinor = ordinary.taxableIncomeMinor,
            ordinaryTaxAfterRebateMinor = ordinaryAfterRebate,
            specialRateTaxMinor = special,
            cessMinor = cess,
            totalMinor = Math.addExact(beforeCess, cess),
            warnings = listOf(
                "Capital-gain classification, set-off, grandfathering and STT eligibility are user-provided and not inferred.",
                "Surcharge, marginal relief, house-property and business rules are excluded; verify the estimate before filing.",
            ),
        )
    }

    private fun percentage(amountMinor: Long, basisPoints: Int): Long =
        BigDecimal(amountMinor).multiply(BigDecimal(basisPoints)).divide(BigDecimal(10_000), 0, RoundingMode.DOWN).longValueExact()
}
