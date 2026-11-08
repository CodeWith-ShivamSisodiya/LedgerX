package com.ledgerx.app.core.finance

data class HealthInputs(
    val emergencyMonths: Double? = null,
    val debtToIncomePercent: Double? = null,
    val insuranceCoverageRatio: Double? = null,
    val savingsRatePercent: Double? = null,
    val monthlySurplusMinor: Long? = null,
    val stableIncomeSharePercent: Double? = null,
    val largestAssetClassPercent: Double? = null,
)

data class HealthDimension(
    val name: String,
    val weight: Int,
    val score: Int?,
    val explanation: String,
    val recommendation: String,
)

data class HealthResult(
    val score: Int?,
    val completenessPercent: Int,
    val dimensions: List<HealthDimension>,
    val criticalWarnings: List<String>,
)

object HealthScoreCalculator {
    fun calculate(input: HealthInputs): HealthResult {
        val dimensions = listOf(
            dimension("Emergency reserves", 20, input.emergencyMonths, { scale(it, 0.0, 6.0) }, "months of essential expenses", "Build toward six months of essential expenses."),
            dimension("Debt affordability", 15, input.debtToIncomePercent, { 100 - scale(it, 20.0, 60.0) }, "debt payments as a share of income", "Reduce expensive debt or refinance only after comparing total cost."),
            dimension("Insurance protection", 15, input.insuranceCoverageRatio, { scale(it, 0.0, 1.0) }, "coverage relative to the stated need", "Review coverage, exclusions, deductibles and dependants."),
            dimension("Savings consistency", 15, input.savingsRatePercent, { scale(it, 0.0, 30.0) }, "confirmed savings rate", "Automate a sustainable contribution after essential obligations."),
            dimension("Liquidity and cashflow", 15, input.monthlySurplusMinor?.toDouble(), { if (it > 0) 100 else 0 }, "monthly confirmed surplus", "Resolve recurring cashflow deficits before taking new commitments."),
            dimension("Income resilience", 10, input.stableIncomeSharePercent, { scale(it, 0.0, 100.0) }, "stable income share", "Maintain reserves appropriate to income variability."),
            dimension("Wealth diversification", 10, input.largestAssetClassPercent, { 100 - scale(it, 40.0, 100.0) }, "largest asset-class concentration", "Review concentration in the context of goals and risk tolerance."),
        )
        val completeWeight = dimensions.filter { it.score != null }.sumOf { it.weight }
        val weighted = dimensions.filter { it.score != null }.sumOf { it.score!! * it.weight }
        val warnings = buildList {
            if (input.emergencyMonths != null && input.emergencyMonths < 1) add("Emergency reserves cover less than one month.")
            if (input.debtToIncomePercent != null && input.debtToIncomePercent > 50) add("Debt payments exceed half of recorded income.")
            if (input.monthlySurplusMinor != null && input.monthlySurplusMinor < 0) add("Recorded monthly cashflow is negative.")
            if (input.insuranceCoverageRatio != null && input.insuranceCoverageRatio < 0.5) add("Recorded protection covers less than half of the stated need.")
        }
        return HealthResult(
            score = if (completeWeight == 100) weighted / 100 else null,
            completenessPercent = completeWeight,
            dimensions = dimensions,
            criticalWarnings = warnings,
        )
    }

    private fun dimension(name: String, weight: Int, value: Double?, formula: (Double) -> Int, unit: String, action: String) =
        HealthDimension(name, weight, value?.let(formula), value?.let { "$it $unit." } ?: "Data is missing; no healthy score is assumed.", action)

    private fun scale(value: Double, low: Double, high: Double): Int =
        (((value - low) / (high - low)) * 100).toInt().coerceIn(0, 100)
}

