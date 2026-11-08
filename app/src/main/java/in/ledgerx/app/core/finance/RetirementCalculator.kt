package com.ledgerx.app.core.finance

import java.math.BigDecimal
import java.math.RoundingMode

data class RetirementProjection(
    val scenario: String,
    val years: Int,
    val projectedCorpusMinor: Long,
    val inflationAdjustedTargetMinor: Long,
    val shortfallMinor: Long,
)

object RetirementCalculator {
    fun project(
        scenario: String,
        currentAge: Int,
        retirementAge: Int,
        currentCorpusMinor: Long,
        monthlyContributionMinor: Long,
        targetInTodayMoneyMinor: Long,
        annualReturnBasisPoints: Int,
        inflationBasisPoints: Int,
    ): RetirementProjection {
        require(currentAge in 18..90 && retirementAge in (currentAge + 1)..100)
        require(currentCorpusMinor >= 0 && monthlyContributionMinor >= 0 && targetInTodayMoneyMinor >= 0)
        val years = retirementAge - currentAge
        val months = years * 12
        val monthlyRate = BigDecimal(annualReturnBasisPoints).divide(BigDecimal(120_000), 16, RoundingMode.HALF_EVEN)
        var corpus = BigDecimal(currentCorpusMinor)
        repeat(months) { corpus = corpus.multiply(BigDecimal.ONE.add(monthlyRate)).add(BigDecimal(monthlyContributionMinor)) }
        var target = BigDecimal(targetInTodayMoneyMinor)
        val annualInflation = BigDecimal.ONE.add(BigDecimal(inflationBasisPoints).divide(BigDecimal(10_000), 16, RoundingMode.HALF_EVEN))
        repeat(years) { target = target.multiply(annualInflation) }
        val projected = corpus.setScale(0, RoundingMode.HALF_UP).longValueExact()
        val inflatedTarget = target.setScale(0, RoundingMode.HALF_UP).longValueExact()
        return RetirementProjection(scenario, years, projected, inflatedTarget, (inflatedTarget - projected).coerceAtLeast(0))
    }
}
