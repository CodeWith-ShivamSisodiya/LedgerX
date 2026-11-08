package com.ledgerx.app.core.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetirementCalculatorTest {
    @Test fun zeroReturnAddsEveryContribution() {
        val result = RetirementCalculator.project("Baseline", 30, 31, 100_00, 1_000_00, 0, 0, 0)
        assertEquals(12_10_000L, result.projectedCorpusMinor)
        assertEquals(0L, result.shortfallMinor)
    }

    @Test fun reportsInflationAdjustedShortfall() {
        val result = RetirementCalculator.project("Adverse", 30, 40, 0, 0, 10_00_000_00, 0, 600)
        assertTrue(result.inflationAdjustedTargetMinor > 10_00_000_00)
        assertEquals(result.inflationAdjustedTargetMinor, result.shortfallMinor)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsRetirementBeforeCurrentAge() {
        RetirementCalculator.project("Invalid", 40, 40, 0, 0, 0, 0, 0)
    }
}
