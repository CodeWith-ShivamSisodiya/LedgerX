package com.ledgerx.app.core.finance

import java.math.BigDecimal
import java.math.RoundingMode

data class ForecastAssumptions(
    val openingMinor: Long,
    val monthlyIncomeMinor: Long,
    val monthlyExpenseMinor: Long,
    val annualIncomeGrowthBasisPoints: Int,
    val annualInflationBasisPoints: Int,
    val annualReturnBasisPoints: Int,
)

data class ForecastPoint(val month: Int, val balanceMinor: Long)
data class ForecastScenario(val name: String, val assumptions: ForecastAssumptions, val points: List<ForecastPoint>)

object ForecastEngine {
    fun project(name: String, assumptions: ForecastAssumptions, months: Int): ForecastScenario {
        require(months in 1..1_200) { "Forecast horizon must be 1 to 1200 months" }
        var balance = BigDecimal(assumptions.openingMinor)
        var income = BigDecimal(assumptions.monthlyIncomeMinor)
        var expense = BigDecimal(assumptions.monthlyExpenseMinor)
        val incomeGrowth = monthlyFactor(assumptions.annualIncomeGrowthBasisPoints)
        val inflation = monthlyFactor(assumptions.annualInflationBasisPoints)
        val returns = monthlyRate(assumptions.annualReturnBasisPoints)
        val points = ArrayList<ForecastPoint>(months + 1)
        points += ForecastPoint(0, assumptions.openingMinor)
        for (month in 1..months) {
            val investmentReturn = if (balance.signum() > 0) balance.multiply(returns) else BigDecimal.ZERO
            balance = balance.add(investmentReturn).add(income).subtract(expense)
            points += ForecastPoint(month, balance.setScale(0, RoundingMode.HALF_EVEN).longValueExact())
            income = income.multiply(incomeGrowth)
            expense = expense.multiply(inflation)
        }
        return ForecastScenario(name, assumptions, points)
    }

    private fun monthlyRate(annualBasisPoints: Int): BigDecimal =
        BigDecimal(annualBasisPoints).divide(BigDecimal(120_000), 16, RoundingMode.HALF_EVEN)

    private fun monthlyFactor(annualBasisPoints: Int): BigDecimal = BigDecimal.ONE.add(monthlyRate(annualBasisPoints))
}
