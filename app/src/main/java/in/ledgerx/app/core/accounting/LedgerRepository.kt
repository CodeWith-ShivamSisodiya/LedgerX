package com.ledgerx.app.core.accounting

import androidx.room.withTransaction
import com.ledgerx.app.core.database.AccountBalance
import com.ledgerx.app.core.database.AccountEntity
import com.ledgerx.app.core.database.AuditLogEntity
import com.ledgerx.app.core.database.CategoryEntity
import com.ledgerx.app.core.database.LedgerDao
import com.ledgerx.app.core.database.LedgerPostingEntity
import com.ledgerx.app.core.database.LedgerXDatabase
import com.ledgerx.app.core.database.TransactionEntity
import com.ledgerx.app.core.database.TransactionRow
import com.ledgerx.app.core.model.Money
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.util.UUID
import java.time.LocalDate
import java.time.ZoneId
import com.ledgerx.app.core.database.MonthlyTotals
import com.ledgerx.app.core.database.NamedAmount
import com.ledgerx.app.core.database.MonthlySpend
import androidx.paging.Pager
import androidx.paging.PagingConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LedgerRepository @Inject constructor(
    private val database: LedgerXDatabase,
    private val dao: LedgerDao,
) {
    data class ExpenseSplit(val categoryId: String, val amount: Money)
    private val clock: Clock = Clock.systemUTC()

    val accountCount: Flow<Int> = dao.observeAccountCount()
    val accountBalances: Flow<List<AccountBalance>> = dao.observeAccountBalances()
    val recentTransactions: Flow<List<TransactionRow>> = dao.observeRecentTransactions(500)
    val monthlyTotals: Flow<MonthlyTotals> = dao.observeMonthlyTotals(LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()
    private val analyticsSinceMillis = LocalDate.now().minusMonths(12).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val expenseByCategory: Flow<List<NamedAmount>> = dao.observeExpenseByCategory(analyticsSinceMillis)
    val expenseByMerchant: Flow<List<NamedAmount>> = dao.observeExpenseByMerchant(analyticsSinceMillis)
    val monthlySpend: Flow<List<MonthlySpend>> = dao.observeMonthlySpend(analyticsSinceMillis)
    val expenseComposition: Flow<List<NamedAmount>> = dao.observeExpenseComposition(analyticsSinceMillis)
    val monthlyLedgerNetWorthMovement: Flow<Long> = dao.observeLedgerNetWorthMovement(LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
    fun pagedTransactions(query: String, type: String) = Pager(PagingConfig(pageSize = 50, initialLoadSize = 100, prefetchDistance = 15, enablePlaceholders = false)) { dao.pagingTransactions(query.trim(), type) }.flow

    suspend fun initialize() = database.withTransaction {
        dao.insertCategories(
                listOf(
                    CategoryEntity("food", "Food & dining", "EXPENSE", "restaurant", false),
                    CategoryEntity("groceries", "Groceries", "EXPENSE", "cart", false),
                    CategoryEntity("transport", "Transport", "EXPENSE", "directions_car", false),
                    CategoryEntity("housing", "Housing", "EXPENSE", "home", false),
                    CategoryEntity("utilities", "Utilities", "EXPENSE", "bolt", false),
                    CategoryEntity("health", "Health", "EXPENSE", "health", false),
                    CategoryEntity("shopping", "Shopping", "EXPENSE", "bag", false),
                    CategoryEntity("education", "Education", "EXPENSE", "school", false),
                    CategoryEntity("entertainment", "Entertainment", "EXPENSE", "movie", false),
                    CategoryEntity("insurance", "Insurance", "EXPENSE", "shield", false),
                    CategoryEntity("maintenance", "Maintenance", "EXPENSE", "build", false),
                    CategoryEntity("taxes", "Taxes", "EXPENSE", "receipt", false),
                    CategoryEntity("fees", "Fees", "EXPENSE", "payments", false),
                    CategoryEntity("salary", "Salary", "INCOME", "work", false),
                    CategoryEntity("freelance", "Freelance", "INCOME", "laptop", false),
                    CategoryEntity("interest", "Interest", "INCOME", "percent", false),
                    CategoryEntity("dividend", "Dividends", "INCOME", "payments", false),
                    CategoryEntity("rent_income", "Rent", "INCOME", "home", false),
                    CategoryEntity("pension", "Pension", "INCOME", "account_balance", false),
                    CategoryEntity("business", "Business", "INCOME", "store", false),
                    CategoryEntity("bonus", "Bonus", "INCOME", "celebration", false),
                    CategoryEntity("trading", "Trading P&L", "INCOME", "show_chart", false),
                    CategoryEntity("reimbursement", "Reimbursement", "INCOME", "replay", false),
                )
            )
    }

    suspend fun createAccount(name: String, kind: String, openingBalance: Money): String =
        database.withTransaction {
            require(name.isNotBlank()) { "Account name is required" }
            val now = clock.millis()
            val accountId = UUID.randomUUID().toString()
            dao.insertAccount(AccountEntity(accountId, name.trim(), kind, openingBalance.currencyCode, createdAtEpochMillis = now))
            if (openingBalance.minor != 0L) {
                val accountOpeningMinor = if (kind == "CREDIT_CARD") -openingBalance.minor else openingBalance.minor
                recordBalancedTransaction(
                    type = "ADJUSTMENT",
                    amount = openingBalance,
                    categoryId = null,
                    merchant = null,
                    note = "Opening balance",
                    postings = listOf(
                        Posting(accountId, if (kind == "CREDIT_CARD") "LIABILITY:$accountId" else "ASSET:$accountId", accountOpeningMinor),
                        Posting(null, "EQUITY:OPENING_BALANCE", -accountOpeningMinor),
                    ),
                    now = now,
                )
            }
            audit("ACCOUNT", accountId, "CREATE", "Created $kind account", now)
            accountId
        }

    suspend fun createCategory(name: String, kind: String, parentCategoryId: String? = null): String = database.withTransaction {
        require(name.isNotBlank() && kind in setOf("EXPENSE", "INCOME"))
        parentCategoryId?.let { parent -> require(dao.categories(kind).any { it.id == parent }) { "Parent category must exist and use the same type" } }
        val id = "user-${UUID.randomUUID()}"
        dao.insertCategory(CategoryEntity(id, name.trim(), kind, "label", true, parentCategoryId))
        audit("CATEGORY", id, "CREATE", name.trim(), clock.millis())
        id
    }

    suspend fun recordExpense(accountId: String, amount: Money, categoryId: String, merchant: String?, note: String?, origin: String = "MANUAL", occurredAtEpochMillis: Long? = null) =
        database.withTransaction {
            require(amount.minor > 0) { "Expense must be greater than zero" }
            val account = requireNotNull(dao.account(accountId)) { "Account not found" }
            require(account.currencyCode == amount.currencyCode) { "Currency mismatch" }
            val now = occurredAtEpochMillis ?: clock.millis()
            recordBalancedTransaction(
                "EXPENSE", amount, categoryId, merchant, note,
                listOf(
                    Posting(accountId, accountLedgerCode(account), -amount.minor),
                    Posting(null, "EXPENSE:$categoryId", amount.minor),
                ), now, origin,
            )
        }

    suspend fun recordIncome(accountId: String, amount: Money, categoryId: String, merchant: String?, note: String?, origin: String = "MANUAL", occurredAtEpochMillis: Long? = null) =
        database.withTransaction {
            require(amount.minor > 0) { "Income must be greater than zero" }
            val account = requireNotNull(dao.account(accountId)) { "Account not found" }
            require(account.currencyCode == amount.currencyCode) { "Currency mismatch" }
            val now = occurredAtEpochMillis ?: clock.millis()
            recordBalancedTransaction(
                "INCOME", amount, categoryId, merchant, note,
                listOf(
                    Posting(accountId, accountLedgerCode(account), amount.minor),
                    Posting(null, "INCOME:$categoryId", -amount.minor),
                ), now, origin,
            )
        }

    suspend fun recordGrossIncome(accountId: String, gross: Money, takeHome: Money, deductions: Money, taxWithheld: Money, categoryId: String, source: String?, note: String?, occurredAtEpochMillis: Long): String = database.withTransaction {
        require(listOf(takeHome, deductions, taxWithheld).all { it.currencyCode == gross.currencyCode && it.minor >= 0 }) { "Income components must use the same currency and cannot be negative" }
        require(gross.minor > 0 && Math.addExact(takeHome.minor, Math.addExact(deductions.minor, taxWithheld.minor)) == gross.minor) { "Gross income must equal take-home, deductions, and tax withheld" }
        val account = requireNotNull(dao.account(accountId)) { "Account not found" }
        require(account.currencyCode == gross.currencyCode) { "Currency mismatch" }
        recordBalancedTransaction("INCOME", gross, categoryId, source, note, buildList {
            if(takeHome.minor>0) add(Posting(accountId, accountLedgerCode(account), takeHome.minor))
            if(taxWithheld.minor>0) add(Posting(null, "ASSET:TDS_RECEIVABLE", taxWithheld.minor))
            if(deductions.minor>0) add(Posting(null, "ASSET:PAYROLL_DEDUCTIONS", deductions.minor))
            add(Posting(null, "INCOME:$categoryId", -gross.minor))
        }, occurredAtEpochMillis)
    }

    suspend fun recordRefund(accountId: String, amount: Money, categoryId: String, merchant: String?, note: String?, occurredAtEpochMillis: Long? = null) =
        database.withTransaction {
            require(amount.minor > 0) { "Refund must be greater than zero" }
            val account = requireNotNull(dao.account(accountId)) { "Account not found" }
            require(account.currencyCode == amount.currencyCode) { "Currency mismatch" }
            recordBalancedTransaction("REFUND", amount, categoryId, merchant, note,
                listOf(Posting(accountId, accountLedgerCode(account), amount.minor), Posting(null, "EXPENSE:$categoryId", -amount.minor)), occurredAtEpochMillis ?: clock.millis())
        }

    suspend fun recordReimbursement(accountId: String, amount: Money, categoryId: String, merchant: String?, note: String?, occurredAtEpochMillis: Long? = null) =
        database.withTransaction {
            require(amount.minor > 0) { "Reimbursement must be greater than zero" }
            val account = requireNotNull(dao.account(accountId)) { "Account not found" }
            require(account.currencyCode == amount.currencyCode) { "Currency mismatch" }
            recordBalancedTransaction("REIMBURSEMENT", amount, categoryId, merchant, note,
                listOf(Posting(accountId, accountLedgerCode(account), amount.minor), Posting(null, "EXPENSE:$categoryId", -amount.minor)), occurredAtEpochMillis ?: clock.millis())
        }

    suspend fun recordSplitExpense(accountId: String, splits: List<ExpenseSplit>, merchant: String?, note: String?): String = database.withTransaction {
        require(splits.size >= 2 && splits.all { it.amount.minor > 0 }) { "A split expense needs at least two positive parts" }
        val account = requireNotNull(dao.account(accountId)) { "Account not found" }
        require(splits.all { it.amount.currencyCode == account.currencyCode }) { "Currency mismatch" }
        val total = splits.fold(0L) { sum, split -> Math.addExact(sum, split.amount.minor) }
        recordBalancedTransaction("EXPENSE", Money(total, account.currencyCode), null, merchant, note,
            listOf(Posting(accountId, accountLedgerCode(account), -total)) + splits.map { Posting(null, "EXPENSE:${it.categoryId}", it.amount.minor) }, clock.millis())
    }

    suspend fun recordImported(accountId: String, signedAmountMinor: Long, categoryId: String, description: String?, occurredAtEpochMillis: Long): String =
        database.withTransaction {
            require(signedAmountMinor != 0L && signedAmountMinor != Long.MIN_VALUE) { "Imported amount is outside the supported range" }
            val account = requireNotNull(dao.account(accountId)) { "Account not found" }
            val amount = Money(kotlin.math.abs(signedAmountMinor), account.currencyCode)
            val expense = signedAmountMinor < 0
            recordBalancedTransaction(
                type = if (expense) "EXPENSE" else "INCOME",
                amount = amount,
                categoryId = categoryId,
                merchant = description,
                note = "Imported statement entry",
                postings = if (expense) listOf(
                    Posting(accountId, accountLedgerCode(account), -amount.minor),
                    Posting(null, "EXPENSE:$categoryId", amount.minor),
                ) else listOf(
                    Posting(accountId, accountLedgerCode(account), amount.minor),
                    Posting(null, "INCOME:$categoryId", -amount.minor),
                ),
                now = occurredAtEpochMillis,
                origin = "IMPORTED",
            )
        }

    suspend fun transfer(sourceAccountId: String, destinationAccountId: String, amount: Money, note: String?) =
        database.withTransaction {
            require(sourceAccountId != destinationAccountId) { "Choose two different accounts" }
            require(amount.minor > 0) { "Transfer must be greater than zero" }
            val source = requireNotNull(dao.account(sourceAccountId))
            val destination = requireNotNull(dao.account(destinationAccountId))
            require(source.currencyCode == amount.currencyCode && destination.currencyCode == amount.currencyCode) {
                "Cross-currency transfers require an explicit exchange rate"
            }
            val now = clock.millis()
            recordBalancedTransaction(
                "TRANSFER", amount, null, null, note,
                listOf(
                    Posting(sourceAccountId, accountLedgerCode(source), -amount.minor),
                    Posting(destinationAccountId, accountLedgerCode(destination), amount.minor),
                ), now,
            )
        }

    suspend fun recordInvestmentPurchase(accountId: String, amount: Money, instrumentId: String, note: String?, occurredAtEpochMillis: Long? = null): String =
        database.withTransaction {
            require(amount.minor > 0) { "Investment cost must be greater than zero" }
            val account = requireNotNull(dao.account(accountId)) { "Account not found" }
            require(account.currencyCode == amount.currencyCode) { "Currency mismatch" }
            recordBalancedTransaction(
                type = "INVESTMENT",
                amount = amount,
                categoryId = null,
                merchant = instrumentId,
                note = note,
                postings = listOf(
                    Posting(accountId, accountLedgerCode(account), -amount.minor),
                    Posting(null, "ASSET:INVESTMENT:$instrumentId", amount.minor),
                ),
                now = occurredAtEpochMillis ?: clock.millis(),
            )
        }

    suspend fun recordInvestmentSale(accountId: String, gross: Money, fee: Money, costBasis: Money, instrumentId: String, note: String?, occurredAtEpochMillis: Long? = null): String = database.withTransaction {
        require(gross.minor > 0 && fee.minor >= 0 && fee.minor < gross.minor && costBasis.minor > 0 && gross.currencyCode == fee.currencyCode && gross.currencyCode == costBasis.currencyCode)
        val account = requireNotNull(dao.account(accountId)) { "Account not found" }
        require(account.currencyCode == gross.currencyCode) { "Currency mismatch" }
        val net = gross.minor - fee.minor
        val realized = gross.minor - costBasis.minor
        recordBalancedTransaction("DIVESTMENT", gross, null, instrumentId, note,
            buildList {
                add(Posting(accountId, accountLedgerCode(account), net))
                if (fee.minor > 0) add(Posting(null, "EXPENSE:INVESTMENT_FEE", fee.minor))
                add(Posting(null, "ASSET:INVESTMENT:$instrumentId", -costBasis.minor))
                if (realized > 0) add(Posting(null, "INCOME:REALIZED_CAPITAL_GAIN", -realized))
                if (realized < 0) add(Posting(null, "EXPENSE:REALIZED_CAPITAL_LOSS", -realized))
            }, occurredAtEpochMillis ?: clock.millis())
    }

    suspend fun recordDividend(accountId: String, gross: Money, taxWithheld: Money, instrumentId: String, occurredAtEpochMillis: Long): String = database.withTransaction {
        require(gross.minor > 0 && taxWithheld.minor >= 0 && taxWithheld.minor < gross.minor && gross.currencyCode == taxWithheld.currencyCode)
        val account = requireNotNull(dao.account(accountId)) { "Account not found" }
        require(account.currencyCode == gross.currencyCode) { "Currency mismatch" }
        val net = gross.minor - taxWithheld.minor
        recordBalancedTransaction(
            "DIVIDEND", gross, "dividend", instrumentId, "Dividend received",
            buildList {
                add(Posting(accountId, accountLedgerCode(account), net))
                if (taxWithheld.minor > 0) add(Posting(null, "ASSET:TDS_RECEIVABLE", taxWithheld.minor))
                add(Posting(null, "INCOME:dividend", -gross.minor))
            }, occurredAtEpochMillis,
        )
    }

    suspend fun recordAssetTransfer(accountId: String, amount: Money, assetLedgerCode: String, note: String?): String = database.withTransaction {
        require(amount.minor > 0) { "Asset transfer must be greater than zero" }
        val account = requireNotNull(dao.account(accountId)) { "Account not found" }
        require(account.currencyCode == amount.currencyCode) { "Currency mismatch" }
        recordBalancedTransaction("INVESTMENT", amount, null, assetLedgerCode, note,
            listOf(Posting(accountId, accountLedgerCode(account), -amount.minor), Posting(null, "ASSET:$assetLedgerCode", amount.minor)), clock.millis())
    }

    suspend fun recordLoanPayment(accountId: String, loanId: String, principal: Money, interest: Money, fee: Money, note: String?, occurredAtEpochMillis: Long? = null): String = database.withTransaction {
        require(listOf(principal, interest, fee).all { it.minor >= 0 && it.currencyCode == principal.currencyCode })
        val total = Math.addExact(principal.minor, Math.addExact(interest.minor, fee.minor))
        require(total > 0 && principal.minor > 0) { "Loan payment and principal component must be positive" }
        val account = requireNotNull(dao.account(accountId)) { "Account not found" }
        require(account.currencyCode == principal.currencyCode) { "Currency mismatch" }
        val postings = buildList {
            add(Posting(accountId, accountLedgerCode(account), -total))
            add(Posting(null, "LIABILITY:$loanId", principal.minor))
            if (interest.minor > 0) add(Posting(null, "EXPENSE:LOAN_INTEREST", interest.minor))
            if (fee.minor > 0) add(Posting(null, "EXPENSE:LOAN_FEE", fee.minor))
        }
        recordBalancedTransaction("LOAN_REPAYMENT", Money(total, principal.currencyCode), null, loanId, note, postings, occurredAtEpochMillis ?: clock.millis())
    }

    suspend fun reverseTransaction(transactionId: String): String = database.withTransaction {
        val original = requireNotNull(dao.transaction(transactionId)) { "Transaction not found" }
        require(original.status == "CONFIRMED") { "Only confirmed transactions can be reversed" }
        require(original.type !in setOf("INVESTMENT", "DIVESTMENT", "LOAN_REPAYMENT")) { "Reverse this item from its wealth workflow so holdings or debt stay consistent" }
        require(original.reconciliationStatus != "RECONCILED") { "Reconciled transactions cannot be changed; create an adjustment instead" }
        val originalPostings = dao.postings(transactionId)
        require(originalPostings.isNotEmpty()) { "Transaction has no ledger postings" }
        val now = clock.millis()
        val reversalId = recordBalancedTransaction(
            type = "REVERSAL",
            amount = Money(original.amountMinor, original.currencyCode),
            categoryId = original.categoryId,
            merchant = original.merchant,
            note = "Reversal of ${original.id}: ${original.note.orEmpty()}",
            postings = originalPostings.map { Posting(it.accountId, it.ledgerCode, Math.negateExact(it.signedMinor)) },
            now = now,
            origin = "MANUAL",
            reversedTransactionId = original.id,
        )
        dao.updateTransactionStatus(original.id, "REVERSED", now)
        audit("TRANSACTION", original.id, "REVERSE", reversalId, now)
        reversalId
    }

    suspend fun replaceSimpleTransaction(transactionId: String, amount: Money, categoryId: String, merchant: String?, note: String?): String = database.withTransaction {
        val original = requireNotNull(dao.transaction(transactionId)) { "Transaction not found" }
        require(original.type in setOf("EXPENSE", "INCOME", "REFUND", "REIMBURSEMENT")) { "This transaction must be edited from its dedicated workflow" }
        require(original.reconciliationStatus != "RECONCILED") { "Reconciled transactions cannot be edited; create an adjustment instead" }
        val accountId = dao.postings(transactionId).mapNotNull { it.accountId }.distinct().singleOrNull()
            ?: error("A simple transaction must affect exactly one account")
        reverseTransaction(transactionId)
        when (original.type) {
            "EXPENSE" -> recordExpense(accountId, amount, categoryId, merchant, note, occurredAtEpochMillis = original.occurredAtEpochMillis)
            "REFUND" -> recordRefund(accountId, amount, categoryId, merchant, note, original.occurredAtEpochMillis)
            "REIMBURSEMENT" -> recordReimbursement(accountId, amount, categoryId, merchant, note, original.occurredAtEpochMillis)
            else -> recordIncome(accountId, amount, categoryId, merchant, note, occurredAtEpochMillis = original.occurredAtEpochMillis)
        }
    }

    private suspend fun recordBalancedTransaction(
        type: String,
        amount: Money,
        categoryId: String?,
        merchant: String?,
        note: String?,
        postings: List<Posting>,
        now: Long,
        origin: String = "MANUAL",
        reversedTransactionId: String? = null,
    ): String {
        LedgerMath.requireBalanced(postings.map { it.signedMinor })
        val id = UUID.randomUUID().toString()
        dao.insertTransaction(
            TransactionEntity(
                id, type, amount.minor, amount.currencyCode, categoryId,
                merchant?.trim()?.takeIf(String::isNotEmpty), note?.trim()?.takeIf(String::isNotEmpty),
                origin = origin, occurredAtEpochMillis = now, createdAtEpochMillis = clock.millis(), modifiedAtEpochMillis = clock.millis(), reversedTransactionId = reversedTransactionId,
            )
        )
        dao.insertPostings(postings.map {
            LedgerPostingEntity(UUID.randomUUID().toString(), id, it.accountId, it.ledgerCode, it.signedMinor, amount.currencyCode)
        })
        check(dao.postingSum(id) == 0L) { "Ledger integrity failure" }
        audit("TRANSACTION", id, "CREATE", type, clock.millis())
        return id
    }

    private suspend fun audit(type: String, id: String, action: String, detail: String, now: Long) {
        dao.insertAudit(AuditLogEntity(UUID.randomUUID().toString(), type, id, action, detail, now))
    }

    private fun accountLedgerCode(account: AccountEntity) = if (account.kind == "CREDIT_CARD") "LIABILITY:${account.id}" else "ASSET:${account.id}"

    private data class Posting(val accountId: String?, val ledgerCode: String, val signedMinor: Long)
}
