package com.ledgerx.app.core.accounting

object LedgerMath {
    fun requireBalanced(signedMinorAmounts: List<Long>) {
        require(signedMinorAmounts.size >= 2) { "A ledger transaction needs at least two postings" }
        val total = signedMinorAmounts.fold(0L) { total, amount -> Math.addExact(total, amount) }
        require(total == 0L) { "Ledger postings must balance to zero; difference was $total" }
    }

    fun savingsRateBasisPoints(incomeMinor: Long, expenseMinor: Long): Int? {
        if (incomeMinor <= 0L) return null
        return (((incomeMinor - expenseMinor).toBigDecimal() * 10_000.toBigDecimal()) /
            incomeMinor.toBigDecimal()).toInt()
    }
}
