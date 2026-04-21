package com.savings.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savings.tracker.data.ExchangeRateService
import com.savings.tracker.data.SavingsEntry
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

    // Fix #21/#28: Format balance in account's own currency
    val accountSymbol   = if (account?.currency == "INR") "₹" else "$"
    val displaySymbol   = if (displayCurrency == "INR") "₹" else "$"
    // Fix #22: Date-only format, no time
    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    LazyColumn(modifier = Modifier.fillMaxSize().background(AppColors.SystemBg)) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(AppColors.HeaderStart, AppColors.HeaderEnd)))
                    .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column {
                            Text(account?.name ?: "Account", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            account?.let {
                                Text("${it.currency} account", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.15f)).padding(16.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Current Balance", fontSize = 11.sp, color = Color.White.copy(alpha = 0.72f), fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            // Fix #21/#28: Show in account's own currency
                            Text(
                                "$accountSymbol${String.format("%,.2f", account?.balance ?: 0.0)}",
                                fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-1).sp
                            )
                            // Also show in display currency if different
                            if (account != null && account.currency != displayCurrency) {
                                val converted = ExchangeRateService.convert(account.balance, account.currency, displayCurrency, exchangeRate)
                                Text("≈ $displaySymbol${String.format("%,.2f", converted)}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Deposit History", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.LabelPrimary)
                Text("${entries.size} deposits", fontSize = 13.sp, color = AppColors.LabelSecondary)
            }
        }

        if (entries.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(52.dp), tint = AppColors.LabelSecondary)
                        Spacer(Modifier.height(12.dp))
                        Text("No deposits yet", color = AppColors.LabelSecondary, fontWeight = FontWeight.Medium)
                        Text("Tap Add Deposit on the home screen", color = AppColors.LabelSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            // Fix #38: Use itemsIndexed — LazyColumn recycles each deposit row individually
            itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
                Card(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = if (index == 0) RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                            else if (index == entries.lastIndex) RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
                            else RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                    elevation = CardDefaults.cardElevation(0.dp)) {
                    DepositRow(entry = entry, color = color, dateFmt = dateFmt, displayCurrency = displayCurrency, exchangeRate = exchangeRate)
                }
                if (index < entries.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, start = 70.dp), color = AppColors.Separator, thickness = 0.5.dp)
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun DepositRow(entry: SavingsEntry, color: Color, dateFmt: SimpleDateFormat, displayCurrency: String, exchangeRate: Double) {
    // Fix #13/#27: Show correct currency symbol per entry
    val entrySymbol = if (entry.currency == "INR") "₹" else "$"
    val displaySymbol = if (displayCurrency == "INR") "₹" else "$"
    val displayAmount = ExchangeRateService.convert(entry.amount, entry.currency, displayCurrency, exchangeRate)

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            // Fix #13: Native currency amount
            Text("+$entrySymbol${String.format("%,.2f", entry.amount)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Success)
            // Show display currency conversion if different
            if (entry.currency != displayCurrency) {
                Text("≈ $displaySymbol${String.format("%,.2f", displayAmount)}", fontSize = 11.sp, color = AppColors.LabelSecondary)
            }
            if (entry.note.isNotBlank()) Text(entry.note, fontSize = 12.sp, color = AppColors.LabelSecondary)
            // Fix #22/#29: Show depositDate (date only, no time)
            Text(dateFmt.format(Date(entry.depositDate)), fontSize = 11.sp, color = AppColors.LabelSecondary)
        }
    }
}
