package com.savings.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savings.tracker.data.BankAccount
import com.savings.tracker.data.Goal
import com.savings.tracker.ui.theme.AppColors
import com.savings.tracker.ui.viewmodel.SavingsViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SavingsViewModel,
    onAccountClick: (Int) -> Unit,
    onTotalSavingsClick: () -> Unit,
    onSettingsClick: () -> Unit            // Fix #1: Settings via gear icon not tab
) {
    val accounts        by viewModel.accounts.collectAsState()
    val allGoals        by viewModel.allGoals.collectAsState()
    val allEntries      by viewModel.allEntries.collectAsState()
    val displayCurrency by viewModel.displayCurrency.collectAsState()
    val exchangeRate    by viewModel.exchangeRate.collectAsState()
    val rateIsLive      by viewModel.rateIsLive.collectAsState()
    val totalInr        by viewModel.totalSavingsInr.collectAsState()
    val totalUsd        by viewModel.totalSavingsUsd.collectAsState()

    val displayTotal    = if (displayCurrency == "INR") totalInr else totalUsd
    val displaySymbol   = if (displayCurrency == "INR") "₹" else "$"
    val moneyFmt        = remember(displayCurrency) {
        if (displayCurrency == "INR") NumberFormat.getInstance(Locale("en", "IN"))
        else NumberFormat.getCurrencyInstance(Locale.US)
    }

    // Fix #20/#30: weekStart inside a derived calculation, not stale closure
    val msPerWeek    = 7L * 24 * 60 * 60 * 1000
    // Fix #4/#20: streak and thisWeek use depositDate, weekStart recomputed in remember
    val streak = remember(allEntries) {
        if (allEntries.isEmpty()) return@remember 0
        val now = System.currentTimeMillis()
        val week = 7L * 24 * 60 * 60 * 1000
        var count = 0; var end = now; var start = now - week
        while (count < 52) {
            val s = start; val e = end
            if (!allEntries.any { it.depositDate in s..e }) break
            count++; end = start; start -= week
        }
        count
    }
    val thisWeekTotal = remember(allEntries) {
        val now = System.currentTimeMillis()
        val start = now - 7L * 24 * 60 * 60 * 1000
        // Fix #4: use depositDate
        allEntries.filter { it.depositDate >= start }.sumOf {
            com.savings.tracker.data.ExchangeRateService.convert(it.amount, it.currency, displayCurrency, exchangeRate)
        }
    }

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddDepositSheet  by remember { mutableStateOf(false) }
    var accountToDelete      by remember { mutableStateOf<BankAccount?>(null) }
    var duplicateNameError   by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppColors.SystemBg)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(AppColors.HeaderStart, AppColors.HeaderEnd)))
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Savings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.5).sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Fix #3: Currency toggle
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable { viewModel.toggleDisplayCurrency() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    if (displayCurrency == "INR") "₹ INR" else "$ USD",
                                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                                )
                            }
                            // Fix #1: Gear icon for Settings
                            IconButton(onClick = onSettingsClick, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    // Exchange rate line
                    Text(
                        if (rateIsLive) "1 USD = ₹${String.format("%.2f", exchangeRate)} · Live"
                        else "1 USD = ₹${String.format("%.2f", exchangeRate)} · Cached",
                        fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onTotalSavingsClick),
                        shape    = RoundedCornerShape(18.dp),
                        colors   = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("TOTAL SAVINGS", fontSize = 10.sp, fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.72f), letterSpacing = 0.6.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "$displaySymbol${String.format("%,.2f", displayTotal)}",
                                fontSize = 36.sp, fontWeight = FontWeight.Bold,
                                color = Color.White, letterSpacing = (-1).sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatPill(
                                    label = "This week",
                                    value = if (thisWeekTotal > 0) "$displaySymbol${String.format("%,.0f", thisWeekTotal)}" else "–",
                                    // Fix #24/#31: checkmark when any deposit made this week
                                    checkmark = thisWeekTotal > 0
                                )
                                StatPill(
                                    label = "Streak",
                                    value = if (streak > 0) "$streak wk${if (streak != 1) "s" else ""}" else "–"
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Accounts", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.LabelPrimary)
                TextButton(onClick = { showAddAccountDialog = true }) {
                    Text("Add new", color = AppColors.Primary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }
        }

        if (accounts.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(56.dp), tint = AppColors.LabelSecondary)
                        Spacer(Modifier.height(12.dp))
                        Text("No accounts yet", color = AppColors.LabelSecondary, fontWeight = FontWeight.Medium)
                        Text("Tap 'Add new' to get started", color = AppColors.LabelSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier  = Modifier.padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(14.dp),
                    colors    = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    // Fix #38: Use index loop inside single Card to keep visual grouping
                    // but with individual AccountRow composables for clarity
                    accounts.forEachIndexed { index, account ->
                        val accountBalanceInDisplay = com.savings.tracker.data.ExchangeRateService
                            .convert(account.balance, account.currency, displayCurrency, exchangeRate)
                        val totalInDisplay = if (displayCurrency == "INR") totalInr else totalUsd
                        // Fix #15/#17: Progress uses currency-converted values
                        val progress = if (totalInDisplay > 0)
                            (accountBalanceInDisplay / totalInDisplay).toFloat().coerceIn(0f, 1f) else 0f

                        AccountRow(
                            account   = account,
                            balance   = accountBalanceInDisplay,
                            symbol    = displaySymbol,
                            progress  = progress,
                            onClick   = { onAccountClick(account.id) },
                            onDelete  = { accountToDelete = account }
                        )
                        if (index < accounts.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = AppColors.Separator, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick  = { showAddDepositSheet = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                enabled  = accounts.isNotEmpty()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Add Deposit", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss    = { showAddAccountDialog = false; duplicateNameError = false },
            onConfirm    = { name, currency ->
                viewModel.addAccount(name, currency, onDuplicate = { duplicateNameError = true })
                if (!duplicateNameError) showAddAccountDialog = false
            },
            showDuplicateError = duplicateNameError
        )
    }

    accountToDelete?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete Account") },
            text  = { Text("Delete \"${acc.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAccount(acc); accountToDelete = null }) {
                    Text("Delete", color = AppColors.Destructive)
                }
            },
            dismissButton = { TextButton(onClick = { accountToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showAddDepositSheet && accounts.isNotEmpty()) {
        AddDepositSheet(
            accounts      = accounts,
            goals         = allGoals,
            displaySymbol = displaySymbol,
            displayCurrency = displayCurrency,
            onDismiss     = { showAddDepositSheet = false },
            onConfirm     = { accountId, amount, note, goalId, depositDate ->
                viewModel.addDeposit(accountId, amount, note, goalId, depositDate)
                showAddDepositSheet = false
            }
        )
    }
}

// ── Sub-composables ─────────────────────────────────────────────────────────

@Composable
private fun StatPill(label: String, value: String, checkmark: Boolean = false) {
    Box(modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Column {
            Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.65f))
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (checkmark) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppColors.Success, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: BankAccount,
    balance: Double,
    symbol: String,
    progress: Float,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val color = AppColors.forAccount(account.id)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(color), contentAlignment = Alignment.Center) {
            Text(account.name.first().uppercaseChar().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(account.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                    // Currency tag
                    Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                        Text(account.currency, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = color)
                    }
                }
                Text("$symbol${String.format("%,.2f", balance)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.LabelPrimary)
            }
            Spacer(Modifier.height(5.dp))
            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(AppColors.Separator)) {
                Box(Modifier.fillMaxWidth(progress).height(3.dp).clip(RoundedCornerShape(2.dp)).background(color))
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppColors.Destructive, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    showDuplicateError: Boolean
) {
    var name     by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Account name") },
                    placeholder = { Text("e.g. HDFC Savings") },
                    isError = showDuplicateError,
                    supportingText = if (showDuplicateError) { { Text("An account with this name already exists") } } else null,
                    singleLine = true
                )
                // Fix #7: Currency picker in add account dialog
                Text("Currency", fontSize = 13.sp, color = AppColors.LabelSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("INR", "USD").forEach { c ->
                        FilterChip(
                            selected = currency == c,
                            onClick  = { currency = c },
                            label    = { Text(if (c == "INR") "₹ INR" else "$ USD") },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.Primary,
                                selectedLabelColor     = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim(), currency) }, enabled = name.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDepositSheet(
    accounts: List<BankAccount>,
    goals: List<Goal>,
    displaySymbol: String,
    displayCurrency: String,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Int, amount: Double, note: String, goalId: Int?, depositDate: Long) -> Unit
) {
    // Fix #9 (Option A): Goal-first flow
    var selectedGoalId    by remember { mutableStateOf<Int?>(null) }
    var selectedAccountId by remember { mutableStateOf(accounts.first().id) }
    var amountText        by remember { mutableStateOf("") }
    var note              by remember { mutableStateOf("") }
    var depositDate       by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker    by remember { mutableStateOf(false) }

    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val accountSymbol   = if (selectedAccount?.currency == "INR") "₹" else "$"
    // Fix #23: Quick amounts adapt to selected account currency
    val quickAmounts    = if (selectedAccount?.currency == "INR")
        listOf(5000.0, 10000.0, 25000.0, 50000.0)
    else
        listOf(500.0, 1000.0, 1500.0, 2000.0)

    val amount  = amountText.toDoubleOrNull()
    val isValid = amount != null && amount > 0

    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AppColors.SystemBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("New Deposit", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.LabelPrimary)
            Spacer(Modifier.height(16.dp))

            // Fix #23: Amount card shows correct currency symbol
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Amount", fontSize = 11.sp, color = AppColors.LabelSecondary, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = amountText, onValueChange = { amountText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError    = !isValid && amountText.isNotEmpty(),
                        prefix     = { Text(accountSymbol, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                        textStyle  = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                        colors     = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Separator)
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickAmounts.forEach { qa ->
                            FilterChip(
                                selected = amountText == qa.toInt().toString(),
                                onClick  = { amountText = qa.toInt().toString() },
                                label    = { Text("$accountSymbol${qa.toInt()}", fontSize = 11.sp) },
                                colors   = FilterChipDefaults.filterChipColors(selectedContainerColor = AppColors.Primary, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Fix #29: Deposit date picker — date only, no time
            Card(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg), elevation = CardDefaults.cardElevation(0.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Deposit date", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                        Text("Tap to change · defaults to today", fontSize = 11.sp, color = AppColors.LabelSecondary)
                    }
                    Text(dateFmt.format(Date(depositDate)), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Primary)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Fix #23 Option A: Goal picker first
            if (goals.isNotEmpty()) {
                Text("Tag to goal (optional)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.LabelPrimary, modifier = Modifier.padding(bottom = 8.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.CardBg), elevation = CardDefaults.cardElevation(0.dp)) {
                    // No goal option
                    Row(modifier = Modifier.fillMaxWidth().clickable { selectedGoalId = null }.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(AppColors.Separator), contentAlignment = Alignment.Center) {
                            Text("–", fontSize = 14.sp, color = AppColors.LabelSecondary)
                        }
                        Text("No goal", modifier = Modifier.weight(1f), fontSize = 14.sp, color = AppColors.LabelSecondary)
                        if (selectedGoalId == null) {
                            Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(AppColors.Primary), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        } else Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(AppColors.Separator))
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 54.dp), color = AppColors.Separator, thickness = 0.5.dp)
                    goals.forEachIndexed { idx, goal ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { selectedGoalId = goal.id }.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(AppColors.Warning.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Text(goal.emoji, fontSize = 14.sp)
                            }
                            Text(goal.name, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                            if (selectedGoalId == goal.id) {
                                Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(AppColors.Primary), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            } else Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(AppColors.Separator))
                        }
                        if (idx < goals.lastIndex) HorizontalDivider(modifier = Modifier.padding(start = 54.dp), color = AppColors.Separator, thickness = 0.5.dp)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Account picker
            Text("To account", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.LabelPrimary, modifier = Modifier.padding(bottom = 8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg), elevation = CardDefaults.cardElevation(0.dp)) {
                accounts.forEachIndexed { idx, acc ->
                    val c = AppColors.forAccount(acc.id)
                    val sel = selectedAccountId == acc.id
                    Row(modifier = Modifier.fillMaxWidth().clickable { selectedAccountId = acc.id }.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(c), contentAlignment = Alignment.Center) {
                            Text(acc.name.first().uppercaseChar().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text(acc.name, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                        Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(c.copy(alpha = 0.15f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                            Text(acc.currency, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = c)
                        }
                        Spacer(Modifier.width(4.dp))
                        if (sel) {
                            Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(AppColors.Primary), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        } else Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(AppColors.Separator))
                    }
                    if (idx < accounts.lastIndex) HorizontalDivider(modifier = Modifier.padding(start = 54.dp), color = AppColors.Separator, thickness = 0.5.dp)
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Note (optional)") }, placeholder = { Text("Weekly transfer") }, singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Separator, unfocusedContainerColor = AppColors.CardBg, focusedContainerColor = AppColors.CardBg))

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (isValid) onConfirm(selectedAccountId, amount!!, note.trim(), selectedGoalId, depositDate) },
                enabled  = isValid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) {
                Text("Add new", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = depositDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { depositDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
