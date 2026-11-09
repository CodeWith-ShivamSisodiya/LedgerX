package com.ledgerx.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.paging.PagingData
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import com.ledgerx.app.R
import com.ledgerx.app.core.accounting.LedgerMath
import com.ledgerx.app.core.finance.HealthInputs
import com.ledgerx.app.core.finance.HealthScoreCalculator
import com.ledgerx.app.core.finance.ForecastAssumptions
import com.ledgerx.app.core.finance.ForecastEngine
import com.ledgerx.app.core.finance.LoanCalculator
import com.ledgerx.app.core.finance.RetirementCalculator
import com.ledgerx.app.core.finance.XirrCalculator
import com.ledgerx.app.core.finance.DatedCashFlow
import com.ledgerx.app.core.database.AccountBalance
import com.ledgerx.app.core.database.TransactionRow
import com.ledgerx.app.core.database.ReceiptAttachmentEntity
import com.ledgerx.app.core.model.Money
import com.ledgerx.app.core.importing.CsvParser
import com.ledgerx.app.core.importing.XlsxParser
import java.time.Instant
import java.time.LocalDate
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
fun LedgerXApp(
    viewModel: LedgerXViewModel,
    sessionUnlocked: Boolean,
    deviceAuthenticationAvailable: Boolean,
    onAuthenticate: () -> Unit,
    onLocalDataDeleted: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    when {
        state.accountCount < 0 -> LoadingScreen()
        state.appLockEnabled && !sessionUnlocked -> AppLockScreen(onAuthenticate)
        state.accountCount == 0 -> OnboardingScreen(state.busy, viewModel.themeMode.collectAsState().value, deviceAuthenticationAvailable, viewModel::setThemeMode, viewModel::completeOnboarding)
        else -> MainShell(state, viewModel, deviceAuthenticationAvailable, onLocalDataDeleted)
    }
}

@Composable
private fun AppLockScreen(onAuthenticate: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Lock, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
        Text("LedgerX is locked", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Authenticate to access your private financial records.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAuthenticate) { Text("Unlock") }
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
    themeMode: String,
    deviceAuthenticationAvailable: Boolean,
    onThemeMode: (String) -> Unit,
    onComplete: (String, String, String, String, Boolean) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("SAVINGS") }
    var opening by remember { mutableStateOf("") }
    var income by remember { mutableStateOf("") }
    var enableLock by remember { mutableStateOf(false) }

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
            } else if (step == 1) {
                Text("Choose your appearance", style = MaterialTheme.typography.headlineSmall)
                Text("You can change this later. All themes keep high-contrast financial labels.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "AMOLED dark").forEach { (value,label) -> FilterChip(themeMode == value, { onThemeMode(value) }, { Text(label) }) }
                }
            } else if (step == 2) {
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
            } else if (step == 3) {
                Text("Monthly income (optional)", style = MaterialTheme.typography.headlineSmall)
                Text("Add confirmed take-home income now, or skip and record income later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyField(income, { income = it }, "Monthly take-home income")
            } else {
                Text("Protect LedgerX (optional)", style = MaterialTheme.typography.headlineSmall)
                Text(if(deviceAuthenticationAvailable) "Require biometrics or device credential whenever LedgerX resumes." else "Configure a secure device lock in Android settings to enable this later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilterChip(enableLock, { if(deviceAuthenticationAvailable) enableLock=!enableLock }, { Text(if(enableLock) "App lock enabled" else "Enable app lock") }, enabled=deviceAuthenticationAvailable)
            }
        }
        Button(
            onClick = {
                if (step < 4) step++ else onComplete(name, kind, opening, income, enableLock)
            },
            enabled = !busy && (step != 2 || name.isNotBlank()),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(when(step){0->"Get started";1,2,3->"Continue";else->"Open my dashboard"})
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
private fun MainShell(state: LedgerXUiState, viewModel: LedgerXViewModel, deviceAuthenticationAvailable: Boolean, onLocalDataDeleted: () -> Unit) {
    var tab by remember { mutableStateOf(MainTab.HOME) }
    var showQuickEntry by remember { mutableStateOf(false) }
    var planningEntry by remember { mutableStateOf<String?>(null) }
    var advancedEntry by remember { mutableStateOf<String?>(null) }
    var importPayload by remember { mutableStateOf<Pair<String, String>?>(null) }
    var receiptTargetId by remember { mutableStateOf<String?>(null) }
    var editTarget by remember { mutableStateOf<TransactionRow?>(null) }
    var showDataDeletion by remember { mutableStateOf(false) }
    var policyDocumentTargetId by remember { mutableStateOf<String?>(null) }
    var documentPassword by remember { mutableStateOf("") }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                val name = it.lastPathSegment ?: "statement.csv"
                val bytes = context.contentResolver.openInputStream(it)?.use { input -> input.readBytes() } ?: error("Cannot read statement")
                require(bytes.size <= 10 * 1024 * 1024) { "Statement file exceeds the 10 MB offline import limit" }
                val canonicalCsv = if (name.lowercase().endsWith(".xlsx")) CsvParser.encode(XlsxParser.parse(bytes)) else bytes.toString(Charsets.UTF_8)
                name to canonicalCsv
            }.onSuccess { payload -> importPayload = payload }.onFailure(viewModel::showError)
        }
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> uri?.let { viewModel.createBackup(it, documentPassword) } }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { viewModel.restoreBackup(it, documentPassword) } }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let(viewModel::exportTransactions) }
    val financialReportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let(viewModel::exportFinancialReport) }
    val caLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { viewModel.exportCa(it, state.taxProfile?.financialYear ?: "2026-27") } }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val receiptLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val transactionId = receiptTargetId
        if (uri != null && transactionId != null) viewModel.attachReceipt(transactionId, uri)
        receiptTargetId = null
    }
    val policyDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val policyId = policyDocumentTargetId
        if (uri != null && policyId != null) viewModel.attachPolicyDocument(policyId, uri)
        policyDocumentTargetId = null
    }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            if (it.endsWith("recorded")) { showQuickEntry = false; advancedEntry = null; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            if (it.endsWith("saved") || it.endsWith("attached") || it == "Account archived") { planningEntry = null; advancedEntry = null }
            if (it == "Statement imported") importPayload = null
            if (it == "Transaction updated") editTarget = null
            if (it.contains("backup", ignoreCase = true) || it.contains("restored", ignoreCase = true)) documentPassword = ""
            val undoId = state.lastUndoTransactionId
            val result = snackbar.showSnackbar(it, actionLabel = undoId?.let { "Undo" }, withDismissAction = true)
            viewModel.clearMessage()
            if (result == SnackbarResult.ActionPerformed && undoId != null) viewModel.reverseTransaction(undoId)
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
            MainTab.HOME -> HomeScreen(state, padding, { advancedEntry="BILL_PAYMENT:$it" }, viewModel::confirmScheduledIncome, { tab=MainTab.WEALTH }, { tab=MainTab.INSIGHTS }, { tab=MainTab.ACTIVITY }, { tab=MainTab.HEALTH })
            MainTab.ACTIVITY -> ActivityScreen(viewModel.pagedTransactions, state.importBatches, state.receipts, padding, { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/plain")) }, { advancedEntry = "SPLIT" }, { advancedEntry = "CATEGORY" }, { advancedEntry = "INCOME_DETAIL" }, viewModel::setActivityQuery, viewModel::setActivityType, viewModel::reverseTransaction, { id -> receiptTargetId = id; receiptLauncher.launch(arrayOf("image/*", "application/pdf")) }, viewModel::removeReceipt, { advancedEntry = "TAG:$it" }, { editTarget = it }, viewModel::rollbackImport)
            MainTab.WEALTH -> WealthScreen(state, padding, onAdd = {
                if (it in setOf("BILL", "LOAN", "GOAL")) planningEntry = it else advancedEntry = it
            }, onPolicyDocument = { id -> policyDocumentTargetId=id; policyDocumentLauncher.launch(arrayOf("image/*","application/pdf")) }, onRemovePolicyDocument = viewModel::removePolicyDocument)
            MainTab.INSIGHTS -> InsightsScreen(state, padding)
            MainTab.HEALTH -> HealthScreen(
                state, padding, deviceAuthenticationAvailable, viewModel::setAppLockEnabled,
                themeMode = viewModel.themeMode.collectAsState().value, onThemeMode = viewModel::setThemeMode,
                textScale = viewModel.textScale.collectAsState().value, onTextScale = viewModel::setTextScale,
                remindersEnabled = viewModel.remindersEnabled.collectAsState().value, onRemindersEnabled = viewModel::setRemindersEnabled,
                localeTag = viewModel.localeTag.collectAsState().value, onLocaleTag = viewModel::setLocaleTag,
                password = documentPassword, onPasswordChange = { documentPassword = it },
                onBackup = { backupLauncher.launch("LedgerX-${LocalDate.now()}.lxb") },
                onRestore = { restoreLauncher.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) },
                onExport = { exportLauncher.launch("LedgerX-transactions-${LocalDate.now()}.csv") },
                onFinancialReport = { financialReportLauncher.launch("LedgerX-financial-report-${LocalDate.now()}.csv") },
                onCaExport = { caLauncher.launch("LedgerX-CA-${state.taxProfile?.financialYear ?: "2026-27"}.csv") },
                onTax = { advancedEntry = "TAX" },
                onHealthSnapshot = viewModel::saveHealthSnapshot,
                onNotifications = {
                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onDeleteData = { showDataDeletion = true },
                onDashboardLayout = viewModel::setDashboardLayout,
                onPlanningAssumptions = viewModel::savePlanningAssumptions,
            )
        }
    }

    if (showQuickEntry) {
        QuickEntrySheet(
            accounts = state.accounts,
            availableCategories = state.categories,
            merchantSuggestions = state.merchantSuggestions.map { it.name },
            preferredAccountId = viewModel.lastAccountId.collectAsState().value,
            preferredCategoryId = viewModel.lastCategoryId.collectAsState().value,
            busy = state.busy,
            onDismiss = { showQuickEntry = false },
            onSave = viewModel::recordTransaction,
            onTransfer = viewModel::transfer,
        )
    }
    planningEntry?.let { kind ->
        PlanningEntrySheet(
            kind = kind,
            busy = state.busy,
            onDismiss = { planningEntry = null },
            onAddBill = viewModel::addBill,
            onAddLoan = viewModel::addLoan,
            onAddPolicy = viewModel::addPolicy,
            onAddGoal = viewModel::addGoal,
        )
    }
    advancedEntry?.let { kind ->
        AdvancedEntrySheet(kind, state, state.busy, { advancedEntry = null }, viewModel)
    }
    importPayload?.let { (name, csv) ->
        ImportSheet(name, csv, state, state.busy, { importPayload = null; viewModel.clearImportPreview() }, viewModel)
    }
    editTarget?.let { row -> EditTransactionSheet(row, state.categories, state.busy, { editTarget = null }, viewModel::editTransaction) }
    if (showDataDeletion) DeleteDataSheet(state.busy, { showDataDeletion = false }) { confirmation -> viewModel.deleteAllLocalData(confirmation, onLocalDataDeleted) }
}

@Composable
private fun HomeScreen(state: LedgerXUiState, padding: PaddingValues, onBillPaid: (String) -> Unit, onConfirmIncome: (String) -> Unit, onOpenWealth: () -> Unit, onOpenInsights: () -> Unit, onOpenActivity: () -> Unit, onOpenHealth: () -> Unit) {
    val income = state.monthlyTakeHomeMinor
    val expenses = state.monthlyExpenseMinor
    val investments = state.confirmedMonthlyInvestmentMinor
    // These figures reflect recorded activity and plans, never illustrative demo balances.
    val remainder = income - expenses - investments
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("MY MONEY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Your money, at a glance", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Spending, saving and wealth in one place.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenWealth),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("YOUR NET WORTH", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(Money(state.netWorthMinor).format(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Everything you own, minus outstanding loans", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("See wealth breakdown  →", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("This month", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Income", income, Icons.Default.ArrowDownward, Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                MetricCard("Expenses", expenses, Icons.Default.ArrowUpward, Modifier.weight(1f), MaterialTheme.colorScheme.error)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Recorded investing", investments, Icons.Default.Insights, Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                MetricCard("Estimated left", remainder, Icons.Default.AccountBalanceWallet, Modifier.weight(1f), if (remainder >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Understand your monthly flow", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Recorded take-home − recorded expenses − recorded investing = estimated left", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text("This is a records-based estimate, not your current bank balance or a future-month forecast. Avoid logging transfers or investments again as ordinary expenses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onOpenInsights) { Text("Explore spending insights") }
                }
            }
        }
        item {
            Text("What you own", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SimpleMoneyRow("Bank & cash", state.accounts.filter { it.kind != "CREDIT_CARD" }.sumOf { it.balanceMinor })
                    SimpleMoneyRow("Investments", state.investmentValueMinor)
                    SimpleMoneyRow("FDs & deposits", state.deposits.sumOf { it.principalMinor })
                    SimpleMoneyRow("Retirement", state.retirementValueMinor)
                    SimpleMoneyRow("Property, gold & assets", state.physicalAssetValueMinor)
                    HorizontalDivider()
                    SimpleMoneyRow("Outstanding loans", state.loans.sumOf { it.outstandingPrincipalMinor }, liability = true)
                    OutlinedButton(onClick = onOpenWealth, modifier = Modifier.fillMaxWidth()) { Text("Manage wealth & assets") }
                }
            }
        }
        if (state.bills.any { it.status == "DUE" }) {
            item { Text("Upcoming payments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(state.bills.filter { it.status == "DUE" }.take(3), key = { "due-${it.id}" }) { bill ->
                Card(shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(bill.name, fontWeight = FontWeight.SemiBold)
                            Text("${Money(bill.amountMinor, bill.currencyCode).format()} · Due ${LocalDate.ofEpochDay(bill.dueEpochDay)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { onBillPaid(bill.id) }) { Text("Pay") }
                    }
                }
            }
        }
        if (state.pendingOccurrences.isNotEmpty()) {
            item { Text("Income to confirm", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(state.pendingOccurrences.take(3), key = { "income-${it.id}" }) { occurrence ->
                val template = state.recurring.firstOrNull { it.id == occurrence.templateId }
                Card(shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(template?.merchant ?: "Scheduled income", fontWeight = FontWeight.SemiBold)
                            Text("Expected ${LocalDate.ofEpochDay(occurrence.dueEpochDay)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { onConfirmIncome(occurrence.id) }) { Text("Received") }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onOpenActivity) { Text("See all") }
            }
        }
        if (state.transactions.isEmpty()) {
            item { EmptyCard("Start tracking", "Tap Add to log a transaction. Add existing assets from Wealth.") }
        } else {
            items(state.transactions.take(4), key = { "recent-${it.id}" }) { TransactionItem(it) }
        }
    }
}

@Composable
private fun SimpleMoneyRow(label: String, amount: Long, liability: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text((if (liability) "−" else "") + Money(amount).format(), fontWeight = FontWeight.SemiBold, color = if (liability) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
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
private fun ActivityScreen(transactions: Flow<PagingData<TransactionRow>>, importBatches: List<com.ledgerx.app.core.database.ImportBatchEntity>, receipts: List<ReceiptAttachmentEntity>, padding: PaddingValues, onImport: () -> Unit, onSplit: () -> Unit, onCategory: () -> Unit, onDetailedIncome: () -> Unit, onQuery: (String) -> Unit, onType: (String) -> Unit, onReverse: (String) -> Unit, onReceipt: (String) -> Unit, onRemoveReceipt: (String) -> Unit, onTag: (String) -> Unit, onEdit: (TransactionRow) -> Unit, onRollbackImport: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") }
    val pagedItems = transactions.collectAsLazyPagingItems()
    LaunchedEffect(Unit) { onQuery(""); onType("ALL") }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Activity", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onImport) { Text("Import CSV/XLSX") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onSplit) { Text("Split") }
                    OutlinedButton(onClick = onCategory) { Text("Category") }
                    OutlinedButton(onClick = onDetailedIncome) { Text("Gross income") }
                }
            }
        }
        item { OutlinedTextField(query, { query = it; onQuery(it) }, Modifier.fillMaxWidth(), label = { Text("Search merchant, note, account") }, singleLine = true) }
        item { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { listOf("ALL", "EXPENSE", "INCOME", "TRANSFER", "REFUND", "REIMBURSEMENT", "LOAN_REPAYMENT", "INVESTMENT", "DIVESTMENT").chunked(3).forEach { chunk -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chunk.forEach { type -> FilterChip(filter == type, { filter = type; onType(type) }, { Text(type.replace('_',' ').lowercase().replaceFirstChar(Char::uppercase)) }) } } } } }
        if(importBatches.isNotEmpty()) {
            item { Text("Statement imports",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold) }
            items(importBatches.take(5),key={"batch-${it.id}"}) { batch -> Card(shape=RoundedCornerShape(18.dp)){Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(batch.fileName,fontWeight=FontWeight.SemiBold);Text("${batch.acceptedCount} imported • ${batch.rejectedCount} duplicate/rejected • ${batch.status.lowercase().replace('_',' ')}",color=MaterialTheme.colorScheme.onSurfaceVariant)};if(batch.status=="COMPLETED")OutlinedButton(onClick={onRollbackImport(batch.id)}){Text("Rollback")}}} }
        }
        if (pagedItems.loadState.refresh is LoadState.NotLoading && pagedItems.itemCount == 0) item { EmptyCard("No matching activity", "Adjust the search or import a bank statement.") }
        if (pagedItems.loadState.refresh is LoadState.Error) item { EmptyCard("Could not load activity", (pagedItems.loadState.refresh as LoadState.Error).error.message ?: "Database query failed") }
        items(pagedItems.itemCount, key = { index -> pagedItems.peek(index)?.id ?: index }) { index -> pagedItems[index]?.let { row -> TransactionItem(row, onReverse, onReceipt, onRemoveReceipt, receipts.filter { it.transactionId == row.id }, onTag, onEdit) } }
        if (pagedItems.loadState.append is LoadState.Loading) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment=Alignment.Center){CircularProgressIndicator(Modifier.size(24.dp))} }
    }
}

@Composable
private fun TransactionItem(row: TransactionRow, onReverse: ((String) -> Unit)? = null, onReceipt: ((String) -> Unit)? = null, onRemoveReceipt: ((String) -> Unit)? = null, receipts: List<ReceiptAttachmentEntity> = emptyList(), onTag: ((String) -> Unit)? = null, onEdit: ((TransactionRow) -> Unit)? = null) {
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
            Text(listOfNotNull(row.accountName, date, row.origin.takeUnless { it == "MANUAL" }?.lowercase()?.replaceFirstChar(Char::uppercase), row.reconciliationStatus.takeIf { it == "RECONCILED" }?.let { "Reconciled" }).joinToString(" • "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            (if (expense) "−" else "+") + Money(row.amountMinor, row.currencyCode).format(),
            color = if (expense) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        if (onReverse != null && row.origin != "BILL" && row.type !in setOf("REVERSAL", "INVESTMENT", "DIVESTMENT", "LOAN_REPAYMENT")) {
            OutlinedButton(onClick = { onReverse(row.id) }) { Text("Undo") }
        }
    }
    if (onReceipt != null || onTag != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onEdit != null && row.origin != "BILL" && row.type in setOf("EXPENSE", "INCOME", "REFUND", "REIMBURSEMENT") && row.reconciliationStatus != "RECONCILED") OutlinedButton(onClick = { onEdit(row) }) { Text("Edit") }
            onReceipt?.let { OutlinedButton(onClick = { it(row.id) }) { Text(if(receipts.isEmpty()) "Receipt" else "Receipt (${receipts.size})") } }
            if(receipts.isNotEmpty() && onRemoveReceipt!=null) OutlinedButton(onClick={onRemoveReceipt(receipts.first().id)}){Text("Remove latest")}
            onTag?.let { OutlinedButton(onClick = { it(row.id) }) { Text("Tag") } }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun WealthScreen(state: LedgerXUiState, padding: PaddingValues, onAdd: (String) -> Unit, onPolicyDocument: (String) -> Unit, onRemovePolicyDocument: (String) -> Unit) {
    var showAdvancedTools by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("My wealth", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Text("Savings, investments, assets and loans in one place", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TOTAL NET WORTH", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(Money(state.netWorthMinor).format(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Recorded assets minus outstanding loans", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Wealth breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    SimpleMoneyRow("Bank and cash", state.accounts.filter { it.kind != "CREDIT_CARD" }.sumOf { it.balanceMinor })
                    SimpleMoneyRow("Stocks and mutual funds", state.investmentValueMinor)
                    SimpleMoneyRow("FD and RD", state.deposits.sumOf { it.principalMinor })
                    SimpleMoneyRow("Retirement savings", state.retirementValueMinor)
                    SimpleMoneyRow("Gold, property and assets", state.physicalAssetValueMinor)
                    HorizontalDivider()
                    SimpleMoneyRow("Outstanding loans", state.loans.sumOf { it.outstandingPrincipalMinor }, liability = true)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add your money & assets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAdd("DEPOSIT") }, modifier = Modifier.weight(1f)) { Text("FD / RD") }
                    OutlinedButton(onClick = { onAdd("INVESTMENT") }, modifier = Modifier.weight(1f)) { Text("Investment") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAdd("ASSET") }, modifier = Modifier.weight(1f)) { Text("Gold / Asset") }
                    OutlinedButton(onClick = { onAdd("LOAN") }, modifier = Modifier.weight(1f)) { Text("Loan") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAdd("POLICY") }, modifier = Modifier.weight(1f)) { Text("Insurance") }
                    OutlinedButton(onClick = { onAdd("ACCOUNT") }, modifier = Modifier.weight(1f)) { Text("Account") }
                }
                OutlinedButton(onClick = { showAdvancedTools = !showAdvancedTools }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showAdvancedTools) "Hide advanced tools" else "More actions and advanced tools")
                }
            }
        }
        if (showAdvancedTools) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Advanced tools", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAdd("ACCOUNT") }) { Text("Account") }
                    OutlinedButton(onClick = { onAdd("BILL") }) { Text("Bill") }
                    OutlinedButton(onClick = { onAdd("LOAN") }) { Text("Loan") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAdd("POLICY") }) { Text("Policy") }
                    OutlinedButton(onClick = { onAdd("GOAL") }) { Text("Goal") }
                    OutlinedButton(onClick = { onAdd("INVESTMENT") }) { Text("Investment") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAdd("VALUATION") }, enabled = state.instruments.isNotEmpty()) { Text("Price") }
                    OutlinedButton(onClick = { onAdd("DEPOSIT") }) { Text("FD/RD") }
                    OutlinedButton(onClick = { onAdd("RETIREMENT") }) { Text("Retirement") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAdd("ASSET") }) { Text("Asset") }
                    OutlinedButton(onClick = { onAdd("ASSET_VALUATION") }, enabled=state.assets.isNotEmpty()) { Text("Asset value") }
                    OutlinedButton(onClick = { onAdd("ASSET_MAINTENANCE") }, enabled=state.assets.isNotEmpty()) { Text("Maintenance") }
                    OutlinedButton(onClick = { onAdd("RECURRING") }) { Text("Recurring") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAdd("INVESTMENT_SALE") }, enabled = state.instruments.isNotEmpty()) { Text("Sell holding") }
                    OutlinedButton(onClick = { onAdd("INVESTMENT_BUY") }, enabled = state.instruments.isNotEmpty()) { Text("Buy more") }
                    OutlinedButton(onClick = { onAdd("CORPORATE_ACTION") }, enabled = state.instruments.isNotEmpty()) { Text("Split / bonus") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAdd("DIVIDEND") }, enabled = state.instruments.isNotEmpty()) { Text("Dividend") }
                    OutlinedButton(onClick = { onAdd("SIP") }, enabled = state.instruments.isNotEmpty()) { Text("SIP") }
                    OutlinedButton(onClick = { onAdd("RD_CONTRIBUTION") }, enabled = state.deposits.any { it.type == "RD" }) { Text("RD deposit") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAdd("RETIREMENT_EVENT") }) { Text("Retirement event") }
                    OutlinedButton(onClick = { onAdd("CARD_STATEMENT") }, enabled = state.accounts.any { it.kind == "CREDIT_CARD" }) { Text("Card statement") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAdd("RENAME_ACCOUNT") }, enabled = state.accounts.isNotEmpty()) { Text("Rename account") }
                    OutlinedButton(onClick = { onAdd("ARCHIVE_ACCOUNT") }, enabled = state.accounts.size > 1) { Text("Archive") }
                    OutlinedButton(onClick = { onAdd("RECONCILE") }, enabled = state.accounts.isNotEmpty()) { Text("Reconcile") }
                }
                if(state.archivedAccounts.isNotEmpty()) OutlinedButton(onClick={onAdd("RESTORE_ACCOUNT")}){Text("Restore archived account")}
            }
        }
        if (state.instruments.isNotEmpty()) {
            item { Text("Investments • ${Money(state.investmentValueMinor).format()}", style = MaterialTheme.typography.titleLarge) }
            items(state.instruments, key = { it.id }) { instrument ->
                val trades = state.investmentTransactions.filter { it.instrumentId == instrument.id }
                val quantityMicros = trades.sumOf { if (it.type == "SELL") -it.quantityMicros else it.quantityMicros }
                val quantity = java.math.BigDecimal.valueOf(quantityMicros, 6).stripTrailingZeros().toPlainString()
                val price = state.valuations.filter { it.instrumentId == instrument.id }.maxByOrNull { it.priceEpochDay }
                val currentValue = price?.let { runCatching { Math.multiplyExact(quantityMicros, it.priceMinor) / 1_000_000L }.getOrNull() }
                val lots = state.investmentLots.filter { it.instrumentId == instrument.id }
                val remainingCost = lots.sumOf { it.remainingCostMinor }
                val averageCost = if(quantityMicros>0) java.math.BigDecimal(remainingCost).multiply(java.math.BigDecimal(1_000_000)).divide(java.math.BigDecimal(quantityMicros),2,java.math.RoundingMode.HALF_UP).toLong() else 0L
                val realizedGain = lots.sumOf { it.realizedGainMinor } - trades.filter { it.type == "SELL" }.sumOf { it.feesMinor }
                val unrealizedGain = currentValue?.let { it - remainingCost }
                val absoluteReturn = currentValue?.takeIf { remainingCost>0 }?.let { (it-remainingCost)*100.0/remainingCost }
                val dividendIncome = state.dividends.filter { it.instrumentId == instrument.id }.sumOf { it.grossMinor }
                val flows = trades.filter { it.type == "BUY" || it.type == "SELL" }.map { trade ->
                    val gross = Math.multiplyExact(trade.quantityMicros, trade.priceMinor) / 1_000_000L
                    DatedCashFlow(LocalDate.ofEpochDay(trade.tradeEpochDay), if (trade.type == "SELL") gross - trade.feesMinor else -(gross + trade.feesMinor))
                } + state.dividends.filter { it.instrumentId == instrument.id }.map { DatedCashFlow(LocalDate.ofEpochDay(it.receivedEpochDay), it.grossMinor) } + listOfNotNull(currentValue?.let { DatedCashFlow(LocalDate.now(), it) })
                val xirr = runCatching { XirrCalculator.calculate(flows) }.getOrNull()
                EmptyCard(instrument.name, "${instrument.assetClass} • $quantity units • cost ${Money(remainingCost).format()} • average ${Money(averageCost).format()}/unit" + (price?.let { " • ${Money(it.priceMinor).format()} on ${LocalDate.ofEpochDay(it.priceEpochDay)} (${it.source})" } ?: " • add a dated valuation") + (unrealizedGain?.let { " • unrealized ${Money(it).format()}" } ?: "") + (absoluteReturn?.let { " • absolute ${"%.2f".format(it)}%" } ?: "") + " • realized ${Money(realizedGain).format()} • dividends ${Money(dividendIncome).format()}" + (xirr?.let { " • XIRR ${"%.2f".format(it*100)}%" } ?: ""))
            }
            items(state.instruments.filter { instrument -> state.valuations.count { it.instrumentId == instrument.id } >= 2 }, key = { "chart-${it.id}" }) { instrument ->
                val history=state.valuations.filter{it.instrumentId==instrument.id}.sortedBy{it.priceEpochDay}
                AccessibleLineChart(listOf(history.map{it.priceMinor}),listOf(MaterialTheme.colorScheme.primary),"Dated valuation history for ${instrument.name}, from ${Money(history.first().priceMinor).format()} on ${LocalDate.ofEpochDay(history.first().priceEpochDay)} to ${Money(history.last().priceMinor).format()} on ${LocalDate.ofEpochDay(history.last().priceEpochDay)} across ${history.size} manual or imported prices.")
            }
            item {
                val allocation = state.instruments.groupBy { it.assetClass }.mapValues { (_, instruments) -> instruments.sumOf { instrument -> val q=state.investmentTransactions.filter{it.instrumentId==instrument.id}.sumOf{if(it.type=="SELL")-it.quantityMicros else it.quantityMicros}; val p=state.valuations.filter{it.instrumentId==instrument.id}.maxByOrNull{it.priceEpochDay}?.priceMinor ?: 0L; runCatching{Math.multiplyExact(q,p)/1_000_000}.getOrDefault(0L) } }.filterValues{it>0}
                val total=allocation.values.sum()
                EmptyCard("Portfolio allocation",if(total==0L)"Add dated valuations to calculate allocation." else allocation.entries.sortedByDescending{it.value}.joinToString(" • "){"${it.key.replace('_',' ')} ${"%.0f".format(it.value*100.0/total)}%"})
            }
        }
        if (state.deposits.isNotEmpty()) {
            item { Text("Fixed income", style = MaterialTheme.typography.titleLarge) }
            items(state.deposits, key = { it.id }) { deposit -> EmptyCard(deposit.name, "${deposit.type} • ${Money(deposit.principalMinor).format()} • matures ${LocalDate.ofEpochDay(deposit.maturityEpochDay)}" + (deposit.lockInEndEpochDay?.let { " • locked until ${LocalDate.ofEpochDay(it)}" } ?: "") + (deposit.withdrawalRestrictions?.let { " • $it" } ?: "")) }
        }
        if (state.dividends.isNotEmpty() || state.sips.isNotEmpty()) {
            item { Text("Investment income & plans", style = MaterialTheme.typography.titleLarge) }
            items(state.dividends.take(6), key = { it.id }) { EmptyCard("Dividend", "Gross ${Money(it.grossMinor).format()} • TDS ${Money(it.taxWithheldMinor).format()} • ${LocalDate.ofEpochDay(it.receivedEpochDay)}") }
            items(state.sips, key = { it.id }) { sip -> Card(shape=RoundedCornerShape(18.dp)){Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("SIP • ${sip.frequency.lowercase().replaceFirstChar(Char::uppercase)}",fontWeight=FontWeight.SemiBold);Text("${Money(sip.amountMinor).format()} • next due ${LocalDate.ofEpochDay(sip.nextDueEpochDay)} • not invested until confirmed",color=MaterialTheme.colorScheme.onSurfaceVariant)};OutlinedButton(onClick={onAdd("SIP_PURCHASE:${sip.id}")}){Text("Confirm")}}} }
        }
        if (state.retirementContributions.isNotEmpty()) {
            item { Text("Retirement", style = MaterialTheme.typography.titleLarge) }
            items(state.retirementContributions.take(8), key = { it.id }) { contribution -> EmptyCard(contribution.scheme, "${Money(contribution.employeeMinor + contribution.employerMinor).format()} contribution • ${LocalDate.ofEpochDay(contribution.contributionEpochDay)}" + if (contribution.scheme == "EPS") " • pension entitlement, excluded from liquid net worth" else "") }
        }
        if (state.retirementEvents.isNotEmpty()) {
            items(state.retirementEvents.take(6), key = { it.id }) { EmptyCard("${it.scheme} • ${it.type.replace('_',' ').lowercase().replaceFirstChar(Char::uppercase)}", "${LocalDate.ofEpochDay(it.eventEpochDay)}${it.employerName?.let { employer -> " • $employer" }.orEmpty()}${it.note?.let { note -> " • $note" }.orEmpty()}") }
        }
        if (state.assets.isNotEmpty()) {
            item { Text("Physical assets • ${Money(state.physicalAssetValueMinor).format()}", style = MaterialTheme.typography.titleLarge) }
            items(state.assets, key = { it.id }) { asset ->
                val value = state.assetValuations.filter { it.assetId == asset.id }.maxByOrNull { it.valuationEpochDay }
                EmptyCard(asset.name, "${asset.type} • estimated market ${Money(value?.estimatedMarketMinor ?: asset.purchaseMinor).format()}" + (value?.estimatedResaleMinor?.let { " • estimated resale ${Money(it).format()}" } ?: "") + (asset.weightMilligrams?.let { " • ${java.math.BigDecimal.valueOf(it,3).stripTrailingZeros().toPlainString()}g at ${java.math.BigDecimal.valueOf(asset.purityBasisPoints?.toLong() ?: 0,2).stripTrailingZeros().toPlainString()}% purity" } ?: "") + (asset.linkedLoanId?.let { id -> state.loans.firstOrNull{it.id==id}?.let{" • linked debt ${it.name}: ${Money(it.outstandingPrincipalMinor).format()}"} } ?: ""))
            }
        }
        if (state.loans.isNotEmpty()) {
            item { Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Debt", style = MaterialTheme.typography.titleLarge);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={onAdd("LOAN_PAYMENT")}){Text("Record payment")};OutlinedButton(onClick={onAdd("LOAN_RATE_CHANGE")}){Text("Change rate")}}} }
            items(state.loans, key = { it.id }) { loan ->
                val impact = runCatching { LoanCalculator.simulatePrepayment(loan.outstandingPrincipalMinor, loan.annualRateBasisPoints, loan.remainingMonths, (loan.outstandingPrincipalMinor / 10).coerceAtLeast(1)) }.getOrNull()
                val schedule = state.loanSchedules.filter { it.loanId == loan.id }
                val paid = schedule.count { it.status == "PAID" }
                val remainingInterest = schedule.filter { it.status == "DUE" }.sumOf { it.interestMinor }
                EmptyCard(loan.name, "Outstanding ${Money(loan.outstandingPrincipalMinor).format()} • EMI ${Money(loan.emiMinor).format()}" + (loan.nextDueEpochDay?.let { " • next due ${LocalDate.ofEpochDay(it)}" } ?: "") + (schedule.takeIf { it.isNotEmpty() }?.let { " • schedule $paid/${it.size} paid • remaining scheduled interest ${Money(remainingInterest).format()}" } ?: "") + (impact?.let { " • a 10% prepayment could reduce scheduled interest by ${Money(it.interestSavedMinor).format()}" } ?: ""))
            }
        }
        if (state.policies.isNotEmpty()) {
            item { Text("Protection", style = MaterialTheme.typography.titleLarge) }
            items(state.policies, key = { it.id }) { policy -> val documents=state.policyDocuments.filter{it.policyId==policy.id}; Card(shape=RoundedCornerShape(18.dp)){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(policy.name,fontWeight=FontWeight.SemiBold);Text("${policy.type.replace('_',' ')} • ${policy.insurer} • coverage ${Money(policy.coverageMinor).format()} • premium ${Money(policy.premiumMinor).format()} • renews ${LocalDate.ofEpochDay(policy.renewalEpochDay)}",color=MaterialTheme.colorScheme.onSurfaceVariant);policy.deductibleMinor?.let{Text("Deductible ${Money(it).format()}")};policy.nominees?.let{Text("Nominees: $it")};policy.exclusions?.let{Text("Exclusions / notes: $it")};documents.firstOrNull()?.let{Text("${documents.size} encrypted document${if(documents.size==1)"" else "s"} • latest ${it.displayName} (${it.sizeBytes} bytes)",color=MaterialTheme.colorScheme.onSurfaceVariant)};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={onPolicyDocument(policy.id)}){Text("Attach document")};if(documents.isNotEmpty())OutlinedButton(onClick={onRemovePolicyDocument(documents.first().id)}){Text("Remove latest")}}}} }
        }
        if (state.cardStatements.isNotEmpty()) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) { Text("Credit-card statements", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick={onAdd("CARD_PAYMENT")}, enabled=state.cardStatements.any{it.status!="PAID"}){Text("Pay") } } }
            items(state.cardStatements, key = { it.id }) { EmptyCard("Due ${LocalDate.ofEpochDay(it.dueEpochDay)}", "Statement ${Money(it.statementBalanceMinor).format()} • minimum ${Money(it.minimumDueMinor).format()} • ${it.status}") }
        }
        if (state.goals.isNotEmpty()) {
            item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Goals", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick={onAdd("GOAL_ALLOCATE")}){Text("Allocate")}} }
            items(state.goals, key = { it.id }) { goal ->
                val funded=state.goalAllocations.filter{it.goalId==goal.id}.sumOf{it.allocatedMinor}
                val shortfall=(goal.targetMinor-funded).coerceAtLeast(0); val completion=goal.monthlyContributionMinor.takeIf{it>0}?.let{LocalDate.now().plusMonths((shortfall+it-1)/it)}
                EmptyCard(goal.name, "${Money(funded).format()} of ${Money(goal.targetMinor).format()} (${if(goal.targetMinor>0) "%.0f".format(funded.toDouble()*100/goal.targetMinor) else "0"}%) • shortfall ${Money(shortfall).format()} • target ${LocalDate.ofEpochDay(goal.targetEpochDay)}" + (completion?.let{" • current contribution pace reaches target around $it"}?:" • add a monthly contribution to estimate completion") + ". Allocations label existing assets and do not inflate net worth.")
            }
        }
        item { Text("Account composition", style = MaterialTheme.typography.titleLarge) }
        items(state.accounts, key = { it.id }) { AccountRow(it) }
        item { Text("Market values remain manual and dated; LedgerX never presents an offline estimate as a live price.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun InsightsScreen(state: LedgerXUiState, padding: PaddingValues) {
    val overspending = state.monthlyExpenseMinor > state.monthlyTakeHomeMinor && state.monthlyExpenseMinor > 0
    val liquidOpening = state.accounts.filter { it.kind != "CREDIT_CARD" }.sumOf { it.balanceMinor }
    val baseline = ForecastEngine.project("Baseline", ForecastAssumptions(liquidOpening, state.monthlyTakeHomeMinor, state.monthlyExpenseMinor, 0, 600, 0), 12)
    val optimistic = ForecastEngine.project("Optimistic", ForecastAssumptions(liquidOpening, state.monthlyTakeHomeMinor, state.monthlyExpenseMinor, 500, 400, 600), 12)
    val adverse = ForecastEngine.project("Adverse", ForecastAssumptions(liquidOpening, state.monthlyTakeHomeMinor, state.monthlyExpenseMinor, -500, 800, -500), 12)
    val fiveYear = ForecastEngine.project("Baseline", ForecastAssumptions(liquidOpening, state.monthlyTakeHomeMinor, state.monthlyExpenseMinor, 0, 600, 0), 60)
    val tenYear = ForecastEngine.project("Baseline", ForecastAssumptions(liquidOpening, state.monthlyTakeHomeMinor, state.monthlyExpenseMinor, 0, 600, 0), 120)
    val calendar = buildList {
        state.bills.filter { it.status == "DUE" }.forEach { add(Triple(it.dueEpochDay, "Bill", "${it.name} • ${Money(it.amountMinor).format()}")) }
        state.loans.mapNotNull { loan -> loan.nextDueEpochDay?.let { Triple(it, "EMI", "${loan.name} • ${Money(loan.emiMinor).format()}") } }.forEach { add(it) }
        state.policies.forEach { add(Triple(it.renewalEpochDay, "Insurance", "${it.name} renewal")) }
        state.recurring.forEach { add(Triple(it.nextDueEpochDay, it.type.lowercase().replaceFirstChar(Char::uppercase), it.merchant ?: "Recurring transaction")) }
        state.sips.forEach { add(Triple(it.nextDueEpochDay, "SIP", "${Money(it.amountMinor).format()} planned; confirm actual units and price")) }
        state.deposits.forEach { add(Triple(it.maturityEpochDay, "Deposit maturity", "${it.name} • ${it.maturityMinor?.let { amount -> Money(amount).format() } ?: Money(it.principalMinor).format()}")) }
        state.goals.forEach { add(Triple(it.targetEpochDay, "Goal milestone", it.name)) }
        state.cardStatements.filter{it.status!="PAID"}.forEach { add(Triple(it.dueEpochDay, "Card payment", "${Money(it.statementBalanceMinor-it.paidMinor).format()} outstanding")) }
        state.pendingOccurrences.forEach { occurrence -> state.recurring.firstOrNull{it.id==occurrence.templateId}?.let { add(Triple(occurrence.dueEpochDay,"Income confirmation",it.merchant ?: "Scheduled income")) } }
        state.notificationSchedules.filter { it.sourceType == "ADVANCE_TAX" }.forEach { schedule -> add(Triple(Instant.ofEpochMilli(schedule.dueAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay(),"Tax checkpoint",schedule.title + " • verify current applicability and official due date")) }
    }.sortedBy { it.first }
    val evidenceInsights = buildList {
        val liquid = state.accounts.filter { it.kind != "CREDIT_CARD" }.sumOf { it.balanceMinor }.coerceAtLeast(0)
        if (state.monthlyExpenseMinor > 0 && java.math.BigDecimal.valueOf(liquid) < java.math.BigDecimal.valueOf(state.monthlyExpenseMinor).multiply(java.math.BigDecimal.valueOf(3))) add("Reserve runway" to "Detected ${"%.1f".format(liquid.toDouble()/state.monthlyExpenseMinor)} months from recorded liquid balances and monthly expenses. Impact: a short disruption could create a cash deficit. Confidence: medium because essential spending is not separately tagged. Action: review expenses and build toward an appropriate reserve.")
        val emi = state.loans.sumOf { it.emiMinor }
        if (state.monthlyTakeHomeMinor > 0 && emi.toDouble() * 100 / state.monthlyTakeHomeMinor > 40) add("Debt pressure" to "Recorded EMIs use ${"%.0f".format(emi.toDouble()*100/state.monthlyTakeHomeMinor)}% of confirmed take-home income. Impact: ${Money(emi).format()} is committed monthly. Confidence: high for recorded loans. Action: compare prepayment scenarios and total borrowing cost.")
        val total = state.investmentValueMinor + state.deposits.sumOf { it.principalMinor } + state.physicalAssetValueMinor
        if (total > 0 && state.physicalAssetValueMinor.toDouble() * 100 / total > 70) add("Wealth concentration" to "Physical assets represent ${"%.0f".format(state.physicalAssetValueMinor.toDouble()*100/total)}% of tracked invested wealth. Impact depends on valuation and liquidity. Confidence: low to medium because market values are manual. Action: review concentration against goals and risk tolerance; no product recommendation is implied.")
        state.goals.forEach { goal -> val funded=state.goalAllocations.filter{it.goalId==goal.id}.sumOf{it.allocatedMinor}; if(funded<goal.targetMinor) add("Goal shortfall: ${goal.name}" to "Recorded allocation is ${Money(funded).format()} against ${Money(goal.targetMinor).format()}; shortfall ${Money(goal.targetMinor-funded).format()}. Confidence: high for entered values. Action: review the ${Money(goal.monthlyContributionMinor).format()} monthly plan without double-counting underlying accounts.") }
        val recurringBills = state.bills.filter { it.frequency != null }
        val annualSubscriptions = recurringBills.fold(0L) { total, bill -> Math.addExact(total, Math.multiplyExact(bill.amountMinor, when(bill.frequency?.uppercase()){ "WEEKLY"->52L;"MONTHLY"->12L;"QUARTERLY"->4L;else->1L })) }
        if (annualSubscriptions > 0) add("Recurring commitment cost" to "Recorded bills and subscriptions imply approximately ${Money(annualSubscriptions).format()} per year. Confidence: medium because frequency labels and future price changes are user-entered. Action: review each renewal before it becomes due.")
        recurringBills.groupBy { (it.merchant ?: it.name).trim().lowercase() }.filterValues { it.size > 1 }.forEach { (_, duplicates) -> add("Possible duplicate subscription" to "${duplicates.size} active entries appear to refer to ${duplicates.first().merchant ?: duplicates.first().name}. Estimated annual impact depends on their recorded frequencies. Confidence: medium. Action: verify that each entry is intentional before cancelling anything.") }
        state.bills.filter { it.status == "DUE" && it.dueEpochDay < LocalDate.now().toEpochDay() }.forEach { add("Missed obligation: ${it.name}" to "The recorded due date ${LocalDate.ofEpochDay(it.dueEpochDay)} has passed and the bill is still marked due. Potential impact: ${Money(it.amountMinor).format()} plus any provider fees not known to LedgerX. Confidence: high for the entered status. Action: verify payment directly with the provider.") }
        state.expenseByCategory.firstOrNull()?.let { top -> add("Largest 12-month spending category" to "${top.name} is the largest recorded category at ${Money(top.amountMinor).format()}. Confidence: high for confirmed, categorised transactions. Action: open Activity and verify its underlying records before changing a budget.") }
        state.expenseByMerchant.firstOrNull()?.takeIf { it.name != "Unspecified merchant" }?.let { top -> add("Largest 12-month merchant total" to "${top.name} totals ${Money(top.amountMinor).format()} across confirmed expenses. Confidence: high for records with merchant names. Action: search Activity for this merchant to inspect frequency and necessity.") }
        if(state.monthlySpend.size >= 6) {
            val latest=state.monthlySpend.takeLast(3).map{it.amountMinor}.average(); val prior=state.monthlySpend.dropLast(3).takeLast(3).map{it.amountMinor}.average()
            if(prior>0 && latest>prior*1.1) add("Lifestyle-inflation signal" to "Average recorded spending in the latest three populated months is ${"%.0f".format((latest/prior-1)*100)}% above the preceding three. Confidence: medium because missing months and one-off costs can distort this comparison. Action: compare category and merchant totals before drawing a conclusion.")
        }
        val composed=state.expenseComposition.sumOf{it.amountMinor}; val committed=state.expenseComposition.firstOrNull{it.name=="Recurring / fixed"}?.amountMinor ?: 0
        if(composed>0) add("Spending composition" to "Confirmed recurring templates account for ${"%.0f".format(committed*100.0/composed)}% of 12-month recorded expenses (${Money(committed).format()}). Other spending is not automatically labelled discretionary. Confidence: high for origin labels. Action: review recurring commitments separately from flexible costs.")
        if(state.loanInterestAndFees12m>0) add("Interest and fee leakage" to "Recorded loan payments include ${Money(state.loanInterestAndFees12m).format()} of interest and fees over the last 12 months. Confidence: high for entered payment splits. Action: compare prepayment scenarios and lender terms; do not assume a refinance is cheaper without total-cost analysis.")
        val recordedExpenses=state.transactions.filter{it.type=="EXPENSE"}
        val averageExpense=recordedExpenses.map{it.amountMinor}.takeIf{it.size>=5}?.average()
        averageExpense?.let { average -> recordedExpenses.firstOrNull{it.amountMinor>average*3}?.let { unusual -> add("Unusual recorded expense" to "${unusual.merchant ?: unusual.categoryId ?: "An expense"} at ${Money(unusual.amountMinor).format()} is more than three times the recent transaction average of ${Money(average.toLong()).format()}. Confidence: low to medium because legitimate one-off purchases are common. Action: verify the underlying Activity entry and receipt; no fraud conclusion is implied.") } }
        state.bills.filter{it.pricePreviousMinor!=null && it.amountMinor>it.pricePreviousMinor!!}.forEach { bill -> add("Growing recurring cost: ${bill.name}" to "The latest recorded amount is ${Money(bill.amountMinor).format()}, up ${Money(bill.amountMinor-bill.pricePreviousMinor!!).format()} from the preceding entry. Confidence: high for entered prices. Action: verify the provider bill and compare the annual impact before renewing.") }
        if(state.monthlyExpenseMinor>0 && liquid.toDouble()>state.monthlyExpenseMinor*12.0 && state.goals.isEmpty()) add("Unallocated liquid cash" to "Recorded liquid balances equal ${"%.1f".format(liquid.toDouble()/state.monthlyExpenseMinor)} months of current spending. Impact and suitability depend on near-term obligations and risk tolerance. Confidence: medium because goals and essential spending may be incomplete. Action: add goals and upcoming obligations before deciding whether any cash is truly idle; LedgerX does not recommend a product.")
        baseline.points.firstOrNull{it.balanceMinor<0}?.let { shortage -> add("Projected liquidity shortage" to "The baseline cashflow scenario first falls below zero around month ${shortage.month}, using confirmed take-home income and current expense assumptions. Confidence: scenario-dependent. Action: inspect upcoming obligations, reduce the forecast horizon assumptions, or add missing income—do not treat this projection as certain.") }
    }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            EmptyCard(
                if (overspending) "Cashflow needs attention" else "No urgent pattern detected",
                if (overspending) "Recorded expenses exceed recorded take-home income by ${Money(state.monthlyExpenseMinor - state.monthlyTakeHomeMinor).format()}. Add missing income before acting on this signal."
                else "LedgerX needs more transaction history before it can make evidence-based recommendations.",
            )
        }
        item { Text("Insights are calculated locally from confirmed records. Missing data is never interpreted as healthy.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (evidenceInsights.isNotEmpty()) {
            item { Text("Actionable findings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            items(evidenceInsights, key={it.first}) { EmptyCard(it.first,it.second) }
        }
        item { Text("Recorded financial charts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        item { AccessibleBarChart(listOf("Take-home income" to state.monthlyTakeHomeMinor, "Expenses" to state.monthlyExpenseMinor, "Investments" to state.confirmedMonthlyInvestmentMinor), "Current-month recorded take-home income ${Money(state.monthlyTakeHomeMinor).format()}, expenses ${Money(state.monthlyExpenseMinor).format()}, and investment purchases ${Money(state.confirmedMonthlyInvestmentMinor).format()}.") }
        if(state.expenseByCategory.isNotEmpty()) item { AccessibleBarChart(state.expenseByCategory.take(6).map{it.name to it.amountMinor},"Top recorded expense categories over the last 12 months: ${state.expenseByCategory.take(6).joinToString { "${it.name} ${Money(it.amountMinor).format()}" }}.") }
        if(state.expenseByMerchant.isNotEmpty()) item { AccessibleBarChart(state.expenseByMerchant.take(6).map{it.name to it.amountMinor},"Top recorded merchants over the last 12 months: ${state.expenseByMerchant.take(6).joinToString { "${it.name} ${Money(it.amountMinor).format()}" }}.") }
        val assetChart = listOf("Cash" to state.accounts.sumOf{it.balanceMinor}.coerceAtLeast(0),"Investments" to state.investmentValueMinor,"Deposits" to state.deposits.sumOf{it.principalMinor},"Retirement" to state.retirementValueMinor,"Physical" to state.physicalAssetValueMinor).filter{it.second>0}
        if(assetChart.isNotEmpty()) item { AccessibleBarChart(assetChart,"Current net-worth asset decomposition: ${assetChart.joinToString { "${it.first} ${Money(it.second).format()}" }}. Liabilities are shown separately in the debt chart.") }
        state.loans.firstOrNull()?.let { loan -> val reduction=state.loanSchedules.filter{it.loanId==loan.id}.map{it.closingMinor}; if(reduction.size>=2) item { AccessibleLineChart(listOf(reduction),listOf(MaterialTheme.colorScheme.error),"Scheduled debt reduction for ${loan.name}, from ${Money(reduction.first()).format()} after the first installment to ${Money(reduction.last()).format()} after ${reduction.size} installments. This is the recorded amortization schedule, not lender confirmation.") } }
        if(state.goals.isNotEmpty()) item { AccessibleBarChart(state.goals.take(4).flatMap{goal->listOf("${goal.name} funded" to state.goalAllocations.filter{it.goalId==goal.id}.sumOf{it.allocatedMinor},"${goal.name} target" to goal.targetMinor)},"Goal progress chart. ${state.goals.take(4).joinToString { goal -> val funded=state.goalAllocations.filter{it.goalId==goal.id}.sumOf{it.allocatedMinor}; "${goal.name} ${Money(funded).format()} of ${Money(goal.targetMinor).format()}" }}.") }
        if(state.monthlySpend.size>=2) {
            item { Text("Recorded spending trend",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold) }
            item { AccessibleLineChart(listOf(state.monthlySpend.map{it.amountMinor}),listOf(MaterialTheme.colorScheme.error),"Monthly expense trend from ${state.monthlySpend.first().month} ${Money(state.monthlySpend.first().amountMinor).format()} to ${state.monthlySpend.last().month} ${Money(state.monthlySpend.last().amountMinor).format()} across ${state.monthlySpend.size} populated months.") }
        }
        item { Text("12-month cashflow scenarios", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        item { AccessibleLineChart(listOf(baseline.points.map{it.balanceMinor}, optimistic.points.map{it.balanceMinor}, adverse.points.map{it.balanceMinor}), listOf(MaterialTheme.colorScheme.primary, Color(0xFF55E6B5), MaterialTheme.colorScheme.error), "Cashflow forecast chart. Baseline ends at ${Money(baseline.points.last().balanceMinor).format()}, optimistic at ${Money(optimistic.points.last().balanceMinor).format()}, adverse at ${Money(adverse.points.last().balanceMinor).format()} after 12 months.") }
        item { EmptyCard("Baseline", "Projected balance ${Money(baseline.points.last().balanceMinor).format()} • 6% annual expense inflation") }
        item { EmptyCard("Optimistic", "Projected balance ${Money(optimistic.points.last().balanceMinor).format()} • 5% income growth, 4% inflation, 6% return") }
        item { EmptyCard("Adverse", "Projected balance ${Money(adverse.points.last().balanceMinor).format()} • −5% income growth, 8% inflation, −5% return") }
        item { EmptyCard("Forecast horizons", "7-day cashflow ${Money(liquidOpening + (state.monthlyTakeHomeMinor - state.monthlyExpenseMinor) * 7 / 30).format()} • 30-day ${Money(baseline.points[1].balanceMinor).format()} • 90-day ${Money(baseline.points[3].balanceMinor).format()}") }
        item { EmptyCard("Long-range baseline", "5 years ${Money(fiveYear.points.last().balanceMinor).format()} • 10 years ${Money(tenYear.points.last().balanceMinor).format()}. These are deterministic assumptions, not guarantees.") }
        if (adverse.points.any { it.balanceMinor < 0 }) item { EmptyCard("Liquidity warning", "The adverse scenario falls below zero. Review upcoming obligations and reserve assumptions.") }
        item { Text("Financial calendar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        if (calendar.isEmpty()) item { EmptyCard("No scheduled events", "Bills, EMIs, insurance renewals, and recurring transactions appear here.") }
        else items(calendar.take(12), key = { "${it.first}-${it.second}-${it.third}" }) { event -> EmptyCard("${event.second} • ${LocalDate.ofEpochDay(event.first)}", event.third) }
    }
}

@Composable
private fun AccessibleLineChart(series: List<List<Long>>, colors: List<Color>, summary: String) {
    val values = series.flatten()
    if (values.size < 2) return
    val minimum = values.minOrNull() ?: 0L
    val maximum = values.maxOrNull() ?: minimum
    val range = (maximum - minimum).takeIf { it > 0 } ?: 1L
    Card(shape=RoundedCornerShape(18.dp)) {
        Canvas(Modifier.fillMaxWidth().height(180.dp).padding(16.dp).semantics { contentDescription = summary }) {
            series.forEachIndexed { seriesIndex, points ->
                if (points.size > 1) points.zipWithNext().forEachIndexed { index, pair ->
                    fun point(position: Int, value: Long) = Offset(
                        x = size.width * position / (points.size - 1).toFloat(),
                        y = size.height - size.height * (value - minimum).toFloat() / range.toFloat(),
                    )
                    drawLine(colors[seriesIndex % colors.size], point(index,pair.first), point(index+1,pair.second), strokeWidth=4.dp.toPx(), cap=StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun AccessibleBarChart(values: List<Pair<String, Long>>, summary: String) {
    val visible = values.filter { it.second >= 0 }.take(8)
    if (visible.isEmpty()) return
    var selected by remember(visible) { mutableIntStateOf(visible.indices.maxByOrNull { visible[it].second } ?: 0) }
    val maximum = visible.maxOf { it.second }.coerceAtLeast(1L)
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.semantics { contentDescription = summary }) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                visible.forEachIndexed { index, item ->
                    val height = (112.0 * item.second / maximum).toInt().coerceAtLeast(4).dp
                    Column(Modifier.weight(1f).clickable { selected = index }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height(height).background(if (index == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
                        Text(item.first, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Text("${visible[selected].first}: ${Money(visible[selected].second).format()}", fontWeight = FontWeight.SemiBold)
            Text("Tap a bar for its exact recorded value.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HealthScreen(
    state: LedgerXUiState, padding: PaddingValues, deviceAuthenticationAvailable: Boolean,
    onSetAppLock: (Boolean) -> Unit, password: String, onPasswordChange: (String) -> Unit,
    themeMode: String, onThemeMode: (String) -> Unit,
    textScale: Float, onTextScale: (Float) -> Unit,
    remindersEnabled: Boolean, onRemindersEnabled: (Boolean) -> Unit,
    localeTag: String, onLocaleTag: (String) -> Unit,
    onBackup: () -> Unit, onRestore: () -> Unit, onExport: () -> Unit, onFinancialReport: () -> Unit, onCaExport: () -> Unit,
    onTax: () -> Unit, onNotifications: () -> Unit,
    onDeleteData: () -> Unit,
    onDashboardLayout: (List<String>) -> Unit,
    onHealthSnapshot: (Int?, Int, String, String) -> Unit,
    onPlanningAssumptions: (String, String, String, String, String, String, String) -> Unit,
) {
    val savingsRate = LedgerMath.savingsRateBasisPoints(state.monthlyTakeHomeMinor, state.monthlyExpenseMinor)
    val debtPayments = state.loans.sumOf { it.emiMinor }
    val hasCashflow = state.transactions.any { it.type == "INCOME" || it.type == "EXPENSE" }
    val liquid = state.accounts.filter { it.kind != "CREDIT_CARD" }.sumOf { it.balanceMinor }.coerceAtLeast(0)
    val incomeRows = state.transactions.filter { it.type == "INCOME" }
    val stableIncome = incomeRows.filter { it.categoryId == "salary" || it.categoryId == "pension" }.sumOf { it.amountMinor }
    val incomeTotal = incomeRows.sumOf { it.amountMinor }
    val classes = listOf(state.accounts.sumOf { it.balanceMinor }.coerceAtLeast(0), state.investmentValueMinor, state.deposits.sumOf { it.principalMinor }, state.retirementValueMinor, state.physicalAssetValueMinor).filter { it > 0 }
    val totalAssets = classes.sum()
    val assumptions = state.planningAssumptions
    val essentialExpense = assumptions?.essentialMonthlyExpenseMinor?.takeIf { it > 0 }
    val insuranceNeed = assumptions?.statedInsuranceNeedMinor?.takeIf { it > 0 }
    val insuranceCoverage = state.policies.filter { it.active && it.type in setOf("HEALTH", "TERM_LIFE") }.sumOf { it.coverageMinor }
    val health = HealthScoreCalculator.calculate(
        HealthInputs(
            emergencyMonths = essentialExpense?.let { liquid.toDouble() / it },
            debtToIncomePercent = if (state.monthlyTakeHomeMinor > 0) debtPayments * 100.0 / state.monthlyTakeHomeMinor else null,
            savingsRatePercent = savingsRate?.div(100.0),
            monthlySurplusMinor = if (hasCashflow) state.monthlyTakeHomeMinor - state.monthlyExpenseMinor else null,
            stableIncomeSharePercent = incomeTotal.takeIf { it > 0 }?.let { stableIncome * 100.0 / it },
            largestAssetClassPercent = totalAssets.takeIf { it > 0 }?.let { classes.maxOrNull()!! * 100.0 / it },
            insuranceCoverageRatio = insuranceNeed?.let { insuranceCoverage.toDouble() / it },
        )
    )
    val retirementScenarios = assumptions?.let { input ->
        val currentAge=input.currentAge; val retirementAge=input.retirementAge; val target=input.retirementTargetMinor
        val retirementMonthlyContribution = state.retirementContributions.filter { it.contributionEpochDay >= LocalDate.now().minusMonths(12).toEpochDay() }.sumOf { it.employeeMinor + it.employerMinor } / 12
        if(currentAge!=null && retirementAge!=null && target!=null) listOf(
            RetirementCalculator.project("Baseline",currentAge,retirementAge,state.retirementValueMinor,retirementMonthlyContribution,target,input.returnBasisPoints,input.inflationBasisPoints),
            RetirementCalculator.project("Optimistic",currentAge,retirementAge,state.retirementValueMinor,retirementMonthlyContribution,target,input.returnBasisPoints+200,(input.inflationBasisPoints-100).coerceAtLeast(0)),
            RetirementCalculator.project("Adverse",currentAge,retirementAge,state.retirementValueMinor,retirementMonthlyContribution,target,(input.returnBasisPoints-300).coerceAtLeast(-5_000),input.inflationBasisPoints+200),
        ) else emptyList()
    }.orEmpty()
    LaunchedEffect(health.score, health.completenessPercent, health.criticalWarnings) {
        val dimensionsJson = health.dimensions.joinToString(prefix = "[", postfix = "]") { "{\"name\":\"${it.name}\",\"score\":${it.score ?: "null"},\"weight\":${it.weight}}" }
        val warningsJson = health.criticalWarnings.joinToString(prefix = "[", postfix = "]") { "\"${it.replace("\"", "\\\"")}\"" }
        onHealthSnapshot(health.score, health.completenessPercent, dimensionsJson, warningsJson)
    }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Financial health", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { EmptyCard(health.score?.let { "Health score: $it/100" } ?: "Score unavailable", if (health.score == null) "Complete all seven dimensions before LedgerX presents an overall score." else "All required dimensions are available.") }
        item { EmptyCard("Data completeness", "${health.completenessPercent}% of weighted inputs available") }
        val scoredHistory = state.healthHistory.mapNotNull { it.score }.reversed()
        if(scoredHistory.size >= 2) item { AccessibleLineChart(listOf(scoredHistory.map(Int::toLong)),listOf(MaterialTheme.colorScheme.primary),"Financial health history from ${scoredHistory.first()} to ${scoredHistory.last()} across ${scoredHistory.size} daily snapshots.") }
        if(state.healthHistory.isNotEmpty()) item { EmptyCard("Health history","${state.healthHistory.size} daily snapshot${if(state.healthHistory.size==1)"" else "s"} retained locally. Latest completeness ${state.healthHistory.first().completenessPercent}%.") }
        items(health.dimensions, key = { it.name }) { dimension ->
            EmptyCard(dimension.name + (dimension.score?.let { " • $it/100" } ?: ""), dimension.explanation + " " + dimension.recommendation)
        }
        items(health.criticalWarnings) { warning -> EmptyCard("Critical warning", warning) }
        item {
            var essential by remember(assumptions) { mutableStateOf(assumptions?.essentialMonthlyExpenseMinor?.let { Money(it).formatMajorForInput() }.orEmpty()) }
            var need by remember(assumptions) { mutableStateOf(assumptions?.statedInsuranceNeedMinor?.let { Money(it).formatMajorForInput() }.orEmpty()) }
            var age by remember(assumptions) { mutableStateOf(assumptions?.currentAge?.toString().orEmpty()) }
            var retireAge by remember(assumptions) { mutableStateOf(assumptions?.retirementAge?.toString().orEmpty()) }
            var target by remember(assumptions) { mutableStateOf(assumptions?.retirementTargetMinor?.let { Money(it).formatMajorForInput() }.orEmpty()) }
            var inflation by remember(assumptions) { mutableStateOf(assumptions?.inflationBasisPoints?.let { java.math.BigDecimal.valueOf(it.toLong(),2).stripTrailingZeros().toPlainString() } ?: "6") }
            var expectedReturn by remember(assumptions) { mutableStateOf(assumptions?.returnBasisPoints?.let { java.math.BigDecimal.valueOf(it.toLong(),2).stripTrailingZeros().toPlainString() } ?: "8") }
            Card(shape=RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Planning assumptions",fontWeight=FontWeight.SemiBold)
                Text("Enter essential spending and your own protection need. Missing values remain unscored.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyField(essential,{essential=it},"Essential monthly expenses")
                MoneyField(need,{need=it},"Stated insurance need")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ OutlinedTextField(age,{age=it.filter(Char::isDigit)},Modifier.weight(1f),label={Text("Current age")});OutlinedTextField(retireAge,{retireAge=it.filter(Char::isDigit)},Modifier.weight(1f),label={Text("Retirement age")}) }
                MoneyField(target,{target=it},"Retirement target in today's money")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ DecimalField(inflation,{inflation=it},"Inflation %","6",Modifier.weight(1f));DecimalField(expectedReturn,{expectedReturn=it},"Expected return %","8",Modifier.weight(1f)) }
                Button(onClick={onPlanningAssumptions(essential,need,age,retireAge,target,inflation,expectedReturn)},enabled=!state.busy,modifier=Modifier.fillMaxWidth()){Text("Save assumptions")}
            } }
        }
        if(retirementScenarios.isNotEmpty()) {
            item { Text("Retirement scenarios",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold) }
            item { AccessibleBarChart(retirementScenarios.flatMap{listOf("${it.scenario} corpus" to it.projectedCorpusMinor,"${it.scenario} target" to it.inflationAdjustedTargetMinor)},"Retirement projection scenarios: ${retirementScenarios.joinToString { "${it.scenario} corpus ${Money(it.projectedCorpusMinor).format()} versus inflation-adjusted target ${Money(it.inflationAdjustedTargetMinor).format()}" }}.") }
            items(retirementScenarios,key={it.scenario}) { projection -> EmptyCard(projection.scenario,"At age ${assumptions?.retirementAge}: projected ${Money(projection.projectedCorpusMinor).format()} • inflation-adjusted target ${Money(projection.inflationAdjustedTargetMinor).format()} • shortfall ${Money(projection.shortfallMinor).format()}") }
            item { Text("These projections use constant user-entered rates and confirmed contributions; they are scenarios, not promises or investment advice.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        item { Text("This is a planning indicator, not a credit score or guarantee of financial safety.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Privacy lock", fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            state.appLockEnabled -> "Biometric or device-credential authentication is required whenever LedgerX resumes."
                            deviceAuthenticationAvailable -> "Protect LedgerX whenever it leaves the foreground."
                            else -> "Configure a secure device lock before enabling LedgerX authentication."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { onSetAppLock(!state.appLockEnabled) },
                        enabled = state.appLockEnabled || deviceAuthenticationAvailable,
                    ) { Text(if (state.appLockEnabled) "Disable app lock" else "Enable app lock") }
                    Text("Appearance", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SYSTEM","LIGHT","DARK").forEach { mode -> FilterChip(themeMode == mode, { onThemeMode(mode) }, { Text(mode.lowercase().replaceFirstChar(Char::uppercase)) }) }
                    }
                    Text("Text size", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf(1f to "Standard",1.15f to "Large",1.3f to "Extra large").forEach { (scale,label) -> FilterChip(kotlin.math.abs(textScale-scale)<0.01f,{onTextScale(scale)},{Text(label)}) }
                    }
                    Text("Currency and number locale",fontWeight=FontWeight.SemiBold)
                    Text("Base currency: INR. Offline LedgerX does not combine currencies without exchange-rate evidence.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ listOf("en-IN" to "English (India)","hi-IN" to "Hindi numerals").forEach{(tag,label)->FilterChip(localeTag==tag,{onLocaleTag(tag)},{Text(label)})} }
                }
            }
        }
        item {
            Card(shape=RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("Dashboard customization",fontWeight=FontWeight.SemiBold)
                    Text("Choose which planning cards appear on Home. Accounts and recent activity remain available.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    val keys=listOf("NET_WORTH" to "Net worth","CASHFLOW" to "Cashflow","UPCOMING" to "Upcoming","GOALS" to "Goals")
                    keys.chunked(2).forEach { chunk -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { chunk.forEach { (key,label) -> val selected=state.dashboardVisible(key); FilterChip(selected,{ val current=keys.map{it.first}.filter{state.dashboardVisible(it)}.toMutableList(); if(selected)current.remove(key)else current.add(key); if(current.isNotEmpty())onDashboardLayout(current + listOf("INSIGHTS","HEALTH")) },{Text(label)}) } } }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tax planning", fontWeight = FontWeight.SemiBold)
                    state.taxEstimates?.let { (old, new) ->
                        Text("TY 2026-27 estimate • Old ${Money(old.totalMinor).format()} • New ${Money(new.totalMinor).format()}")
                        Text("Estimated balance after TDS/advance tax: ${Money(minOf(old.totalMinor, new.totalMinor) - (state.taxProfile?.tdsMinor ?: 0) - (state.taxProfile?.advanceTaxMinor ?: 0)).format()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } ?: Text("Add gross income, eligible old-regime deductions, TDS, and advance tax to compare regimes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Includes ordinary income plus user-classified section 111A/112A gains and cess. Surcharge, marginal relief, loss set-off, grandfathering and eligibility need professional verification.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onTax) { Text("Tax profile") }
                        OutlinedButton(onClick = onCaExport, enabled = state.taxProfile != null) { Text("CA export") }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Backup and data", fontWeight = FontWeight.SemiBold)
                    Text("Portable backups use AES-256-GCM and a password-derived key. A 12+ character password is required and cannot be recovered.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(password, onPasswordChange, Modifier.fillMaxWidth(), label = { Text("Backup password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onBackup, enabled = password.length >= 12 && !state.busy) { Text("Create backup") }
                        OutlinedButton(onClick = onRestore, enabled = password.length >= 12 && !state.busy) { Text("Restore") }
                    }
                    OutlinedButton(onClick = onExport, enabled = !state.busy) { Text("Export transactions CSV") }
                    OutlinedButton(onClick = onFinancialReport, enabled = !state.busy) { Text("Export advanced financial report") }
                    OutlinedButton(onClick=onDeleteData, enabled=!state.busy) { Text("Permanently delete local data") }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Local reminders", fontWeight = FontWeight.SemiBold)
                    Text("Allow notifications for upcoming bills, EMIs, and insurance renewals. Scheduling stays on-device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilterChip(remindersEnabled,{onRemindersEnabled(!remindersEnabled)},{Text(if(remindersEnabled)"Reminders on" else "Reminders off")})
                    if(remindersEnabled) OutlinedButton(onClick = onNotifications) { Text("Allow Android notifications") }
                }
            }
        }
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
private fun PlanningEntrySheet(
    kind: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onAddBill: (String, String, String, String?) -> Unit,
    onAddLoan: (String, String, String, String) -> Unit,
    onAddPolicy: (String, String, String, String, String) -> Unit,
    onAddGoal: (String, String, String, String, String) -> Unit,
) {
    var name by remember(kind) { mutableStateOf("") }
    var first by remember(kind) { mutableStateOf("") }
    var second by remember(kind) { mutableStateOf("") }
    var third by remember(kind) { mutableStateOf("") }
    var date by remember(kind) { mutableStateOf(LocalDate.now().plusMonths(1).toString()) }
    val title = when (kind) { "BILL" -> "Add bill"; "LOAN" -> "Add loan"; "POLICY" -> "Add insurance"; else -> "Add goal" }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
            when (kind) {
                "BILL" -> {
                    MoneyField(first, { first = it }, "Amount")
                    OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Due date (YYYY-MM-DD)") }, singleLine = true)
                    OutlinedTextField(second, { second = it }, Modifier.fillMaxWidth(), label = { Text("Frequency (optional)") }, singleLine = true)
                }
                "LOAN" -> {
                    MoneyField(first, { first = it }, "Outstanding principal")
                    OutlinedTextField(second, { second = it }, Modifier.fillMaxWidth(), label = { Text("Annual interest %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(third, { third = it }, Modifier.fillMaxWidth(), label = { Text("Remaining months") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
                "POLICY" -> {
                    OutlinedTextField(first, { first = it }, Modifier.fillMaxWidth(), label = { Text("Insurer") }, singleLine = true)
                    MoneyField(second, { second = it }, "Coverage")
                    MoneyField(third, { third = it }, "Premium")
                    OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Renewal date (YYYY-MM-DD)") }, singleLine = true)
                }
                else -> {
                    ChoiceRow(listOf("EMERGENCY_FUND","EDUCATION","TRAVEL","PURCHASE","HOME_DOWN_PAYMENT","RETIREMENT","OTHER"),third.ifBlank{"OTHER"}){third=it}
                    MoneyField(first, { first = it }, "Target amount")
                    MoneyField(second, { second = it }, "Monthly contribution (optional)")
                    OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Target date (YYYY-MM-DD)") }, singleLine = true)
                }
            }
            Button(
                onClick = {
                    when (kind) {
                        "BILL" -> onAddBill(name, first, date, second.ifBlank { null })
                        "LOAN" -> onAddLoan(name, first, second, third)
                        "POLICY" -> onAddPolicy(name, first, second, third, date)
                        else -> onAddGoal(name, third.ifBlank{"OTHER"}, first, date, second)
                    }
                },
                enabled = !busy && name.isNotBlank() && first.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Save") }
        }
    }
}

@Composable
private fun MoneyField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { char -> char.isDigit() || char == '.' }) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        prefix = { Text("₹ ") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

@Composable
private fun SignedMoneyField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, { next -> onValueChange(next.filterIndexed { index, char -> char.isDigit() || char == '.' || (char == '-' && index == 0) }) }, Modifier.fillMaxWidth(), label={Text(label)}, prefix={Text("₹ ")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal), singleLine=true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedEntrySheet(kind: String, state: LedgerXUiState, busy: Boolean, onDismiss: () -> Unit, vm: LedgerXViewModel) {
    val entryKind = kind.substringBefore(':')
    var account by remember(kind, state.accounts) { mutableStateOf((when(entryKind){"CARD_STATEMENT"->state.accounts.firstOrNull { it.kind == "CREDIT_CARD" };"CARD_PAYMENT"->state.accounts.firstOrNull { it.kind != "CREDIT_CARD" };else->state.accounts.firstOrNull()})?.id.orEmpty()) }
    var a by remember(kind) { mutableStateOf("") }; var b by remember(kind) { mutableStateOf("") }
    var c by remember(kind) { mutableStateOf("") }; var d by remember(kind) { mutableStateOf("") }
    var e by remember(kind) { mutableStateOf("") }; var f by remember(kind) { mutableStateOf("") }
    var g by remember(kind) { mutableStateOf("") }
    var h by remember(kind) { mutableStateOf("") }; var i by remember(kind) { mutableStateOf("") }; var j by remember(kind) { mutableStateOf("") }
    var k by remember(kind) { mutableStateOf("") }
    val today = LocalDate.now().toString()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(entrySheetTitle(entryKind), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            if (entryKind in setOf("BILL_PAYMENT", "INCOME_DETAIL", "INVESTMENT", "INVESTMENT_BUY", "INVESTMENT_SALE", "DIVIDEND", "SIP", "DEPOSIT", "RD_CONTRIBUTION", "ASSET_MAINTENANCE", "RECURRING", "RECONCILE", "CARD_STATEMENT", "CARD_PAYMENT", "ARCHIVE_ACCOUNT", "RENAME_ACCOUNT", "GOAL_ALLOCATE", "LOAN_PAYMENT", "SPLIT")) item { AccountChoices(when(entryKind){"CARD_STATEMENT"->state.accounts.filter { it.kind == "CREDIT_CARD" };"CARD_PAYMENT"->state.accounts.filter { it.kind != "CREDIT_CARD" };else->state.accounts}, account) { account = it } }
            when (entryKind) {
                "ACCOUNT" -> { item { TextField(a,{a=it},"Account name") }; item { ChoiceRow(listOf("SAVINGS","CURRENT","CASH","WALLET","CREDIT_CARD","OTHER"),b.ifBlank{"SAVINGS"}){b=it} }; item { MoneyField(c,{c=it},"Opening / current balance") } }
                "BILL_PAYMENT" -> { val bill=state.bills.firstOrNull{it.id==kind.substringAfter(':')}; item { EmptyCard(bill?.name?:"Bill",bill?.let{"${Money(it.amountMinor,it.currencyCode).format()} due ${LocalDate.ofEpochDay(it.dueEpochDay)}"}?:"Bill is unavailable") }; item { Text("Expense category",fontWeight=FontWeight.SemiBold);LabeledChoiceRow(state.categories.filter{it.kind=="EXPENSE"}.map{it.id to it.name},a){a=it} }; item { TextField(b,{b=it},"Payment date",today) }; item { Text("Saving records the expense and marks the bill paid atomically.",color=MaterialTheme.colorScheme.onSurfaceVariant) } }
                "INCOME_DETAIL" -> { item { MoneyField(a,{a=it},"Gross income") }; item { MoneyField(b,{b=it},"Take-home cash received") }; item { MoneyField(c,{c=it},"Other deductions") }; item { MoneyField(d,{d=it},"Tax withheld / TDS") }; item { Text("Income category",fontWeight=FontWeight.SemiBold);LabeledChoiceRow(state.categories.filter{it.kind=="INCOME"}.map{it.id to it.name},e){e=it} }; item { TextField(f,{f=it},"Employer / source") }; item { TextField(g,{g=it},"Note (optional)") }; item { TextField(h,{h=it},"Received date",today) }; item { Text("Gross must equal take-home plus deductions and tax withheld. LedgerX records only take-home in the selected cash account.",color=MaterialTheme.colorScheme.onSurfaceVariant) } }
                "POLICY" -> { item { TextField(a,{a=it},"Policy name") }; item { ChoiceRow(listOf("HEALTH","TERM_LIFE","MOTOR","OTHER"),b.ifBlank{"HEALTH"}){b=it} }; item { TextField(c,{c=it},"Insurer") }; item { MoneyField(d,{d=it},"Coverage / sum assured") }; item { MoneyField(e,{e=it},"Premium") }; item { MoneyField(f,{f=it},"Deductible (optional)") }; item { TextField(g,{g=it},"Renewal date",LocalDate.now().plusYears(1).toString()) }; item { TextField(h,{h=it},"Nominees (optional)") }; item { TextField(i,{i=it},"Exclusions / notes (optional)") } }
                "INVESTMENT" -> {
                    item { TextField(a,{a=it},"Instrument name") }; item { TextField(b,{b=it},"Symbol (optional)") }
                    item { ChoiceRow(listOf("EQUITY","ETF","MUTUAL_FUND"), c.ifBlank { "EQUITY" }) { c=it } }
                    item { DecimalField(d,{d=it},"Quantity") }; item { MoneyField(e,{e=it},"Price per unit") }; item { MoneyField(f,{f=it},"Fees (optional)") }; item { TextField(g,{g=it},"Trade date",today) }
                }
                "INVESTMENT_BUY" -> { item { LabeledChoiceRow(state.instruments.map{it.id to it.name},a){a=it} }; item { DecimalField(b,{b=it},"Quantity purchased") }; item { MoneyField(c,{c=it},"Price per unit") }; item { MoneyField(d,{d=it},"Fees (optional)") }; item { TextField(e,{e=it},"Trade date",today) } }
                "INVESTMENT_SALE" -> { item { LabeledChoiceRow(state.instruments.map{it.id to it.name},a){a=it} }; item { DecimalField(b,{b=it},"Quantity sold") }; item { MoneyField(c,{c=it},"Price per unit") }; item { MoneyField(d,{d=it},"Fees") }; item { TextField(e,{e=it},"Trade date",today) } }
                "CORPORATE_ACTION" -> { item { LabeledChoiceRow(state.instruments.map{it.id to it.name},a){a=it} }; item { ChoiceRow(listOf("SPLIT","BONUS"),b.ifBlank{"SPLIT"}){b=it} }; item { DecimalField(c,{c=it},"Additional quantity credited") }; item { TextField(d,{d=it},"Effective date",today) } }
                "DIVIDEND" -> { item { LabeledChoiceRow(state.instruments.map{it.id to it.name},a){a=it} }; item { MoneyField(b,{b=it},"Gross dividend") }; item { MoneyField(c,{c=it},"Tax withheld / TDS") }; item { TextField(d,{d=it},"Received date",today) } }
                "SIP" -> { item { LabeledChoiceRow(state.instruments.map{it.id to it.name},a){a=it} }; item { MoneyField(b,{b=it},"Planned contribution") }; item { ChoiceRow(listOf("WEEKLY","MONTHLY","QUARTERLY","YEARLY"),c.ifBlank{"MONTHLY"}){c=it} }; item { DecimalField(d,{d=it},"Every N periods","1") }; item { TextField(e,{e=it},"Next due date",today) }; item { Text("A SIP remains scheduled until you confirm the actual purchase price and units; it is not counted as invested in advance.", color=MaterialTheme.colorScheme.onSurfaceVariant) } }
                "SIP_PURCHASE" -> { val sip=state.sips.firstOrNull{it.id==kind.substringAfter(':')}; item { Text("Planned ${sip?.let{Money(it.amountMinor).format()} ?: "amount unavailable"} for ${sip?.let{s->state.instruments.firstOrNull{it.id==s.instrumentId}?.name} ?: "instrument"}",color=MaterialTheme.colorScheme.onSurfaceVariant) }; item { DecimalField(a,{a=it},"Actual units received") }; item { MoneyField(b,{b=it},"Actual price per unit") }; item { MoneyField(c,{c=it},"Fees (optional)") }; item { TextField(d,{d=it},"Trade date",today) } }
                "VALUATION" -> {
                    item { LabeledChoiceRow(state.instruments.map { it.id to it.name }, c) { c=it } }
                    item { MoneyField(a,{a=it},"Price per unit") }; item { TextField(b,{b=it},"Price date",today) }; item { TextField(d,{d=it},"Source / assumption","Manual user entry") }
                }
                "DEPOSIT" -> {
                    item { TextField(a,{a=it},"Deposit name") }; item { ChoiceRow(listOf("FD","RD"), b.ifBlank { "FD" }) { b=it } }
                    item { MoneyField(c,{c=it},"Principal") }; item { DecimalField(d,{d=it},"Annual interest %") }; item { TextField(e,{e=it},"Opened date",today) }; item { TextField(f,{f=it},"Maturity date",LocalDate.now().plusYears(1).toString()) }; item { MoneyField(g,{g=it},"Expected maturity amount (optional)") }; item { TextField(h,{h=it},"Lock-in end date (optional)") }; item { TextField(i,{i=it},"Withdrawal restrictions / penalty (optional)") }
                }
                "RD_CONTRIBUTION" -> { item { LabeledChoiceRow(state.deposits.filter{it.type=="RD"}.map{it.id to it.name},a){a=it} }; item { MoneyField(b,{b=it},"Contribution") }; item { TextField(c,{c=it},"Contribution date",today) } }
                "RETIREMENT" -> {
                    item { ChoiceRow(listOf("EPF","EPS","PPF","NPS"), a.ifBlank { "EPF" }) { a=it } }; item { MoneyField(b,{b=it},"Employee / personal contribution") }; item { MoneyField(c,{c=it},"Employer contribution") }; item { MoneyField(d,{d=it},"EPS pension contribution") }; item { TextField(e,{e=it},"Contribution date",today) }; item { TextField(f,{f=it},"Note (optional)") }
                }
                "RETIREMENT_EVENT" -> { item { ChoiceRow(listOf("EPF","EPS","PPF","NPS"),a.ifBlank{"EPF"}){a=it} }; item { ChoiceRow(listOf("EMPLOYER_CHANGE","TRANSFER","WITHDRAWAL","PENSION_ENTITLEMENT","NOTE"),b.ifBlank{"NOTE"}){b=it} }; item { TextField(c,{c=it},"Employer (optional)") }; item { MoneyField(d,{d=it},"Amount (optional)") }; item { TextField(e,{e=it},"Event date",today) }; item { TextField(f,{f=it},"Note (optional)") } }
                "ASSET" -> {
                    item { TextField(a,{a=it},"Asset name") }; item { ChoiceRow(listOf("REAL_ESTATE","VEHICLE","GOLD","SILVER","OTHER"), b.ifBlank { "REAL_ESTATE" }) { b=it } }; item { MoneyField(c,{c=it},"Purchase cost") }; item { TextField(d,{d=it},"Purchase date",today) }; item { DecimalField(e,{e=it},"Ownership %","100") }; item { MoneyField(f,{f=it},"Estimated market value") }; item { MoneyField(g,{g=it},"Estimated realizable resale (optional)") }; if(b=="GOLD"||b=="SILVER"){item{DecimalField(h,{h=it},"Weight in grams")};item{DecimalField(i,{i=it},"Purity %")}}; item { MoneyField(j,{j=it},"Purchase transaction costs (optional)") }; item { Text("Associated debt (optional)",fontWeight=FontWeight.SemiBold);LabeledChoiceRow(listOf("" to "None")+state.loans.map{it.id to it.name},k){k=it} }
                }
                "ASSET_MAINTENANCE" -> { item { LabeledChoiceRow(state.assets.map{it.id to it.name},a){a=it} }; item { TextField(b,{b=it},"Maintenance description") }; item { MoneyField(c,{c=it},"Cost") }; item { TextField(d,{d=it},"Date",today) } }
                "ASSET_VALUATION" -> { item { LabeledChoiceRow(state.assets.map{it.id to it.name},a){a=it} }; item { MoneyField(b,{b=it},"Estimated market value") }; item { MoneyField(c,{c=it},"Estimated realizable resale (optional)") }; item { TextField(d,{d=it},"Valuation date",today) }; item { TextField(e,{e=it},"Source","Manual user estimate") }; item { TextField(f,{f=it},"Assumptions (optional)") } }
                "RECURRING" -> {
                    item { ChoiceRow(listOf("EXPENSE","INCOME"), a.ifBlank { "EXPENSE" }) { a=it } }; item { MoneyField(b,{b=it},"Amount") }; item { TextField(c,{c=it},"Category ID",if(a=="INCOME") "salary" else "utilities") }; item { TextField(d,{d=it},"Merchant / source") }; item { ChoiceRow(listOf("DAILY","WEEKLY","MONTHLY","YEARLY"), e.ifBlank { "MONTHLY" }) { e=it } }; item { DecimalField(f,{f=it},"Every N periods","1") }; item { TextField(g,{g=it},"Next due date",today) }
                }
                "RECONCILE" -> { item { TextField(a,{a=it},"Statement end date",today) }; item { SignedMoneyField(b,{b=it},"Statement closing balance") }; item { Text("Use a negative closing balance for liabilities such as credit cards. A zero difference marks confirmed transactions through this date reconciled.", color=MaterialTheme.colorScheme.onSurfaceVariant) } }
                "CARD_STATEMENT" -> { item { TextField(a,{a=it},"Statement date",today) }; item { TextField(b,{b=it},"Payment due date",LocalDate.now().plusDays(20).toString()) }; item { MoneyField(c,{c=it},"Statement balance") }; item { MoneyField(d,{d=it},"Minimum due") } }
                "CARD_PAYMENT" -> { item { LabeledChoiceRow(state.cardStatements.filter{it.status!="PAID"}.map{it.id to "Due ${LocalDate.ofEpochDay(it.dueEpochDay)} • ${Money(it.statementBalanceMinor-it.paidMinor).format()}"},a){a=it} }; item { MoneyField(b,{b=it},"Payment amount") }; item { Text("Card settlement is an account transfer; it does not create a second expense.", color=MaterialTheme.colorScheme.onSurfaceVariant) } }
                "ARCHIVE_ACCOUNT" -> { item { Text("Archived accounts retain their complete ledger and audit history but are removed from active balances and entry choices.", color=MaterialTheme.colorScheme.onSurfaceVariant) } }
                "RENAME_ACCOUNT" -> { item { TextField(a,{a=it},"New account name") } }
                "RESTORE_ACCOUNT" -> { item { LabeledChoiceRow(state.archivedAccounts.map{it.id to it.name},a){a=it} }; item { Text("Restoring makes the account and its complete retained history active again.",color=MaterialTheme.colorScheme.onSurfaceVariant) } }
                "GOAL_ALLOCATE" -> { item { LabeledChoiceRow(state.goals.map{it.id to it.name},a){a=it} }; item { MoneyField(b,{b=it},"Allocated balance") }; item { Text("This earmarks part of an existing account; it does not create another asset.",color=MaterialTheme.colorScheme.onSurfaceVariant) } }
                "LOAN_PAYMENT" -> { item { LabeledChoiceRow(state.loans.map{it.id to it.name},a){a=it} }; item { MoneyField(b,{b=it},"Principal component") }; item { MoneyField(c,{c=it},"Interest component") }; item { MoneyField(d,{d=it},"Fee (optional)") }; item { TextField(e,{e=it},"Payment date",today) } }
                "LOAN_RATE_CHANGE" -> { item { LabeledChoiceRow(state.loans.filter{it.active}.map{it.id to it.name},a){a=it} }; item { DecimalField(b,{b=it},"New annual interest %") }; item { TextField(c,{c=it},"Effective / next due date",today) }; item { Text("LedgerX rebuilds the remaining amortization schedule from the recorded outstanding principal. Confirm the lender's actual schedule independently.",color=MaterialTheme.colorScheme.onSurfaceVariant) } }
                "SPLIT" -> { item { TextField(a,{a=it},"First category ID","food") }; item { MoneyField(b,{b=it},"First amount") }; item { TextField(c,{c=it},"Second category ID","shopping") }; item { MoneyField(d,{d=it},"Second amount") }; item { TextField(e,{e=it},"Merchant (optional)") }; item { TextField(f,{f=it},"Note (optional)") } }
                "CATEGORY" -> { item { TextField(a,{a=it},"Category name") }; item { ChoiceRow(listOf("EXPENSE","INCOME"),b.ifBlank{"EXPENSE"}){b=it} }; item { Text("Parent category (optional)",fontWeight=FontWeight.SemiBold); LabeledChoiceRow(listOf("" to "None") + state.categories.filter{it.kind==b.ifBlank{"EXPENSE"}&&it.parentCategoryId==null}.map{it.id to it.name},c){c=it} } }
                "TAG" -> { item { TextField(a,{a=it},"Tag name") }; item { Text("Tags remain local and are included in encrypted backups.", color=MaterialTheme.colorScheme.onSurfaceVariant) } }
                else -> {
                    item { TextField(a,{a=it},"Tax year","2026-27") }; item { MoneyField(b,{b=it},"Salary / pension") }; item { MoneyField(c,{c=it},"Interest income") }; item { MoneyField(d,{d=it},"Dividend income") }; item { MoneyField(e,{e=it},"STCG u/s 111A") }; item { MoneyField(f,{f=it},"LTCG u/s 112A") }; item { MoneyField(g,{g=it},"Eligible old-regime deductions") }; item { MoneyField(h,{h=it},"TDS") }; item { MoneyField(i,{i=it},"Advance tax") }; item { ChoiceRow(listOf("NEW","OLD"), j.ifBlank { "NEW" }) { j=it } }; item { Text("Special-rate gains use user-supplied classifications. LedgerX does not infer holding period, STT eligibility, loss set-off, or grandfathering.", color=MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            item {
                Button(onClick = {
                    when(entryKind) {
                        "ACCOUNT" -> vm.createAccount(a,b.ifBlank{"SAVINGS"},c)
                        "BILL_PAYMENT" -> vm.payBill(kind.substringAfter(':'),account,a.ifBlank{"utilities"},b.ifBlank{today})
                        "INCOME_DETAIL" -> vm.recordDetailedIncome(account,a,b,c,d,e.ifBlank{"salary"},f,g,h.ifBlank{today})
                        "POLICY" -> vm.addPolicyFull(a,b.ifBlank{"HEALTH"},c,d,e,f,g,h,i)
                        "INVESTMENT" -> vm.addInvestment(account,a,b,c.ifBlank{"EQUITY"},d,e,f,g.ifBlank{today})
                        "INVESTMENT_BUY" -> vm.buyExistingInvestment(account,a,b,c,d,e.ifBlank{today})
                        "INVESTMENT_SALE" -> vm.sellInvestment(account,a,b,c,d,e.ifBlank{today})
                        "CORPORATE_ACTION" -> vm.addCorporateAction(a,c,d.ifBlank{today},b.ifBlank{"SPLIT"})
                        "DIVIDEND" -> vm.addDividend(account,a,b,c,d.ifBlank{today})
                        "SIP" -> vm.addSip(account,a,b,c.ifBlank{"MONTHLY"},d.ifBlank{"1"},e.ifBlank{today})
                        "SIP_PURCHASE" -> vm.confirmSipPurchase(kind.substringAfter(':'),a,b,c,d.ifBlank{today})
                        "VALUATION" -> vm.addValuation(c,a,b.ifBlank{today},d.ifBlank{"Manual user entry"})
                        "DEPOSIT" -> vm.addDeposit(account,a,b.ifBlank{"FD"},c,d,e.ifBlank{today},f,g,h,i)
                        "RD_CONTRIBUTION" -> vm.addRdContribution(account,a,b,c.ifBlank{today})
                        "RETIREMENT" -> vm.addRetirement(a.ifBlank{"EPF"},b,c,d,e.ifBlank{today},f)
                        "RETIREMENT_EVENT" -> vm.addRetirementEvent(a.ifBlank{"EPF"},b.ifBlank{"NOTE"},c,d,e.ifBlank{today},f)
                        "ASSET" -> vm.addAsset(a,b.ifBlank{"OTHER"},c,d.ifBlank{today},e.ifBlank{"100"},f,g,h,i,j,k)
                        "ASSET_MAINTENANCE" -> vm.addAssetMaintenance(account,a,b,c,d.ifBlank{today})
                        "ASSET_VALUATION" -> vm.addAssetValuation(a,b,c,d.ifBlank{today},e.ifBlank{"Manual user estimate"},f)
                        "RECURRING" -> vm.addRecurring(account,a.ifBlank{"EXPENSE"},b,c.ifBlank{if(a=="INCOME")"salary" else "utilities"},d,e.ifBlank{"MONTHLY"},f.ifBlank{"1"},g.ifBlank{today})
                        "RECONCILE" -> vm.reconcile(account,a.ifBlank{today},b)
                        "CARD_STATEMENT" -> vm.addCardStatement(account,a.ifBlank{today},b,c,d)
                        "CARD_PAYMENT" -> vm.payCardStatement(account,a,b)
                        "ARCHIVE_ACCOUNT" -> vm.archiveAccount(account)
                        "RENAME_ACCOUNT" -> vm.renameAccount(account,a)
                        "RESTORE_ACCOUNT" -> vm.restoreAccount(a)
                        "GOAL_ALLOCATE" -> vm.allocateGoal(a,account,b)
                        "LOAN_PAYMENT" -> vm.payLoan(account,a,b,c,d,e.ifBlank{today})
                        "LOAN_RATE_CHANGE" -> vm.updateLoanRate(a,b,c.ifBlank{today})
                        "SPLIT" -> vm.addSplitExpense(account,a,b,c,d,e,f)
                        "CATEGORY" -> vm.createCategory(a,b.ifBlank{"EXPENSE"},c)
                        "TAG" -> vm.addTransactionTag(kind.substringAfter(':'),a)
                        else -> vm.saveTax(a.ifBlank{"2026-27"},b,c,d,e,f,g,h,i,j.ifBlank{"NEW"})
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) { if(busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth=2.dp) else Text("Save") }
            }
        }
    }
}

private fun entrySheetTitle(kind: String) = when(kind) {
    "ACCOUNT" -> "Add account"; "BILL_PAYMENT" -> "Pay bill"; "INCOME_DETAIL" -> "Record gross and take-home income"; "POLICY" -> "Add insurance policy"
    "INVESTMENT" -> "Add investment purchase"; "INVESTMENT_BUY" -> "Buy existing investment"; "INVESTMENT_SALE" -> "Sell investment"
    "CORPORATE_ACTION" -> "Record split or bonus"; "DIVIDEND" -> "Record dividend"; "SIP" -> "Create SIP schedule"; "SIP_PURCHASE" -> "Confirm SIP purchase"
    "VALUATION" -> "Add dated market price"; "DEPOSIT" -> "Add fixed deposit"; "RD_CONTRIBUTION" -> "Record RD contribution"
    "RETIREMENT" -> "Add retirement contribution"; "RETIREMENT_EVENT" -> "Add retirement event"; "ASSET" -> "Add physical asset"; "ASSET_VALUATION" -> "Add asset valuation"; "ASSET_MAINTENANCE" -> "Record asset maintenance"
    "RECURRING" -> "Add recurring transaction"; "RECONCILE" -> "Reconcile account"; "CARD_STATEMENT" -> "Add credit-card statement"; "CARD_PAYMENT" -> "Pay credit-card statement"
    "ARCHIVE_ACCOUNT" -> "Archive account"; "RENAME_ACCOUNT" -> "Rename account"; "RESTORE_ACCOUNT" -> "Restore archived account"; "GOAL_ALLOCATE" -> "Allocate existing assets to goal"
    "LOAN_PAYMENT" -> "Record loan payment"; "LOAN_RATE_CHANGE" -> "Update loan interest rate"; "SPLIT" -> "Split expense"; "CATEGORY" -> "Create category"; "TAG" -> "Attach transaction tag"
    else -> "Tax profile"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportSheet(fileName: String, csv: String, state: LedgerXUiState, busy: Boolean, onDismiss: () -> Unit, vm: LedgerXViewModel) {
    val headers = remember(csv) { runCatching { CsvParser.parse(csv).headers }.getOrDefault(emptyList()) }
    var account by remember(state.accounts) { mutableStateOf(state.accounts.firstOrNull()?.id.orEmpty()) }
    var date by remember(headers) { mutableStateOf(headers.firstOrNull { it.contains("date",true) }.orEmpty()) }
    var description by remember(headers) { mutableStateOf(headers.firstOrNull { it.contains("description",true)||it.contains("narration",true)||it.contains("merchant",true) }.orEmpty()) }
    var amount by remember(headers) { mutableStateOf(headers.firstOrNull { it.contains("amount",true) }.orEmpty()) }
    var expenseCategory by remember(state.categories) { mutableStateOf(state.categories.firstOrNull{it.kind=="EXPENSE"}?.id.orEmpty()) }
    var incomeCategory by remember(state.categories) { mutableStateOf(state.categories.firstOrNull{it.kind=="INCOME"}?.id.orEmpty()) }
    ModalBottomSheet(onDismissRequest=onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding(), contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item { Text("Import bank statement", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold) }
            item { Text(fileName, color=MaterialTheme.colorScheme.onSurfaceVariant) }; item { AccountChoices(state.accounts,account){account=it} }
            item { Text("Date column",fontWeight=FontWeight.SemiBold); LabeledChoiceRow(headers.map{it to it},date){date=it} }
            item { Text("Description column",fontWeight=FontWeight.SemiBold); LabeledChoiceRow(headers.map{it to it},description){description=it} }
            item { Text("Signed amount column",fontWeight=FontWeight.SemiBold); LabeledChoiceRow(headers.map{it to it},amount){amount=it}; Text("Use negative amounts for expenses and positive amounts for income. Dates may be YYYY-MM-DD or Excel date serials.",style=MaterialTheme.typography.bodySmall) }
            item { Text("Default expense category",fontWeight=FontWeight.SemiBold); LabeledChoiceRow(state.categories.filter{it.kind=="EXPENSE"}.map{it.id to it.name},expenseCategory){expenseCategory=it} }
            item { Text("Default income category",fontWeight=FontWeight.SemiBold); LabeledChoiceRow(state.categories.filter{it.kind=="INCOME"}.map{it.id to it.name},incomeCategory){incomeCategory=it} }
            item { OutlinedButton(onClick={vm.previewImport(csv,date,description,amount)},enabled=headers.isNotEmpty()&&date.isNotBlank()&&description.isNotBlank()&&amount.isNotBlank()){Text("Validate preview")} }
            state.importPreview?.let { preview ->
                item { EmptyCard(if(preview.isValid) "${preview.rows.size} rows ready" else "Validation needs attention", "${preview.rows.count{it.error==null}} valid • ${preview.rows.count{it.error!=null}} invalid") }
                items(preview.rows.take(10),key={it.rowNumber}) { row -> EmptyCard("Row ${row.rowNumber} • ${row.date ?: "Invalid date"}", row.error ?: "${row.description} • ${Money(kotlin.math.abs(row.signedAmountMinor ?: 0)).format()}") }
                item { Button(onClick={vm.commitImport(account,fileName,csv,date,description,amount,expenseCategory,incomeCategory)},enabled=preview.isValid&&!busy&&account.isNotBlank()&&expenseCategory.isNotBlank()&&incomeCategory.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Import ${preview.rows.size} rows") } }
            }
        }
    }
}

@Composable private fun TextField(value:String,onChange:(String)->Unit,label:String,default:String="") { if(value.isEmpty()&&default.isNotEmpty()) LaunchedEffect(default){onChange(default)}; OutlinedTextField(value,onChange,Modifier.fillMaxWidth(),label={Text(label)},singleLine=true) }
@Composable private fun DecimalField(value:String,onChange:(String)->Unit,label:String,default:String="",modifier:Modifier=Modifier.fillMaxWidth()) { if(value.isEmpty()&&default.isNotEmpty()) LaunchedEffect(default){onChange(default)}; OutlinedTextField(value,{onChange(it.filter{ch->ch.isDigit()||ch=='.'})},modifier,label={Text(label)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true) }
@Composable private fun AccountChoices(accounts:List<AccountBalance>,selected:String,onSelect:(String)->Unit) { Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text("Account",fontWeight=FontWeight.SemiBold); LabeledChoiceRow(accounts.map{it.id to it.name},selected,onSelect)} }
@Composable private fun ChoiceRow(values:List<String>,selected:String,onSelect:(String)->Unit)=LabeledChoiceRow(values.map{it to it.replace('_',' ').lowercase().replaceFirstChar(Char::uppercase)},selected,onSelect)
@Composable private fun LabeledChoiceRow(values:List<Pair<String,String>>,selected:String,onSelect:(String)->Unit) { Column(verticalArrangement=Arrangement.spacedBy(6.dp)){values.chunked(3).forEach{chunk->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){chunk.forEach{(id,label)->FilterChip(selected==id,{onSelect(id)},{Text(label,maxLines=1,overflow=TextOverflow.Ellipsis)})}}}} }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTransactionSheet(
    row: TransactionRow,
    categories: List<com.ledgerx.app.core.database.CategoryEntity>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit,
) {
    var amount by remember(row.id) { mutableStateOf(java.math.BigDecimal.valueOf(row.amountMinor, 2).stripTrailingZeros().toPlainString()) }
    var category by remember(row.id) { mutableStateOf(row.categoryId.orEmpty()) }
    var merchant by remember(row.id) { mutableStateOf(row.merchant.orEmpty()) }
    var note by remember(row.id) { mutableStateOf(row.note.orEmpty()) }
    val kind = if (row.type == "INCOME") "INCOME" else "EXPENSE"
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start=20.dp,end=20.dp,bottom=28.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Edit ${row.type.lowercase()}", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold)
            Text("The original entry is retained in the audit trail and replaced by balanced reversal and correction postings.", color=MaterialTheme.colorScheme.onSurfaceVariant)
            MoneyField(amount,{amount=it},"Amount")
            Text("Category", fontWeight=FontWeight.SemiBold)
            LabeledChoiceRow(categories.filter{it.kind==kind}.map{it.id to it.name},category){category=it}
            OutlinedTextField(merchant,{merchant=it},Modifier.fillMaxWidth(),label={Text("Merchant / source")},singleLine=true)
            OutlinedTextField(note,{note=it},Modifier.fillMaxWidth(),label={Text("Note")},maxLines=2)
            Button(onClick={onSave(row.id,amount,category,merchant,note)},enabled=!busy&&amount.isNotBlank()&&category.isNotBlank(),modifier=Modifier.fillMaxWidth().height(52.dp)){if(busy)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)else Text("Save correction")}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteDataSheet(busy: Boolean, onDismiss: () -> Unit, onDelete: (String) -> Unit) {
    var confirmation by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest=onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start=20.dp,end=20.dp,bottom=28.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Permanently delete LedgerX data", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold, color=MaterialTheme.colorScheme.error)
            Text("This removes the encrypted database, receipt attachments, settings, wrapped database secret, and Android Keystore key from this device. It cannot be undone. Create and verify an encrypted backup first if you may need these records.")
            Text("Type DELETE LEDGERX to continue.", fontWeight=FontWeight.SemiBold)
            OutlinedTextField(confirmation,{confirmation=it},Modifier.fillMaxWidth(),label={Text("Confirmation")},singleLine=true)
            Button(onClick={onDelete(confirmation)},enabled=!busy&&confirmation=="DELETE LEDGERX",modifier=Modifier.fillMaxWidth().height(52.dp)) { if(busy) CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp) else Text("Delete permanently") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickEntrySheet(
    accounts: List<AccountBalance>,
    availableCategories: List<com.ledgerx.app.core.database.CategoryEntity>,
    merchantSuggestions: List<String>,
    preferredAccountId: String?,
    preferredCategoryId: String?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String) -> Unit,
    onTransfer: (String, String, String, String) -> Unit,
) {
    var type by remember { mutableStateOf("EXPENSE") }
    var accountId by remember(accounts, preferredAccountId) { mutableStateOf(accounts.firstOrNull { it.id == preferredAccountId }?.id ?: accounts.firstOrNull()?.id.orEmpty()) }
    var destinationAccountId by remember(accounts) { mutableStateOf(accounts.getOrNull(1)?.id.orEmpty()) }
    var amount by remember { mutableStateOf("") }
    var category by remember(availableCategories, preferredCategoryId) { mutableStateOf(availableCategories.firstOrNull { it.id == preferredCategoryId && it.kind == "EXPENSE" }?.id ?: "food") }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    val amountFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { amountFocus.requestFocus() }
    val categories = availableCategories.filter { it.kind == if (type == "INCOME") "INCOME" else "EXPENSE" }.map { it.id to it.name }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            Modifier.fillMaxWidth().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Text("Quick entry", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "EXPENSE", onClick = { type = "EXPENSE"; category = "food" }, label = { Text("Expense") })
                    FilterChip(selected = type == "INCOME", onClick = { type = "INCOME"; category = "salary" }, label = { Text("Income") })
                    FilterChip(selected = type == "TRANSFER", onClick = { type = "TRANSFER" }, label = { Text("Transfer") })
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "REFUND", onClick = { type = "REFUND"; category = "shopping" }, label = { Text("Refund") })
                    FilterChip(selected = type == "REIMBURSEMENT", onClick = { type = "REIMBURSEMENT"; category = "health" }, label = { Text("Reimbursement") })
                  }
                }
            }
            item {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' } },
                    modifier = Modifier.fillMaxWidth().focusRequester(amountFocus),
                    label = { Text("Amount") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
            item {
                Text(if (type == "TRANSFER") "From account" else "Account", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.take(3).forEach { account ->
                        FilterChip(selected = accountId == account.id, onClick = { accountId = account.id }, label = { Text(account.name) })
                    }
                }
            }
            if (type == "TRANSFER") item {
                Text("To account", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.filter { it.id != accountId }.take(3).forEach { account ->
                        FilterChip(selected = destinationAccountId == account.id, onClick = { destinationAccountId = account.id }, label = { Text(account.name) })
                    }
                }
            }
            if (type != "TRANSFER") item {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                LabeledChoiceRow(categories, category) { category = it }
            }
            if (type != "TRANSFER") item { OutlinedTextField(merchant, { merchant = it }, Modifier.fillMaxWidth(), label = { Text(if (type in setOf("EXPENSE", "REFUND", "REIMBURSEMENT")) "Merchant (optional)" else "Source (optional)") }, singleLine = true); if(merchantSuggestions.isNotEmpty()) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){merchantSuggestions.take(3).forEach{suggestion->FilterChip(merchant.equals(suggestion,true),{merchant=suggestion},{Text(suggestion,maxLines=1,overflow=TextOverflow.Ellipsis)})}} }
            if (type != "TRANSFER") item { OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Date (YYYY-MM-DD)") }, singleLine = true) }
            item { OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Note (optional)") }, maxLines = 2) }
            item {
                Button(
                    onClick = {
                        if (type == "TRANSFER") onTransfer(accountId, destinationAccountId, amount, note)
                        else onSave(type, accountId, amount, category, merchant, note, date)
                    },
                    enabled = !busy && amount.isNotBlank() && accountId.isNotBlank() && (type != "TRANSFER" || (destinationAccountId.isNotBlank() && destinationAccountId != accountId)),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Save ${type.lowercase()}")
                }
            }
        }
    }
}
