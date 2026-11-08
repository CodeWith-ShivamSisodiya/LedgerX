package com.ledgerx.app.core.finance

import java.math.BigDecimal
import java.math.RoundingMode

data class LoanPayment(
    val installment: Int,
    val openingMinor: Long,
    val paymentMinor: Long,
    val principalMinor: Long,
    val interestMinor: Long,
    val closingMinor: Long,
)

data class LoanSchedule(
    val emiMinor: Long,
    val totalInterestMinor: Long,
    val payments: List<LoanPayment>,
)
data class PrepaymentImpact(val newPrincipalMinor: Long, val originalInterestMinor: Long, val revisedInterestMinor: Long, val interestSavedMinor: Long)

object LoanCalculator {
    fun simulatePrepayment(principalMinor: Long, annualRateBasisPoints: Int, months: Int, prepaymentMinor: Long): PrepaymentImpact {
        require(prepaymentMinor in 1 until principalMinor) { "Prepayment must be positive and below outstanding principal" }
        val original = amortize(principalMinor, annualRateBasisPoints, months)
        val revisedPrincipal = principalMinor - prepaymentMinor
        val revised = amortize(revisedPrincipal, annualRateBasisPoints, months)
        return PrepaymentImpact(revisedPrincipal, original.totalInterestMinor, revised.totalInterestMinor, original.totalInterestMinor - revised.totalInterestMinor)
    }

    fun amortize(principalMinor: Long, annualRateBasisPoints: Int, months: Int): LoanSchedule {
        require(principalMinor > 0) { "Principal must be positive" }
        require(annualRateBasisPoints >= 0) { "Interest rate cannot be negative" }
        require(months > 0) { "Tenure must be positive" }

        val monthlyRate = BigDecimal(annualRateBasisPoints)
            .divide(BigDecimal(120_000), 20, RoundingMode.HALF_EVEN)
        val principal = BigDecimal(principalMinor)
        val emi = if (monthlyRate.signum() == 0) {
            principal.divide(BigDecimal(months), 0, RoundingMode.CEILING).longValueExact()
        } else {
            val factor = BigDecimal.ONE.add(monthlyRate).pow(months)
            principal.multiply(monthlyRate).multiply(factor)
                .divide(factor.subtract(BigDecimal.ONE), 0, RoundingMode.HALF_UP)
                .longValueExact()
        }

        var balance = principalMinor
        var interestTotal = 0L
        val rows = ArrayList<LoanPayment>(months)
        for (number in 1..months) {
            val interest = BigDecimal(balance).multiply(monthlyRate)
                .setScale(0, RoundingMode.HALF_UP).longValueExact()
            val principalPaid = if (number == months) balance else minOf(balance, emi - interest)
            val payment = principalPaid + interest
            val closing = balance - principalPaid
            rows += LoanPayment(number, balance, payment, principalPaid, interest, closing)
            interestTotal = Math.addExact(interestTotal, interest)
            balance = closing
        }
        check(balance == 0L) { "Amortization did not close the loan" }
        return LoanSchedule(emi, interestTotal, rows)
    }
}
