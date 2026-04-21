package com.savings.tracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savings.tracker.data.ExchangeRateService
import com.savings.tracker.data.SavingsEntry
import com.savings.tracker.ui.theme.AppColors
import com.savings.tracker.ui.viewmodel.SavingsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SummaryScreen(viewModel: SavingsViewModel, onBack: (() -> Unit)? = null) {
    // Fix #21: Use collected state, not viewModel.accounts.value directly
    val accounts        by viewModel.accounts.collectAsState()
    val allEntries      by viewModel.allEntries.collectAsState()
    val allEntriesAsc   by viewModel.allEntriesAsc.collectAsState()
    val displayCurrency by viewModel.displayCurrency.collectAsState()
    val exchangeRate    by viewModel.exchangeRate.collectAsState()
    val rateIsLive      by viewModel.rateIsLive.collectAsState()
    val totalInr        by viewModel.totalSavingsInr.collectAsState()
    val totalUsd        by viewModel.totalSavingsUsd.collectAsState()

    val displayTotal  = if (displayCurrency == "INR") totalInr else totalUsd
    val displaySymbol = if (displayCurrency == "INR") "₹" else "$"

    val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
    val monthStart = cal.timeInMillis
    val thisMonthTotal = remember(allEntries, displayCurrency, exchangeRate) {
        allEntries.filter { it.depositDate >= monthStart }
            .sumOf { ExchangeRateService.convert(it.amount, it.currency, displayCurrency, exchangeRate) }
    }

    // Fix #10/#24: Convert all amounts to display currency before building chart
    val chartPoints = remember(allEntriesAsc, displayCurrency, exchangeRate) {
        var running = 0.0
        allEntriesAsc.map { entry ->
            running += ExchangeRateService.convert(entry.amount, entry.currency, displayCurrency, exchangeRate)
            // Fix #16/#18: Use depositDate for X-axis
            Pair(entry.depositDate, running)
        }
    }

    // Fix #17/#19: `now` computed inside remember block
    var timeFilter by remember { mutableStateOf(3) }
    val filteredPoints = remember(chartPoints, timeFilter) {
        val now = System.currentTimeMillis()
        val msPerDay = 24L * 60 * 60 * 1000
        val cutoff = when (timeFilter) {
            0 -> now - 30L * msPerDay
            1 -> now - 90L * msPerDay
            2 -> now - 180L * msPerDay
            else -> 0L
        }
        chartPoints.filter { it.first >= cutoff }
    }

    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    LazyColumn(modifier = Modifier.fillMaxSize().background(AppColors.SystemBg)) {
        item {
            Box(modifier = Modifier.fillMaxWidth()
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(AppColors.HeaderStart, AppColors.HeaderEnd)))
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)) {
                Column {
                    if (onBack != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                            Text("Insights", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Insights", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.5).sp)
                            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.2f))
                                .clickable { viewModel.toggleDisplayCurrency() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(if (displayCurrency == "INR") "₹ INR" else "$ USD", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(if (rateIsLive) "1 USD = ₹${String.format("%.2f", exchangeRate)} · Live" else "1 USD = ₹${String.format("%.2f", exchangeRate)} · Cached",
                        fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HeaderStatCard("Total", "$displaySymbol${String.format("%,.0f", displayTotal)}", Modifier.weight(1f))
                        HeaderStatCard("This month", "$displaySymbol${String.format("%,.0f", thisMonthTotal)}", Modifier.weight(1f))
                        HeaderStatCard("Deposits", allEntries.size.toString(), Modifier.weight(1f))
                    }
                }
            }
        }

        // Chart card
        item {
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column {
                            Text("$displaySymbol${String.format("%,.2f", displayTotal)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.LabelPrimary, letterSpacing = (-0.5).sp)
                            if (thisMonthTotal > 0) Text("+$displaySymbol${String.format("%,.0f", thisMonthTotal)} this month", fontSize = 12.sp, color = AppColors.Success, fontWeight = FontWeight.Medium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("1M", "3M", "6M", "All").forEachIndexed { i, lbl ->
                                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(if (timeFilter == i) AppColors.Primary else AppColors.Separator)
                                    .clickable { timeFilter = i }.padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                                    Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (timeFilter == i) Color.White else AppColors.LabelSecondary)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (filteredPoints.size >= 2) {
                        SavingsLineChart(filteredPoints, Modifier.fillMaxWidth().height(120.dp))
                        Spacer(Modifier.height(8.dp))
                        ChartXLabels(filteredPoints)
                    } else {
                        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text("Add deposits to see your chart", color = AppColors.LabelSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Account breakdown
        if (accounts.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                Text("By account", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.LabelPrimary, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(10.dp))
                Card(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.CardBg), elevation = CardDefaults.cardElevation(0.dp)) {
                    accounts.forEachIndexed { index, account ->
                        val color = AppColors.forAccount(account.id)
                        // Fix #15/#17: Convert each account to display currency for % calculation
                        val balInDisplay = ExchangeRateService.convert(account.balance, account.currency, displayCurrency, exchangeRate)
                        val progress = if (displayTotal > 0) (balInDisplay / displayTotal).toFloat().coerceIn(0f, 1f) else 0f
                        val accountSymbol = if (account.currency == "INR") "₹" else "$"
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(color), contentAlignment = Alignment.Center) {
                                Text(account.name.first().uppercaseChar().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text(account.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                                    Text("$accountSymbol${String.format("%,.2f", account.balance)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.LabelPrimary)
                                }
                                Spacer(Modifier.height(5.dp))
                                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(AppColors.Separator)) {
                                    Box(Modifier.fillMaxWidth(progress).height(4.dp).clip(RoundedCornerShape(2.dp)).background(color))
                                }
                                Text("${(progress * 100).toInt()}% of total", fontSize = 11.sp, color = AppColors.LabelSecondary, modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                        if (index < accounts.lastIndex) HorizontalDivider(modifier = Modifier.padding(start = 54.dp), color = AppColors.Separator, thickness = 0.5.dp)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
            Text("All deposits", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.LabelPrimary, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
        }

        if (allEntries.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) { Text("No deposits yet", color = AppColors.LabelSecondary) } }
        } else {
            // Fix #38: itemsIndexed so LazyColumn recycles each row
            itemsIndexed(allEntries, key = { _, e -> e.id }) { index, entry ->
                // Fix #21: Use collected `accounts` state, not viewModel.accounts.value
                val account = accounts.find { it.id == entry.accountId }
                val color = if (account != null) AppColors.forAccount(account.id) else AppColors.LabelSecondary
                val entrySymbol = if (entry.currency == "INR") "₹" else "$"
                val displayAmt = ExchangeRateService.convert(entry.amount, entry.currency, displayCurrency, exchangeRate)

                Card(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = when {
                        allEntries.size == 1 -> RoundedCornerShape(14.dp)
                        index == 0 -> RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                        index == allEntries.lastIndex -> RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
                        else -> RoundedCornerShape(0.dp)
                    },
                    colors = CardDefaults.cardColors(containerColor = AppColors.CardBg), elevation = CardDefaults.cardElevation(0.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(account?.name ?: "Unknown", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                            if (entry.note.isNotBlank()) Text(entry.note, fontSize = 11.sp, color = AppColors.LabelSecondary)
                            // Fix #16/#29: Show depositDate, date only
                            Text(dateFmt.format(Date(entry.depositDate)), fontSize = 11.sp, color = AppColors.LabelSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("+$entrySymbol${String.format("%,.2f", entry.amount)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Success)
                            if (entry.currency != displayCurrency) {
                                Text("≈ $displaySymbol${String.format("%,.2f", displayAmt)}", fontSize = 10.sp, color = AppColors.LabelSecondary)
                            }
                        }
                    }
                }
                if (index < allEntries.lastIndex) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, start = 70.dp), color = AppColors.Separator, thickness = 0.5.dp)
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun SavingsLineChart(chartPoints: List<Pair<Long, Double>>, modifier: Modifier = Modifier) {
    val lineColor = AppColors.Primary
    val fillColor = AppColors.Primary.copy(alpha = 0.10f)
    Canvas(modifier = modifier) {
        if (chartPoints.size < 2) return@Canvas
        val maxVal = chartPoints.maxOf { it.second }.coerceAtLeast(1.0)
        val minTime = chartPoints.minOf { it.first }.toFloat()
        val maxTime = chartPoints.maxOf { it.first }.toFloat()
        val timeRange = (maxTime - minTime).coerceAtLeast(1f)
        fun xOf(t: Long) = ((t.toFloat() - minTime) / timeRange) * size.width
        fun yOf(v: Double) = size.height - (v / maxVal * size.height * 0.88f).toFloat() - size.height * 0.06f
        val linePath = Path(); val areaPath = Path()
        chartPoints.forEachIndexed { i, (t, v) ->
            val x = xOf(t); val y = yOf(v)
            if (i == 0) { linePath.moveTo(x, y); areaPath.moveTo(0f, size.height); areaPath.lineTo(x, y) }
            else { linePath.lineTo(x, y); areaPath.lineTo(x, y) }
        }
        areaPath.lineTo(size.width, size.height); areaPath.close()
        drawPath(areaPath, color = fillColor)
        drawPath(linePath, color = lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val last = chartPoints.last(); val lx = xOf(last.first); val ly = yOf(last.second)
        drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(lx, ly))
        drawCircle(lineColor, radius = 5.dp.toPx(), center = Offset(lx, ly), style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun ChartXLabels(points: List<Pair<Long, Double>>) {
    val fmt = remember { SimpleDateFormat("MMM", Locale.US) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(fmt.format(Date(points.first().first)), fontSize = 10.sp, color = AppColors.LabelSecondary)
        Text(fmt.format(Date(points[points.size / 2].first)), fontSize = 10.sp, color = AppColors.LabelSecondary)
        Text(fmt.format(Date(points.last().first)), fontSize = 10.sp, color = AppColors.Primary, fontWeight = FontWeight.Medium)
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
