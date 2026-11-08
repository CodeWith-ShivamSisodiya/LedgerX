package com.ledgerx.app.core.accounting

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ledgerx.app.core.database.LedgerXDatabase
import com.ledgerx.app.core.model.Money
import com.ledgerx.app.core.planning.ExtendedFinanceRepository
import com.ledgerx.app.core.planning.PlanningRepository
import com.ledgerx.app.core.planning.RecurringProcessor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class LedgerAccountingInstrumentedTest {
    @Test fun detailedIncomeCreditsOnlyTakeHomeCashAndKeepsGrossBalanced() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, LedgerXDatabase::class.java).build()
        val repository = LedgerRepository(database, database.ledgerDao())
        repository.initialize()
        val bank = repository.createAccount("Bank", "SAVINGS", Money(0))
        val id = repository.recordGrossIncome(bank, Money(1_00_000_00), Money(80_000_00), Money(15_000_00), Money(5_000_00), "salary", "Employer", null, 1_700_000_000_000)
        assertEquals(80_000_00L, database.ledgerDao().accountBalance(bank))
        assertEquals(0L, database.ledgerDao().postingSum(id))
        assertTrue(database.ledgerDao().postings(id).any { it.ledgerCode == "ASSET:TDS_RECEIVABLE" && it.signedMinor == 5_000_00L })
        database.close()
    }

    @Test fun creditCardPurchaseCreatesLiabilityAndSettlementDoesNotDuplicateExpense() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, LedgerXDatabase::class.java).build()
        val repository = LedgerRepository(database, database.ledgerDao())
        repository.initialize()
        val cash = repository.createAccount("Bank", "SAVINGS", Money(10_000_00))
        val card = repository.createAccount("Card", "CREDIT_CARD", Money(1_000_00))

        val purchase = repository.recordExpense(card, Money(500_00), "shopping", "Merchant", null)
        assertEquals(-1_500_00L, database.ledgerDao().accountBalance(card))
        assertTrue(database.ledgerDao().postings(purchase).any { it.ledgerCode == "LIABILITY:$card" && it.signedMinor == -500_00L })

        val settlement = repository.transfer(cash, card, Money(1_000_00), "Card bill")
        assertEquals(9_000_00L, database.ledgerDao().accountBalance(cash))
        assertEquals(-500_00L, database.ledgerDao().accountBalance(card))
        assertTrue(database.ledgerDao().postings(settlement).none { it.ledgerCode.startsWith("EXPENSE:") })
        database.close()
    }

    @Test fun recurringExpensePostsButScheduledIncomeWaitsForConfirmation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, LedgerXDatabase::class.java).build()
        val ledger = LedgerRepository(database, database.ledgerDao())
        val planning = PlanningRepository(database.planningDao(), ledger, database, database.ledgerDao(), database.extendedDao())
        val processor = RecurringProcessor(database, database.planningDao(), ledger)
        ledger.initialize()
        val bank = ledger.createAccount("Bank", "SAVINGS", Money(1_000_00))
        val today = LocalDate.of(2026, 1, 31)
        planning.addRecurring(bank, "EXPENSE", Money(100_00), "utilities", "Power", "MONTHLY", 1, today)
        planning.addRecurring(bank, "INCOME", Money(500_00), "salary", "Employer", "MONTHLY", 1, today)

        assertEquals(2, processor.processDue(today))
        assertEquals(900_00L, database.ledgerDao().accountBalance(bank))
        val pending = database.planningDao().pendingOccurrences()
        assertEquals(1, pending.size)
        processor.confirmIncome(pending.single().id)
        assertEquals(1_400_00L, database.ledgerDao().accountBalance(bank))
        database.close()
    }

    @Test fun sipDoesNotMoveCashUntilPurchaseIsConfirmed() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, LedgerXDatabase::class.java).build()
        val ledger = LedgerRepository(database, database.ledgerDao())
        val planning = PlanningRepository(database.planningDao(), ledger, database, database.ledgerDao(), database.extendedDao())
        val extended = ExtendedFinanceRepository(context, database, database.ledgerDao(), database.planningDao(), database.extendedDao(), ledger)
        ledger.initialize()
        val bank = ledger.createAccount("Bank", "SAVINGS", Money(10_000_00))
        val date = LocalDate.of(2026, 2, 1)
        val instrument = planning.addInvestment(bank, "Index fund", "INDEX", "MUTUAL_FUND", 1_000_000, Money(100_00), Money(0), date.minusMonths(1))
        val sip = extended.createSip(instrument, bank, Money(200_00), "MONTHLY", 1, date)
        val beforeConfirmation = database.ledgerDao().accountBalance(bank)

        planning.confirmSipPurchase(sip, 2_000_000, Money(100_00), Money(5_00), date)

        assertEquals(beforeConfirmation - 205_00L, database.ledgerDao().accountBalance(bank))
        assertEquals(date.plusMonths(1).toEpochDay(), database.extendedDao().sip(sip)!!.nextDueEpochDay)
        database.close()
    }

    @Test fun dividendSeparatesCashAndTaxWithheldWithoutBreakingLedgerBalance() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, LedgerXDatabase::class.java).build()
        val ledger = LedgerRepository(database, database.ledgerDao())
        val planning = PlanningRepository(database.planningDao(), ledger, database, database.ledgerDao(), database.extendedDao())
        val extended = ExtendedFinanceRepository(context, database, database.ledgerDao(), database.planningDao(), database.extendedDao(), ledger)
        ledger.initialize()
        val bank = ledger.createAccount("Bank", "SAVINGS", Money(10_000_00))
        val instrument = planning.addInvestment(bank, "Listed equity", "EQ", "EQUITY", 1_000_000, Money(100_00), Money(0), LocalDate.of(2026, 1, 1))
        val balanceBefore = database.ledgerDao().accountBalance(bank)

        val dividend = extended.recordDividend(bank, instrument, Money(1_000_00), Money(100_00), LocalDate.of(2026, 2, 1))
        val record = database.extendedDao().dividend(dividend)!!

        assertEquals(balanceBefore + 900_00L, database.ledgerDao().accountBalance(bank))
        assertEquals(1_000_00L, record.grossMinor)
        assertEquals(100_00L, record.taxWithheldMinor)
        assertEquals(0L, database.ledgerDao().postingSum(record.ledgerTransactionId!!))
        database.close()
    }

    @Test fun loanPaymentMarksInstallmentPaidAndAdvancesDueDate() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, LedgerXDatabase::class.java).build()
        val ledger = LedgerRepository(database, database.ledgerDao())
        val planning = PlanningRepository(database.planningDao(), ledger, database, database.ledgerDao(), database.extendedDao())
        ledger.initialize()
        val bank = ledger.createAccount("Bank", "SAVINGS", Money(1_000_000_00))
        val firstDue = LocalDate.of(2026, 4, 5)
        val loan = planning.addLoan("Home loan", "HOME", Money(500_000_00), 850, 12, firstDue)
        val installment = database.extendedDao().nextDueLoanInstallment(loan)!!

        planning.recordLoanPayment(bank, loan, Money(installment.principalMinor), Money(installment.interestMinor), Money(0), firstDue)

        assertEquals("PAID", database.extendedDao().observeLoanSchedule(loan).first().first().status)
        assertEquals(firstDue.plusMonths(1).toEpochDay(), database.planningDao().loan(loan)!!.nextDueEpochDay)
        database.close()
    }
}
