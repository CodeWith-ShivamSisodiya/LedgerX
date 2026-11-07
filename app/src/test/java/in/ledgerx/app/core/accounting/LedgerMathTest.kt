package com.ledgerx.app.core.accounting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LedgerMathTest {
    @Test
    fun balancedExpenseIsAccepted() {
        LedgerMath.requireBalanced(listOf(-12_345L, 12_345L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unbalancedTransactionIsRejected() {
        LedgerMath.requireBalanced(listOf(-12_345L, 12_344L))
    }

    @Test(expected = ArithmeticException::class)
    fun overflowCannotSilentlyBalance() {
        LedgerMath.requireBalanced(listOf(Long.MAX_VALUE, 1L, Long.MIN_VALUE))
    }

    @Test
    fun savingsRateUsesExactIntegerBasisPoints() {
        assertEquals(2_500, LedgerMath.savingsRateBasisPoints(100_00L, 75_00L))
        assertNull(LedgerMath.savingsRateBasisPoints(0L, 10_00L))
    }
}
