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
import com.savings.tracker.ui.theme.AppColors
import com.savings.tracker.ui.viewmodel.SavingsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: SavingsViewModel) {
    val goals           by viewModel.allGoals.collectAsState()
    val allEntries      by viewModel.allEntries.collectAsState()
    val displayCurrency by viewModel.displayCurrency.collectAsState()
    val exchangeRate    by viewModel.exchangeRate.collectAsState()
    val displaySymbol   = if (displayCurrency == "INR") "₹" else "$"

    var showAddGoalSheet by remember { mutableStateOf(false) }
    var goalToDelete     by remember { mutableStateOf<Goal?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize().background(AppColors.SystemBg)) {
        item {
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AppColors.HeaderStart, AppColors.HeaderEnd)))
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Goals", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.5).sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.2f))
                                .clickable { viewModel.toggleDisplayCurrency() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(if (displayCurrency == "INR") "₹ INR" else "$ USD", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(onClick = { showAddGoalSheet = true }) {
                                Text("+ New", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    // Summary stat row
                    val totalSavedTowardGoals = remember(allEntries, displayCurrency, exchangeRate) {
                        allEntries.filter { it.goalId != null }
                            .sumOf { ExchangeRateService.convert(it.amount, it.currency, displayCurrency, exchangeRate) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HeaderStatCard("Active goals", goals.size.toString(), Modifier.weight(1f))
                        HeaderStatCard("Saved toward goals", "$displaySymbol${String.format("%,.0f", totalSavedTowardGoals)}", Modifier.weight(2f))
                    }
                }
            }
        }

        // Fix #25: Goals empty state
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
                val savedAmount = viewModel.savedAmountForGoal(goal.id, allEntries)
                val targetInDisplay = ExchangeRateService.convert(goal.targetAmount, goal.targetCurrency, displayCurrency, exchangeRate)
                val progress = if (targetInDisplay > 0) (savedAmount / targetInDisplay).toFloat().coerceIn(0f, 1f) else 0f
                val pct = (progress * 100).toInt()
                val isComplete = pct >= 100
                val accentColor = when {
                    isComplete -> AppColors.Success
                    pct >= 75  -> AppColors.Warning
                    else       -> AppColors.Primary
                }
                val dateFmt = remember { SimpleDateFormat("MMM yyyy", Locale.US) }

                Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.CardBg), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(goal.emoji, fontSize = 24.sp)
                                Column {
                                    Text(goal.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.LabelPrimary)
                                    Text("Target: $dateFmt".let { dateFmt.format(Date(goal.targetDate)) },
                                        fontSize = 11.sp, color = AppColors.LabelSecondary)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("$pct%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                IconButton(onClick = { goalToDelete = goal }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppColors.Destructive, modifier = Modifier.size(16.dp))
                                }
                            }
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
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(AppColors.Success.copy(alpha = 0.12f)).padding(8.dp), contentAlignment = Alignment.Center) {
                                Text("🎉 Goal complete!", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Success)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Delete Goal") },
            text  = { Text("Delete \"${goal.name}\"? Existing deposits tagged to this goal will keep their tag but the goal will no longer appear.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteGoal(goal); goalToDelete = null }) { Text("Delete", color = AppColors.Destructive) }
            },
            dismissButton = { TextButton(onClick = { goalToDelete = null }) { Text("Cancel") } }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String, Long) -> Unit
) {
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

            // Emoji picker
            Text("Pick an icon", fontSize = 13.sp, color = AppColors.LabelSecondary, modifier = Modifier.padding(bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                emojiList.forEach { e ->
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (emoji == e) AppColors.Primary.copy(alpha = 0.15f) else AppColors.Separator)
                        .clickable { emoji = e }, contentAlignment = Alignment.Center) {
                        Text(e, fontSize = 18.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Goal name") }, placeholder = { Text("e.g. House Fund") }, singleLine = true,
                shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(10.dp))

            // Target amount + currency
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, modifier = Modifier.weight(1f),
                    label = { Text("Target amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text(if (currency == "INR") "₹" else "$") }, singleLine = true,
                    isError = amountText.isNotEmpty() && amount == null,
                    shape = RoundedCornerShape(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Currency", fontSize = 11.sp, color = AppColors.LabelSecondary)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("INR","USD").forEach { c ->
                            FilterChip(selected = currency == c, onClick = { currency = c },
                                label = { Text(c, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AppColors.Primary, selectedLabelColor = Color.White))
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            // Fix #37: Target date — validate future date in UI
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
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = targetDate,
            selectableDates = object : SelectableDates {
                // Fix #37: Only allow future dates
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis > System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { dpState.selectedDateMillis?.let { targetDate = it }; showDatePicker = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dpState) }
    }
}

@Composable
private fun HeaderStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 10.dp)) {
        Column {
            Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.65f))
            Spacer(Modifier.height(3.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.3).sp)
        }
    }
}
