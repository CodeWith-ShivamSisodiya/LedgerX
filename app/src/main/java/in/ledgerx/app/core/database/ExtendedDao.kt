package com.ledgerx.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtendedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveProfile(value: LocalProfileEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveMerchant(value: MerchantEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTag(value: TagEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun saveTransactionTags(values: List<TransactionTagEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveReceipt(value: ReceiptAttachmentEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveLot(value: InvestmentLotEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveDividend(value: DividendRecordEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSip(value: SipScheduleEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveDepositContribution(value: DepositContributionEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveRetirementEvent(value: RetirementEventEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAssetMaintenance(value: AssetMaintenanceEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveLoanSchedule(values: List<LoanScheduleEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveCardStatement(value: CreditCardStatementEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveDashboardWidgets(values: List<DashboardWidgetEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveBillPrice(value: BillPriceHistoryEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePolicyDocument(value: PolicyDocumentEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePlanningAssumptions(value: PlanningAssumptionEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveIncomeBreakdown(value: IncomeBreakdownEntity)
    @Query("SELECT * FROM income_breakdowns ORDER BY id DESC") fun observeIncomeBreakdowns(): Flow<List<IncomeBreakdownEntity>>

    @Query("SELECT * FROM local_profiles ORDER BY createdAtEpochMillis LIMIT 1") fun observeProfile(): Flow<LocalProfileEntity?>
    @Query("SELECT * FROM local_profiles ORDER BY createdAtEpochMillis LIMIT 1") suspend fun profile(): LocalProfileEntity?
    @Query("SELECT * FROM merchants ORDER BY usageCount DESC, lastUsedAtEpochMillis DESC LIMIT :limit") suspend fun merchantSuggestions(limit: Int = 8): List<MerchantEntity>
    @Query("SELECT * FROM merchants ORDER BY usageCount DESC, lastUsedAtEpochMillis DESC LIMIT 8") fun observeMerchantSuggestions(): Flow<List<MerchantEntity>>
    @Query("SELECT * FROM merchants WHERE normalizedName = :normalized LIMIT 1") suspend fun merchantByNormalized(normalized: String): MerchantEntity?
    @Query("SELECT * FROM tags ORDER BY name") fun observeTags(): Flow<List<TagEntity>>
    @Query("SELECT * FROM receipt_attachments WHERE transactionId = :transactionId ORDER BY createdAtEpochMillis") fun observeReceipts(transactionId: String): Flow<List<ReceiptAttachmentEntity>>
    @Query("SELECT * FROM receipt_attachments ORDER BY createdAtEpochMillis DESC") fun observeAllReceipts(): Flow<List<ReceiptAttachmentEntity>>
    @Query("DELETE FROM receipt_attachments WHERE id = :id") suspend fun deleteReceipt(id: String): Int
    @Query("SELECT * FROM investment_lots WHERE instrumentId = :instrumentId ORDER BY openedEpochDay, id") suspend fun lots(instrumentId: String): List<InvestmentLotEntity>
    @Query("SELECT * FROM investment_lots ORDER BY openedEpochDay, id") fun observeLots(): Flow<List<InvestmentLotEntity>>
    @Query("SELECT * FROM dividend_records ORDER BY receivedEpochDay DESC") fun observeDividends(): Flow<List<DividendRecordEntity>>
    @Query("SELECT * FROM dividend_records WHERE id = :id LIMIT 1") suspend fun dividend(id: String): DividendRecordEntity?
    @Query("SELECT * FROM sip_schedules WHERE active = 1 ORDER BY nextDueEpochDay") fun observeSips(): Flow<List<SipScheduleEntity>>
    @Query("SELECT * FROM sip_schedules WHERE id = :id LIMIT 1") suspend fun sip(id: String): SipScheduleEntity?
    @Query("SELECT * FROM sip_schedules WHERE active = 1 AND nextDueEpochDay <= :throughEpochDay ORDER BY nextDueEpochDay") suspend fun sipsDueThrough(throughEpochDay: Long): List<SipScheduleEntity>
    @Query("SELECT * FROM deposit_contributions ORDER BY contributionEpochDay DESC") fun observeDepositContributions(): Flow<List<DepositContributionEntity>>
    @Query("SELECT * FROM retirement_events ORDER BY eventEpochDay DESC") fun observeRetirementEvents(): Flow<List<RetirementEventEntity>>
    @Query("SELECT * FROM asset_maintenance ORDER BY occurredEpochDay DESC") fun observeAssetMaintenance(): Flow<List<AssetMaintenanceEntity>>
    @Query("SELECT * FROM loan_schedule WHERE loanId = :loanId ORDER BY installmentNumber") fun observeLoanSchedule(loanId: String): Flow<List<LoanScheduleEntity>>
    @Query("SELECT * FROM loan_schedule ORDER BY dueEpochDay, installmentNumber") fun observeLoanSchedules(): Flow<List<LoanScheduleEntity>>
    @Query("SELECT * FROM loan_schedule WHERE loanId = :loanId AND status = 'DUE' ORDER BY installmentNumber LIMIT 1") suspend fun nextDueLoanInstallment(loanId: String): LoanScheduleEntity?
    @Query("UPDATE loan_schedule SET status = :status WHERE id = :scheduleId") suspend fun setLoanScheduleStatus(scheduleId: String, status: String): Int
    @Query("DELETE FROM loan_schedule WHERE loanId = :loanId AND status = 'DUE'") suspend fun deleteDueLoanSchedule(loanId: String)
    @Query("SELECT COALESCE(MAX(installmentNumber), 0) FROM loan_schedule WHERE loanId = :loanId") suspend fun lastLoanInstallmentNumber(loanId: String): Int
    @Query("DELETE FROM loan_schedule WHERE loanId = :loanId") suspend fun deleteLoanSchedule(loanId: String)
    @Query("SELECT * FROM credit_card_statements ORDER BY statementEpochDay DESC") fun observeCardStatements(): Flow<List<CreditCardStatementEntity>>
    @Query("SELECT * FROM credit_card_statements WHERE status != 'PAID' AND dueEpochDay <= :throughEpochDay ORDER BY dueEpochDay") suspend fun cardStatementsDueThrough(throughEpochDay: Long): List<CreditCardStatementEntity>
    @Query("SELECT * FROM credit_card_statements WHERE id = :id LIMIT 1") suspend fun cardStatement(id: String): CreditCardStatementEntity?
    @Query("SELECT * FROM dashboard_widgets WHERE profileId = :profileId ORDER BY position") fun observeDashboardWidgets(profileId: String): Flow<List<DashboardWidgetEntity>>
    @Query("SELECT * FROM dashboard_widgets WHERE profileId = :profileId ORDER BY position") suspend fun dashboardWidgets(profileId: String): List<DashboardWidgetEntity>
    @Query("SELECT * FROM bill_price_history WHERE billId = :billId ORDER BY effectiveEpochDay") fun observeBillPrices(billId: String): Flow<List<BillPriceHistoryEntity>>
    @Query("SELECT * FROM policy_documents WHERE policyId = :policyId ORDER BY createdAtEpochMillis") fun observePolicyDocuments(policyId: String): Flow<List<PolicyDocumentEntity>>
    @Query("SELECT * FROM policy_documents ORDER BY createdAtEpochMillis DESC") fun observeAllPolicyDocuments(): Flow<List<PolicyDocumentEntity>>
    @Query("DELETE FROM policy_documents WHERE id = :id") suspend fun deletePolicyDocument(id: String): Int
    @Query("SELECT * FROM planning_assumptions WHERE id = 'primary' LIMIT 1") fun observePlanningAssumptions(): Flow<PlanningAssumptionEntity?>
    @Query("SELECT * FROM accounts WHERE archived=1 ORDER BY name") fun observeArchivedAccounts(): Flow<List<AccountEntity>>
    @Query("UPDATE accounts SET archived = :archived WHERE id = :accountId") suspend fun setAccountArchived(accountId: String, archived: Boolean): Int
    @Query("UPDATE accounts SET name = :name WHERE id = :accountId") suspend fun renameAccount(accountId: String, name: String): Int
}
