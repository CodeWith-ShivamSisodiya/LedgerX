package com.ledgerx.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import com.ledgerx.app.core.accounting.LedgerRepository
import com.ledgerx.app.core.backup.BackupRepository
import com.ledgerx.app.core.database.*
import com.ledgerx.app.core.finance.IndianTaxRules
import com.ledgerx.app.core.finance.AdvancedIndianTaxEngine
import com.ledgerx.app.core.finance.AdvancedTaxEstimate
import com.ledgerx.app.core.finance.TaxIncomeBreakdown
import com.ledgerx.app.core.importing.ImportPreview
import com.ledgerx.app.core.importing.ImportRepository
import com.ledgerx.app.core.importing.StatementColumnMapping
import com.ledgerx.app.core.model.Money
import com.ledgerx.app.core.planning.PlanningRepository
import com.ledgerx.app.core.planning.RecurringProcessor
import com.ledgerx.app.core.planning.ExtendedFinanceRepository
import com.ledgerx.app.core.reporting.ReportRepository
import com.ledgerx.app.core.security.SecurityPreferences
import com.ledgerx.app.core.security.DataDeletionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

data class LedgerXUiState(
    val accountCount: Int = -1,
    val accounts: List<AccountBalance> = emptyList(), val transactions: List<TransactionRow> = emptyList(),
    val bills: List<BillEntity> = emptyList(), val loans: List<LoanEntity> = emptyList(),
    val policies: List<InsurancePolicyEntity> = emptyList(), val goals: List<GoalEntity> = emptyList(),
    val instruments: List<InvestmentInstrumentEntity> = emptyList(), val investmentTransactions: List<InvestmentTransactionEntity> = emptyList(),
    val valuations: List<ValuationSnapshotEntity> = emptyList(), val deposits: List<DepositEntity> = emptyList(),
    val retirementContributions: List<RetirementContributionEntity> = emptyList(), val assets: List<PhysicalAssetEntity> = emptyList(),
    val assetValuations: List<AssetValuationEntity> = emptyList(), val taxProfile: TaxProfileEntity? = null,
    val taxPayments: List<TaxPaymentEntity> = emptyList(), val recurring: List<RecurringTemplateEntity> = emptyList(),
    val reconciliations: List<ReconciliationEntity> = emptyList(), val importPreview: ImportPreview? = null,
    val appLockEnabled: Boolean = false, val busy: Boolean = false, val message: String? = null,
    val confirmedMonthlyIncomeMinor: Long = 0, val confirmedMonthlyExpenseMinor: Long = 0, val confirmedMonthlyInvestmentMinor: Long = 0,
    val goalAllocations: List<GoalAllocationEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val lastUndoTransactionId: String? = null,
    val dividends: List<DividendRecordEntity> = emptyList(),
    val sips: List<SipScheduleEntity> = emptyList(),
    val depositContributions: List<DepositContributionEntity> = emptyList(),
    val retirementEvents: List<RetirementEventEntity> = emptyList(),
    val cardStatements: List<CreditCardStatementEntity> = emptyList(),
    val investmentLots: List<InvestmentLotEntity> = emptyList(),
    val pendingOccurrences: List<ScheduledOccurrenceEntity> = emptyList(),
    val merchantSuggestions: List<MerchantEntity> = emptyList(),
    val dashboardWidgets: List<DashboardWidgetEntity> = emptyList(),
    val planningAssumptions: PlanningAssumptionEntity? = null,
    val expenseByCategory: List<NamedAmount> = emptyList(),
    val expenseByMerchant: List<NamedAmount> = emptyList(),
    val monthlySpend: List<MonthlySpend> = emptyList(),
    val expenseComposition: List<NamedAmount> = emptyList(),
    val loanInterestAndFees12m: Long = 0,
    val healthHistory: List<HealthScoreSnapshotEntity> = emptyList(),
    val importBatches: List<ImportBatchEntity> = emptyList(),
    val incomeBreakdowns: List<IncomeBreakdownEntity> = emptyList(),
    val monthlyLedgerNetWorthMovement: Long = 0,
    val archivedAccounts: List<AccountEntity> = emptyList(),
    val loanSchedules: List<LoanScheduleEntity> = emptyList(),
    val receipts: List<ReceiptAttachmentEntity> = emptyList(),
    val policyDocuments: List<PolicyDocumentEntity> = emptyList(),
    val notificationSchedules: List<NotificationScheduleEntity> = emptyList(),
    val wealthNotes: List<WealthNoteEntity> = emptyList(),
) {
    val monthlyIncomeMinor get() = confirmedMonthlyIncomeMinor
    val monthlyExpenseMinor get() = confirmedMonthlyExpenseMinor
    val monthlyTakeHomeMinor get(): Long {
        val monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val reductions = incomeBreakdowns.filter { it.occurredAtEpochMillis >= monthStart }.sumOf { it.grossMinor - it.takeHomeMinor }
        return (confirmedMonthlyIncomeMinor - reductions).coerceAtLeast(0)
    }
    val investmentValueMinor: Long get() = instruments.sumOf { instrument ->
        val quantity = investmentTransactions.filter { it.instrumentId == instrument.id }.sumOf { if (it.type == "SELL") -it.quantityMicros else it.quantityMicros }
        val price = valuations.filter { it.instrumentId == instrument.id }.maxByOrNull { it.priceEpochDay }?.priceMinor
            ?: investmentTransactions.filter { it.instrumentId == instrument.id }.maxByOrNull { it.tradeEpochDay }?.priceMinor ?: 0L
        runCatching { BigDecimal(quantity).multiply(BigDecimal(price)).divide(BigDecimal(1_000_000), 0, java.math.RoundingMode.HALF_UP).longValueExact() }.getOrDefault(0L)
    }
    val retirementValueMinor get() = (retirementContributions.filter { it.scheme != "EPS" }.sumOf { it.employeeMinor + it.employerMinor } - retirementEvents.filter { it.type == "WITHDRAWAL" }.sumOf { it.amountMinor ?: 0L }).coerceAtLeast(0)
    val physicalAssetValueMinor: Long get() = assets.sumOf { asset ->
        val latest = assetValuations.filter { it.assetId == asset.id }.maxByOrNull { it.valuationEpochDay }?.estimatedMarketMinor ?: asset.purchaseMinor
        BigDecimal(latest).multiply(BigDecimal(asset.ownershipBasisPoints)).divide(BigDecimal(10_000), 0, java.math.RoundingMode.HALF_UP).longValueExact()
    }
    val netWorthMinor get() = Math.subtractExact(
        listOf(accounts.sumOf { it.balanceMinor }, investmentValueMinor, deposits.sumOf { it.principalMinor }, retirementValueMinor, physicalAssetValueMinor).fold(0L) { total, value -> Math.addExact(total, value) },
        loans.sumOf { it.outstandingPrincipalMinor },
    )
    val taxEstimates: Pair<AdvancedTaxEstimate, AdvancedTaxEstimate>? get() = taxProfile?.takeIf { it.financialYear == "2026-27" }?.let {
        val componentsTotal = it.salaryIncomeMinor + it.interestIncomeMinor + it.dividendIncomeMinor + it.shortTermCapitalGain111AMinor + it.longTermCapitalGain112AMinor
        val input = TaxIncomeBreakdown(
            salaryMinor = if (componentsTotal == 0L) it.grossIncomeMinor else it.salaryIncomeMinor,
            interestMinor = it.interestIncomeMinor, dividendMinor = it.dividendIncomeMinor,
            shortTermCapitalGain111AMinor = it.shortTermCapitalGain111AMinor,
            longTermCapitalGain112AMinor = it.longTermCapitalGain112AMinor,
            eligibleDeductionsMinor = it.eligibleDeductionsMinor,
        )
        AdvancedIndianTaxEngine.estimate(input, IndianTaxRules.taxYear2026Old()) to
            AdvancedIndianTaxEngine.estimate(input, IndianTaxRules.taxYear2026New())
    }
    fun dashboardVisible(key: String): Boolean = dashboardWidgets.firstOrNull { it.widgetKey == key }?.visible ?: true
    val lastUpdatedEpochMillis: Long? get() = listOfNotNull(transactions.maxOfOrNull { it.occurredAtEpochMillis }, valuations.maxOfOrNull { LocalDate.ofEpochDay(it.priceEpochDay).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }, assetValuations.maxOfOrNull { LocalDate.ofEpochDay(it.valuationEpochDay).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }).maxOrNull()
    val financialHealth get(): com.ledgerx.app.core.finance.HealthResult {
        val liquid=accounts.filter{it.kind!="CREDIT_CARD"}.sumOf{it.balanceMinor}.coerceAtLeast(0)
        val debtPayments=loans.sumOf{it.emiMinor}; val incomeRows=transactions.filter{it.type=="INCOME"}; val incomeTotal=incomeRows.sumOf{it.amountMinor}; val stableIncome=incomeRows.filter{it.categoryId=="salary"||it.categoryId=="pension"}.sumOf{it.amountMinor}
        val classes=listOf(accounts.sumOf{it.balanceMinor}.coerceAtLeast(0),investmentValueMinor,deposits.sumOf{it.principalMinor},retirementValueMinor,physicalAssetValueMinor).filter{it>0}; val totalAssets=classes.sum()
        val essential=planningAssumptions?.essentialMonthlyExpenseMinor?.takeIf{it>0}; val need=planningAssumptions?.statedInsuranceNeedMinor?.takeIf{it>0}; val coverage=policies.filter{it.active&&it.type in setOf("HEALTH","TERM_LIFE")}.sumOf{it.coverageMinor}
        return com.ledgerx.app.core.finance.HealthScoreCalculator.calculate(com.ledgerx.app.core.finance.HealthInputs(essential?.let{liquid.toDouble()/it},if(monthlyTakeHomeMinor>0)debtPayments*100.0/monthlyTakeHomeMinor else null,need?.let{coverage.toDouble()/it},com.ledgerx.app.core.accounting.LedgerMath.savingsRateBasisPoints(monthlyTakeHomeMinor,monthlyExpenseMinor)?.div(100.0),if(transactions.any{it.type=="INCOME"||it.type=="EXPENSE"})monthlyTakeHomeMinor-monthlyExpenseMinor else null,incomeTotal.takeIf{it>0}?.let{stableIncome*100.0/it},totalAssets.takeIf{it>0}?.let{classes.maxOrNull()!!*100.0/it}))
    }
}

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LedgerXViewModel @Inject constructor(
    private val ledger: LedgerRepository, private val planning: PlanningRepository,
    private val recurringProcessor: RecurringProcessor, private val security: SecurityPreferences,
    private val importer: ImportRepository, private val backups: BackupRepository, private val reports: ReportRepository,
    private val extended: ExtendedFinanceRepository,
    private val deletion: DataDeletionRepository,
    private val wealthNotesDao: WealthNotesDao,
) : ViewModel() {
    val themeMode: StateFlow<String> = security.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "SYSTEM")
    val lastAccountId: StateFlow<String?> = security.lastAccountId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val lastCategoryId: StateFlow<String?> = security.lastCategoryId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val textScale: StateFlow<Float> = security.textScale.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1f)
    val remindersEnabled: StateFlow<Boolean> = security.remindersEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val localeTag: StateFlow<String> = security.localeTag.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "en-IN")
    private val busy = MutableStateFlow(false); private val message = MutableStateFlow<String?>(null)
    private val lastUndoTransactionId = MutableStateFlow<String?>(null)
    private val importPreview = MutableStateFlow<ImportPreview?>(null)
    private val activityQuery = MutableStateFlow("")
    private val activityType = MutableStateFlow("ALL")
    val pagedTransactions = combine(activityQuery, activityType) { query, type -> query to type }
        .flatMapLatest { (query, type) -> ledger.pagedTransactions(query, type) }
        .cachedIn(viewModelScope)
    private data class CoreBase(val count: Int, val accounts: List<AccountBalance>, val tx: List<TransactionRow>, val totals: MonthlyTotals)
    private data class Analytics(val categories: List<NamedAmount>, val merchants: List<NamedAmount>, val months: List<MonthlySpend>, val composition: List<NamedAmount>, val netWorthMovement: Long)
    private data class Core(val base: CoreBase, val categories: List<CategoryEntity>, val analytics: Analytics)
    private data class Daily(val bills: List<BillEntity>, val loans: List<LoanEntity>, val policies: List<InsurancePolicyEntity>, val goals: List<GoalEntity>, val lock: Boolean)
    private data class WealthA(val instruments: List<InvestmentInstrumentEntity>, val tx: List<InvestmentTransactionEntity>, val values: List<ValuationSnapshotEntity>, val deposits: List<DepositEntity>, val retirement: List<RetirementContributionEntity>)
    private data class WealthB(val assets: List<PhysicalAssetEntity>, val values: List<AssetValuationEntity>, val tax: TaxProfileEntity?, val payments: List<TaxPaymentEntity>, val recurring: List<RecurringTemplateEntity>)
    private data class Planning(val daily: Daily, val a: WealthA, val b: WealthB, val reconciliations: List<ReconciliationEntity>, val allocations: List<GoalAllocationEntity>)
    private data class ExtendedA(val dividends: List<DividendRecordEntity>, val sips: List<SipScheduleEntity>, val lots: List<InvestmentLotEntity>, val merchants: List<MerchantEntity>, val income: List<IncomeBreakdownEntity>)
    private data class ExtendedB(val contributions: List<DepositContributionEntity>, val retirementEvents: List<RetirementEventEntity>, val statements: List<CreditCardStatementEntity>, val widgets: List<DashboardWidgetEntity>, val assumptions: PlanningAssumptionEntity?)
    private data class ExtendedC(val loanSchedules: List<LoanScheduleEntity>, val receipts: List<ReceiptAttachmentEntity>, val policyDocuments: List<PolicyDocumentEntity>)
    private data class Extended(val a: ExtendedA, val b: ExtendedB, val c: ExtendedC)
    private data class History(val health: List<HealthScoreSnapshotEntity>, val imports: List<ImportBatchEntity>, val archivedAccounts: List<AccountEntity>)
    private data class PlanningAux(val pending: List<ScheduledOccurrenceEntity>, val loanInterestAndFees12m: Long, val history: History, val notifications: List<NotificationScheduleEntity>)
    private data class PlanningAll(val base: Planning, val extended: Extended, val aux: PlanningAux)
    private data class Notice(val message: String?, val undoId: String?)
    private val coreBase = combine(ledger.accountCount, ledger.accountBalances, ledger.recentTransactions, ledger.monthlyTotals) { c, a, t, totals -> CoreBase(c, a, t, totals) }
    private val analytics = combine(ledger.expenseByCategory, ledger.expenseByMerchant, ledger.monthlySpend, ledger.expenseComposition, ledger.monthlyLedgerNetWorthMovement) { c, m, months, composition, movement -> Analytics(c, m, months, composition, movement) }
    private val core = combine(coreBase, ledger.categories, analytics) { base, categories, analysis -> Core(base, categories, analysis) }
    private val daily = combine(planning.bills, planning.loans, planning.policies, planning.goals, security.appLockEnabled) { b, l, p, g, lock -> Daily(b, l, p, g, lock) }
    private val wealthA = combine(planning.instruments, planning.investmentTransactions, planning.valuations, planning.deposits, planning.retirementContributions) { i, t, v, d, r -> WealthA(i, t, v, d, r) }
    private val wealthB = combine(planning.assets, planning.assetValuations, planning.taxProfile, planning.taxPayments, planning.recurring) { a, v, t, p, r -> WealthB(a, v, t, p, r) }
    private val planningBase = combine(daily, wealthA, wealthB, planning.reconciliations, planning.goalAllocations) { d, a, b, r, allocations -> Planning(d, a, b, r, allocations) }
    private val extendedA = combine(extended.dividends, extended.sips, extended.investmentLots, extended.merchantSuggestions, extended.incomeBreakdowns) { d, s, lots, merchants, income -> ExtendedA(d, s, lots, merchants, income) }
    private val extendedB = combine(extended.depositContributions, extended.retirementEvents, extended.cardStatements, extended.dashboardWidgets, extended.planningAssumptions) { c, r, statements, widgets, assumptions -> ExtendedB(c, r, statements, widgets, assumptions) }
    private val extendedC = combine(extended.loanSchedules, extended.receipts, extended.policyDocuments) { schedules, receipts, documents -> ExtendedC(schedules, receipts, documents) }
    private val extendedState = combine(extendedA, extendedB, extendedC) { a, b, c -> Extended(a, b, c) }
    private val history = combine(planning.healthHistory, planning.importBatches, extended.archivedAccounts) { health, imports, archived -> History(health, imports, archived) }
    private val planningAux = combine(planning.pendingOccurrences, planning.loanInterestAndFees12m, history, planning.notifications) { pending, leakage, history, notifications -> PlanningAux(pending, leakage, history, notifications) }
    private val planningState = combine(planningBase, extendedState, planningAux) { base, extras, aux -> PlanningAll(base, extras, aux) }

    private val notice = combine(message, lastUndoTransactionId) { text, undo -> Notice(text, undo) }
    val state: StateFlow<LedgerXUiState> = combine(core, planningState, importPreview, busy, notice) { c, p, preview, isBusy, notice ->
        LedgerXUiState(
            accountCount = c.base.count, accounts = c.base.accounts, transactions = c.base.tx,
            bills = p.base.daily.bills, loans = p.base.daily.loans, policies = p.base.daily.policies, goals = p.base.daily.goals,
            instruments = p.base.a.instruments, investmentTransactions = p.base.a.tx, valuations = p.base.a.values,
            deposits = p.base.a.deposits, retirementContributions = p.base.a.retirement,
            assets = p.base.b.assets, assetValuations = p.base.b.values, taxProfile = p.base.b.tax, taxPayments = p.base.b.payments,
            recurring = p.base.b.recurring, reconciliations = p.base.reconciliations, importPreview = preview,
            appLockEnabled = p.base.daily.lock, busy = isBusy, message = notice.message,
            confirmedMonthlyIncomeMinor = c.base.totals.incomeMinor, confirmedMonthlyExpenseMinor = c.base.totals.expenseMinor,
            confirmedMonthlyInvestmentMinor = c.base.totals.investmentMinor, goalAllocations = p.base.allocations,
            categories = c.categories, lastUndoTransactionId = notice.undoId, dividends = p.extended.a.dividends,
            sips = p.extended.a.sips, depositContributions = p.extended.b.contributions,
            retirementEvents = p.extended.b.retirementEvents, cardStatements = p.extended.b.statements,
            investmentLots = p.extended.a.lots,
            pendingOccurrences = p.aux.pending,
            merchantSuggestions = p.extended.a.merchants,
            dashboardWidgets = p.extended.b.widgets,
            planningAssumptions = p.extended.b.assumptions,
            expenseByCategory = c.analytics.categories, expenseByMerchant = c.analytics.merchants,
            monthlySpend = c.analytics.months, expenseComposition = c.analytics.composition,
            loanInterestAndFees12m = p.aux.loanInterestAndFees12m,
            healthHistory = p.aux.history.health, importBatches = p.aux.history.imports,
            incomeBreakdowns = p.extended.a.income,
            monthlyLedgerNetWorthMovement = c.analytics.netWorthMovement,
            archivedAccounts = p.aux.history.archivedAccounts,
            loanSchedules = p.extended.c.loanSchedules,
            receipts = p.extended.c.receipts,
            policyDocuments = p.extended.c.policyDocuments,
            notificationSchedules = p.aux.notifications,
        )
    }.combine(wealthNotesDao.observeNotes()) { current, notes -> current.copy(wealthNotes = notes) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LedgerXUiState())

    init { viewModelScope.launch { ledger.initialize(); extended.ensureProfile(); planning.ensureOfficialTaxRules(); recurringProcessor.processDue() } }
    fun createAccount(name: String, kind: String, opening: String) = action("Account created") { security.setLastAccountId(ledger.createAccount(name, kind, Money.parseMajor(opening.ifBlank { "0" }))) }
    fun completeOnboarding(name: String, kind: String, opening: String, monthlyIncome: String, enableLock: Boolean) = action("Setup complete") {
        val accountId = ledger.createAccount(name, kind, Money.parseMajor(opening.ifBlank { "0" }))
        security.setLastAccountId(accountId)
        monthlyIncome.takeIf(String::isNotBlank)?.let(Money::parseMajor)?.takeIf { it.minor > 0 }?.let { ledger.recordIncome(accountId, it, "salary", "Expected monthly income", "Optional onboarding income") }
        if (enableLock) security.setAppLockEnabled(true)
    }
    fun createCategory(name: String, kind: String, parent: String = "") = action("Category saved") { ledger.createCategory(name, kind, parent.takeIf(String::isNotBlank)) }
    fun recordTransaction(type: String, account: String, amount: String, category: String, merchant: String, note: String, date: String) = actionWithUndo("${type.lowercase().replaceFirstChar(Char::uppercase)} recorded") {
        val occurredAt = LocalDate.parse(date).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val id = when(type) { "EXPENSE" -> ledger.recordExpense(account, Money.parseMajor(amount), category, merchant, note, occurredAtEpochMillis = occurredAt); "REFUND" -> ledger.recordRefund(account, Money.parseMajor(amount), category, merchant, note, occurredAt); "REIMBURSEMENT" -> ledger.recordReimbursement(account, Money.parseMajor(amount), category, merchant, note, occurredAt); else -> ledger.recordIncome(account, Money.parseMajor(amount), category, merchant, note, occurredAtEpochMillis = occurredAt) }
        extended.rememberMerchant(merchant, category)
        security.setLastAccountId(account)
        security.setLastCategoryId(category)
        id
    }
    fun recordDetailedIncome(account: String, gross: String, takeHome: String, deductions: String, taxWithheld: String, category: String, source: String, note: String, date: String) = actionWithUndo("Detailed income recorded") {
        extended.recordDetailedIncome(account, Money.parseMajor(gross), Money.parseMajor(takeHome), Money.parseMajor(deductions.ifBlank { "0" }), Money.parseMajor(taxWithheld.ifBlank { "0" }), category, source, note, LocalDate.parse(date))
    }
    fun transfer(from: String, to: String, amount: String, note: String) = actionWithUndo("Transfer recorded") { ledger.transfer(from, to, Money.parseMajor(amount), note).also { security.setLastAccountId(from) } }
    fun reverseTransaction(id: String) = action("Transaction reversed") { ledger.reverseTransaction(id) }
    fun editTransaction(id: String, amount: String, category: String, merchant: String, note: String) = action("Transaction updated") { ledger.replaceSimpleTransaction(id, Money.parseMajor(amount), category, merchant, note); extended.rememberMerchant(merchant, category) }
    fun addSplitExpense(account: String, categoryOne: String, amountOne: String, categoryTwo: String, amountTwo: String, merchant: String, note: String) = actionWithUndo("Split expense recorded") {
        ledger.recordSplitExpense(account, listOf(LedgerRepository.ExpenseSplit(categoryOne, Money.parseMajor(amountOne)), LedgerRepository.ExpenseSplit(categoryTwo, Money.parseMajor(amountTwo))), merchant, note)
    }
    fun setAppLockEnabled(value: Boolean) = action(if (value) "App lock enabled" else "App lock disabled") { security.setAppLockEnabled(value) }
    fun setThemeMode(value: String) = action("Theme updated") { security.setThemeMode(value) }
    fun setTextScale(value: Float) = action("Text size updated") { security.setTextScale(value) }
    fun setRemindersEnabled(value: Boolean) = action(if(value) "Reminders enabled" else "Reminders disabled") { security.setRemindersEnabled(value) }
    fun setLocaleTag(value: String) = action("Number locale updated") { security.setLocaleTag(value) }
    fun addBill(name: String, amount: String, due: String, frequency: String?) = action("Bill saved") { planning.addBill(name, Money.parseMajor(amount), LocalDate.parse(due), frequency) }
    fun markBillPaid(id: String) = action("Bill marked paid") { planning.markBillPaid(id) }
    fun payBill(id: String, account: String, category: String, date: String) = action("Bill payment saved") { planning.payBill(id,account,category,LocalDate.parse(date)) }
    fun addLoan(name: String, principal: String, rate: String, months: String) = action("Loan saved") { planning.addLoan(name, "OTHER", Money.parseMajor(principal), basisPoints(rate), months.toInt(), LocalDate.now().plusMonths(1)) }
    fun payLoan(account: String, loan: String, principal: String, interest: String, fee: String, date: String) = action("Loan payment saved") { planning.recordLoanPayment(account, loan, Money.parseMajor(principal), Money.parseMajor(interest.ifBlank{"0"}), Money.parseMajor(fee.ifBlank{"0"}), LocalDate.parse(date)) }
    fun updateLoanRate(loan: String, rate: String, effective: String) = action("Loan rate and schedule updated") { planning.updateLoanRate(loan,basisPoints(rate),LocalDate.parse(effective)) }
    fun addPolicy(name: String, insurer: String, coverage: String, premium: String, renewal: String) = action("Policy saved") { planning.addPolicy(name, "OTHER", insurer, Money.parseMajor(coverage), Money.parseMajor(premium), LocalDate.parse(renewal)) }
    fun addPolicyFull(name: String, type: String, insurer: String, coverage: String, premium: String, deductible: String, renewal: String, nominees: String, exclusions: String) = action("Policy saved") { planning.addPolicy(name, type, insurer, Money.parseMajor(coverage), Money.parseMajor(premium), LocalDate.parse(renewal), deductible.takeIf(String::isNotBlank)?.let(Money::parseMajor), nominees, exclusions) }
    fun addGoal(name: String, type: String, target: String, due: String, monthly: String) = action("Goal saved") { planning.addGoal(name, type, Money.parseMajor(target), LocalDate.parse(due), Money.parseMajor(monthly.ifBlank { "0" })) }
    fun allocateGoal(goal: String, account: String, amount: String) = action("Goal allocation saved") { planning.allocateGoal(goal, account, Money.parseMajor(amount)) }
    fun addRecurring(account: String, type: String, amount: String, category: String, merchant: String, frequency: String, interval: String, due: String) = action("Recurring transaction saved") { planning.addRecurring(account, type, Money.parseMajor(amount), category, merchant, frequency, interval.toInt(), LocalDate.parse(due)) }
    fun confirmScheduledIncome(id: String) = action("Scheduled income confirmed") { recurringProcessor.confirmIncome(id) }
    fun addInvestment(account: String, name: String, symbol: String, assetClass: String, quantity: String, price: String, fees: String, date: String) = action("Investment saved") { planning.addInvestment(account, name, symbol, assetClass, quantityMicros(quantity), Money.parseMajor(price), Money.parseMajor(fees.ifBlank { "0" }), LocalDate.parse(date)) }
    fun buyExistingInvestment(account: String, instrument: String, quantity: String, price: String, fees: String, date: String) = action("Investment purchase saved") { planning.buyExistingInvestment(account, instrument, quantityMicros(quantity), Money.parseMajor(price), Money.parseMajor(fees.ifBlank { "0" }), LocalDate.parse(date)) }
    fun addValuation(instrument: String, price: String, date: String, source: String) = action("Valuation saved") { planning.addValuation(instrument, Money.parseMajor(price), LocalDate.parse(date), source) }
    fun sellInvestment(account: String, instrument: String, quantity: String, price: String, fees: String, date: String) = action("Investment sale saved") { planning.sellInvestment(account, instrument, quantityMicros(quantity), Money.parseMajor(price), Money.parseMajor(fees.ifBlank{"0"}), LocalDate.parse(date)) }
    fun addCorporateAction(instrument: String, quantity: String, date: String, type: String) = action("Corporate action saved") { planning.addCorporateQuantity(instrument, quantityMicros(quantity), LocalDate.parse(date), type) }
    fun addDividend(account: String, instrument: String, gross: String, tds: String, date: String) = action("Dividend saved") { extended.recordDividend(account, instrument, Money.parseMajor(gross), Money.parseMajor(tds.ifBlank { "0" }), LocalDate.parse(date)) }
    fun addSip(account: String, instrument: String, amount: String, frequency: String, interval: String, due: String) = action("SIP schedule saved") { extended.createSip(instrument, account, Money.parseMajor(amount), frequency, interval.toInt(), LocalDate.parse(due)) }
    fun confirmSipPurchase(sip: String, quantity: String, price: String, fees: String, date: String) = action("SIP purchase saved") { planning.confirmSipPurchase(sip, quantityMicros(quantity), Money.parseMajor(price), Money.parseMajor(fees.ifBlank{"0"}), LocalDate.parse(date)) }
    fun addDeposit(account: String, name: String, type: String, principal: String, rate: String, opened: String, maturity: String, maturityAmount: String, lockInEnd: String, restrictions: String) = action("Deposit saved") { planning.addDeposit(account, name, type, Money.parseMajor(principal), basisPoints(rate), LocalDate.parse(opened), LocalDate.parse(maturity), maturityAmount.takeIf(String::isNotBlank)?.let(Money::parseMajor), lockInEnd.takeIf(String::isNotBlank)?.let(LocalDate::parse), restrictions) }
    fun addRetirement(scheme: String, employee: String, employer: String, pension: String, date: String, note: String) = action("Retirement contribution saved") { planning.addRetirementContribution(scheme, Money.parseMajor(employee.ifBlank { "0" }), Money.parseMajor(employer.ifBlank { "0" }), Money.parseMajor(pension.ifBlank { "0" }), LocalDate.parse(date), note) }
    fun addRetirementEvent(scheme: String, type: String, employer: String, amount: String, date: String, note: String) = action("Retirement event saved") { extended.recordRetirementEvent(scheme, type, LocalDate.parse(date), employer.takeIf(String::isNotBlank), amount.takeIf(String::isNotBlank)?.let(Money::parseMajor), note) }
    fun addRdContribution(account: String, deposit: String, amount: String, date: String) = action("RD contribution saved") { extended.contributeToDeposit(deposit, account, Money.parseMajor(amount), LocalDate.parse(date)) }
    fun addCardStatement(account: String, statementDate: String, dueDate: String, balance: String, minimum: String) = action("Card statement saved") { extended.saveCardStatement(account, LocalDate.parse(statementDate), LocalDate.parse(dueDate), Money.parseMajor(balance), Money.parseMajor(minimum.ifBlank { "0" })) }
    fun payCardStatement(account: String, statement: String, amount: String) = action("Card payment saved") { extended.payCardStatement(statement, account, Money.parseMajor(amount)) }
    fun archiveAccount(account: String) = action("Account archived") { extended.archiveAccount(account, true) }
    fun restoreAccount(account: String) = action("Account restored") { extended.archiveAccount(account, false) }
    fun renameAccount(account: String, name: String) = action("Account renamed") { extended.renameAccount(account, name) }
    fun setDashboardLayout(keys: List<String>) = action("Dashboard layout saved") { extended.saveDashboardLayout("primary", keys) }
    fun savePlanningAssumptions(essential: String, insuranceNeed: String, currentAge: String, retirementAge: String, retirementTarget: String, inflation: String, expectedReturn: String) = action("Planning assumptions saved") {
        extended.savePlanningAssumptions(
            Money.parseMajor(essential.ifBlank { "0" }), Money.parseMajor(insuranceNeed.ifBlank { "0" }),
            currentAge.takeIf(String::isNotBlank)?.toInt(), retirementAge.takeIf(String::isNotBlank)?.toInt(),
            retirementTarget.takeIf(String::isNotBlank)?.let(Money::parseMajor),
            basisPoints(inflation.ifBlank { "6" }), basisPoints(expectedReturn.ifBlank { "8" }),
        )
    }
    fun attachReceipt(transactionId: String, uri: Uri) = action("Receipt encrypted and attached") { extended.attachReceipt(transactionId, uri) }
    fun attachPolicyDocument(policyId: String, uri: Uri) = action("Policy document encrypted and attached") { extended.attachPolicyDocument(policyId, uri) }
    fun removeReceipt(id: String) = action("Receipt attachment removed") { extended.removeReceipt(id) }
    fun removePolicyDocument(id: String) = action("Policy document removed") { extended.removePolicyDocument(id) }
    fun addTransactionTag(transactionId: String, name: String) = action("Tag attached") { extended.createAndAttachTag(transactionId, name) }
    fun addAsset(name: String, type: String, purchase: String, date: String, ownership: String, market: String, resale: String, weightGrams: String, purityPercent: String, transactionCost: String, linkedLoan: String) = action("Asset saved") {
        val weightMg = weightGrams.takeIf(String::isNotBlank)?.toBigDecimal()?.movePointRight(3)?.longValueExact()
        val purityBp = purityPercent.takeIf(String::isNotBlank)?.toBigDecimal()?.movePointRight(2)?.intValueExact()
        planning.addAsset(name, type, Money.parseMajor(purchase), LocalDate.parse(date), ownership.toInt(), Money.parseMajor(market), resale.takeIf(String::isNotBlank)?.let(Money::parseMajor), weightMg, purityBp, transactionCost.takeIf(String::isNotBlank)?.let(Money::parseMajor), linkedLoan.takeIf(String::isNotBlank))
    }
    fun addAssetMaintenance(account: String, asset: String, description: String, amount: String, date: String) = action("Asset maintenance saved") { extended.recordAssetMaintenance(asset, account.takeIf(String::isNotBlank), description, Money.parseMajor(amount.ifBlank { "0" }), LocalDate.parse(date)) }
    fun addAssetValuation(asset: String, market: String, resale: String, date: String, source: String, assumptions: String) = action("Asset valuation saved") { planning.addAssetValuation(asset,Money.parseMajor(market),resale.takeIf(String::isNotBlank)?.let(Money::parseMajor),LocalDate.parse(date),source,assumptions) }
    fun saveTax(year: String, salary: String, interest: String, dividends: String, stcg: String, ltcg: String, deductions: String, tds: String, advance: String, regime: String) = action("Tax profile saved") {
        planning.saveTaxProfile(year, Money.parseMajor(salary.ifBlank { "0" }), Money.parseMajor(interest.ifBlank { "0" }), Money.parseMajor(dividends.ifBlank { "0" }), Money.parseMajor(stcg.ifBlank { "0" }), Money.parseMajor(ltcg.ifBlank { "0" }), Money.parseMajor(deductions.ifBlank { "0" }), Money.parseMajor(tds.ifBlank { "0" }), Money.parseMajor(advance.ifBlank { "0" }), regime)
    }
    fun saveHealthSnapshot(score: Int?, completeness: Int, dimensions: String, warnings: String) { viewModelScope.launch { planning.saveHealthSnapshot(score, completeness, dimensions, warnings) } }
    fun reconcile(account: String, end: String, statement: String) = action("Reconciliation saved") { planning.reconcile(account, LocalDate.parse(end), Money.parseSignedMajor(statement)) }
    fun previewImport(csv: String, date: String, description: String, amount: String) { runCatching { importer.preview(csv, StatementColumnMapping(date, description, amount)) }.onSuccess { importPreview.value = it }.onFailure { message.value = it.message ?: "Import preview failed" } }
    fun commitImport(account: String, file: String, csv: String, date: String, description: String, amount: String, expenseCategory: String, incomeCategory: String) = action("Statement imported") { importer.commit(account, file, csv, StatementColumnMapping(date, description, amount), expenseCategory, incomeCategory); importPreview.value = null }
    fun rollbackImport(batchId: String) = action("Import rolled back with audit-preserving reversals") { importer.rollback(batchId) }
    fun clearImportPreview() { importPreview.value = null }
    fun createBackup(uri: Uri, password: String) = action("Encrypted backup verified and saved") { val secret=password.toCharArray(); try { backups.create(uri, secret) } finally { secret.fill('\u0000') } }
    fun restoreBackup(uri: Uri, password: String) = action("Backup verified and restored") { val secret=password.toCharArray(); try { backups.restore(uri, secret) } finally { secret.fill('\u0000') } }
    fun exportTransactions(uri: Uri) = action("Transaction report exported") { reports.exportTransactions(uri) }
    fun exportFinancialReport(uri: Uri) = action("Financial report exported") { reports.exportFinancialReport(uri) }
    fun exportCa(uri: Uri, year: String) = action("CA working report exported") { reports.exportCaReport(uri, year) }
    fun saveWealthNote(id: String?, type: String, holdingId: String, title: String, body: String) = action("Wealth note saved") {
        require(type in setOf("ACCOUNT", "INVESTMENT", "DEPOSIT", "ASSET", "LOAN", "POLICY")) { "Unsupported holding type" }
        require(title.isNotBlank() && body.isNotBlank()) { "Enter a title and details" }
        require(title.length <= 120 && body.length <= 5_000) { "Note is too long" }
        wealthNotesDao.upsert(WealthNoteEntity(id ?: java.util.UUID.randomUUID().toString(), type, holdingId, title.trim(), body.trim(), System.currentTimeMillis()))
    }
    fun deleteWealthNote(id: String) = action("Wealth note deleted") { wealthNotesDao.delete(id) }
    fun clearMessage() { message.value = null; lastUndoTransactionId.value = null }
    fun setActivityQuery(value: String) { activityQuery.value = value }
    fun setActivityType(value: String) { activityType.value = value }
    fun showError(error: Throwable) { message.value = error.message ?: "Unable to read the selected file" }
    fun deleteAllLocalData(confirmation: String, onDeleted: () -> Unit) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            message.value = null
            runCatching { deletion.eraseAllLocalData(confirmation) }
                .onSuccess { onDeleted() }
                .onFailure { message.value = it.message ?: "Unable to delete local data"; busy.value = false }
        }
    }
    private fun action(success: String, block: suspend () -> Unit) { if (busy.value) return; viewModelScope.launch { busy.value = true; message.value = null; lastUndoTransactionId.value=null; runCatching { block() }.onSuccess { message.value = success }.onFailure { message.value = it.message ?: "Something went wrong" }; busy.value = false } }
    private fun actionWithUndo(success: String, block: suspend () -> String) { if (busy.value) return; viewModelScope.launch { busy.value=true; message.value=null; lastUndoTransactionId.value=null; runCatching { block() }.onSuccess { id -> lastUndoTransactionId.value=id; message.value=success }.onFailure { message.value=it.message?:"Something went wrong" }; busy.value=false } }
    private fun basisPoints(percent: String) = percent.toBigDecimal().movePointRight(2).intValueExact()
    private fun quantityMicros(quantity: String) = BigDecimal(quantity).movePointRight(6).longValueExact()
}
