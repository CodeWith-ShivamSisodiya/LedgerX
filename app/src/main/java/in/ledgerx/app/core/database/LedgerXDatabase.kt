package com.ledgerx.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        LedgerPostingEntity::class,
        AuditLogEntity::class,
        RecurringTemplateEntity::class,
        ScheduledOccurrenceEntity::class,
        BillEntity::class,
        ImportBatchEntity::class,
        ImportedRecordEntity::class,
        ReconciliationEntity::class,
        InvestmentInstrumentEntity::class,
        InvestmentTransactionEntity::class,
        ValuationSnapshotEntity::class,
        DepositEntity::class,
        RetirementContributionEntity::class,
        PhysicalAssetEntity::class,
        AssetValuationEntity::class,
        LoanEntity::class,
        LoanEventEntity::class,
        InsurancePolicyEntity::class,
        GoalEntity::class,
        GoalAllocationEntity::class,
        TaxProfileEntity::class,
        TaxRuleEntity::class,
        TaxPaymentEntity::class,
        HealthScoreSnapshotEntity::class,
        FinancialInsightEntity::class,
        ForecastResultEntity::class,
        NotificationScheduleEntity::class,
        BackupMetadataEntity::class,
        LocalProfileEntity::class,
        MerchantEntity::class,
        TagEntity::class,
        TransactionTagEntity::class,
        ReceiptAttachmentEntity::class,
        InvestmentLotEntity::class,
        DividendRecordEntity::class,
        SipScheduleEntity::class,
        DepositContributionEntity::class,
        RetirementEventEntity::class,
        AssetMaintenanceEntity::class,
        LoanScheduleEntity::class,
        CreditCardStatementEntity::class,
        DashboardWidgetEntity::class,
        BillPriceHistoryEntity::class,
        PolicyDocumentEntity::class,
        PlanningAssumptionEntity::class,
        IncomeBreakdownEntity::class,
        WealthNoteEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class LedgerXDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao
    abstract fun planningDao(): PlanningDao
    abstract fun wealthNotesDao(): WealthNotesDao
    abstract fun extendedDao(): ExtendedDao
}
