package in.ledgerx.app.core.accounting

import androidx.room.withTransaction
import in.ledgerx.app.core.database.AccountBalance
import in.ledgerx.app.core.database.AccountEntity
import in.ledgerx.app.core.database.AuditLogEntity
import in.ledgerx.app.core.database.CategoryEntity
import in.ledgerx.app.core.database.LedgerDao
import in.ledgerx.app.core.database.LedgerPostingEntity
import in.ledgerx.app.core.database.LedgerXDatabase
import in.ledgerx.app.core.database.TransactionEntity
import in.ledgerx.app.core.database.TransactionRow
import in.ledgerx.app.core.model.Money
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LedgerRepository @Inject constructor(
    private val database: LedgerXDatabase,
    private val dao: LedgerDao,
) {
    private val clock: Clock = Clock.systemUTC()

    val accountCount: Flow<Int> = dao.observeAccountCount()
    val accountBalances: Flow<List<AccountBalance>> = dao.observeAccountBalances()
    val recentTransactions: Flow<List<TransactionRow>> = dao.observeRecentTransactions(50)

    suspend fun initialize() = database.withTransaction {
        if (dao.categoryCount() == 0) {
            dao.insertCategories(
                listOf(
                    CategoryEntity("food", "Food & dining", "EXPENSE", "restaurant", false),
                    CategoryEntity("groceries", "Groceries", "EXPENSE", "cart", false),
                    CategoryEntity("transport", "Transport", "EXPENSE", "directions_car", false),
                    CategoryEntity("housing", "Housing", "EXPENSE", "home", false),
                    CategoryEntity("utilities", "Utilities", "EXPENSE", "bolt", false),
                    CategoryEntity("health", "Health", "EXPENSE", "health", false),
                    CategoryEntity("shopping", "Shopping", "EXPENSE", "bag", false),
                    CategoryEntity("salary", "Salary", "INCOME", "work", false),
                    CategoryEntity("freelance", "Freelance", "INCOME", "laptop", false),
                    CategoryEntity("interest", "Interest", "INCOME", "percent", false),
                )
            )
        }
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

    suspend fun recordExpense(accountId: String, amount: Money, categoryId: String, merchant: String?, note: String?) =
        database.withTransaction {
            require(amount.minor > 0) { "Expense must be greater than zero" }
            val account = requireNotNull(dao.account(accountId)) { "Account not found" }
            require(account.currencyCode == amount.currencyCode) { "Currency mismatch" }
            val now = clock.millis()
            recordBalancedTransaction(
                "EXPENSE", amount, categoryId, merchant, note,
                listOf(
                    Posting(accountId, "ASSET:$accountId", -amount.minor),
                    Posting(null, "EXPENSE:$categoryId", amount.minor),
                ), now,
            )
        }

    suspend fun recordIncome(accountId: String, amount: Money, categoryId: String, merchant: String?, note: String?) =
        database.withTransaction {
            require(amount.minor > 0) { "Income must be greater than zero" }
            val account = requireNotNull(dao.account(accountId)) { "Account not found" }
            require(account.currencyCode == amount.currencyCode) { "Currency mismatch" }
            val now = clock.millis()
            recordBalancedTransaction(
                "INCOME", amount, categoryId, merchant, note,
                listOf(
                    Posting(accountId, "ASSET:$accountId", amount.minor),
                    Posting(null, "INCOME:$categoryId", -amount.minor),
                ), now,
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
                    Posting(sourceAccountId, "ASSET:$sourceAccountId", -amount.minor),
                    Posting(destinationAccountId, "ASSET:$destinationAccountId", amount.minor),
                ), now,
            )
        }

    private suspend fun recordBalancedTransaction(
        type: String,
        amount: Money,
        categoryId: String?,
        merchant: String?,
        note: String?,
        postings: List<Posting>,
        now: Long,
    ): String {
        LedgerMath.requireBalanced(postings.map { it.signedMinor })
        val id = UUID.randomUUID().toString()
        dao.insertTransaction(
            TransactionEntity(
                id, type, amount.minor, amount.currencyCode, categoryId,
                merchant?.trim()?.takeIf(String::isNotEmpty), note?.trim()?.takeIf(String::isNotEmpty),
                occurredAtEpochMillis = now, createdAtEpochMillis = now, modifiedAtEpochMillis = now,
            )
        )
        dao.insertPostings(postings.map {
            LedgerPostingEntity(UUID.randomUUID().toString(), id, it.accountId, it.ledgerCode, it.signedMinor, amount.currencyCode)
        })
        check(dao.postingSum(id) == 0L) { "Ledger integrity failure" }
        audit("TRANSACTION", id, "CREATE", type, now)
        return id
    }

    private suspend fun audit(type: String, id: String, action: String, detail: String, now: Long) {
        dao.insertAudit(AuditLogEntity(UUID.randomUUID().toString(), type, id, action, detail, now))
    }

    private data class Posting(val accountId: String?, val ledgerCode: String, val signedMinor: Long)
}
