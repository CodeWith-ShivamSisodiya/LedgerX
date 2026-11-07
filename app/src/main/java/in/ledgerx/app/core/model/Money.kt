package in.ledgerx.app.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class Money(val minor: Long, val currencyCode: String = "INR") {
    operator fun plus(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Currency mismatch" }
        return copy(minor = Math.addExact(minor, other.minor))
    }

    operator fun minus(other: Money): Money = plus(other.copy(minor = -other.minor))

    fun format(locale: Locale = Locale("en", "IN")): String =
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(currencyCode)
            maximumFractionDigits = 2
        }.format(BigDecimal.valueOf(minor, 2))

    companion object {
        fun parseMajor(value: String, currencyCode: String = "INR"): Money {
            val normalized = value.trim().replace(",", "")
            val major = normalized.toBigDecimalOrNull() ?: error("Enter a valid amount")
            require(major.signum() >= 0) { "Amount cannot be negative" }
            val minor = major.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
            return Money(minor, currencyCode)
        }
    }
}

