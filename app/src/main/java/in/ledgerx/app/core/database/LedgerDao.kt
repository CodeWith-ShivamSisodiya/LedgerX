package com.ledgerx.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Insert suspend fun insertAccount(account: AccountEntity)
    @Insert suspend fun insertTransaction(transaction: TransactionEntity)
    @Insert suspend fun insertPostings(postings: List<LedgerPostingEntity>)
    @Insert suspend fun insertAudit(entry: AuditLogEntity)
    @Insert suspend fun insertCategories(categories: List<CategoryEntity>)

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
               t.merchant, t.note, t.occurredAtEpochMillis, a.name AS accountName
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

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int

    @Query("SELECT * FROM categories WHERE kind = :kind ORDER BY name")
    suspend fun categories(kind: String): List<CategoryEntity>

    @Query("SELECT * FROM accounts WHERE id = :id AND archived = 0")
    suspend fun account(id: String): AccountEntity?

    @Query("SELECT COALESCE(SUM(signedMinor), 0) FROM ledger_postings WHERE transactionId = :transactionId")
    suspend fun postingSum(transactionId: String): Long
}
