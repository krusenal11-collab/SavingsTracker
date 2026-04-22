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
import com.savings.tracker.data.ExchangeRateService
import com.savings.tracker.data.Goal
import com.savings.tracker.data.TransactionType
import com.savings.tracker.ui.theme.AppColors
import com.savings.tracker.ui.viewmodel.SavingsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: SavingsViewModel) {
    val goals           by viewModel.allGoals.collectAsState()
    val allEntries      by viewModel.allEntries.collectAsState()
    val accounts        by viewModel.accounts.collectAsState()
    val displayCurrency by viewModel.displayCurrency.collectAsState()
    val exchangeRate    by viewModel.exchangeRate.collectAsState()
    val displaySymbol   = if (displayCurrency == "INR") "₹" else "$"

    var showAddGoalSheet     by remember { mutableStateOf(false) }
    var goalToDelete         by remember { mutableStateOf<Goal?>(null) }
    var goalToWithdrawFrom   by remember { mutableStateOf<Goal?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize().background(AppColors.SystemBg)) {
        item {
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AppColors.HeaderStart, AppColors.HeaderEnd)))
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Goals", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.5).sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable { viewModel.toggleDisplayCurrency() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(if (displayCurrency == "INR") "₹ INR" else "$ USD", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(onClick = { showAddGoalSheet = true }) {
                                Text("+ New", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    val totalSavedTowardGoals = remember(allEntries, displayCurrency, exchangeRate) {
                        allEntries.filter { it.goalId != null }.sumOf { entry ->
                            val amt = ExchangeRateService.convert(entry.amount, entry.currency, displayCurrency, exchangeRate)
                            if (entry.transactionType == TransactionType.DEPOSIT) amt else -amt
                        }.coerceAtLeast(0.0)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GoalStatCard("Active goals", goals.size.toString(), Modifier.weight(1f))
                        GoalStatCard("Saved toward goals", "$displaySymbol${String.format("%,.0f", totalSavedTowardGoals)}", Modifier.weight(2f))
                    }
                }
            }
        }

        if (goals.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎯", fontSize = 56.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("No goals yet", color = AppColors.LabelPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text("Tap '+ New' to create your first savings goal", color = AppColors.LabelSecondary, fontSize = 14.sp)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { showAddGoalSheet = true }, shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)) {
                            Text("Create a goal", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            item { Spacer(Modifier.height(16.dp)) }
            items(goals, key = { it.id }) { goal ->
                val savedAmount    = viewModel.savedAmountForGoal(goal.id, allEntries)
                val targetInDisplay = ExchangeRateService.convert(goal.targetAmount, goal.targetCurrency, displayCurrency, exchangeRate)
                val progress        = if (targetInDisplay > 0) (savedAmount / targetInDisplay).toFloat().coerceIn(0f, 1f) else 0f
                val pct             = (progress * 100).toInt()
                val isComplete      = pct >= 100
                val accentColor     = when { isComplete -> AppColors.Success; pct >= 75 -> AppColors.Warning; else -> AppColors.Primary }
                val dateFmt         = remember { SimpleDateFormat("MMM yyyy", Locale.US) }

                Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                    elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(goal.emoji, fontSize = 24.sp)
                                Column {
                                    Text(goal.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.LabelPrimary)
                                    Text("Target by ${dateFmt.format(Date(goal.targetDate))}", fontSize = 11.sp, color = AppColors.LabelSecondary)
                                }
                            }
                            Text("$pct%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        }
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(AppColors.Separator)) {
                            Box(Modifier.fillMaxWidth(progress).height(6.dp).clip(RoundedCornerShape(3.dp)).background(accentColor))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("$displaySymbol${String.format("%,.0f", savedAmount)} saved", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.LabelPrimary)
                            Text("$displaySymbol${String.format("%,.0f", (targetInDisplay - savedAmount).coerceAtLeast(0.0))} to go", fontSize = 13.sp, color = AppColors.LabelSecondary)
                        }
                        if (isComplete) {
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(AppColors.Success.copy(alpha = 0.12f)).padding(8.dp), contentAlignment = Alignment.Center) {
                                Text("🎉 Goal complete!", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Success)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        // Action buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Withdraw from goal
                            OutlinedButton(
                                onClick = { goalToWithdrawFrom = goal },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Warning)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Withdraw", fontSize = 12.sp)
                            }
                            // Delete goal
                            OutlinedButton(
                                onClick = { goalToDelete = goal },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Destructive)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // ── Smart Goal Deletion Dialog ─────────────────────────────────────────
    goalToDelete?.let { goal ->
        val savedAmount = viewModel.savedAmountForGoal(goal.id, allEntries)
        val targetInDisplay = ExchangeRateService.convert(goal.targetAmount, goal.targetCurrency, displayCurrency, exchangeRate)
        val isComplete  = savedAmount >= targetInDisplay
        val displaySymbolLocal = if (displayCurrency == "INR") "₹" else "$"

        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Delete \"${goal.name}\"?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isComplete) {
                        Text("This goal is complete. How would you like to delete it?", color = AppColors.LabelSecondary)
                        Spacer(Modifier.height(4.dp))
                        // Option 1: Withdraw funds
                        Card(modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AppColors.Destructive.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(10.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Withdraw funds from accounts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Destructive)
                                Text("Removes $displaySymbolLocal${String.format("%,.0f", savedAmount)} from your account balances. Use this if you've actually spent or moved this money out.", fontSize = 11.sp, color = AppColors.LabelSecondary, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    } else {
                        val pctComplete = if (targetInDisplay > 0) (savedAmount / targetInDisplay * 100).toInt() else 0
                        Text("This goal is only $pctComplete% complete. Deleting it will unlink all deposits — the money stays in your accounts.", color = AppColors.LabelSecondary)
                    }
                    HorizontalDivider(color = AppColors.Separator, thickness = 0.5.dp)
                    Text(
                        "Choosing 'Remove tracking only' keeps the money in your accounts and preserves deposit history — the goal tag is simply removed.",
                        fontSize = 12.sp, color = AppColors.LabelSecondary
                    )
                }
            },
            confirmButton = {
                if (isComplete) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = {
                            viewModel.deleteGoal(goal, withdrawFunds = true)
                            goalToDelete = null
                        }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Destructive),
                            shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("Withdraw & delete", fontSize = 13.sp)
                        }
                        OutlinedButton(onClick = {
                            viewModel.deleteGoal(goal, withdrawFunds = false)
                            goalToDelete = null
                        }, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("Remove tracking only", fontSize = 13.sp)
                        }
                    }
                } else {
                    TextButton(onClick = {
                        viewModel.deleteGoal(goal, withdrawFunds = false)
                        goalToDelete = null
                    }) { Text("Remove tracking", color = AppColors.Destructive) }
                }
            },
            dismissButton = { TextButton(onClick = { goalToDelete = null }) { Text("Cancel") } }
        )
    }

    // ── Withdraw from Goal Sheet ───────────────────────────────────────────
    goalToWithdrawFrom?.let { goal ->
        var withdrawSheetError by remember(goal.id) { mutableStateOf<String?>(null) }
        WithdrawFromGoalSheet(
            goal            = goal,
            accounts        = accounts,
            displayCurrency = displayCurrency,
            exchangeRate    = exchangeRate,
            savedAmount     = viewModel.savedAmountForGoal(goal.id, allEntries),
            externalError   = withdrawSheetError,
            onDismiss       = { goalToWithdrawFrom = null; withdrawSheetError = null },
            onConfirm       = { accountId, amount, note, date ->
                viewModel.withdrawFromGoal(
                    goalId = goal.id, accountId = accountId,
                    amount = amount, note = note, withdrawalDate = date
                ) { success ->
                    if (success) goalToWithdrawFrom = null
                    else withdrawSheetError = "Insufficient balance in this account"
                }
            }
        )
    }

    if (showAddGoalSheet) {
        AddGoalSheet(
            onDismiss = { showAddGoalSheet = false },
            onConfirm = { name, emoji, amount, currency, date ->
                viewModel.addGoal(name, emoji, amount, currency, date)
                showAddGoalSheet = false
            }
        )
    }
}

// ── Withdraw from Goal Sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithdrawFromGoalSheet(
    goal: Goal,
    accounts: List<com.savings.tracker.data.BankAccount>,
    displayCurrency: String,
    exchangeRate: Double,
    savedAmount: Double,
    externalError: String?,       // Bug 1 fix: error threaded in from parent after coroutine
    onDismiss: () -> Unit,
    onConfirm: (accountId: Int, amount: Double, note: String, date: Long) -> Unit
) {
    var amountText        by remember { mutableStateOf("") }
    var note              by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: -1) }
    var withdrawalDate    by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker    by remember { mutableStateOf(false) }

    val amount  = amountText.toDoubleOrNull()
    val isValid = amount != null && amount > 0 && amount <= savedAmount && selectedAccountId != -1
    val displaySymbol = if (displayCurrency == "INR") "₹" else "$"
    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.SystemBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("Withdraw from ${goal.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.LabelPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Currently saved: $displaySymbol${String.format("%,.2f", savedAmount)}", fontSize = 13.sp, color = AppColors.LabelSecondary)
            Spacer(Modifier.height(16.dp))

            // Amount
            OutlinedTextField(value = amountText, onValueChange = { amountText = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Withdrawal amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text(displaySymbol) },
                isError = externalError != null || errorMessage != null || (amountText.isNotEmpty() && (amount == null || amount > savedAmount)),
                supportingText = when {
                    externalError != null -> { { Text(externalError, color = AppColors.Destructive) } }
                    errorMessage != null -> { { Text(errorMessage!!, color = AppColors.Destructive) } }
                    amount != null && amount > savedAmount -> { { Text("Cannot exceed saved amount of $displaySymbol${String.format("%,.2f", savedAmount)}") } }
                    else -> null
                },
                singleLine = true, shape = RoundedCornerShape(12.dp))

            Spacer(Modifier.height(10.dp))

            // From which account
            Text("From account", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.LabelPrimary, modifier = Modifier.padding(bottom = 8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg), elevation = CardDefaults.cardElevation(0.dp)) {
                accounts.forEachIndexed { idx, acc ->
                    val c = AppColors.forAccount(acc.id)
                    val sel = selectedAccountId == acc.id
                    val accSymbol = if (acc.currency == "INR") "₹" else "$"
                    Row(modifier = Modifier.fillMaxWidth().clickable { selectedAccountId = acc.id }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(c), contentAlignment = Alignment.Center) {
                            Text(acc.name.first().uppercaseChar().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(acc.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                            Text("Balance: $accSymbol${String.format("%,.2f", acc.balance)}", fontSize = 11.sp, color = AppColors.LabelSecondary)
                        }
                        if (sel) Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(AppColors.Warning), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        } else Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(AppColors.Separator))
                    }
                    if (idx < accounts.lastIndex) HorizontalDivider(modifier = Modifier.padding(start = 54.dp), color = AppColors.Separator, thickness = 0.5.dp)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Date
            Card(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg), elevation = CardDefaults.cardElevation(0.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Withdrawal date", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                        Text("When did you withdraw?", fontSize = 11.sp, color = AppColors.LabelSecondary)
                    }
                    Text(dateFmt.format(Date(withdrawalDate)), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Warning)
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Note (optional)") }, placeholder = { Text("Emergency, vacation spend...") },
                singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                if (isValid) onConfirm(selectedAccountId, amount!!, note.trim(), withdrawalDate)
            },
                enabled = isValid, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Warning)) {
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

// ── Add Goal Sheet ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalSheet(onDismiss: () -> Unit, onConfirm: (String, String, Double, String, Long) -> Unit) {
    var name         by remember { mutableStateOf("") }
    var emoji        by remember { mutableStateOf("🎯") }
    var amountText   by remember { mutableStateOf("") }
    var currency     by remember { mutableStateOf("INR") }
    var targetDate   by remember { mutableStateOf(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000) }
    var showDatePicker by remember { mutableStateOf(false) }
    val amount   = amountText.toDoubleOrNull()
    val isValid  = name.isNotBlank() && amount != null && amount > 0
    val dateFmt  = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    val emojiList = listOf("🎯","🏠","✈️","🚗","📱","🎓","💍","🏖️","💰","🎁")

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.SystemBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("New Goal", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.LabelPrimary)
            Spacer(Modifier.height(16.dp))
            Text("Pick an icon", fontSize = 13.sp, color = AppColors.LabelSecondary, modifier = Modifier.padding(bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                emojiList.forEach { e ->
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (emoji == e) AppColors.Primary.copy(alpha = 0.15f) else AppColors.Separator)
                        .clickable { emoji = e }, contentAlignment = Alignment.Center) { Text(e, fontSize = 18.sp) }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Goal name") }, placeholder = { Text("e.g. House Fund") }, singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, modifier = Modifier.weight(1f),
                    label = { Text("Target amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text(if (currency == "INR") "₹" else "$") }, singleLine = true,
                    isError = amountText.isNotEmpty() && amount == null, shape = RoundedCornerShape(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Currency", fontSize = 11.sp, color = AppColors.LabelSecondary)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("INR","USD").forEach { c ->
                            FilterChip(selected = currency == c, onClick = { currency = c }, label = { Text(c, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AppColors.Primary, selectedLabelColor = Color.White))
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Card(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg), elevation = CardDefaults.cardElevation(0.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Target date", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                        Text("When do you want to reach this goal?", fontSize = 11.sp, color = AppColors.LabelSecondary)
                    }
                    Text(dateFmt.format(Date(targetDate)), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Primary)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { if (isValid) onConfirm(name.trim(), emoji, amount!!, currency, targetDate) },
                enabled = isValid, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)) {
                Text("Add new", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = targetDate,
            selectableDates = object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis > System.currentTimeMillis() })
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dpState.selectedDateMillis?.let { targetDate = it }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dpState) }
    }
}

@Composable
private fun GoalStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 10.dp)) {
        Column {
            Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.65f))
            Spacer(Modifier.height(3.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.3).sp)
        }
    }
}
