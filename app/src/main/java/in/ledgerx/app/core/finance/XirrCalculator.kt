package com.ledgerx.app.core.finance

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow

data class DatedCashFlow(val date: LocalDate, val amountMinor: Long)

object XirrCalculator {
    fun calculate(flows: List<DatedCashFlow>, tolerance: Double = 1e-8): Double {
        require(flows.size >= 2) { "At least two cash flows are required" }
        require(flows.any { it.amountMinor < 0 } && flows.any { it.amountMinor > 0 }) {
            "XIRR requires both an outflow and an inflow"
        }
        val ordered = flows.sortedBy { it.date }
        val start = ordered.first().date
        fun npv(rate: Double): Double = ordered.sumOf {
            val years = ChronoUnit.DAYS.between(start, it.date) / 365.0
            it.amountMinor / (1.0 + rate).pow(years)
        }

        var low = -0.9999
        var high = 1.0
        var lowValue = npv(low)
        var highValue = npv(high)
        while (lowValue * highValue > 0 && high < 1_000.0) {
            high *= 2.0
            highValue = npv(high)
        }
        require(lowValue * highValue <= 0) { "Cash flows do not produce a bracketed XIRR" }

        repeat(256) {
            val middle = (low + high) / 2.0
            val value = npv(middle)
            if (abs(value) < tolerance) return middle
            if (lowValue * value <= 0) {
                high = middle
            } else {
                low = middle
                lowValue = value
            }
        }
        return (low + high) / 2.0
    }
}

