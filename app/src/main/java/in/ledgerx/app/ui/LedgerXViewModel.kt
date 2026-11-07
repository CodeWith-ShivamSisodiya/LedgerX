package in.ledgerx.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import in.ledgerx.app.core.accounting.LedgerRepository
import in.ledgerx.app.core.database.AccountBalance
import in.ledgerx.app.core.database.TransactionRow
import in.ledgerx.app.core.model.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LedgerXUiState(
    val accountCount: Int = -1,
    val accounts: List<AccountBalance> = emptyList(),
    val transactions: List<TransactionRow> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
) {
    val netWorthMinor: Long get() = accounts.sumOf { it.balanceMinor }
    val monthlyIncomeMinor: Long get() = transactions.filter { it.type == "INCOME" }.sumOf { it.amountMinor }
    val monthlyExpenseMinor: Long get() = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amountMinor }
}

@HiltViewModel
class LedgerXViewModel @Inject constructor(
    private val repository: LedgerRepository,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<LedgerXUiState> = combine(
        repository.accountCount,
        repository.accountBalances,
        repository.recentTransactions,
        busy,
        message,
    ) { count, accounts, transactions, isBusy, notice ->
        LedgerXUiState(count, accounts, transactions, isBusy, notice)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LedgerXUiState())

    init {
        viewModelScope.launch { repository.initialize() }
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

    fun clearMessage() { message.value = null }

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

