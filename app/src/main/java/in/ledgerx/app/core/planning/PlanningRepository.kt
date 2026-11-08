package com.ledgerx.app.core.planning

import com.ledgerx.app.core.database.BillEntity
import com.ledgerx.app.core.database.GoalEntity
import com.ledgerx.app.core.database.GoalAllocationEntity
import com.ledgerx.app.core.database.InsurancePolicyEntity
import com.ledgerx.app.core.database.LoanEntity
import com.ledgerx.app.core.database.DepositEntity
import com.ledgerx.app.core.database.InvestmentInstrumentEntity
import com.ledgerx.app.core.database.InvestmentTransactionEntity
import com.ledgerx.app.core.database.ValuationSnapshotEntity
import com.ledgerx.app.core.database.RetirementContributionEntity
import com.ledgerx.app.core.database.PhysicalAssetEntity
import com.ledgerx.app.core.database.AssetValuationEntity
import com.ledgerx.app.core.database.RecurringTemplateEntity
import com.ledgerx.app.core.database.ReconciliationEntity
import com.ledgerx.app.core.database.TaxProfileEntity
import com.ledgerx.app.core.database.TaxPaymentEntity
import com.ledgerx.app.core.database.TaxRuleEntity
import com.ledgerx.app.core.database.HealthScoreSnapshotEntity
import com.ledgerx.app.core.database.ExtendedDao
import com.ledgerx.app.core.database.InvestmentLotEntity
import com.ledgerx.app.core.database.LoanScheduleEntity
import com.ledgerx.app.core.database.NotificationScheduleEntity
import com.ledgerx.app.core.database.LedgerXDatabase
import com.ledgerx.app.core.database.LedgerDao
import androidx.room.withTransaction
import com.ledgerx.app.core.database.PlanningDao
import com.ledgerx.app.core.finance.LoanCalculator
import com.ledgerx.app.core.finance.CostBasisLot
import com.ledgerx.app.core.finance.InvestmentCostBasisCalculator
import com.ledgerx.app.core.accounting.LedgerRepository
import com.ledgerx.app.core.model.Money
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanningRepository @Inject constructor(
    private val dao: PlanningDao,
    private val ledger: LedgerRepository,
    private val database: LedgerXDatabase,
    private val ledgerDao: LedgerDao,
    private val extendedDao: ExtendedDao,
) {
    private val clock = Clock.systemUTC()

    val bills: Flow<List<BillEntity>> = dao.observeBills()
    val loans: Flow<List<LoanEntity>> = dao.observeLoans()
    val policies: Flow<List<InsurancePolicyEntity>> = dao.observePolicies()
    val goals: Flow<List<GoalEntity>> = dao.observeGoals()
    val goalAllocations = dao.observeGoalAllocations()
    val instruments = dao.observeInstruments()
    val investmentTransactions = dao.observeInvestmentTransactions()
    val valuations = dao.observeValuations()
    val deposits = dao.observeDeposits()
    val retirementContributions = dao.observeRetirementContributions()
    val assets = dao.observeAssets()
    val assetValuations = dao.observeAssetValuations()
    val taxProfile = dao.observeLatestTaxProfile()
    val taxPayments = dao.observeTaxPayments()
    val recurring = dao.observeRecurring()
    val reconciliations = dao.observeReconciliations()
    val pendingOccurrences = dao.observePendingOccurrences()
    val healthHistory = dao.observeHealthHistory()
    val importBatches = dao.observeImportBatches()
    val notifications = dao.observeNotifications()
    val loanInterestAndFees12m: Flow<Long> = dao.observeLoanInterestAndFees(LocalDate.now(clock).minusMonths(12).toEpochDay())

    suspend fun addBill(name: String, amount: Money, dueDate: LocalDate, frequency: String?): String {
        require(name.isNotBlank() && amount.minor > 0)
        val existing = dao.billByName(name.trim())
        val id = existing?.id ?: UUID.randomUUID().toString()
        dao.saveBill(BillEntity(id, name.trim(), existing?.merchant, amount.minor, amount.currencyCode, dueDate.toEpochDay(), frequency, "DUE", existing?.amountMinor?.takeIf { it != amount.minor }, 3))
        extendedDao.saveBillPrice(com.ledgerx.app.core.database.BillPriceHistoryEntity(UUID.randomUUID().toString(), id, amount.minor, LocalDate.now(clock).toEpochDay()))
        return id
    }
    suspend fun markBillPaid(id: String) { require(dao.markBillPaid(id) == 1) { "Bill is already paid or no longer exists" } }
    suspend fun payBill(id: String, accountId: String, categoryId: String, date: LocalDate): String = database.withTransaction {
        val bill=requireNotNull(dao.bill(id)){"Bill not found"}; require(bill.status=="DUE") { "Bill is already paid" }
        val transactionId=ledger.recordExpense(accountId,Money(bill.amountMinor,bill.currencyCode),categoryId,bill.merchant?:bill.name,"Bill payment: ${bill.name}",origin="BILL",occurredAtEpochMillis=date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        require(dao.markBillPaid(id)==1){"Bill status changed during payment"}
        transactionId
    }

    suspend fun addLoan(
        name: String,
        type: String,
        principal: Money,
        annualRateBasisPoints: Int,
        months: Int,
        nextDueDate: LocalDate?,
    ): String = database.withTransaction {
        require(name.isNotBlank())
        val schedule = LoanCalculator.amortize(principal.minor, annualRateBasisPoints, months)
        val id = UUID.randomUUID().toString()
        dao.saveLoan(LoanEntity(id, name.trim(), type, principal.minor, principal.minor, annualRateBasisPoints, schedule.emiMinor, months, nextDueDate?.toEpochDay(), true))
        nextDueDate?.let { firstDue ->
            extendedDao.saveLoanSchedule(schedule.payments.map { payment ->
                LoanScheduleEntity(
                    id = "$id:${payment.installment}", loanId = id, installmentNumber = payment.installment,
                    dueEpochDay = firstDue.plusMonths((payment.installment - 1).toLong()).toEpochDay(),
                    openingMinor = payment.openingMinor, emiMinor = payment.paymentMinor,
                    principalMinor = payment.principalMinor, interestMinor = payment.interestMinor,
                    closingMinor = payment.closingMinor, status = "DUE",
                )
            })
        }
        id
    }

    suspend fun recordLoanPayment(accountId: String, loanId: String, principal: Money, interest: Money, fee: Money, date: LocalDate): String = database.withTransaction {
        val loan = requireNotNull(dao.loan(loanId)) { "Loan not found" }
        require(principal.minor in 1..loan.outstandingPrincipalMinor) { "Principal component exceeds outstanding balance" }
        val ledgerId = ledger.recordLoanPayment(accountId, loanId, principal, interest, fee, "Loan payment ${loan.name}", date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
        val remaining = loan.outstandingPrincipalMinor - principal.minor
        val paidInstallment = extendedDao.nextDueLoanInstallment(loanId)
        paidInstallment?.let { expected ->
            extendedDao.saveLoanSchedule(listOf(expected.copy(
                emiMinor = Math.addExact(principal.minor, interest.minor),
                principalMinor = principal.minor,
                interestMinor = interest.minor,
                closingMinor = remaining,
                status = "PAID",
            )))
        }
        extendedDao.deleteDueLoanSchedule(loanId)
        val remainingMonths = if (remaining > 0) (loan.remainingMonths - 1).coerceAtLeast(1) else 0
        val nextDue = paidInstallment?.dueEpochDay?.let { LocalDate.ofEpochDay(it).plusMonths(1) }
            ?: loan.nextDueEpochDay?.let { LocalDate.ofEpochDay(it).plusMonths(1) }
        val revised = if (remaining > 0) LoanCalculator.amortize(remaining, loan.annualRateBasisPoints, remainingMonths) else null
        revised?.let { schedule ->
            val firstNumber = (paidInstallment?.installmentNumber ?: extendedDao.lastLoanInstallmentNumber(loanId)) + 1
            val firstDate = nextDue ?: date.plusMonths(1)
            extendedDao.saveLoanSchedule(schedule.payments.map { payment ->
                val number = firstNumber + payment.installment - 1
                LoanScheduleEntity("$loanId:$number", loanId, number, firstDate.plusMonths((payment.installment - 1).toLong()).toEpochDay(), payment.openingMinor, payment.paymentMinor, payment.principalMinor, payment.interestMinor, payment.closingMinor, "DUE")
            })
        }
        dao.saveLoan(loan.copy(
            outstandingPrincipalMinor = remaining,
            annualRateBasisPoints = loan.annualRateBasisPoints,
            emiMinor = revised?.emiMinor ?: 0,
            remainingMonths = remainingMonths,
            nextDueEpochDay = if (remaining > 0) (nextDue ?: date.plusMonths(1)).toEpochDay() else null,
            active = remaining > 0,
        ))
        dao.saveLoanEvent(com.ledgerx.app.core.database.LoanEventEntity(UUID.randomUUID().toString(), loanId, "PAYMENT", principal.minor, interest.minor, fee.minor, null, date.toEpochDay(), ledgerId))
        ledgerId
    }

    suspend fun updateLoanRate(loanId: String, annualRateBasisPoints: Int, effectiveDate: LocalDate) = database.withTransaction {
        val loan = requireNotNull(dao.loan(loanId)) { "Loan not found" }
        require(annualRateBasisPoints in 0..10_000) { "Annual rate must be between 0% and 100%" }
        require(loan.outstandingPrincipalMinor > 0 && loan.remainingMonths > 0) { "Loan is already repaid" }
        val schedule = LoanCalculator.amortize(loan.outstandingPrincipalMinor, annualRateBasisPoints, loan.remainingMonths)
        dao.saveLoan(loan.copy(annualRateBasisPoints=annualRateBasisPoints,emiMinor=schedule.emiMinor,nextDueEpochDay=effectiveDate.toEpochDay()))
        extendedDao.deleteLoanSchedule(loanId)
        extendedDao.saveLoanSchedule(schedule.payments.map { payment -> LoanScheduleEntity("$loanId:${payment.installment}",loanId,payment.installment,effectiveDate.plusMonths((payment.installment-1).toLong()).toEpochDay(),payment.openingMinor,payment.paymentMinor,payment.principalMinor,payment.interestMinor,payment.closingMinor,"DUE") })
        dao.saveLoanEvent(com.ledgerx.app.core.database.LoanEventEntity(UUID.randomUUID().toString(),loanId,"RATE_CHANGE",0,0,0,annualRateBasisPoints,effectiveDate.toEpochDay(),null))
    }

    suspend fun addPolicy(
        name: String,
        type: String,
        insurer: String,
        coverage: Money,
        premium: Money,
        renewalDate: LocalDate,
        deductible: Money? = null,
        nominees: String? = null,
        exclusions: String? = null,
    ): String {
        require(name.isNotBlank() && insurer.isNotBlank() && coverage.minor >= 0 && premium.minor >= 0)
        require(coverage.currencyCode == premium.currencyCode && (deductible == null || deductible.currencyCode == coverage.currencyCode))
        val id = UUID.randomUUID().toString()
        dao.savePolicy(InsurancePolicyEntity(id, name.trim(), type, insurer.trim(), coverage.minor, premium.minor, deductible?.minor, renewalDate.toEpochDay(), nominees?.trim()?.takeIf(String::isNotEmpty), exclusions?.trim()?.takeIf(String::isNotEmpty), null, true))
        return id
    }

    suspend fun addGoal(name: String, type: String, target: Money, targetDate: LocalDate, monthlyContribution: Money): String {
        require(name.isNotBlank() && target.minor > 0 && monthlyContribution.minor >= 0)
        require(target.currencyCode == monthlyContribution.currencyCode)
        val id = UUID.randomUUID().toString()
        dao.saveGoal(GoalEntity(id, name.trim(), type, target.minor, targetDate.toEpochDay(), monthlyContribution.minor, "ACTIVE", clock.millis()))
        return id
    }

    suspend fun allocateGoal(goalId: String, accountId: String, amount: Money) {
        require(amount.minor >= 0)
        val goal = requireNotNull(dao.goal(goalId)) { "Goal not found" }
        require(Math.addExact(amount.minor, dao.otherGoalAllocations(goalId, accountId)) <= goal.targetMinor) { "Combined allocations cannot exceed the goal target" }
        require(Math.addExact(amount.minor, dao.otherAccountAllocations(accountId, goalId)) <= ledgerDao.accountBalance(accountId).coerceAtLeast(0)) { "Combined goal allocations cannot exceed the selected account balance" }
        dao.saveGoalAllocation(GoalAllocationEntity(goalId, accountId, amount.minor, clock.millis()))
    }

    suspend fun addRecurring(
        accountId: String,
        type: String,
        amount: Money,
        categoryId: String?,
        merchant: String?,
        frequency: String,
        intervalCount: Int,
        nextDueDate: LocalDate,
    ): String {
        require(type == "EXPENSE" || type == "INCOME")
        require(amount.minor > 0 && intervalCount > 0)
        require(frequency in setOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY"))
        val id = UUID.randomUUID().toString()
        dao.saveRecurring(RecurringTemplateEntity(id, accountId, type, amount.minor, amount.currencyCode, categoryId, merchant?.trim(), frequency, intervalCount, nextDueDate.toEpochDay(), true))
        return id
    }

    suspend fun addInvestment(
        accountId: String,
        name: String,
        symbol: String?,
        assetClass: String,
        quantityMicros: Long,
        price: Money,
        fees: Money,
        tradeDate: LocalDate,
    ): String = database.withTransaction {
        require(name.isNotBlank() && quantityMicros > 0 && price.minor >= 0 && fees.minor >= 0)
        require(price.currencyCode == fees.currencyCode)
        val instrumentId = UUID.randomUUID().toString()
        dao.saveInstrument(InvestmentInstrumentEntity(instrumentId, name.trim(), symbol?.trim()?.ifBlank { null }, null, null, assetClass, price.currencyCode))
        val unitsCost = BigDecimal(quantityMicros).multiply(BigDecimal(price.minor)).divide(BigDecimal(1_000_000), 0, RoundingMode.HALF_UP).longValueExact()
        val totalCost = Math.addExact(unitsCost, fees.minor)
        val investmentTransactionId = UUID.randomUUID().toString()
        val occurredAt = tradeDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val ledgerId = ledger.recordInvestmentPurchase(accountId, Money(totalCost, price.currencyCode), instrumentId, "Investment purchase: ${name.trim()}", occurredAt)
        dao.saveInvestmentTransaction(InvestmentTransactionEntity(investmentTransactionId, instrumentId, accountId, "BUY", quantityMicros, price.minor, fees.minor, tradeDate.toEpochDay(), ledgerId))
        extendedDao.saveLot(InvestmentLotEntity(UUID.randomUUID().toString(), instrumentId, investmentTransactionId, tradeDate.toEpochDay(), quantityMicros, quantityMicros, totalCost, totalCost, 0, 0, null))
        instrumentId
    }

    suspend fun addValuation(instrumentId: String, price: Money, date: LocalDate, source: String): String {
        require(price.minor >= 0 && source.isNotBlank())
        val id = UUID.randomUUID().toString()
        dao.saveValuation(ValuationSnapshotEntity(id, instrumentId, price.minor, price.currencyCode, date.toEpochDay(), source.trim(), clock.millis()))
        return id
    }

    suspend fun buyExistingInvestment(accountId: String, instrumentId: String, quantityMicros: Long, price: Money, fees: Money, tradeDate: LocalDate): String = database.withTransaction {
        val instrument = requireNotNull(dao.instrument(instrumentId)) { "Instrument not found" }
        require(quantityMicros > 0 && price.minor >= 0 && fees.minor >= 0 && price.currencyCode == fees.currencyCode && price.currencyCode == instrument.currencyCode)
        val unitsCost = BigDecimal(quantityMicros).multiply(BigDecimal(price.minor)).divide(BigDecimal(1_000_000), 0, RoundingMode.HALF_UP).longValueExact()
        val totalCost = Math.addExact(unitsCost, fees.minor)
        val investmentTransactionId = UUID.randomUUID().toString()
        val occurredAt = tradeDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val ledgerId = ledger.recordInvestmentPurchase(accountId, Money(totalCost, price.currencyCode), instrumentId, "Investment purchase: ${instrument.name}", occurredAt)
        dao.saveInvestmentTransaction(InvestmentTransactionEntity(investmentTransactionId, instrumentId, accountId, "BUY", quantityMicros, price.minor, fees.minor, tradeDate.toEpochDay(), ledgerId))
        extendedDao.saveLot(InvestmentLotEntity(UUID.randomUUID().toString(), instrumentId, investmentTransactionId, tradeDate.toEpochDay(), quantityMicros, quantityMicros, totalCost, totalCost, 0, 0, null))
        investmentTransactionId
    }

    suspend fun confirmSipPurchase(sipId: String, quantityMicros: Long, price: Money, fees: Money, tradeDate: LocalDate): String = database.withTransaction {
        val sip = requireNotNull(extendedDao.sip(sipId)) { "SIP schedule not found" }
        require(sip.active) { "SIP schedule is inactive" }
        require(tradeDate.toEpochDay() >= sip.nextDueEpochDay) { "Trade date cannot be before the scheduled due date" }
        val transactionId = buyExistingInvestment(sip.accountId, sip.instrumentId, quantityMicros, price, fees, tradeDate)
        val nextDue = RecurrenceEngine.next(LocalDate.ofEpochDay(sip.nextDueEpochDay), sip.frequency, sip.intervalCount)
        extendedDao.saveSip(sip.copy(nextDueEpochDay = nextDue.toEpochDay()))
        transactionId
    }

    suspend fun sellInvestment(accountId: String, instrumentId: String, quantityMicros: Long, price: Money, fees: Money, date: LocalDate): String = database.withTransaction {
        require(quantityMicros > 0 && price.minor > 0 && fees.minor >= 0 && price.currencyCode == fees.currencyCode)
        val available = dao.investmentQuantity(instrumentId)
        require(quantityMicros <= available) { "Sale quantity exceeds recorded holding" }
        val grossMinor = BigDecimal(quantityMicros).multiply(BigDecimal(price.minor)).divide(BigDecimal(1_000_000), 0, RoundingMode.HALF_UP).longValueExact()
        val lots = ensureLots(instrumentId).filter { it.remainingQuantityMicros > 0 }
        require(lots.sumOf { it.remainingQuantityMicros } >= quantityMicros) { "Cost-basis lots do not cover this sale" }
        val basis = InvestmentCostBasisCalculator.fifo(lots.map { CostBasisLot(it.id, it.remainingQuantityMicros, it.remainingCostMinor) }, quantityMicros, grossMinor)
        val byId = lots.associateBy { it.id }
        val updatedLots = basis.allocations.map { allocation ->
            val lot = requireNotNull(byId[allocation.lotId])
            val remainingQuantity = lot.remainingQuantityMicros - allocation.quantityMicros
            lot.copy(
                remainingQuantityMicros = remainingQuantity,
                remainingCostMinor = lot.remainingCostMinor - allocation.costMinor,
                realizedProceedsMinor = Math.addExact(lot.realizedProceedsMinor, allocation.proceedsMinor),
                realizedGainMinor = Math.addExact(lot.realizedGainMinor, allocation.proceedsMinor - allocation.costMinor),
                closedEpochDay = if (remainingQuantity == 0L) date.toEpochDay() else null,
            )
        }
        val occurredAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val ledgerId = ledger.recordInvestmentSale(accountId, Money(grossMinor, price.currencyCode), fees, Money(basis.costBasisMinor, price.currencyCode), instrumentId, "Investment sale", occurredAt)
        val id = UUID.randomUUID().toString()
        dao.saveInvestmentTransaction(InvestmentTransactionEntity(id, instrumentId, accountId, "SELL", quantityMicros, price.minor, fees.minor, date.toEpochDay(), ledgerId))
        updatedLots.forEach { extendedDao.saveLot(it) }
        id
    }

    suspend fun addCorporateQuantity(instrumentId: String, quantityMicros: Long, date: LocalDate, type: String): String = database.withTransaction {
        require(quantityMicros > 0 && type in setOf("SPLIT", "BONUS"))
        requireNotNull(dao.instrument(instrumentId)) { "Instrument not found" }
        val lots = ensureLots(instrumentId).filter { it.remainingQuantityMicros > 0 }
        val currentQuantity = lots.sumOf { it.remainingQuantityMicros }
        require(currentQuantity > 0) { "Corporate action needs an existing holding" }
        var additionalRemaining = quantityMicros
        lots.forEachIndexed { index, lot ->
            val additional = if (index == lots.lastIndex) additionalRemaining else BigDecimal(quantityMicros).multiply(BigDecimal(lot.remainingQuantityMicros)).divide(BigDecimal(currentQuantity), 0, RoundingMode.HALF_UP).longValueExact().coerceAtMost(additionalRemaining)
            additionalRemaining -= additional
            extendedDao.saveLot(lot.copy(originalQuantityMicros = Math.addExact(lot.originalQuantityMicros, additional), remainingQuantityMicros = Math.addExact(lot.remainingQuantityMicros, additional)))
        }
        check(additionalRemaining == 0L)
        val id = UUID.randomUUID().toString()
        dao.saveInvestmentTransaction(InvestmentTransactionEntity(id, instrumentId, "CORPORATE_ACTION", type, quantityMicros, 0, 0, date.toEpochDay(), null))
        id
    }

    suspend fun addDeposit(accountId: String, name: String, type: String, principal: Money, annualRateBasisPoints: Int, opened: LocalDate, maturity: LocalDate, maturityAmount: Money?, lockInEnd: LocalDate? = null, withdrawalRestrictions: String? = null): String = database.withTransaction {
        require(name.isNotBlank() && type in setOf("FD", "RD") && principal.minor > 0 && annualRateBasisPoints >= 0 && maturity >= opened)
        require(maturityAmount == null || maturityAmount.currencyCode == principal.currencyCode)
        val id = UUID.randomUUID().toString()
        ledger.recordAssetTransfer(accountId, principal, "DEPOSIT:$id", "$type creation: ${name.trim()}")
        require(lockInEnd == null || !lockInEnd.isBefore(opened)) { "Lock-in end cannot precede opening" }
        dao.saveDeposit(DepositEntity(id, name.trim(), type, principal.minor, annualRateBasisPoints, opened.toEpochDay(), maturity.toEpochDay(), maturityAmount?.minor, accountId, lockInEnd?.toEpochDay(), withdrawalRestrictions?.trim()?.takeIf(String::isNotEmpty)))
        id
    }

    suspend fun addRetirementContribution(scheme: String, employee: Money, employer: Money, pension: Money, date: LocalDate, note: String?): String {
        require(scheme in setOf("EPF", "EPS", "PPF", "NPS"))
        require(employee.minor >= 0 && employer.minor >= 0 && pension.minor >= 0)
        require(employee.currencyCode == employer.currencyCode && employee.currencyCode == pension.currencyCode)
        require(employee.minor + employer.minor + pension.minor > 0)
        val id = UUID.randomUUID().toString()
        val fyStart = if (date.monthValue >= 4) date.year else date.year - 1
        dao.saveRetirementContribution(RetirementContributionEntity(id, scheme, employee.minor, employer.minor, pension.minor, date.toEpochDay(), "$fyStart-${(fyStart + 1).toString().takeLast(2)}", note?.trim()))
        return id
    }

    suspend fun addAsset(name: String, type: String, purchase: Money, purchaseDate: LocalDate, ownershipPercent: Int, market: Money, resale: Money?, weightMilligrams: Long?, purityBasisPoints: Int?, transactionCost: Money?, linkedLoanId: String? = null): String {
        require(name.isNotBlank() && purchase.minor >= 0 && market.minor >= 0 && ownershipPercent in 1..100)
        require(purchase.currencyCode == market.currencyCode && (resale == null || resale.currencyCode == purchase.currencyCode))
        require(transactionCost == null || (transactionCost.minor >= 0 && transactionCost.currencyCode == purchase.currencyCode))
        if (type == "GOLD" || type == "SILVER") require(weightMilligrams != null && weightMilligrams > 0 && purityBasisPoints != null && purityBasisPoints in 1..10_000) { "Precious metals require positive weight and purity" }
        val id = UUID.randomUUID().toString()
        linkedLoanId?.let { requireNotNull(dao.loan(it)) { "Linked loan not found" } }
        dao.saveAsset(PhysicalAssetEntity(id, name.trim(), type, purchase.minor, purchaseDate.toEpochDay(), ownershipPercent * 100, linkedLoanId, weightMilligrams, purityBasisPoints, transactionCost?.minor, false))
        dao.saveAssetValuation(AssetValuationEntity(UUID.randomUUID().toString(), id, market.minor, resale?.minor, LocalDate.now(clock).toEpochDay(), "USER_ESTIMATE", "Manual offline estimate"))
        return id
    }

    suspend fun addAssetValuation(assetId: String, market: Money, resale: Money?, date: LocalDate, source: String, assumptions: String?): String {
        requireNotNull(dao.asset(assetId)) { "Asset not found" }
        require(market.minor >= 0 && (resale == null || resale.minor >= 0))
        val id=UUID.randomUUID().toString()
        dao.saveAssetValuation(AssetValuationEntity(id,assetId,market.minor,resale?.minor,date.toEpochDay(),source.trim().ifBlank{"Manual user estimate"},assumptions?.trim()?.takeIf(String::isNotEmpty)))
        return id
    }

    suspend fun reconcile(accountId: String, statementEnd: LocalDate, statementBalance: Money): String {
        val id = UUID.randomUUID().toString()
        val endMillis = statementEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        val ledgerBalanceMinor = ledgerDao.accountBalanceAt(accountId, endMillis)
        val difference = Math.subtractExact(statementBalance.minor, ledgerBalanceMinor)
        val completed = if (difference == 0L) clock.millis() else null
        dao.saveReconciliation(ReconciliationEntity(id, accountId, statementEnd.toEpochDay(), statementBalance.minor, ledgerBalanceMinor, difference, completed))
        if (difference == 0L) {
            dao.markAccountReconciled(accountId, endMillis, clock.millis())
        }
        return id
    }

    suspend fun saveTaxProfile(financialYear: String, salary: Money, interest: Money, dividends: Money, stcg111a: Money, ltcg112a: Money, deductions: Money, tds: Money, advanceTax: Money, preferredRegime: String?): String {
        require(Regex("\\d{4}-\\d{2}").matches(financialYear))
        val values = listOf(salary, interest, dividends, stcg111a, ltcg112a, deductions, tds, advanceTax)
        require(values.all { it.minor >= 0 && it.currencyCode == salary.currencyCode })
        val gross = listOf(salary, interest, dividends, stcg111a, ltcg112a).fold(0L) { total, value -> Math.addExact(total, value.minor) }
        val existingId = "tax-$financialYear"
        dao.saveTaxProfile(TaxProfileEntity(existingId, financialYear, preferredRegime, gross, deductions.minor, tds.minor, advanceTax.minor, clock.millis(), salary.minor, interest.minor, dividends.minor, stcg111a.minor, ltcg112a.minor))
        val startYear = financialYear.substringBefore('-').toInt()
        val today = LocalDate.now(clock)
        listOf(LocalDate.of(startYear,6,15),LocalDate.of(startYear,9,15),LocalDate.of(startYear,12,15),LocalDate.of(startYear+1,3,15)).forEach { deadline ->
            dao.saveNotification(NotificationScheduleEntity("tax-$financialYear-${deadline}","ADVANCE_TAX",existingId,"Advance-tax checkpoint • $deadline",deadline.atTime(9,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),"financial-reminders",!deadline.isBefore(today)))
        }
        return existingId
    }

    suspend fun addTaxPayment(financialYear: String, type: String, amount: Money, date: LocalDate, reference: String?): String {
        require(amount.minor > 0)
        val id = UUID.randomUUID().toString()
        dao.saveTaxPayment(TaxPaymentEntity(id, financialYear, type, amount.minor, date.toEpochDay(), reference?.trim()))
        return id
    }

    suspend fun ensureOfficialTaxRules() {
        val verifiedAt = 1_793_990_400_000L // 2026-11-07; update only after re-verifying official sources.
        dao.saveTaxRule(TaxRuleEntity("2026-27-new-v1", "2026-27", "NEW", 1, LocalDate.of(2026,4,1).toEpochDay(), "{\"standardDeductionMinor\":7500000,\"rebateLimitMinor\":120000000,\"rebateMaximumMinor\":6000000,\"cessBasisPoints\":400,\"eligibleOldRegimeDeductions\":false,\"stcg111aBasisPoints\":2000,\"ltcg112aBasisPoints\":1250,\"ltcg112aExemptionMinor\":12500000,\"slabs\":[[0,40000000,0],[40000000,80000000,500],[80000000,120000000,1000],[120000000,160000000,1500],[160000000,200000000,2000],[200000000,240000000,2500],[240000000,null,3000]]}", "https://www.indiabudget.gov.in/doc/memo.pdf", verifiedAt))
        dao.saveTaxRule(TaxRuleEntity("2026-27-old-v1", "2026-27", "OLD", 1, LocalDate.of(2026,4,1).toEpochDay(), "{\"standardDeductionMinor\":5000000,\"rebateLimitMinor\":50000000,\"rebateMaximumMinor\":1250000,\"cessBasisPoints\":400,\"eligibleOldRegimeDeductions\":true,\"stcg111aBasisPoints\":2000,\"ltcg112aBasisPoints\":1250,\"ltcg112aExemptionMinor\":12500000,\"slabs\":[[0,25000000,0],[25000000,50000000,500],[50000000,100000000,2000],[100000000,null,3000]]}", "https://www.indiabudget.gov.in/doc/memo.pdf", verifiedAt))
    }

    suspend fun saveHealthSnapshot(score: Int?, completeness: Int, dimensionsJson: String, warningsJson: String) {
        val date = LocalDate.now(clock)
        dao.saveHealthSnapshot(HealthScoreSnapshotEntity("health-${date}", score, completeness, dimensionsJson, warningsJson, clock.millis()))
    }

    private suspend fun ensureLots(instrumentId: String): List<InvestmentLotEntity> {
        val existing = extendedDao.lots(instrumentId)
        if (existing.isNotEmpty()) return existing
        val buys = dao.investmentTransactionsForInstrument(instrumentId).filter { it.type == "BUY" }
        val generated = buys.map { transaction ->
            val cost = Math.addExact(
                BigDecimal(transaction.quantityMicros).multiply(BigDecimal(transaction.priceMinor)).divide(BigDecimal(1_000_000), 0, RoundingMode.HALF_UP).longValueExact(),
                transaction.feesMinor,
            )
            InvestmentLotEntity("migrated-lot-${transaction.id}", instrumentId, transaction.id, transaction.tradeEpochDay, transaction.quantityMicros, transaction.quantityMicros, cost, cost, 0, 0, null)
        }
        generated.forEach { extendedDao.saveLot(it) }
        return generated
    }
}
