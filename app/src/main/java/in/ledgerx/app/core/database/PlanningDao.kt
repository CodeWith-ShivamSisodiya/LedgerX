package com.ledgerx.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanningDao {
    @Query("SELECT COALESCE(SUM(interestMinor + feeMinor),0) FROM loan_events WHERE eventEpochDay >= :sinceEpochDay")
    fun observeLoanInterestAndFees(sinceEpochDay: Long): Flow<Long>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveRecurring(value: RecurringTemplateEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun saveOccurrence(value: ScheduledOccurrenceEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveBill(value: BillEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun saveImportBatch(value: ImportBatchEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun saveImportedRecords(values: List<ImportedRecordEntity>)
    @Query("SELECT * FROM import_batches ORDER BY createdAtEpochMillis DESC") fun observeImportBatches(): Flow<List<ImportBatchEntity>>
    @Query("SELECT * FROM import_batches WHERE id=:id LIMIT 1") suspend fun importBatch(id: String): ImportBatchEntity?
    @Query("SELECT * FROM imported_records WHERE batchId=:batchId ORDER BY sourceRow") suspend fun importedRecords(batchId: String): List<ImportedRecordEntity>
    @Query("UPDATE import_batches SET status=:status WHERE id=:id") suspend fun updateImportBatchStatus(id: String, status: String): Int
    @Query("UPDATE imported_records SET status=:status WHERE batchId=:batchId") suspend fun updateImportedRecordStatus(batchId: String, status: String): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveReconciliation(value: ReconciliationEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveInstrument(value: InvestmentInstrumentEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveInvestmentTransaction(value: InvestmentTransactionEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveValuation(value: ValuationSnapshotEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveDeposit(value: DepositEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveRetirementContribution(value: RetirementContributionEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAsset(value: PhysicalAssetEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAssetValuation(value: AssetValuationEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveLoan(value: LoanEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveLoanEvent(value: LoanEventEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePolicy(value: InsurancePolicyEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveGoal(value: GoalEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveGoalAllocation(value: GoalAllocationEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTaxProfile(value: TaxProfileEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTaxRule(value: TaxRuleEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTaxPayment(value: TaxPaymentEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveHealthSnapshot(value: HealthScoreSnapshotEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveInsight(value: FinancialInsightEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveForecast(value: ForecastResultEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveNotification(value: NotificationScheduleEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveBackupMetadata(value: BackupMetadataEntity)

    @Query("SELECT * FROM bills ORDER BY dueEpochDay") fun observeBills(): Flow<List<BillEntity>>
    @Query("SELECT * FROM bills WHERE LOWER(name) = LOWER(:name) LIMIT 1") suspend fun billByName(name: String): BillEntity?
    @Query("SELECT * FROM bills WHERE id=:id LIMIT 1") suspend fun bill(id: String): BillEntity?
    @Query("SELECT * FROM loans WHERE active = 1 ORDER BY nextDueEpochDay") fun observeLoans(): Flow<List<LoanEntity>>
    @Query("SELECT * FROM insurance_policies WHERE active = 1 ORDER BY renewalEpochDay") fun observePolicies(): Flow<List<InsurancePolicyEntity>>
    @Query("SELECT * FROM insurance_policies WHERE id = :id LIMIT 1") suspend fun policy(id: String): InsurancePolicyEntity?
    @Query("SELECT * FROM goals WHERE status = 'ACTIVE' ORDER BY targetEpochDay") fun observeGoals(): Flow<List<GoalEntity>>
    @Query("SELECT * FROM goal_allocations") fun observeGoalAllocations(): Flow<List<GoalAllocationEntity>>
    @Query("SELECT * FROM financial_insights WHERE status = 'ACTIVE' ORDER BY createdAtEpochMillis DESC") fun observeInsights(): Flow<List<FinancialInsightEntity>>
    @Query("SELECT * FROM health_snapshots ORDER BY calculatedAtEpochMillis DESC LIMIT 1") fun observeLatestHealth(): Flow<HealthScoreSnapshotEntity?>
    @Query("SELECT * FROM health_snapshots ORDER BY calculatedAtEpochMillis DESC LIMIT 90") fun observeHealthHistory(): Flow<List<HealthScoreSnapshotEntity>>
    @Query("SELECT * FROM investment_instruments ORDER BY name") fun observeInstruments(): Flow<List<InvestmentInstrumentEntity>>
    @Query("SELECT * FROM investment_transactions ORDER BY tradeEpochDay DESC") fun observeInvestmentTransactions(): Flow<List<InvestmentTransactionEntity>>
    @Query("SELECT * FROM valuation_snapshots ORDER BY priceEpochDay DESC") fun observeValuations(): Flow<List<ValuationSnapshotEntity>>
    @Query("SELECT * FROM deposits ORDER BY maturityEpochDay") fun observeDeposits(): Flow<List<DepositEntity>>
    @Query("SELECT * FROM retirement_contributions ORDER BY contributionEpochDay DESC") fun observeRetirementContributions(): Flow<List<RetirementContributionEntity>>
    @Query("SELECT * FROM physical_assets WHERE archived = 0 ORDER BY purchaseEpochDay DESC") fun observeAssets(): Flow<List<PhysicalAssetEntity>>
    @Query("SELECT * FROM asset_valuations ORDER BY valuationEpochDay DESC") fun observeAssetValuations(): Flow<List<AssetValuationEntity>>
    @Query("SELECT * FROM tax_profiles ORDER BY financialYear DESC LIMIT 1") fun observeLatestTaxProfile(): Flow<TaxProfileEntity?>
    @Query("SELECT * FROM tax_payments ORDER BY paidEpochDay DESC") fun observeTaxPayments(): Flow<List<TaxPaymentEntity>>
    @Query("SELECT * FROM reconciliations ORDER BY statementEndEpochDay DESC") fun observeReconciliations(): Flow<List<ReconciliationEntity>>
    @Query("SELECT * FROM notification_schedules WHERE enabled = 1 ORDER BY dueAtEpochMillis") fun observeNotifications(): Flow<List<NotificationScheduleEntity>>
    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1") suspend fun goal(id: String): GoalEntity?
    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1") suspend fun loan(id: String): LoanEntity?
    @Query("SELECT * FROM deposits WHERE id = :id LIMIT 1") suspend fun deposit(id: String): DepositEntity?
    @Query("SELECT * FROM physical_assets WHERE id = :id LIMIT 1") suspend fun asset(id: String): PhysicalAssetEntity?
    @Query("SELECT * FROM investment_instruments WHERE id = :id LIMIT 1") suspend fun instrument(id: String): InvestmentInstrumentEntity?
    @Query("SELECT COALESCE(SUM(CASE WHEN type='SELL' THEN -quantityMicros ELSE quantityMicros END),0) FROM investment_transactions WHERE instrumentId = :instrumentId") suspend fun investmentQuantity(instrumentId: String): Long
    @Query("SELECT * FROM investment_transactions WHERE instrumentId = :instrumentId ORDER BY tradeEpochDay, id") suspend fun investmentTransactionsForInstrument(instrumentId: String): List<InvestmentTransactionEntity>
    @Query("SELECT COALESCE(SUM(allocatedMinor),0) FROM goal_allocations WHERE goalId = :goalId AND accountId != :accountId") suspend fun otherGoalAllocations(goalId: String, accountId: String): Long
    @Query("SELECT COALESCE(SUM(allocatedMinor),0) FROM goal_allocations WHERE accountId = :accountId AND goalId != :goalId") suspend fun otherAccountAllocations(accountId: String, goalId: String): Long
    @Query("SELECT * FROM tax_profiles WHERE financialYear = :financialYear LIMIT 1") suspend fun taxProfile(financialYear: String): TaxProfileEntity?
    @Query("SELECT * FROM tax_payments WHERE financialYear = :financialYear ORDER BY paidEpochDay") suspend fun taxPayments(financialYear: String): List<TaxPaymentEntity>
    @Query("SELECT * FROM recurring_templates WHERE active = 1 ORDER BY nextDueEpochDay") fun observeRecurring(): Flow<List<RecurringTemplateEntity>>
    @Query("SELECT * FROM recurring_templates WHERE id = :id LIMIT 1") suspend fun recurring(id: String): RecurringTemplateEntity?
    @Query("SELECT * FROM scheduled_occurrences WHERE status = 'PENDING' ORDER BY dueEpochDay") fun observePendingOccurrences(): Flow<List<ScheduledOccurrenceEntity>>
    @Query("SELECT * FROM scheduled_occurrences WHERE status = 'PENDING' ORDER BY dueEpochDay") suspend fun pendingOccurrences(): List<ScheduledOccurrenceEntity>
    @Query("SELECT * FROM recurring_templates WHERE active = 1 AND nextDueEpochDay <= :epochDay ORDER BY nextDueEpochDay") suspend fun dueRecurring(epochDay: Long): List<RecurringTemplateEntity>
    @Query("SELECT * FROM scheduled_occurrences WHERE id = :id") suspend fun occurrence(id: String): ScheduledOccurrenceEntity?
    @Query("SELECT * FROM bills WHERE status = 'DUE' AND dueEpochDay <= :throughEpochDay ORDER BY dueEpochDay") suspend fun billsDueThrough(throughEpochDay: Long): List<BillEntity>
    @Query("SELECT * FROM loans WHERE active = 1 AND nextDueEpochDay IS NOT NULL AND nextDueEpochDay <= :throughEpochDay ORDER BY nextDueEpochDay") suspend fun loansDueThrough(throughEpochDay: Long): List<LoanEntity>
    @Query("SELECT * FROM insurance_policies WHERE active = 1 AND renewalEpochDay <= :throughEpochDay ORDER BY renewalEpochDay") suspend fun policiesDueThrough(throughEpochDay: Long): List<InsurancePolicyEntity>
    @Query("SELECT * FROM recurring_templates WHERE active = 1 AND nextDueEpochDay <= :throughEpochDay ORDER BY nextDueEpochDay") suspend fun recurringDueThrough(throughEpochDay: Long): List<RecurringTemplateEntity>
    @Query("SELECT * FROM deposits WHERE maturityEpochDay <= :throughEpochDay ORDER BY maturityEpochDay") suspend fun depositsDueThrough(throughEpochDay: Long): List<DepositEntity>
    @Query("SELECT * FROM goals WHERE status = 'ACTIVE' AND targetEpochDay <= :throughEpochDay ORDER BY targetEpochDay") suspend fun goalsDueThrough(throughEpochDay: Long): List<GoalEntity>
    @Query("SELECT * FROM notification_schedules WHERE enabled = 1 AND dueAtEpochMillis <= :throughMillis ORDER BY dueAtEpochMillis") suspend fun notificationsDueThrough(throughMillis: Long): List<NotificationScheduleEntity>
    @Query("SELECT fingerprint FROM imported_records WHERE fingerprint IN (:fingerprints)") suspend fun existingFingerprints(fingerprints: List<String>): List<String>
    @Query("UPDATE transactions SET reconciliationStatus = 'RECONCILED', modifiedAtEpochMillis = :modifiedAt WHERE id IN (SELECT transactionId FROM ledger_postings WHERE accountId = :accountId) AND occurredAtEpochMillis <= :statementEndMillis AND status = 'CONFIRMED'")
    suspend fun markAccountReconciled(accountId: String, statementEndMillis: Long, modifiedAt: Long)
    @Query("UPDATE bills SET status = 'PAID' WHERE id = :id AND status = 'DUE'") suspend fun markBillPaid(id: String): Int
}
