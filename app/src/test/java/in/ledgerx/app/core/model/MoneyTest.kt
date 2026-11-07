package in.ledgerx.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test
    fun parsesIndianMajorAmountIntoExactMinorUnits() {
        assertEquals(12_345_67L, Money.parseMajor("1,23,45.67").minor)
    }

    @Test(expected = ArithmeticException::class)
    fun rejectsFractionsSmallerThanOnePaise() {
        Money.parseMajor("1.001")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCurrencyMismatch() {
        Money(100, "INR") + Money(100, "USD")
    }
}

