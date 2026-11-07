package in.ledgerx.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import in.ledgerx.app.R
import in.ledgerx.app.core.accounting.LedgerMath
import in.ledgerx.app.core.database.AccountBalance
import in.ledgerx.app.core.database.TransactionRow
import in.ledgerx.app.core.model.Money
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    ACTIVITY("Activity", Icons.Default.ReceiptLong),
    WEALTH("Wealth", Icons.Default.AccountBalanceWallet),
    INSIGHTS("Insights", Icons.Default.Insights),
    HEALTH("Health", Icons.Default.HealthAndSafety),
}

@Composable
fun LedgerXApp(viewModel: LedgerXViewModel) {
    val state by viewModel.state.collectAsState()
    when {
        state.accountCount < 0 -> LoadingScreen()
        state.accountCount == 0 -> OnboardingScreen(state.busy, viewModel::createAccount)
        else -> MainShell(state, viewModel)
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun OnboardingScreen(
    busy: Boolean,
    onCreateAccount: (String, String, String) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("SAVINGS") }
    var opening by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 44.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(
                Modifier.size(88.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ledgerx_logo),
                    contentDescription = "LedgerX logo",
                    modifier = Modifier.size(76.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Text("LedgerX", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            if (step == 0) {
                Text("Your private financial command center", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Your financial data stays on this device. LedgerX has no ads, analytics, account sign-in, or internet permission.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrivacyPoint("Encrypted local database")
                PrivacyPoint("Balances come from auditable ledger entries")
                PrivacyPoint("You stay in control of exports and backups")
            } else {
                Text("Create your first account", style = MaterialTheme.typography.headlineSmall)
                Text("Start with the account you use most. Advanced setup can wait.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Account name") },
                    placeholder = { Text("e.g. Salary account") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SAVINGS" to "Savings", "CASH" to "Cash", "CREDIT_CARD" to "Credit card").forEach { (value, label) ->
                        FilterChip(selected = kind == value, onClick = { kind = value }, label = { Text(label) })
                    }
                }
                OutlinedTextField(
                    value = opening,
                    onValueChange = { opening = it.filter { char -> char.isDigit() || char == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Current balance (optional)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        }
        Button(
            onClick = {
                if (step == 0) step = 1 else onCreateAccount(name, kind, opening)
            },
            enabled = !busy && (step == 0 || name.isNotBlank()),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(if (step == 0) "Get started" else "Open my dashboard")
        }
    }
}

@Composable
private fun PrivacyPoint(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
        Text(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(state: LedgerXUiState, viewModel: LedgerXViewModel) {
    var tab by remember { mutableStateOf(MainTab.HOME) }
    var showQuickEntry by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
            if (it.endsWith("recorded")) showQuickEntry = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == MainTab.HOME || tab == MainTab.ACTIVITY) {
                ExtendedFloatingActionButton(
                    onClick = { showQuickEntry = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Add") },
                )
            }
        },
    ) { padding ->
        when (tab) {
            MainTab.HOME -> HomeScreen(state, padding)
            MainTab.ACTIVITY -> ActivityScreen(state.transactions, padding)
            MainTab.WEALTH -> WealthScreen(state, padding)
            MainTab.INSIGHTS -> InsightsScreen(state, padding)
            MainTab.HEALTH -> HealthScreen(state, padding)
        }
    }

    if (showQuickEntry) {
        QuickEntrySheet(
            accounts = state.accounts,
            busy = state.busy,
            onDismiss = { showQuickEntry = false },
            onSave = viewModel::recordTransaction,
        )
    }
}

@Composable
private fun HomeScreen(state: LedgerXUiState, padding: PaddingValues) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Good to see you", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Overview", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                Text("OFFLINE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text("Total net worth", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        Money(state.netWorthMinor).format(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("From ${state.accounts.size} active account${if (state.accounts.size == 1) "" else "s"}")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Income", state.monthlyIncomeMinor, Icons.Default.ArrowDownward, Modifier.weight(1f), Color(0xFF55E6B5))
                MetricCard("Expenses", state.monthlyExpenseMinor, Icons.Default.ArrowUpward, Modifier.weight(1f), MaterialTheme.colorScheme.error)
            }
        }
        item {
            Text("Accounts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        items(state.accounts, key = { it.id }) { AccountRow(it) }
        item {
            Text("Recent activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        }
        if (state.transactions.isEmpty()) item { EmptyCard("No transactions yet", "Tap Add to record your first income or expense.") }
        else items(state.transactions.take(5), key = { it.id }) { TransactionItem(it) }
    }
}

@Composable
private fun MetricCard(title: String, amount: Long, icon: ImageVector, modifier: Modifier, tint: Color) {
    Card(modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = tint)
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(Money(amount).format(), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AccountRow(account: AccountBalance) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(account.name, fontWeight = FontWeight.SemiBold)
                Text(account.kind.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(Money(account.balanceMinor, account.currencyCode).format(), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActivityScreen(transactions: List<TransactionRow>, padding: PaddingValues) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Activity", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        if (transactions.isEmpty()) item { EmptyCard("Nothing here yet", "Recorded transactions will appear here.") }
        else items(transactions, key = { it.id }) { TransactionItem(it) }
    }
}

@Composable
private fun TransactionItem(row: TransactionRow) {
    val expense = row.type == "EXPENSE"
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(42.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (expense) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(row.merchant ?: row.type.lowercase().replaceFirstChar(Char::uppercase), fontWeight = FontWeight.SemiBold)
            val date = Instant.ofEpochMilli(row.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM"))
            Text(listOfNotNull(row.accountName, date).joinToString(" • "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            (if (expense) "−" else "+") + Money(row.amountMinor, row.currencyCode).format(),
            color = if (expense) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun WealthScreen(state: LedgerXUiState, padding: PaddingValues) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Wealth", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { EmptyCard("Net worth", Money(state.netWorthMinor).format()) }
        item { Text("Account composition", style = MaterialTheme.typography.titleLarge) }
        items(state.accounts, key = { it.id }) { AccountRow(it) }
        item { Text("Investments are added in Phase 3. No market value is estimated without dated user-provided prices.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun InsightsScreen(state: LedgerXUiState, padding: PaddingValues) {
    val overspending = state.monthlyExpenseMinor > state.monthlyIncomeMinor && state.monthlyExpenseMinor > 0
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            EmptyCard(
                if (overspending) "Cashflow needs attention" else "No urgent pattern detected",
                if (overspending) "Recorded expenses exceed recorded income by ${Money(state.monthlyExpenseMinor - state.monthlyIncomeMinor).format()}. Add missing income before acting on this signal."
                else "LedgerX needs more transaction history before it can make evidence-based recommendations.",
            )
        }
        item { Text("Insights are calculated locally from confirmed records. Missing data is never interpreted as healthy.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun HealthScreen(state: LedgerXUiState, padding: PaddingValues) {
    val savingsRate = LedgerMath.savingsRateBasisPoints(state.monthlyIncomeMinor, state.monthlyExpenseMinor)
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Financial health", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { EmptyCard("Score unavailable", "Only cashflow data is available. Add debt, insurance, emergency reserves, income resilience, and allocation data for a responsible score.") }
        item { EmptyCard("Data completeness", "1 of 7 dimensions has partial data") }
        item { EmptyCard("Savings consistency", savingsRate?.let { "Current recorded savings rate: ${it / 100.0}%" } ?: "Add confirmed income to calculate this dimension.") }
        item { Text("This is a planning indicator, not a credit score or guarantee of financial safety.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun EmptyCard(title: String, detail: String) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickEntrySheet(
    accounts: List<AccountBalance>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit,
) {
    var type by remember { mutableStateOf("EXPENSE") }
    var accountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id.orEmpty()) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("food") }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val categories = if (type == "EXPENSE") listOf("food" to "Food", "groceries" to "Groceries", "transport" to "Transport", "shopping" to "Shopping")
    else listOf("salary" to "Salary", "freelance" to "Freelance", "interest" to "Interest")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            Modifier.fillMaxWidth().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Text("Quick entry", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "EXPENSE", onClick = { type = "EXPENSE"; category = "food" }, label = { Text("Expense") })
                    FilterChip(selected = type == "INCOME", onClick = { type = "INCOME"; category = "salary" }, label = { Text("Income") })
                }
            }
            item {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
            item {
                Text("Account", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.take(3).forEach { account ->
                        FilterChip(selected = accountId == account.id, onClick = { accountId = account.id }, label = { Text(account.name) })
                    }
                }
            }
            item {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.take(4).forEach { (id, label) ->
                        FilterChip(selected = category == id, onClick = { category = id }, label = { Text(label) })
                    }
                }
            }
            item { OutlinedTextField(merchant, { merchant = it }, Modifier.fillMaxWidth(), label = { Text(if (type == "EXPENSE") "Merchant (optional)" else "Source (optional)") }, singleLine = true) }
            item { OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Note (optional)") }, maxLines = 2) }
            item {
                Button(
                    onClick = { onSave(type, accountId, amount, category, merchant, note) },
                    enabled = !busy && amount.isNotBlank() && accountId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Save ${type.lowercase()}")
                }
            }
        }
    }
}
