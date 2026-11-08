package com.ledgerx.app.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "local_profiles")
data class LocalProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val baseCurrencyCode: String,
    val localeTag: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "merchants", indices = [Index(value = ["normalizedName"], unique = true)])
data class MerchantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val normalizedName: String,
    val defaultCategoryId: String?,
    val usageCount: Int,
    val lastUsedAtEpochMillis: Long,
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(@PrimaryKey val id: String, val name: String, val colorArgb: Long?)

@Entity(tableName = "transaction_tags", primaryKeys = ["transactionId", "tagId"], indices = [Index("tagId")])
data class TransactionTagEntity(val transactionId: String, val tagId: String)

@Entity(tableName = "receipt_attachments", indices = [Index("transactionId")])
data class ReceiptAttachmentEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val displayName: String,
    val mimeType: String,
    val storedFileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val content: ByteArray,
    val createdAtEpochMillis: Long,
)

@Entity(tableName = "investment_lots", indices = [Index("instrumentId"), Index("openedByTransactionId"), Index("closedEpochDay")])
data class InvestmentLotEntity(
    @PrimaryKey val id: String,
    val instrumentId: String,
    val openedByTransactionId: String,
    val openedEpochDay: Long,
    val originalQuantityMicros: Long,
    val remainingQuantityMicros: Long,
    val totalCostMinor: Long,
    val remainingCostMinor: Long,
    val realizedProceedsMinor: Long,
    val realizedGainMinor: Long,
    val closedEpochDay: Long?,
)

@Entity(tableName = "dividend_records", indices = [Index("instrumentId"), Index("receivedEpochDay")])
data class DividendRecordEntity(
    @PrimaryKey val id: String,
    val instrumentId: String,
    val accountId: String,
    val grossMinor: Long,
    val taxWithheldMinor: Long,
    val receivedEpochDay: Long,
    val ledgerTransactionId: String,
)

@Entity(tableName = "sip_schedules", indices = [Index("instrumentId"), Index("nextDueEpochDay"), Index("active")])
data class SipScheduleEntity(
    @PrimaryKey val id: String,
    val instrumentId: String,
    val accountId: String,
    val amountMinor: Long,
    val frequency: String,
    val intervalCount: Int,
    val nextDueEpochDay: Long,
    val active: Boolean,
)

@Entity(tableName = "deposit_contributions", indices = [Index("depositId"), Index("contributionEpochDay")])
data class DepositContributionEntity(
    @PrimaryKey val id: String,
    val depositId: String,
    val accountId: String,
    val amountMinor: Long,
    val contributionEpochDay: Long,
    val ledgerTransactionId: String,
)

@Entity(tableName = "retirement_events", indices = [Index("scheme"), Index("eventEpochDay")])
data class RetirementEventEntity(
    @PrimaryKey val id: String,
    val scheme: String,
    val type: String,
    val employerName: String?,
    val amountMinor: Long?,
    val eventEpochDay: Long,
    val note: String?,
)

@Entity(tableName = "asset_maintenance", indices = [Index("assetId"), Index("occurredEpochDay")])
data class AssetMaintenanceEntity(
    @PrimaryKey val id: String,
    val assetId: String,
    val description: String,
    val amountMinor: Long,
    val occurredEpochDay: Long,
    val ledgerTransactionId: String?,
)

@Entity(tableName = "loan_schedule", indices = [Index("loanId"), Index(value = ["loanId", "installmentNumber"], unique = true), Index("dueEpochDay")])
data class LoanScheduleEntity(
    @PrimaryKey val id: String,
    val loanId: String,
    val installmentNumber: Int,
    val dueEpochDay: Long,
    val openingMinor: Long,
    val emiMinor: Long,
    val principalMinor: Long,
    val interestMinor: Long,
    val closingMinor: Long,
    val status: String,
)

@Entity(tableName = "credit_card_statements", indices = [Index("accountId"), Index(value = ["accountId", "statementEpochDay"], unique = true), Index("dueEpochDay")])
data class CreditCardStatementEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val statementEpochDay: Long,
    val dueEpochDay: Long,
    val statementBalanceMinor: Long,
    val minimumDueMinor: Long,
    val paidMinor: Long,
    val status: String,
)

@Entity(tableName = "dashboard_widgets", primaryKeys = ["profileId", "widgetKey"], indices = [Index("position")])
data class DashboardWidgetEntity(val profileId: String, val widgetKey: String, val position: Int, val visible: Boolean)

@Entity(tableName = "bill_price_history", indices = [Index("billId"), Index("effectiveEpochDay")])
data class BillPriceHistoryEntity(
    @PrimaryKey val id: String,
    val billId: String,
    val amountMinor: Long,
    val effectiveEpochDay: Long,
)

@Entity(tableName = "policy_documents", indices = [Index("policyId")])
data class PolicyDocumentEntity(
    @PrimaryKey val id: String,
    val policyId: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
    val content: ByteArray,
    val createdAtEpochMillis: Long,
)

@Entity(tableName = "planning_assumptions")
data class PlanningAssumptionEntity(
    @PrimaryKey val id: String,
    val essentialMonthlyExpenseMinor: Long,
    val statedInsuranceNeedMinor: Long,
    val currentAge: Int?,
    val retirementAge: Int?,
    val retirementTargetMinor: Long?,
    val inflationBasisPoints: Int,
    val returnBasisPoints: Int,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "income_breakdowns", indices = [Index("transactionId")])
data class IncomeBreakdownEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val grossMinor: Long,
    val takeHomeMinor: Long,
    val deductionsMinor: Long,
    val taxWithheldMinor: Long,
    val occurredAtEpochMillis: Long,
)
