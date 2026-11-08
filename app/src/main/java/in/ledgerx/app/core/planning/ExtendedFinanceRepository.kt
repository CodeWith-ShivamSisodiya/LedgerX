package com.ledgerx.app.core.planning

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ledgerx.app.core.accounting.LedgerRepository
import com.ledgerx.app.core.database.*
import com.ledgerx.app.core.model.Money
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtendedFinanceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LedgerXDatabase,
    private val ledgerDao: LedgerDao,
    private val planningDao: PlanningDao,
    private val dao: ExtendedDao,
    private val ledger: LedgerRepository,
) {
    private val clock = Clock.systemUTC()

    val profile: Flow<LocalProfileEntity?> = dao.observeProfile()
    val tags: Flow<List<TagEntity>> = dao.observeTags()
    val dividends: Flow<List<DividendRecordEntity>> = dao.observeDividends()
    val investmentLots: Flow<List<InvestmentLotEntity>> = dao.observeLots()
    val merchantSuggestions: Flow<List<MerchantEntity>> = dao.observeMerchantSuggestions()
    val sips: Flow<List<SipScheduleEntity>> = dao.observeSips()
    val depositContributions: Flow<List<DepositContributionEntity>> = dao.observeDepositContributions()
    val retirementEvents: Flow<List<RetirementEventEntity>> = dao.observeRetirementEvents()
    val maintenance: Flow<List<AssetMaintenanceEntity>> = dao.observeAssetMaintenance()
    val cardStatements: Flow<List<CreditCardStatementEntity>> = dao.observeCardStatements()
    val loanSchedules: Flow<List<LoanScheduleEntity>> = dao.observeLoanSchedules()
    val receipts: Flow<List<ReceiptAttachmentEntity>> = dao.observeAllReceipts()
    val policyDocuments: Flow<List<PolicyDocumentEntity>> = dao.observeAllPolicyDocuments()
    val dashboardWidgets: Flow<List<DashboardWidgetEntity>> = dao.observeDashboardWidgets("primary")
    val planningAssumptions: Flow<PlanningAssumptionEntity?> = dao.observePlanningAssumptions()
    val incomeBreakdowns: Flow<List<IncomeBreakdownEntity>> = dao.observeIncomeBreakdowns()
    val archivedAccounts: Flow<List<AccountEntity>> = dao.observeArchivedAccounts()

    suspend fun ensureProfile(displayName: String = "My finances") {
        if (dao.profile() == null) dao.saveProfile(LocalProfileEntity("primary", displayName.trim().ifBlank { "My finances" }, "INR", "en-IN", clock.millis(), clock.millis()))
        if (dao.dashboardWidgets("primary").isEmpty()) saveDashboardLayout("primary", listOf("NET_WORTH","CASHFLOW","UPCOMING","GOALS","INSIGHTS","HEALTH"))
    }

    suspend fun rememberMerchant(name: String?, categoryId: String?) {
        val clean = name?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val normalized = clean.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
        val id = sha256(normalized.toByteArray()).take(24)
        val existing = dao.merchantByNormalized(normalized)
        dao.saveMerchant(MerchantEntity(id, clean, normalized, categoryId, (existing?.usageCount ?: 0) + 1, clock.millis()))
    }

    suspend fun createTag(name: String, colorArgb: Long? = null): String {
        val clean = name.trim()
        require(clean.isNotEmpty() && clean.length <= 40) { "Tag must contain 1–40 characters" }
        val id = UUID.randomUUID().toString()
        dao.saveTag(TagEntity(id, clean, colorArgb))
        return id
    }

    suspend fun attachTags(transactionId: String, tagIds: List<String>) {
        requireNotNull(ledgerDao.transaction(transactionId)) { "Transaction not found" }
        dao.saveTransactionTags(tagIds.distinct().map { TransactionTagEntity(transactionId, it) })
    }

    suspend fun attachReceipt(transactionId: String, displayName: String, mimeType: String, bytes: ByteArray): String {
        requireNotNull(ledgerDao.transaction(transactionId)) { "Transaction not found" }
        require(bytes.isNotEmpty() && bytes.size <= 10 * 1024 * 1024) { "Receipt must be between 1 byte and 10 MB" }
        val id = UUID.randomUUID().toString()
        dao.saveReceipt(ReceiptAttachmentEntity(id, transactionId, displayName.trim().take(120), mimeType.take(100), "$id.bin", bytes.size.toLong(), sha256(bytes), bytes, clock.millis()))
        return id
    }

    suspend fun attachReceipt(transactionId: String, uri: Uri): String {
        val bytes = readDocument(uri)
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "receipt"
        return attachReceipt(transactionId, name, context.contentResolver.getType(uri) ?: "application/octet-stream", bytes)
    }

    suspend fun attachPolicyDocument(policyId: String, uri: Uri): String {
        requireNotNull(planningDao.policy(policyId)) { "Insurance policy not found" }
        val bytes = readDocument(uri)
        val id = UUID.randomUUID().toString()
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "policy-document"
        dao.savePolicyDocument(PolicyDocumentEntity(id, policyId, name.take(120), (context.contentResolver.getType(uri) ?: "application/octet-stream").take(100), bytes.size.toLong(), sha256(bytes), bytes, clock.millis()))
        return id
    }

    suspend fun removeReceipt(id: String) { check(dao.deleteReceipt(id) == 1) { "Receipt attachment not found" } }

    suspend fun removePolicyDocument(id: String) { check(dao.deletePolicyDocument(id) == 1) { "Policy document not found" } }

    private fun readDocument(uri: Uri): ByteArray = requireNotNull(context.contentResolver.openInputStream(uri)) { "Cannot open document" }.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
                require(output.size() <= 10 * 1024 * 1024) { "Document exceeds the 10 MB encrypted attachment limit" }
            }
            val value = output.toByteArray()
            require(value.isNotEmpty()) { "Document is empty" }
            value
        }

    suspend fun createAndAttachTag(transactionId: String, name: String): String = database.withTransaction {
        val tagId = createTag(name)
        attachTags(transactionId, listOf(tagId))
        tagId
    }

    suspend fun recordDividend(accountId: String, instrumentId: String, gross: Money, taxWithheld: Money, date: LocalDate): String = database.withTransaction {
        requireNotNull(planningDao.instrument(instrumentId)) { "Instrument not found" }
        val occurredAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val ledgerId = ledger.recordDividend(accountId, gross, taxWithheld, instrumentId, occurredAt)
        val id = UUID.randomUUID().toString()
        dao.saveDividend(DividendRecordEntity(id, instrumentId, accountId, gross.minor, taxWithheld.minor, date.toEpochDay(), ledgerId))
        id
    }

    suspend fun createSip(instrumentId: String, accountId: String, amount: Money, frequency: String, intervalCount: Int, nextDue: LocalDate): String {
        requireNotNull(planningDao.instrument(instrumentId)) { "Instrument not found" }
        requireNotNull(ledgerDao.account(accountId)) { "Account not found" }
        require(amount.minor > 0 && intervalCount > 0 && frequency in setOf("WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"))
        val id = UUID.randomUUID().toString()
        dao.saveSip(SipScheduleEntity(id, instrumentId, accountId, amount.minor, frequency, intervalCount, nextDue.toEpochDay(), true))
        return id
    }

    suspend fun contributeToDeposit(depositId: String, accountId: String, amount: Money, date: LocalDate): String = database.withTransaction {
        val deposit = requireNotNull(planningDao.deposit(depositId)) { "Deposit not found" }
        require(deposit.type == "RD") { "Additional contributions are available only for recurring deposits" }
        val ledgerId = ledger.recordAssetTransfer(accountId, amount, "DEPOSIT:$depositId", "RD contribution: ${deposit.name}")
        planningDao.saveDeposit(deposit.copy(principalMinor = Math.addExact(deposit.principalMinor, amount.minor)))
        val id = UUID.randomUUID().toString()
        dao.saveDepositContribution(DepositContributionEntity(id, depositId, accountId, amount.minor, date.toEpochDay(), ledgerId))
        id
    }

    suspend fun recordRetirementEvent(scheme: String, type: String, date: LocalDate, employer: String?, amount: Money?, note: String?): String {
        require(scheme in setOf("EPF", "EPS", "PPF", "NPS"))
        require(type in setOf("EMPLOYER_CHANGE", "TRANSFER", "WITHDRAWAL", "PENSION_ENTITLEMENT", "NOTE"))
        val id = UUID.randomUUID().toString()
        dao.saveRetirementEvent(RetirementEventEntity(id, scheme, type, employer?.trim(), amount?.minor, date.toEpochDay(), note?.trim()))
        return id
    }

    suspend fun recordAssetMaintenance(assetId: String, accountId: String?, description: String, amount: Money, date: LocalDate): String = database.withTransaction {
        requireNotNull(planningDao.asset(assetId)) { "Asset not found" }
        require(description.isNotBlank() && amount.minor >= 0)
        val ledgerId = if (amount.minor > 0 && accountId != null) ledger.recordExpense(accountId, amount, "maintenance", description, "Asset maintenance", occurredAtEpochMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()) else null
        val id = UUID.randomUUID().toString()
        dao.saveAssetMaintenance(AssetMaintenanceEntity(id, assetId, description.trim(), amount.minor, date.toEpochDay(), ledgerId))
        id
    }

    suspend fun saveCardStatement(accountId: String, statementDate: LocalDate, dueDate: LocalDate, balance: Money, minimumDue: Money): String {
        val account = requireNotNull(ledgerDao.account(accountId)) { "Account not found" }
        require(account.kind == "CREDIT_CARD" && dueDate >= statementDate && balance.minor >= 0 && minimumDue.minor in 0..balance.minor)
        val id = "$accountId:${statementDate.toEpochDay()}"
        dao.saveCardStatement(CreditCardStatementEntity(id, accountId, statementDate.toEpochDay(), dueDate.toEpochDay(), balance.minor, minimumDue.minor, 0, "DUE"))
        return id
    }

    suspend fun payCardStatement(statementId: String, sourceAccountId: String, amount: Money): String = database.withTransaction {
        val statement = requireNotNull(dao.cardStatement(statementId)) { "Card statement not found" }
        require(statement.status != "PAID") { "Card statement is already paid" }
        val remaining = statement.statementBalanceMinor - statement.paidMinor
        require(amount.minor in 1..remaining) { "Payment exceeds the outstanding statement amount" }
        val ledgerId = ledger.transfer(sourceAccountId, statement.accountId, amount, "Credit-card statement payment")
        val paid = Math.addExact(statement.paidMinor, amount.minor)
        dao.saveCardStatement(statement.copy(paidMinor = paid, status = if (paid == statement.statementBalanceMinor) "PAID" else "PART_PAID"))
        ledgerId
    }

    suspend fun archiveAccount(accountId: String, archived: Boolean) {
        require(dao.setAccountArchived(accountId, archived) == 1) { "Account not found" }
    }

    suspend fun renameAccount(accountId: String, name: String) {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Account name is required" }
        require(clean.length <= 80) { "Account name is too long" }
        require(dao.renameAccount(accountId, clean) == 1) { "Account not found" }
    }

    suspend fun saveDashboardLayout(profileId: String, orderedVisibleKeys: List<String>) {
        val allowed = setOf("NET_WORTH", "CASHFLOW", "HEALTH", "UPCOMING", "GOALS", "INSIGHTS")
        require(orderedVisibleKeys.isNotEmpty() && orderedVisibleKeys.distinct().size == orderedVisibleKeys.size && orderedVisibleKeys.all { it in allowed })
        dao.saveDashboardWidgets(allowed.map { key -> DashboardWidgetEntity(profileId, key, orderedVisibleKeys.indexOf(key).let { if (it < 0) Int.MAX_VALUE else it }, key in orderedVisibleKeys) })
    }

    suspend fun savePlanningAssumptions(
        essentialMonthlyExpense: Money,
        statedInsuranceNeed: Money,
        currentAge: Int?,
        retirementAge: Int?,
        retirementTarget: Money?,
        inflationBasisPoints: Int,
        returnBasisPoints: Int,
    ) {
        require(essentialMonthlyExpense.minor >= 0 && statedInsuranceNeed.minor >= 0)
        require(retirementTarget == null || retirementTarget.minor >= 0)
        require(currentAge == null || currentAge in 18..90)
        require(retirementAge == null || retirementAge in 19..100)
        require(currentAge == null || retirementAge == null || retirementAge > currentAge)
        require(inflationBasisPoints in 0..5_000 && returnBasisPoints in -5_000..10_000)
        dao.savePlanningAssumptions(
            PlanningAssumptionEntity(
                id = "primary",
                essentialMonthlyExpenseMinor = essentialMonthlyExpense.minor,
                statedInsuranceNeedMinor = statedInsuranceNeed.minor,
                currentAge = currentAge,
                retirementAge = retirementAge,
                retirementTargetMinor = retirementTarget?.minor,
                inflationBasisPoints = inflationBasisPoints,
                returnBasisPoints = returnBasisPoints,
                updatedAtEpochMillis = clock.millis(),
            )
        )
    }

    suspend fun recordDetailedIncome(accountId: String, gross: Money, takeHome: Money, deductions: Money, taxWithheld: Money, categoryId: String, source: String?, note: String?, date: LocalDate): String = database.withTransaction {
        val occurredAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val transactionId = ledger.recordGrossIncome(accountId, gross, takeHome, deductions, taxWithheld, categoryId, source, note, occurredAt)
        dao.saveIncomeBreakdown(IncomeBreakdownEntity(UUID.randomUUID().toString(), transactionId, gross.minor, takeHome.minor, deductions.minor, taxWithheld.minor, occurredAt))
        transactionId
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
