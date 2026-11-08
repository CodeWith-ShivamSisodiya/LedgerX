package com.ledgerx.app.core.database

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_templates", indices = [Index("nextDueEpochDay"), Index("active")])
data class RecurringTemplateEntity(@PrimaryKey val id: String, val accountId: String, val type: String, val amountMinor: Long, val currencyCode: String, val categoryId: String?, val merchant: String?, val frequency: String, val intervalCount: Int, val nextDueEpochDay: Long, val active: Boolean)

@Entity(tableName = "scheduled_occurrences", indices = [Index("templateId"), Index("dueEpochDay"), Index("status")])
data class ScheduledOccurrenceEntity(@PrimaryKey val id: String, val templateId: String, val dueEpochDay: Long, val status: String, val transactionId: String?)

@Entity(tableName = "bills", indices = [Index("dueEpochDay"), Index("status"), Index("merchant")])
data class BillEntity(@PrimaryKey val id: String, val name: String, val merchant: String?, val amountMinor: Long, val currencyCode: String, val dueEpochDay: Long, val frequency: String?, val status: String, val pricePreviousMinor: Long?, val reminderDaysBefore: Int)

@Entity(tableName = "import_batches", indices = [Index("sourceHash"), Index("createdAtEpochMillis")])
data class ImportBatchEntity(@PrimaryKey val id: String, val accountId: String, val fileName: String, val sourceHash: String, val rowCount: Int, val acceptedCount: Int, val rejectedCount: Int, val status: String, val createdAtEpochMillis: Long)

@Entity(tableName = "imported_records", indices = [Index(value = ["fingerprint"], unique = true), Index("batchId")])
data class ImportedRecordEntity(@PrimaryKey val id: String, val batchId: String, val fingerprint: String, val sourceRow: Int, val transactionId: String?, val status: String, val error: String?)

@Entity(tableName = "reconciliations", indices = [Index("accountId"), Index("statementEndEpochDay")])
data class ReconciliationEntity(@PrimaryKey val id: String, val accountId: String, val statementEndEpochDay: Long, val statementBalanceMinor: Long, val ledgerBalanceMinor: Long, val differenceMinor: Long, val completedAtEpochMillis: Long?)

@Entity(tableName = "investment_instruments", indices = [Index(value = ["symbol", "exchange"], unique = true), Index("assetClass")])
data class InvestmentInstrumentEntity(@PrimaryKey val id: String, val name: String, val symbol: String?, val exchange: String?, val isin: String?, val assetClass: String, val currencyCode: String)

@Entity(tableName = "investment_transactions", indices = [Index("instrumentId"), Index("accountId"), Index("tradeEpochDay")])
data class InvestmentTransactionEntity(@PrimaryKey val id: String, val instrumentId: String, val accountId: String, val type: String, val quantityMicros: Long, val priceMinor: Long, val feesMinor: Long, val tradeEpochDay: Long, val ledgerTransactionId: String?)

@Entity(tableName = "valuation_snapshots", indices = [Index(value = ["instrumentId", "priceEpochDay"], unique = true)])
data class ValuationSnapshotEntity(@PrimaryKey val id: String, val instrumentId: String, val priceMinor: Long, val currencyCode: String, val priceEpochDay: Long, val source: String, val capturedAtEpochMillis: Long)

@Entity(tableName = "deposits", indices = [Index("maturityEpochDay"), Index("type")])
data class DepositEntity(@PrimaryKey val id: String, val name: String, val type: String, val principalMinor: Long, val annualRateBasisPoints: Int, val openedEpochDay: Long, val maturityEpochDay: Long, val maturityMinor: Long?, val accountId: String?, val lockInEndEpochDay: Long? = null, val withdrawalRestrictions: String? = null)

@Entity(tableName = "retirement_contributions", indices = [Index("scheme"), Index("contributionEpochDay")])
data class RetirementContributionEntity(@PrimaryKey val id: String, val scheme: String, val employeeMinor: Long, val employerMinor: Long, val pensionMinor: Long, val contributionEpochDay: Long, val financialYear: String, val note: String?)

@Entity(tableName = "physical_assets", indices = [Index("type"), Index("archived")])
data class PhysicalAssetEntity(@PrimaryKey val id: String, val name: String, val type: String, val purchaseMinor: Long, val purchaseEpochDay: Long, val ownershipBasisPoints: Int, val linkedLoanId: String?, val weightMilligrams: Long?, val purityBasisPoints: Int?, val transactionCostMinor: Long?, val archived: Boolean)

@Entity(tableName = "asset_valuations", indices = [Index(value = ["assetId", "valuationEpochDay"], unique = true)])
data class AssetValuationEntity(@PrimaryKey val id: String, val assetId: String, val estimatedMarketMinor: Long, val estimatedResaleMinor: Long?, val valuationEpochDay: Long, val source: String, val assumptions: String?)

@Entity(tableName = "loans", indices = [Index("type"), Index("active")])
data class LoanEntity(@PrimaryKey val id: String, val name: String, val type: String, val originalPrincipalMinor: Long, val outstandingPrincipalMinor: Long, val annualRateBasisPoints: Int, val emiMinor: Long, val remainingMonths: Int, val nextDueEpochDay: Long?, val active: Boolean)

@Entity(tableName = "loan_events", indices = [Index("loanId"), Index("eventEpochDay")])
data class LoanEventEntity(@PrimaryKey val id: String, val loanId: String, val type: String, val principalMinor: Long, val interestMinor: Long, val feeMinor: Long, val annualRateBasisPoints: Int?, val eventEpochDay: Long, val ledgerTransactionId: String?)

@Entity(tableName = "insurance_policies", indices = [Index("type"), Index("renewalEpochDay"), Index("active")])
data class InsurancePolicyEntity(@PrimaryKey val id: String, val name: String, val type: String, val insurer: String, val coverageMinor: Long, val premiumMinor: Long, val deductibleMinor: Long?, val renewalEpochDay: Long, val nominees: String?, val exclusions: String?, val documentUri: String?, val active: Boolean)

@Entity(tableName = "goals", indices = [Index("targetEpochDay"), Index("status")])
data class GoalEntity(@PrimaryKey val id: String, val name: String, val type: String, val targetMinor: Long, val targetEpochDay: Long, val monthlyContributionMinor: Long, val status: String, val createdAtEpochMillis: Long)

@Entity(tableName = "goal_allocations", primaryKeys = ["goalId", "accountId"], indices = [Index("accountId")])
data class GoalAllocationEntity(val goalId: String, val accountId: String, val allocatedMinor: Long, val updatedAtEpochMillis: Long)

@Entity(tableName = "tax_profiles", indices = [Index(value = ["financialYear"], unique = true)])
data class TaxProfileEntity(
    @PrimaryKey val id: String,
    val financialYear: String,
    val preferredRegime: String?,
    val grossIncomeMinor: Long,
    val eligibleDeductionsMinor: Long,
    val tdsMinor: Long,
    val advanceTaxMinor: Long,
    val updatedAtEpochMillis: Long,
    @ColumnInfo(defaultValue = "0") val salaryIncomeMinor: Long = 0,
    @ColumnInfo(defaultValue = "0") val interestIncomeMinor: Long = 0,
    @ColumnInfo(defaultValue = "0") val dividendIncomeMinor: Long = 0,
    @ColumnInfo(defaultValue = "0") val shortTermCapitalGain111AMinor: Long = 0,
    @ColumnInfo(defaultValue = "0") val longTermCapitalGain112AMinor: Long = 0,
)

@Entity(tableName = "tax_rules", indices = [Index(value = ["financialYear", "regime", "version"], unique = true)])
data class TaxRuleEntity(@PrimaryKey val id: String, val financialYear: String, val regime: String, val version: Int, val effectiveEpochDay: Long, val ruleJson: String, val verifiedSource: String, val verifiedAtEpochMillis: Long)

@Entity(tableName = "tax_payments", indices = [Index("financialYear"), Index("paidEpochDay")])
data class TaxPaymentEntity(@PrimaryKey val id: String, val financialYear: String, val type: String, val amountMinor: Long, val paidEpochDay: Long, val reference: String?)

@Entity(tableName = "health_snapshots", indices = [Index("calculatedAtEpochMillis")])
data class HealthScoreSnapshotEntity(@PrimaryKey val id: String, val score: Int?, val completenessPercent: Int, val dimensionsJson: String, val warningsJson: String, val calculatedAtEpochMillis: Long)

@Entity(tableName = "financial_insights", indices = [Index("status"), Index("createdAtEpochMillis"), Index("type")])
data class FinancialInsightEntity(@PrimaryKey val id: String, val type: String, val title: String, val evidence: String, val impactMinor: Long?, val confidence: String, val suggestedAction: String, val status: String, val createdAtEpochMillis: Long)

@Entity(tableName = "forecast_results", indices = [Index("scenario"), Index("createdAtEpochMillis")])
data class ForecastResultEntity(@PrimaryKey val id: String, val scenario: String, val horizonMonths: Int, val assumptionsJson: String, val pointsJson: String, val createdAtEpochMillis: Long)

@Entity(tableName = "notification_schedules", indices = [Index("dueAtEpochMillis"), Index("enabled")])
data class NotificationScheduleEntity(@PrimaryKey val id: String, val sourceType: String, val sourceId: String, val title: String, val dueAtEpochMillis: Long, val channel: String, val enabled: Boolean)

@Entity(tableName = "backup_metadata", indices = [Index("createdAtEpochMillis")])
data class BackupMetadataEntity(@PrimaryKey val id: String, val formatVersion: Int, val fileName: String, val checksumSha256: String, val createdAtEpochMillis: Long, val verifiedAtEpochMillis: Long?, val recordCount: Long)
