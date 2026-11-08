package com.ledgerx.app.core.planning

import java.time.LocalDate

object RecurrenceEngine {
    fun next(date: LocalDate, frequency: String, interval: Int): LocalDate {
        require(interval > 0)
        return when (frequency) {
            "DAILY" -> date.plusDays(interval.toLong())
            "WEEKLY" -> date.plusWeeks(interval.toLong())
            "MONTHLY" -> date.plusMonths(interval.toLong())
            "QUARTERLY" -> date.plusMonths(interval.toLong() * 3)
            "YEARLY" -> date.plusYears(interval.toLong())
            else -> error("Unsupported frequency: $frequency")
        }
    }

    fun dueDates(firstDue: LocalDate, through: LocalDate, frequency: String, interval: Int, maximum: Int = 1_000): List<LocalDate> {
        val result = mutableListOf<LocalDate>()
        var cursor = firstDue
        while (!cursor.isAfter(through) && result.size < maximum) {
            result += cursor
            cursor = next(cursor, frequency, interval)
        }
        return result
    }
}
