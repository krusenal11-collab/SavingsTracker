package com.savings.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.savings.tracker.data.ExchangeRateService
import com.savings.tracker.data.SavingsEntry
import com.savings.tracker.data.TransactionType
import com.savings.tracker.ui.theme.AppColors
import com.savings.tracker.ui.viewmodel.SavingsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: Int,
    viewModel: SavingsViewModel,
    onBack: () -> Unit
) {
    val accounts        by viewModel.accounts.collectAsState()
    val displayCurrency by viewModel.displayCurrency.collectAsState()
    val exchangeRate    by viewModel.exchangeRate.collectAsState()
    val account         = accounts.find { it.id == accountId }
    val color           = if (account != null) AppColors.forAccount(account.id) else AppColors.Primary

    val entriesFlow = remember(accountId) { viewModel.getEntriesForAccount(accountId) }
    val entries     by entriesFlow.collectAsState(emptyList())

    val accountSymbol  = if (account?.currency == "INR") "₹" else "$"
    val displaySymbol  = if (displayCurrency == "INR") "₹" else "$"
    val dateFmt        = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    var showWithdrawSheet  by remember { mutableStateOf(false) }
    var withdrawError      by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize().background(AppColors.SystemBg)) {
        item {
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AppColors.HeaderStart, AppColors.HeaderEnd)))
                .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column {
                            Text(account?.name ?: "Account", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            account?.let { Text("${it.currency} account", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f)) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.15f)).padding(16.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Current Balance", fontSize = 11.sp, color = Color.White.copy(alpha = 0.72f), fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            Text("$accountSymbol${String.format("%,.2f", account?.balance ?: 0.0)}",
                                fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-1).sp)
                            if (account != null && account.currency != displayCurrency) {
                                val converted = ExchangeRateService.convert(account.balance, account.currency, displayCurrency, exchangeRate)
                                Text("≈ $displaySymbol${String.format("%,.2f", converted)}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                            Spacer(Modifier.height(12.dp))
                            // Withdraw button
                            OutlinedButton(
                                onClick = { showWithdrawSheet = true },
                                colors  = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border  = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                                shape   = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Withdraw", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Transaction History", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.LabelPrimary)
                Text("${entries.size} records", fontSize = 13.sp, color = AppColors.LabelSecondary)
            }
        }

        if (entries.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(52.dp), tint = AppColors.LabelSecondary)
                        Spacer(Modifier.height(12.dp))
                        Text("No transactions yet", color = AppColors.LabelSecondary, fontWeight = FontWeight.Medium)
                        Text("Tap Add Deposit on the home screen", color = AppColors.LabelSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
                val isWithdrawal = entry.transactionType == TransactionType.WITHDRAWAL
                Card(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = when {
                        entries.size == 1 -> RoundedCornerShape(14.dp)
                        index == 0 -> RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                        index == entries.lastIndex -> RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
                        else -> RoundedCornerShape(0.dp)
                    },
                    colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                    elevation = CardDefaults.cardElevation(0.dp)) {
                    TransactionRow(
                        entry           = entry,
                        color           = color,
                        isWithdrawal    = isWithdrawal,
                        dateFmt         = dateFmt,
                        displayCurrency = displayCurrency,
                        exchangeRate    = exchangeRate,
                        accountSymbol   = accountSymbol,
                        displaySymbol   = displaySymbol
                    )
                }
                if (index < entries.lastIndex) HorizontalDivider(modifier = Modifier.padding(start = 70.dp), color = AppColors.Separator, thickness = 0.5.dp)
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showWithdrawSheet) {
        WithdrawFromAccountSheet(
            account               = account,
            displayCurrency       = displayCurrency,
            exchangeRate          = exchangeRate,
            errorMessage          = withdrawError,
            onDismiss             = { showWithdrawSheet = false; withdrawError = null },
            onConfirm             = { amount, note, date ->
                viewModel.withdrawFromAccount(
                    accountId      = accountId,
                    amount         = amount,
                    note           = note,
                    withdrawalDate = date
                ) { success ->
                    if (success) {
                        showWithdrawSheet = false
                        withdrawError = null
                    } else {
                        withdrawError = "Insufficient balance in this account"
                    }
                }
            }
        )
    }
}

@Composable
private fun TransactionRow(
    entry: SavingsEntry,
    color: Color,
    isWithdrawal: Boolean,
    dateFmt: SimpleDateFormat,
    displayCurrency: String,
    exchangeRate: Double,
    accountSymbol: String,
    displaySymbol: String
) {
    val entrySymbol   = if (entry.currency == "INR") "₹" else "$"
    val displayAmount = ExchangeRateService.convert(entry.amount, entry.currency, displayCurrency, exchangeRate)
    val amountColor   = if (isWithdrawal) AppColors.Destructive else AppColors.Success
    val prefix        = if (isWithdrawal) "−" else "+"
    val iconBg        = if (isWithdrawal) AppColors.Destructive.copy(alpha = 0.12f) else color.copy(alpha = 0.15f)
    val iconTint      = if (isWithdrawal) AppColors.Destructive else color

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(if (isWithdrawal) Icons.Default.ArrowUpward else Icons.Default.TrendingUp,
                contentDescription = null, tint = iconTint, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("$prefix$entrySymbol${String.format("%,.2f", entry.amount)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = amountColor)
            if (entry.currency != displayCurrency) Text("≈ $displaySymbol${String.format("%,.2f", displayAmount)}", fontSize = 11.sp, color = AppColors.LabelSecondary)
            if (entry.note.isNotBlank()) Text(entry.note, fontSize = 12.sp, color = AppColors.LabelSecondary)
            Text(dateFmt.format(Date(entry.depositDate)), fontSize = 11.sp, color = AppColors.LabelSecondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithdrawFromAccountSheet(
    account: com.savings.tracker.data.BankAccount?,
    displayCurrency: String,
    exchangeRate: Double,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String, date: Long) -> Unit
) {
    var amountText     by remember { mutableStateOf("") }
    var note           by remember { mutableStateOf("") }
    var withdrawalDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amount        = amountText.toDoubleOrNull()
    val accountSymbol = if (account?.currency == "INR") "₹" else "$"
    val maxBalance    = account?.balance ?: 0.0
    val isValid       = amount != null && amount > 0 && amount <= maxBalance
    val dateFmt       = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.SystemBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("Withdraw from ${account?.name ?: "Account"}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.LabelPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Available: $accountSymbol${String.format("%,.2f", maxBalance)}", fontSize = 13.sp, color = AppColors.LabelSecondary)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(value = amountText, onValueChange = { amountText = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Amount to withdraw") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text(accountSymbol) }, singleLine = true,
                isError = errorMessage != null || (amountText.isNotEmpty() && (amount == null || amount > maxBalance)),
                supportingText = when {
                    errorMessage != null -> { { Text(errorMessage, color = AppColors.Destructive) } }
                    amount != null && amount > maxBalance -> { { Text("Cannot exceed balance of $accountSymbol${String.format("%,.2f", maxBalance)}") } }
                    else -> null
                },
                shape = RoundedCornerShape(12.dp))

            Spacer(Modifier.height(10.dp))
            Card(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg), elevation = CardDefaults.cardElevation(0.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Withdrawal date", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                        Text("When did you withdraw?", fontSize = 11.sp, color = AppColors.LabelSecondary)
                    }
                    Text(dateFmt.format(Date(withdrawalDate)), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Destructive)
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Note (optional)") }, placeholder = { Text("Emergency, bank charges...") },
                singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(16.dp))
            Button(onClick = { if (isValid) onConfirm(amount!!, note.trim(), withdrawalDate) },
                enabled = isValid, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Destructive)) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Withdraw", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = withdrawalDate)
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dpState.selectedDateMillis?.let { withdrawalDate = it }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dpState) }
    }
}
