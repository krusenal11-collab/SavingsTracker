package com.savings.tracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savings.tracker.data.SavingsEntry
import com.savings.tracker.ui.theme.AppColors
import com.savings.tracker.ui.viewmodel.SavingsViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SummaryScreen(viewModel: SavingsViewModel) {
    val accounts      by viewModel.accounts.collectAsState()
    val totalSavings  by viewModel.totalSavings.collectAsState()
    val allEntries    by viewModel.allEntries.collectAsState()
    val allEntriesAsc by viewModel.allEntriesAsc.collectAsState()

    val moneyFmt = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val dateFmt  = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    // Stat calculations
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
    val monthStart = cal.timeInMillis
    val thisMonthTotal = remember(allEntries) { allEntries.filter { it.createdAt >= monthStart }.sumOf { it.amount } }

    // Build chart data: cumulative running total from each deposit
    val chartPoints = remember(allEntriesAsc) {
        var running = 0.0
        allEntriesAsc.map { entry ->
            running += entry.amount
            Pair(entry.createdAt, running)
        }
    }

    // Time filter state: 0=1M, 1=3M, 2=6M, 3=All
    var timeFilter by remember { mutableStateOf(3) }
    val msPerDay   = 24L * 60 * 60 * 1000
    val now        = System.currentTimeMillis()
    val filterMs   = when (timeFilter) {
        0 -> now - 30L  * msPerDay
        1 -> now - 90L  * msPerDay
        2 -> now - 180L * msPerDay
        else -> 0L
    }
    val filteredPoints = remember(chartPoints, timeFilter) {
        chartPoints.filter { it.first >= filterMs }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SystemBg)
    ) {
        // ── Header ───────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(AppColors.HeaderStart, AppColors.HeaderEnd)
                        )
                    )
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)
            ) {
                Column {
                    Text("Insights", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.5).sp)
                    Spacer(Modifier.height(16.dp))
                    // Stat row
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HeaderStatCard(label = "Total saved",    value = moneyFmt.format(totalSavings),    modifier = Modifier.weight(1f))
                        HeaderStatCard(label = "This month",     value = moneyFmt.format(thisMonthTotal),  modifier = Modifier.weight(1f))
                        HeaderStatCard(label = "Deposits",       value = allEntries.size.toString(),       modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // ── Chart card ───────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier  = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(moneyFmt.format(totalSavings), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.LabelPrimary, letterSpacing = (-0.5).sp)
                            if (thisMonthTotal > 0) {
                                Text("+${moneyFmt.format(thisMonthTotal)} this month", fontSize = 12.sp, color = AppColors.Success, fontWeight = FontWeight.Medium)
                            }
                        }
                        // Time filter chips
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("1M", "3M", "6M", "All").forEachIndexed { i, label ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (timeFilter == i) AppColors.Primary else AppColors.Separator)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .clickable(enabled = true) { timeFilter = i },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (timeFilter == i) Color.White else AppColors.LabelSecondary)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (filteredPoints.size >= 2) {
                        SavingsLineChart(
                            chartPoints = filteredPoints,
                            modifier    = Modifier.fillMaxWidth().height(120.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        // X-axis month labels
                        ChartXLabels(filteredPoints)
                    } else {
                        Box(
                            modifier         = Modifier.fillMaxWidth().height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Add deposits to see your chart", color = AppColors.LabelSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // ── Account breakdown ────────────────────────────────────────────
        if (accounts.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                Text("By account", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.LabelPrimary, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(10.dp))
                Card(
                    modifier  = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape     = RoundedCornerShape(14.dp),
                    colors    = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    accounts.forEachIndexed { index, account ->
                        val color    = AppColors.forAccount(account.id)
                        val progress = if (totalSavings > 0) (account.balance / totalSavings).toFloat().coerceIn(0f, 1f) else 0f
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier         = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(account.name.first().uppercaseChar().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text(account.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                                    Text(moneyFmt.format(account.balance), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.LabelPrimary)
                                }
                                Spacer(Modifier.height(5.dp))
                                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(AppColors.Separator)) {
                                    Box(Modifier.fillMaxWidth(progress).height(4.dp).clip(RoundedCornerShape(2.dp)).background(color))
                                }
                                Text("${(progress * 100).toInt()}% of total", fontSize = 11.sp, color = AppColors.LabelSecondary, modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                        if (index < accounts.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(start = 54.dp), color = AppColors.Separator, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        // ── All deposits feed ────────────────────────────────────────────
        item {
            Spacer(Modifier.height(20.dp))
            Text("All deposits", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.LabelPrimary, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
        }

        if (allEntries.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                    Text("No deposits yet", color = AppColors.LabelSecondary, fontSize = 14.sp)
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
                    allEntries.forEachIndexed { index, entry ->
                        val account = viewModel.accounts.value.find { it.id == entry.accountId }
                        val color   = if (account != null) AppColors.forAccount(account.id) else AppColors.LabelSecondary
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier         = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(account?.name ?: "Unknown", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                                if (entry.note.isNotBlank()) {
                                    Text(entry.note, fontSize = 11.sp, color = AppColors.LabelSecondary)
                                }
                                Text(dateFmt.format(Date(entry.createdAt)), fontSize = 11.sp, color = AppColors.LabelSecondary)
                            }
                            Text("+${moneyFmt.format(entry.amount)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Success)
                        }
                        if (index < allEntries.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(start = 54.dp), color = AppColors.Separator, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

// ── Chart ──────────────────────────────────────────────────────────────────

@Composable
fun SavingsLineChart(
    chartPoints: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier
) {
    val lineColor = AppColors.Primary
    val fillColor = AppColors.Primary.copy(alpha = 0.10f)

    Canvas(modifier = modifier) {
        if (chartPoints.size < 2) return@Canvas

        val maxVal   = chartPoints.maxOf { it.second }.coerceAtLeast(1.0)
        val minTime  = chartPoints.minOf { it.first }.toFloat()
        val maxTime  = chartPoints.maxOf { it.first }.toFloat()
        val timeRange = (maxTime - minTime).coerceAtLeast(1f)

        fun xOf(t: Long) = ((t.toFloat() - minTime) / timeRange) * size.width
        fun yOf(v: Double) = size.height - (v / maxVal * size.height * 0.88f).toFloat() - size.height * 0.06f

        val linePath = Path()
        val areaPath = Path()

        chartPoints.forEachIndexed { i, (t, v) ->
            val x = xOf(t); val y = yOf(v)
            if (i == 0) {
                linePath.moveTo(x, y)
                areaPath.moveTo(0f, size.height)
                areaPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                areaPath.lineTo(x, y)
            }
        }
        areaPath.lineTo(size.width, size.height)
        areaPath.close()

        drawPath(areaPath, color = fillColor)
        drawPath(linePath, color = lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Endpoint dot
        val last = chartPoints.last()
        val lx = xOf(last.first); val ly = yOf(last.second)
        drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(lx, ly))
        drawCircle(lineColor,   radius = 5.dp.toPx(), center = Offset(lx, ly), style = Stroke(width = 2.dp.toPx()))

        // Dashed vertical line from endpoint to bottom
        val dashLen = 4.dp.toPx(); val gapLen = 3.dp.toPx()
        var y = ly + dashLen
        while (y < size.height) {
            drawLine(lineColor.copy(alpha = 0.3f), Offset(lx, y), Offset(lx, (y + dashLen).coerceAtMost(size.height)), strokeWidth = 1.dp.toPx())
            y += dashLen + gapLen
        }
    }
}

@Composable
private fun ChartXLabels(points: List<Pair<Long, Double>>) {
    if (points.size < 2) return
    val fmt = remember { SimpleDateFormat("MMM", Locale.US) }
    val first = fmt.format(Date(points.first().first))
    val mid   = fmt.format(Date(points[points.size / 2].first))
    val last  = fmt.format(Date(points.last().first))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(first, fontSize = 10.sp, color = AppColors.LabelSecondary)
        Text(mid,   fontSize = 10.sp, color = AppColors.LabelSecondary)
        Text(last,  fontSize = 10.sp, color = AppColors.Primary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HeaderStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Column {
            Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.65f), fontWeight = FontWeight.Normal)
            Spacer(Modifier.height(3.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.3).sp)
        }
    }
}

// Make Box clickable — needed for time filter chips
private fun Modifier.clickable(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(enabled = enabled, onClick = onClick))
