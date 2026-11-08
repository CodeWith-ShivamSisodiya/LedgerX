package com.ledgerx.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.ledgerx.app.core.accounting.LedgerRepository
import com.ledgerx.app.core.database.AccountBalance
import com.ledgerx.app.core.database.TransactionRow
import com.ledgerx.app.core.database.BillEntity
import com.ledgerx.app.core.database.GoalEntity
import com.ledgerx.app.core.database.InsurancePolicyEntity
import com.ledgerx.app.core.database.LoanEntity
import com.ledgerx.app.core.model.Money
import com.ledgerx.app.core.planning.PlanningRepository
import com.ledgerx.app.core.planning.RecurringProcessor
import com.ledgerx.app.core.security.SecurityPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LegacyLedgerXUiState(
    val accountCount: Int = -1,
    val accounts: List<AccountBalance> = emptyList(),
    val transactions: List<TransactionRow> = emptyList(),
    val bills: List<BillEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val policies: List<InsurancePolicyEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val appLockEnabled: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
) {
    val netWorthMinor: Long get() = accounts.sumOf { it.balanceMinor }
    val monthlyIncomeMinor: Long get() = transactions.filter { it.type == "INCOME" }.sumOf { it.amountMinor }
    val monthlyExpenseMinor: Long get() = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amountMinor }
}

@HiltViewModel
class LegacyLedgerXViewModel @Inject constructor(
    private val repository: LedgerRepository,
    private val planningRepository: PlanningRepository,
    private val recurringProcessor: RecurringProcessor,
    private val securityPreferences: SecurityPreferences,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private data class CoreState(val count: Int, val accounts: List<AccountBalance>, val transactions: List<TransactionRow>)
    private data class PlanningState(val bills: List<BillEntity>, val loans: List<LoanEntity>, val policies: List<InsurancePolicyEntity>, val goals: List<GoalEntity>, val appLockEnabled: Boolean)

    private val coreState = combine(
        repository.accountCount,
        repository.accountBalances,
        repository.recentTransactions,
    ) { count, accounts, transactions -> CoreState(count, accounts, transactions) }

    private val planningState = combine(
        planningRepository.bills,
        planningRepository.loans,
        planningRepository.policies,
        planningRepository.goals,
        securityPreferences.appLockEnabled,
    ) { bills, loans, policies, goals, appLockEnabled -> PlanningState(bills, loans, policies, goals, appLockEnabled) }

    val state: StateFlow<LegacyLedgerXUiState> = combine(
        coreState,
        planningState,
        busy,
        message,
    ) { core, planning, isBusy, notice ->
        LegacyLedgerXUiState(core.count, core.accounts, core.transactions, planning.bills, planning.loans, planning.policies, planning.goals, planning.appLockEnabled, isBusy, notice)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LegacyLedgerXUiState())

    init {
        viewModelScope.launch {
            repository.initialize()
            recurringProcessor.processDue()
        }
    }

    fun createAccount(name: String, kind: String, openingBalance: String) = launchAction("Account created") {
        repository.createAccount(name, kind, Money.parseMajor(openingBalance.ifBlank { "0" }))
    }

    fun recordTransaction(
        type: String,
        accountId: String,
        amount: String,
        categoryId: String,
        merchant: String,
        note: String,
    ) = launchAction(if (type == "EXPENSE") "Expense recorded" else "Income recorded") {
        val money = Money.parseMajor(amount)
        if (type == "EXPENSE") {
            repository.recordExpense(accountId, money, categoryId, merchant, note)
        } else {
            repository.recordIncome(accountId, money, categoryId, merchant, note)
        }
    }

    fun transfer(sourceAccountId: String, destinationAccountId: String, amount: String, note: String) = launchAction("Transfer recorded") {
        repository.transfer(sourceAccountId, destinationAccountId, Money.parseMajor(amount), note)
    }

    fun clearMessage() { message.value = null }

    fun setAppLockEnabled(enabled: Boolean) = launchAction(if (enabled) "App lock enabled" else "App lock disabled") {
        securityPreferences.setAppLockEnabled(enabled)
    }

    fun addBill(name: String, amount: String, dueDateIso: String, frequency: String?) = launchAction("Bill saved") {
        planningRepository.addBill(name, Money.parseMajor(amount), LocalDate.parse(dueDateIso), frequency)
    }

    fun addLoan(name: String, principal: String, annualRatePercent: String, months: String) = launchAction("Loan saved") {
        val rateBasisPoints = annualRatePercent.toBigDecimal().movePointRight(2).intValueExact()
        planningRepository.addLoan(name, "OTHER", Money.parseMajor(principal), rateBasisPoints, months.toInt(), null)
    }

    fun addPolicy(name: String, insurer: String, coverage: String, premium: String, renewalDateIso: String) = launchAction("Policy saved") {
        planningRepository.addPolicy(name, "OTHER", insurer, Money.parseMajor(coverage), Money.parseMajor(premium), LocalDate.parse(renewalDateIso))
    }

    fun addGoal(name: String, target: String, targetDateIso: String, monthlyContribution: String) = launchAction("Goal saved") {
        planningRepository.addGoal(name, "OTHER", Money.parseMajor(target), LocalDate.parse(targetDateIso), Money.parseMajor(monthlyContribution.ifBlank { "0" }))
    }

    private fun launchAction(success: String, action: suspend () -> Unit) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            message.value = null
            runCatching { action() }
                .onSuccess { message.value = success }
                .onFailure { message.value = it.message ?: "Something went wrong" }
            busy.value = false
        }
    }
}
