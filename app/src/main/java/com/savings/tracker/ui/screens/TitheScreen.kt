package com.savings.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.savings.tracker.data.TitheEntry
import com.savings.tracker.ui.theme.AppColors
import com.savings.tracker.ui.viewmodel.SavingsViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitheScreen(viewModel: SavingsViewModel) {
    val accounts        by viewModel.accounts.collectAsState()
    val titheEntries    by viewModel.allTitheEntries.collectAsState()
    val tithePrefs      by viewModel.tithePrefs.collectAsState()
    val displayCurrency by viewModel.displayCurrency.collectAsState()
    val exchangeRate    by viewModel.exchangeRate.collectAsState()
    val displaySymbol   = if (displayCurrency == "INR") "₹" else "$"

    // Fix #37: Initialise from persisted tithe prefs
    var paycheckText  by remember(tithePrefs) { mutableStateOf(if (tithePrefs.lastPaycheckAmount > 0) tithePrefs.lastPaycheckAmount.toInt().toString() else "") }
    var paycheckCurrency by remember(tithePrefs) { mutableStateOf(tithePrefs.lastPaycheckCurrency) }
    var percentage    by remember(tithePrefs) { mutableStateOf(tithePrefs.lastPercent.toFloat()) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: -1) }
    var paycheckDate  by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val paycheckAmount  = paycheckText.toDoubleOrNull() ?: 0.0
    val donationAmount  = paycheckAmount * percentage / 100.0
    val paycheckSymbol  = if (paycheckCurrency == "INR") "₹" else "$"
    val isValid         = paycheckAmount > 0 && selectedAccountId != -1

    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    val shortFmt = remember { SimpleDateFormat("MMM yyyy", Locale.US) }

    // Totals in display currency
    val totalGiven = remember(titheEntries, displayCurrency, exchangeRate) {
        titheEntries.sumOf { ExchangeRateService.convert(it.donationAmount, it.paycheckCurrency, displayCurrency, exchangeRate) }
    }
    val thisYearGiven = remember(titheEntries, displayCurrency, exchangeRate) {
        val yearStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        titheEntries.filter { it.paycheckDate >= yearStart }
            .sumOf { ExchangeRateService.convert(it.donationAmount, it.paycheckCurrency, displayCurrency, exchangeRate) }
    }
    val givingStreak = remember(titheEntries) { titheEntries.size }   // simple count

    LazyColumn(modifier = Modifier.fillMaxSize().background(AppColors.SystemBg)) {
        item {
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF4A148C), Color(0xFF6A1B9A))))
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Tithe", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.5).sp)
                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.2f))
                            .clickable { viewModel.toggleDisplayCurrency() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(if (displayCurrency == "INR") "₹ INR" else "$ USD", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text("A portion of every paycheck, set aside for good", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TitheStatCard("Total given", "$displaySymbol${String.format("%,.0f", totalGiven)}", Modifier.weight(1f))
                        TitheStatCard("This year", "$displaySymbol${String.format("%,.0f", thisYearGiven)}", Modifier.weight(1f))
                        TitheStatCard("Paychecks", givingStreak.toString(), Modifier.weight(1f))
                    }
                }
            }
        }

        // ── New paycheck entry ─────────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            Text("RECORD NEW TITHE", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelSecondary,
                letterSpacing = 0.5.sp, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
        }

        // Paycheck amount card
        item {
            Card(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = paycheckText, onValueChange = { paycheckText = it },
                            modifier = Modifier.weight(1f), label = { Text("Paycheck amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            prefix = { Text(paycheckSymbol) }, singleLine = true,
                            shape = RoundedCornerShape(12.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Currency", fontSize = 11.sp, color = AppColors.LabelSecondary)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("INR","USD").forEach { c ->
                                    FilterChip(selected = paycheckCurrency == c, onClick = { paycheckCurrency = c },
                                        label = { Text(c, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AppColors.TithePurple, selectedLabelColor = Color.White))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Paycheck date
                    Row(Modifier.fillMaxWidth().clickable { showDatePicker = true }
                        .clip(RoundedCornerShape(8.dp)).background(AppColors.SystemBg).padding(10.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Paycheck date", fontSize = 13.sp, color = AppColors.LabelPrimary)
                        Text(dateFmt.format(Date(paycheckDate)), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TithePurple)
                    }
                }
            }
        }

        // Slider card
        item {
            Spacer(Modifier.height(10.dp))
            Card(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Donation percentage", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                        Text("${percentage.roundToInt()}%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.TithePurple)
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(value = percentage, onValueChange = { percentage = it }, valueRange = 1f..30f, steps = 28,
                        colors = SliderDefaults.colors(thumbColor = AppColors.TithePurple, activeTrackColor = AppColors.TithePurple))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("1%", fontSize = 10.sp, color = AppColors.LabelSecondary)
                        Text("10%", fontSize = 10.sp, color = AppColors.LabelSecondary)
                        Text("20%", fontSize = 10.sp, color = AppColors.LabelSecondary)
                        Text("30%", fontSize = 10.sp, color = AppColors.LabelSecondary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(AppColors.TithePurple.copy(alpha = 0.1f)).padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Donation amount", fontSize = 13.sp, color = AppColors.LabelPrimary)
                            Text("$paycheckSymbol${String.format("%,.2f", donationAmount)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.TithePurple)
                        }
                    }
                }
            }
        }

        // Account picker
        if (accounts.isNotEmpty()) {
            item {
                Spacer(Modifier.height(10.dp))
                Text("FROM ACCOUNT", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelSecondary,
                    letterSpacing = 0.5.sp, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(14.dp),
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
                            if (sel) Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(AppColors.TithePurple), contentAlignment = Alignment.Center) {
                                Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(AppColors.Separator))
                        }
                        if (idx < accounts.lastIndex) HorizontalDivider(modifier = Modifier.padding(start = 54.dp), color = AppColors.Separator, thickness = 0.5.dp)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                if (isValid) {
                    viewModel.addTitheEntry(paycheckAmount, paycheckCurrency, percentage.toDouble(), donationAmount, selectedAccountId, paycheckDate)
                    paycheckText = ""
                }
            }, enabled = isValid,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.TithePurple)) {
                Text("Add new", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

        // ── History ───────────────────────────────────────────────────────
        if (titheEntries.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Text("HISTORY", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelSecondary,
                    letterSpacing = 0.5.sp, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(8.dp))
            }
            items(titheEntries, key = { it.id }) { entry ->
                val entrySymbol = if (entry.paycheckCurrency == "INR") "₹" else "$"
                Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.CardBg), elevation = CardDefaults.cardElevation(0.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(AppColors.TithePurple.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Text("💜", fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${entry.donationPercent.roundToInt()}% of $entrySymbol${String.format("%,.0f", entry.paycheckAmount)}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                            Text(shortFmt.format(Date(entry.paycheckDate)), fontSize = 11.sp, color = AppColors.LabelSecondary)
                        }
                        Text("$entrySymbol${String.format("%,.2f", entry.donationAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.TithePurple)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = paycheckDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dpState.selectedDateMillis?.let { paycheckDate = it }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dpState) }
    }
}

@Composable
private fun TitheStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 10.dp)) {
        Column {
            Text(label, fontSize = 8.sp, color = Color.White.copy(alpha = 0.65f))
            Spacer(Modifier.height(3.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.3).sp)
        }
    }
}
