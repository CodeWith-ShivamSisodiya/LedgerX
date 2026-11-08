package com.ledgerx.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingSource

@Dao
interface LedgerDao {
    @Insert suspend fun insertAccount(account: AccountEntity)
    @Insert suspend fun insertTransaction(transaction: TransactionEntity)
    @Insert suspend fun insertPostings(postings: List<LedgerPostingEntity>)
    @Insert suspend fun insertAudit(entry: AuditLogEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertCategories(categories: List<CategoryEntity>)
    @Insert suspend fun insertCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM accounts WHERE archived = 0")
    fun observeAccountCount(): Flow<Int>

    @Query(
        """
        SELECT a.id, a.name, a.kind, a.currencyCode,
               COALESCE(SUM(p.signedMinor), 0) AS balanceMinor
        FROM accounts a
        LEFT JOIN ledger_postings p ON p.accountId = a.id
        WHERE a.archived = 0
        GROUP BY a.id
        ORDER BY a.createdAtEpochMillis
        """
    )
    fun observeAccountBalances(): Flow<List<AccountBalance>>

    @Query(
        """
        SELECT t.id, t.type, t.amountMinor, t.currencyCode, t.categoryId,
               t.merchant, t.note, t.occurredAtEpochMillis, a.name AS accountName,
               t.origin, t.reconciliationStatus
        FROM transactions t
        LEFT JOIN ledger_postings p ON p.transactionId = t.id AND p.accountId IS NOT NULL
        LEFT JOIN accounts a ON a.id = p.accountId
        WHERE t.status = 'CONFIRMED'
        GROUP BY t.id
        ORDER BY t.occurredAtEpochMillis DESC, t.createdAtEpochMillis DESC
        LIMIT :limit
        """
    )
    fun observeRecentTransactions(limit: Int): Flow<List<TransactionRow>>

    @Query("SELECT (SELECT -COALESCE(SUM(p.signedMinor),0) FROM ledger_postings p JOIN transactions t ON t.id=p.transactionId WHERE t.status='CONFIRMED' AND t.type!='REVERSAL' AND t.occurredAtEpochMillis>=:monthStartMillis AND p.ledgerCode LIKE 'INCOME:%') AS incomeMinor, (SELECT COALESCE(SUM(p.signedMinor),0) FROM ledger_postings p JOIN transactions t ON t.id=p.transactionId WHERE t.status='CONFIRMED' AND t.type!='REVERSAL' AND t.occurredAtEpochMillis>=:monthStartMillis AND p.ledgerCode LIKE 'EXPENSE:%') AS expenseMinor, (SELECT COALESCE(SUM(amountMinor),0) FROM transactions WHERE status='CONFIRMED' AND type='INVESTMENT' AND occurredAtEpochMillis>=:monthStartMillis) AS investmentMinor")
    fun observeMonthlyTotals(monthStartMillis: Long): Flow<MonthlyTotals>

    @Query("SELECT COALESCE(c.name, t.categoryId, 'Uncategorised') AS name, COALESCE(SUM(t.amountMinor),0) AS amountMinor FROM transactions t LEFT JOIN categories c ON c.id=t.categoryId WHERE t.status='CONFIRMED' AND t.type='EXPENSE' AND t.occurredAtEpochMillis>=:sinceMillis GROUP BY COALESCE(c.name,t.categoryId,'Uncategorised') ORDER BY amountMinor DESC LIMIT :limit")
    fun observeExpenseByCategory(sinceMillis: Long, limit: Int = 8): Flow<List<NamedAmount>>

    @Query("SELECT COALESCE(NULLIF(TRIM(t.merchant),''),'Unspecified merchant') AS name, COALESCE(SUM(t.amountMinor),0) AS amountMinor FROM transactions t WHERE t.status='CONFIRMED' AND t.type='EXPENSE' AND t.occurredAtEpochMillis>=:sinceMillis GROUP BY COALESCE(NULLIF(TRIM(t.merchant),''),'Unspecified merchant') ORDER BY amountMinor DESC LIMIT :limit")
    fun observeExpenseByMerchant(sinceMillis: Long, limit: Int = 8): Flow<List<NamedAmount>>

    @Query("SELECT strftime('%Y-%m', occurredAtEpochMillis/1000, 'unixepoch', 'localtime') AS month, COALESCE(SUM(amountMinor),0) AS amountMinor FROM transactions WHERE status='CONFIRMED' AND type='EXPENSE' AND occurredAtEpochMillis>=:sinceMillis GROUP BY month ORDER BY month")
    fun observeMonthlySpend(sinceMillis: Long): Flow<List<MonthlySpend>>

    @Query("SELECT CASE WHEN origin='RECURRING' THEN 'Recurring / fixed' ELSE 'Other recorded spending' END AS name, COALESCE(SUM(amountMinor),0) AS amountMinor FROM transactions WHERE status='CONFIRMED' AND type='EXPENSE' AND occurredAtEpochMillis>=:sinceMillis GROUP BY CASE WHEN origin='RECURRING' THEN 'Recurring / fixed' ELSE 'Other recorded spending' END ORDER BY amountMinor DESC")
    fun observeExpenseComposition(sinceMillis: Long): Flow<List<NamedAmount>>

    @Query("SELECT COALESCE(SUM(p.signedMinor),0) FROM ledger_postings p JOIN transactions t ON t.id=p.transactionId WHERE t.status='CONFIRMED' AND t.occurredAtEpochMillis>=:sinceMillis AND (p.ledgerCode LIKE 'ASSET:%' OR p.ledgerCode LIKE 'LIABILITY:%')")
    fun observeLedgerNetWorthMovement(sinceMillis: Long): Flow<Long>

    @Query(
        """
        SELECT t.id, t.type, t.amountMinor, t.currencyCode, t.categoryId,
               t.merchant, t.note, t.occurredAtEpochMillis, a.name AS accountName,
               t.origin, t.reconciliationStatus
        FROM transactions t
        LEFT JOIN ledger_postings p ON p.transactionId = t.id AND p.accountId IS NOT NULL
        LEFT JOIN accounts a ON a.id = p.accountId
        GROUP BY t.id ORDER BY t.occurredAtEpochMillis
        """
    )
    suspend fun allTransactionsForExport(): List<TransactionRow>

    @Query(
        """
        SELECT t.id, t.type, t.amountMinor, t.currencyCode, t.categoryId,
               t.merchant, t.note, t.occurredAtEpochMillis, a.name AS accountName,
               t.origin, t.reconciliationStatus
        FROM transactions t
        LEFT JOIN ledger_postings p ON p.transactionId = t.id AND p.accountId IS NOT NULL
        LEFT JOIN accounts a ON a.id = p.accountId
        WHERE t.status = 'CONFIRMED'
          AND (:type = 'ALL' OR t.type = :type)
          AND (:query = '' OR LOWER(COALESCE(t.merchant,'') || ' ' || COALESCE(t.note,'') || ' ' || COALESCE(a.name,'') || ' ' || COALESCE(t.categoryId,'')) LIKE '%' || LOWER(:query) || '%')
        GROUP BY t.id
        ORDER BY t.occurredAtEpochMillis DESC, t.createdAtEpochMillis DESC
        """
    )
    fun pagingTransactions(query: String, type: String): PagingSource<Int, TransactionRow>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int

    @Query("SELECT * FROM categories WHERE kind = :kind ORDER BY name")
    suspend fun categories(kind: String): List<CategoryEntity>
    @Query("SELECT * FROM categories ORDER BY kind, name") fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id AND archived = 0")
    suspend fun account(id: String): AccountEntity?
    @Query("SELECT COALESCE(SUM(signedMinor),0) FROM ledger_postings WHERE accountId = :accountId") suspend fun accountBalance(accountId: String): Long
    @Query("SELECT COALESCE(SUM(p.signedMinor),0) FROM ledger_postings p JOIN transactions t ON t.id=p.transactionId WHERE p.accountId=:accountId AND t.status='CONFIRMED' AND t.occurredAtEpochMillis<=:throughMillis") suspend fun accountBalanceAt(accountId: String, throughMillis: Long): Long

    @Query("SELECT * FROM transactions WHERE id = :id") suspend fun transaction(id: String): TransactionEntity?
    @Query("SELECT * FROM ledger_postings WHERE transactionId = :transactionId") suspend fun postings(transactionId: String): List<LedgerPostingEntity>
    @Query("UPDATE transactions SET status = :status, modifiedAtEpochMillis = :modifiedAt WHERE id = :id") suspend fun updateTransactionStatus(id: String, status: String, modifiedAt: Long)

    @Query("SELECT COALESCE(SUM(signedMinor), 0) FROM ledger_postings WHERE transactionId = :transactionId")
    suspend fun postingSum(transactionId: String): Long
}
