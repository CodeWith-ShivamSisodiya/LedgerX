package com.ledgerx.app.core.planning

import androidx.room.withTransaction
import com.ledgerx.app.core.accounting.LedgerRepository
import com.ledgerx.app.core.database.LedgerXDatabase
import com.ledgerx.app.core.database.PlanningDao
import com.ledgerx.app.core.database.ScheduledOccurrenceEntity
import com.ledgerx.app.core.model.Money
import java.time.LocalDate
import java.util.UUID
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringProcessor @Inject constructor(
    private val database: LedgerXDatabase,
    private val dao: PlanningDao,
    private val ledger: LedgerRepository,
) {
    suspend fun processDue(today: LocalDate = LocalDate.now()): Int {
        var processed = 0
        for (template in dao.dueRecurring(today.toEpochDay())) {
            var current = template
            while (current.nextDueEpochDay <= today.toEpochDay()) {
                val dueDate = LocalDate.ofEpochDay(current.nextDueEpochDay)
                val nextDue = RecurrenceEngine.next(dueDate, current.frequency, current.intervalCount).toEpochDay()
                database.withTransaction {
                val occurrenceId = "${template.id}:${dueDate.toEpochDay()}"
                if (dao.occurrence(occurrenceId) == null) {
                    if (current.type == "INCOME") {
                        dao.saveOccurrence(ScheduledOccurrenceEntity(occurrenceId, template.id, dueDate.toEpochDay(), "PENDING", null))
                    } else {
                        val money = Money(current.amountMinor, current.currencyCode)
                        val occurredAt = dueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val transactionId = ledger.recordExpense(current.accountId, money, requireNotNull(current.categoryId), current.merchant, "Recurring transaction", "RECURRING", occurredAt)
                        dao.saveOccurrence(ScheduledOccurrenceEntity(occurrenceId, template.id, dueDate.toEpochDay(), "CONFIRMED", transactionId))
                    }
                    processed++
                }
                    dao.saveRecurring(current.copy(nextDueEpochDay = nextDue))
                }
                current = current.copy(nextDueEpochDay = nextDue)
            }
        }
        return processed
    }

    suspend fun confirmIncome(occurrenceId: String): String = database.withTransaction {
        val occurrence = requireNotNull(dao.occurrence(occurrenceId)) { "Scheduled income not found" }
        require(occurrence.status == "PENDING" && occurrence.transactionId == null) { "Scheduled income is already resolved" }
        val template = requireNotNull(dao.recurring(occurrence.templateId)) { "Recurring template not found" }
        require(template.type == "INCOME") { "Only scheduled income requires receipt confirmation" }
        val money = Money(template.amountMinor, template.currencyCode)
        val occurredAt = LocalDate.ofEpochDay(occurrence.dueEpochDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val transactionId = ledger.recordIncome(template.accountId, money, requireNotNull(template.categoryId), template.merchant, "Confirmed recurring income", "RECURRING", occurredAt)
        dao.saveOccurrence(occurrence.copy(status = "CONFIRMED", transactionId = transactionId))
        transactionId
    }
}
