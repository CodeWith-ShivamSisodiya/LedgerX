package com.ledgerx.app.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts", indices = [Index("name")])
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val currencyCode: String,
    val archived: Boolean = false,
    val createdAtEpochMillis: Long,
)

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true), Index("parentCategoryId")])
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val icon: String,
    val userDefined: Boolean,
    val parentCategoryId: String? = null,
)

@Entity(tableName = "transactions", indices = [Index("occurredAtEpochMillis"), Index("type")])
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountMinor: Long,
    val currencyCode: String,
    val categoryId: String?,
    val merchant: String?,
    val note: String?,
    val status: String = "CONFIRMED",
    val origin: String = "MANUAL",
    val reconciliationStatus: String = "UNRECONCILED",
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val modifiedAtEpochMillis: Long,
    val reversedTransactionId: String? = null,
)

@Entity(
    tableName = "ledger_postings",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("transactionId"), Index("accountId")],
)
data class LedgerPostingEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val accountId: String?,
    val ledgerCode: String,
    val signedMinor: Long,
    val currencyCode: String,
)

@Entity(tableName = "audit_log", indices = [Index("entityId"), Index("createdAtEpochMillis")])
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val detail: String,
    val createdAtEpochMillis: Long,
)

data class AccountBalance(
    val id: String,
    val name: String,
    val kind: String,
    val currencyCode: String,
    val balanceMinor: Long,
)

data class TransactionRow(
    val id: String,
    val type: String,
    val amountMinor: Long,
    val currencyCode: String,
    val categoryId: String?,
    val merchant: String?,
    val note: String?,
    val occurredAtEpochMillis: Long,
    val accountName: String?,
    val origin: String,
    val reconciliationStatus: String,
)

data class MonthlyTotals(val incomeMinor: Long, val expenseMinor: Long, val investmentMinor: Long)

data class NamedAmount(val name: String, val amountMinor: Long)
data class MonthlySpend(val month: String, val amountMinor: Long)
