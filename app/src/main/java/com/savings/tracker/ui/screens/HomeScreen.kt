package com.savings.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.savings.tracker.ui.theme.AppColors
import com.savings.tracker.ui.viewmodel.SavingsViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SavingsViewModel,
    onAccountClick: (Int) -> Unit,
    onTotalSavingsClick: () -> Unit
) {
    val accounts     by viewModel.accounts.collectAsState()
    val totalSavings by viewModel.totalSavings.collectAsState()
    val allEntries   by viewModel.allEntries.collectAsState()
    val moneyFmt     = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    var showAddAccountDialog  by remember { mutableStateOf(false) }
    var showAddDepositSheet   by remember { mutableStateOf(false) }
    var accountToDelete       by remember { mutableStateOf<BankAccount?>(null) }

    // Weekly streak: count consecutive weeks that had at least one deposit
    val streak = remember(allEntries) {
        if (allEntries.isEmpty()) return@remember 0
        val msPerWeek = 7L * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        var count = 0
        var weekStart = now - msPerWeek
        var weekEnd   = now
        while (true) {
            val ws = weekStart; val we = weekEnd
            val hasDeposit = allEntries.any { it.createdAt in ws..we }
            if (!hasDeposit) break
            count++
            weekEnd   = weekStart
            weekStart -= msPerWeek
            if (count > 52) break
        }
        count
    }

    // This week deposits total
    val msPerWeek       = 7L * 24 * 60 * 60 * 1000
    val weekStart       = System.currentTimeMillis() - msPerWeek
    val thisWeekTotal   = remember(allEntries) {
        allEntries.filter { it.createdAt >= weekStart }.sumOf { it.amount }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SystemBg)
    ) {
        // ── Gradient header ──────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(AppColors.HeaderStart, AppColors.HeaderEnd)
                        )
                    )
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp)
            ) {
                Column {
                    // Top row: title + avatar
                    Row(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment   = Alignment.CenterVertically
                    ) {
                        Text(
                            "Savings",
                            fontSize   = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                        Box(
                            modifier          = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(50))
                                .background(AppColors.Primary),
                            contentAlignment  = Alignment.Center
                        ) {
                            Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Total savings hero card — tappable → Summary screen
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onTotalSavingsClick),
                        shape  = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "TOTAL SAVINGS",
                                fontSize      = 10.sp,
                                fontWeight    = FontWeight.Medium,
                                color         = Color.White.copy(alpha = 0.72f),
                                letterSpacing = 0.6.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                moneyFmt.format(totalSavings),
                                fontSize   = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White,
                                letterSpacing = (-1).sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatPill(
                                    label = "This week",
                                    value = if (thisWeekTotal > 0) moneyFmt.format(thisWeekTotal) else "–",
                                    checkmark = thisWeekTotal >= 1000.0
                                )
                                StatPill(
                                    label = "Streak",
                                    value = if (streak > 0) "$streak week${if (streak != 1) "s" else ""}" else "–"
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Accounts section ─────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
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
                Box(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment  = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint     = AppColors.LabelSecondary
                        )
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
                    accounts.forEachIndexed { index, account ->
                        AccountRow(
                            account      = account,
                            totalSavings = totalSavings,
                            moneyFmt     = moneyFmt,
                            onClick      = { onAccountClick(account.id) },
                            onDelete     = { accountToDelete = account }
                        )
                        if (index < accounts.lastIndex) {
                            HorizontalDivider(
                                modifier  = Modifier.padding(start = 56.dp),
                                color     = AppColors.Separator,
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }

        // ── Add Deposit CTA ──────────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick  = { showAddDepositSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                enabled = accounts.isNotEmpty()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Add Deposit", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name ->
                viewModel.addAccount(name)
                showAddAccountDialog = false
            }
        )
    }

    accountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete Account") },
            text  = { Text("Delete \"${account.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAccount(account)
                    accountToDelete = null
                }) { Text("Delete", color = AppColors.Destructive) }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddDepositSheet && accounts.isNotEmpty()) {
        AddDepositSheet(
            accounts = accounts,
            moneyFmt = moneyFmt,
            onDismiss = { showAddDepositSheet = false },
            onConfirm = { accountId, amount, note ->
                viewModel.addDeposit(accountId, amount, note)
                showAddDepositSheet = false
            }
        )
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────

@Composable
private fun StatPill(label: String, value: String, checkmark: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Column {
            Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.65f), fontWeight = FontWeight.Normal)
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
    totalSavings: Double,
    moneyFmt: NumberFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val color    = AppColors.forAccount(account.id)
    val progress = if (totalSavings > 0) (account.balance / totalSavings).toFloat().coerceIn(0f, 1f) else 0f

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Color badge with initial
        Box(
            modifier         = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                account.name.first().uppercaseChar().toString(),
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(account.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                Text(moneyFmt.format(account.balance), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.LabelPrimary)
            }
            Spacer(Modifier.height(5.dp))
            // Progress bar showing this account's share of total savings
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppColors.Separator)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppColors.Destructive, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AddAccountDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Account") },
        text  = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Account name") },
                placeholder = { Text("e.g. Chase Savings") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDepositSheet(
    accounts: List<BankAccount>,
    moneyFmt: NumberFormat,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Int, amount: Double, note: String) -> Unit
) {
    var selectedAccountId by remember { mutableStateOf(accounts.first().id) }
    var amountText        by remember { mutableStateOf("1000") }
    var note              by remember { mutableStateOf("") }
    val amount            = amountText.toDoubleOrNull()
    val isValid           = amount != null && amount > 0

    val quickAmounts = listOf(500.0, 1000.0, 1500.0, 2000.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AppColors.SystemBg,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "New Deposit",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = AppColors.LabelPrimary
            )
            Spacer(Modifier.height(20.dp))

            // Amount display card
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(14.dp),
                colors    = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Amount", fontSize = 11.sp, color = AppColors.LabelSecondary, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value             = amountText,
                        onValueChange     = { amountText = it },
                        keyboardOptions   = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine        = true,
                        isError           = !isValid && amountText.isNotEmpty(),
                        prefix            = { Text("$", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                        textStyle         = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                        colors            = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AppColors.Primary,
                            unfocusedBorderColor = AppColors.Separator
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    // Quick amount chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        quickAmounts.forEach { qa ->
                            val selected = amountText == qa.toInt().toString()
                            FilterChip(
                                selected = selected,
                                onClick  = { amountText = qa.toInt().toString() },
                                label    = { Text("$${qa.toInt()}", fontSize = 12.sp) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppColors.Primary,
                                    selectedLabelColor     = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Account selector
            Text("To account", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.LabelPrimary, modifier = Modifier.padding(bottom = 8.dp))
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(14.dp),
                colors    = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                accounts.forEachIndexed { index, account ->
                    val color    = AppColors.forAccount(account.id)
                    val selected = selectedAccountId == account.id
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAccountId = account.id }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier         = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(account.name.first().uppercaseChar().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text(account.name, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                        Text(moneyFmt.format(account.balance), fontSize = 13.sp, color = AppColors.LabelSecondary)
                        if (selected) {
                            Box(
                                modifier         = Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(AppColors.Primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        } else {
                            Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(AppColors.Separator))
                        }
                    }
                    if (index < accounts.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(start = 54.dp), color = AppColors.Separator, thickness = 0.5.dp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Note field
            OutlinedTextField(
                value             = note,
                onValueChange     = { note = it },
                modifier          = Modifier.fillMaxWidth(),
                label             = { Text("Note (optional)") },
                placeholder       = { Text("Weekly transfer") },
                singleLine        = true,
                shape             = RoundedCornerShape(12.dp),
                colors            = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = AppColors.Primary,
                    unfocusedBorderColor = AppColors.Separator,
                    unfocusedContainerColor = AppColors.CardBg,
                    focusedContainerColor   = AppColors.CardBg
                )
            )

            Spacer(Modifier.height(16.dp))

            // CTA
            Button(
                onClick  = { if (isValid) onConfirm(selectedAccountId, amount!!, note.trim()) },
                enabled  = isValid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) {
                Text("Add Deposit", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}
