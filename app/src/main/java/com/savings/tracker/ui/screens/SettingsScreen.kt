package com.savings.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.savings.tracker.data.NotificationPrefs
import com.savings.tracker.ui.theme.AppColors
import com.savings.tracker.ui.viewmodel.SavingsViewModel
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SavingsViewModel, onBack: () -> Unit = {}) {
    val savedPrefs by viewModel.notificationPrefs.collectAsState()
    var localPrefs by remember(savedPrefs) { mutableStateOf(savedPrefs) }
    var saved      by remember { mutableStateOf(false) }

    LaunchedEffect(saved) {
        if (saved) { delay(2000); saved = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SystemBg)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AppColors.HeaderStart, AppColors.HeaderEnd)))
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp)
        ) {
            Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.5).sp)
        }

        Spacer(Modifier.height(20.dp))

        // Section: Notifications
        Text("NOTIFICATIONS", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelSecondary,
            letterSpacing = 0.5.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp))
        Spacer(Modifier.height(8.dp))

        Card(
            modifier  = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            shape     = RoundedCornerShape(14.dp),
            colors    = CardDefaults.cardColors(containerColor = AppColors.CardBg),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            // Enable toggle
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Weekly Reminder", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelPrimary)
                    Text("Get notified to transfer savings", fontSize = 13.sp, color = AppColors.LabelSecondary)
                }
                Switch(
                    checked = localPrefs.enabled,
                    onCheckedChange = { localPrefs = localPrefs.copy(enabled = it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppColors.Success)
                )
            }
        }

        if (localPrefs.enabled) {
            Spacer(Modifier.height(16.dp))
            Text("REMINDER DAY", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelSecondary,
                letterSpacing = 0.5.sp, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))

            Card(
                modifier  = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape     = RoundedCornerShape(14.dp),
                colors    = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val days = listOf(Calendar.SUNDAY to "Sun", Calendar.MONDAY to "Mon",
                        Calendar.TUESDAY to "Tue", Calendar.WEDNESDAY to "Wed",
                        Calendar.THURSDAY to "Thu", Calendar.FRIDAY to "Fri", Calendar.SATURDAY to "Sat")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        days.forEach { (day, label) ->
                            val selected = localPrefs.dayOfWeek == day
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) AppColors.Primary else AppColors.Separator),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label.take(1),
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = if (selected) Color.White else AppColors.LabelSecondary,
                                    modifier   = androidx.compose.ui.Modifier.clickable { localPrefs = localPrefs.copy(dayOfWeek = day) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("REMINDER TIME", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AppColors.LabelSecondary,
                letterSpacing = 0.5.sp, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))

            Card(
                modifier  = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape     = RoundedCornerShape(14.dp),
                colors    = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TimeSpinner(
                        value  = run { val h = localPrefs.hour; if (h == 0) 12 else if (h > 12) h - 12 else h },
                        onUp   = { localPrefs = localPrefs.copy(hour = if (localPrefs.hour == 23) 0 else localPrefs.hour + 1) },
                        onDown = { localPrefs = localPrefs.copy(hour = if (localPrefs.hour == 0) 23 else localPrefs.hour - 1) }
                    )
                    Text(":", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = AppColors.LabelPrimary, modifier = Modifier.padding(horizontal = 4.dp))
                    TimeSpinner(
                        value  = localPrefs.minute,
                        onUp   = { localPrefs = localPrefs.copy(minute = if (localPrefs.minute >= 55) 0 else localPrefs.minute + 5) },
                        onDown = { localPrefs = localPrefs.copy(minute = if (localPrefs.minute == 0) 55 else localPrefs.minute - 5) },
                        twoDigit = true
                    )
                    Spacer(Modifier.width(16.dp))
                    FilledTonalButton(
                        onClick = {
                            val h = localPrefs.hour
                            localPrefs = localPrefs.copy(hour = if (h < 12) h + 12 else h - 12)
                        },
                        shape  = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = AppColors.Primary.copy(alpha = 0.12f))
                    ) {
                        Text(if (localPrefs.hour < 12) "AM" else "PM", fontWeight = FontWeight.Bold, color = AppColors.Primary)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (saved) {
            Text(
                "Reminder saved!",
                color    = AppColors.Success,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick  = { viewModel.saveNotificationPrefs(localPrefs); saved = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
        ) {
            Text("Save Settings", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun TimeSpinner(value: Int, onUp: () -> Unit, onDown: () -> Unit, twoDigit: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onUp, modifier = Modifier.size(36.dp)) {
            Text("▲", fontSize = 14.sp, color = AppColors.LabelSecondary)
        }
        Text(
            if (twoDigit) String.format("%02d", value) else String.format("%02d", value),
            fontSize      = 32.sp,
            fontWeight    = FontWeight.Bold,
            color         = AppColors.Primary,
            letterSpacing = (-0.5).sp
        )
        IconButton(onClick = onDown, modifier = Modifier.size(36.dp)) {
            Text("▼", fontSize = 14.sp, color = AppColors.LabelSecondary)
        }
    }
}

private fun androidx.compose.ui.Modifier.clickable(onClick: () -> Unit): androidx.compose.ui.Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))
