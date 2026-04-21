package com.savings.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.savings.tracker.data.SavingsEntry
import com.savings.tracker.ui.theme.AppColors
import com.savings.tracker.ui.viewmodel.SavingsViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: Int,
    viewModel: SavingsViewModel,
    onBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val account  = accounts.find { it.id == accountId }
    val color    = if (account != null) AppColors.forAccount(account.id) else AppColors.Primary

    val entriesFlow = remember(accountId) { viewModel.getEntriesForAccount(accountId) }
    val entries  by entriesFlow.collectAsState(emptyList())

    val moneyFmt = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val dateFmt  = remember { SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.US) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppColors.SystemBg)
    ) {
        // ── Gradient header ──────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(AppColors.HeaderStart, AppColors.HeaderEnd)))
                    .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
            ) {
                Column {
                    // Back button + title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(account?.name ?: "Account", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.3).sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    // Balance card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Current Balance", fontSize = 11.sp, color = Color.White.copy(alpha = 0.72f), fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(moneyFmt.format(account?.balance ?: 0.0), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-1).sp)
                        }
                    }
                }
            }
        }

        // ── Deposit history ──────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
            item {
                Card(
                    modifier  = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape     = RoundedCornerShape(14.dp),
                    colors    = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    entries.forEachIndexed { index, entry ->
                        DepositRow(entry = entry, color = color, moneyFmt = moneyFmt, dateFmt = dateFmt)
                        if (index < entries.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(start = 54.dp), color = AppColors.Separator, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun DepositRow(entry: SavingsEntry, color: Color, moneyFmt: NumberFormat, dateFmt: SimpleDateFormat) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier         = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("+${moneyFmt.format(entry.amount)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Success)
            if (entry.note.isNotBlank()) Text(entry.note, fontSize = 12.sp, color = AppColors.LabelSecondary)
            Text(dateFmt.format(java.util.Date(entry.createdAt)), fontSize = 11.sp, color = AppColors.LabelSecondary)
        }
    }
}
