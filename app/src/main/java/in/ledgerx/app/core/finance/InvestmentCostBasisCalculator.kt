package com.ledgerx.app.core.finance

import java.math.BigDecimal
import java.math.RoundingMode

data class CostBasisLot(val id: String, val quantityMicros: Long, val remainingCostMinor: Long)
data class LotAllocation(val lotId: String, val quantityMicros: Long, val costMinor: Long, val proceedsMinor: Long)
data class CostBasisResult(val costBasisMinor: Long, val grossProceedsMinor: Long, val allocations: List<LotAllocation>) {
    val realizedGainMinor: Long get() = Math.subtractExact(grossProceedsMinor, costBasisMinor)
}

object InvestmentCostBasisCalculator {
    fun fifo(lots: List<CostBasisLot>, saleQuantityMicros: Long, grossProceedsMinor: Long): CostBasisResult {
        require(saleQuantityMicros > 0 && grossProceedsMinor > 0)
        require(lots.all { it.quantityMicros > 0 && it.remainingCostMinor >= 0 })
        require(lots.sumOf { it.quantityMicros } >= saleQuantityMicros) { "Sale quantity exceeds available cost-basis lots" }
        var quantityRemaining = saleQuantityMicros
        var proceedsRemaining = grossProceedsMinor
        var costBasis = 0L
        val allocations = mutableListOf<LotAllocation>()
        lots.forEach { lot ->
            if (quantityRemaining > 0) {
                val consumed = minOf(quantityRemaining, lot.quantityMicros)
                val cost = if (consumed == lot.quantityMicros) lot.remainingCostMinor else proportional(lot.remainingCostMinor, consumed, lot.quantityMicros)
                val proceeds = if (consumed == quantityRemaining) proceedsRemaining else proportional(grossProceedsMinor, consumed, saleQuantityMicros)
                allocations += LotAllocation(lot.id, consumed, cost, proceeds)
                quantityRemaining -= consumed
                proceedsRemaining -= proceeds
                costBasis = Math.addExact(costBasis, cost)
            }
        }
        check(quantityRemaining == 0L && proceedsRemaining == 0L)
        return CostBasisResult(costBasis, grossProceedsMinor, allocations)
    }

    private fun proportional(value: Long, numerator: Long, denominator: Long): Long =
        BigDecimal(value).multiply(BigDecimal(numerator)).divide(BigDecimal(denominator), 0, RoundingMode.HALF_UP).longValueExact()
}
