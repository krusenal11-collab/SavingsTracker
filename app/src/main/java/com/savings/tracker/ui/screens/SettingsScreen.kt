package com.savings.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.savings.tracker.data.NotificationPrefs
import com.savings.tracker.ui.viewmodel.SavingsViewModel
import java.util.Calendar
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SavingsViewModel,
    onBack: () -> Unit
) {
    // Start with current saved preferences as local state
    val savedPrefs by viewModel.notificationPrefs.collectAsState()
    var localPrefs by remember(savedPrefs) { mutableStateOf(savedPrefs) }
    var saved by remember { mutableStateOf(false) }

    // Auto-hide the "Saved!" confirmation after 2 seconds
    LaunchedEffect(saved) {
        if (saved) {
            delay(2000)
            saved = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Weekly Savings Reminder",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "We'll remind you every week to transfer money to your savings accounts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Enable / Disable toggle ──────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Enable Reminder", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Receive weekly push notifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = localPrefs.enabled,
                        onCheckedChange = { localPrefs = localPrefs.copy(enabled = it) }
                    )
                }
            }

            if (localPrefs.enabled) {
                // ── Day picker ───────────────────────────────────────────
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Reminder Day", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        DayPicker(
                            selectedDay = localPrefs.dayOfWeek,
                            onDaySelected = { localPrefs = localPrefs.copy(dayOfWeek = it) }
                        )
                    }
                }

                // ── Time picker ──────────────────────────────────────────
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Reminder Time",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        TimePicker(
                            hour = localPrefs.hour,
                            minute = localPrefs.minute,
                            onTimeChanged = { h, m -> localPrefs = localPrefs.copy(hour = h, minute = m) }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (saved) {
                Text(
                    "✓ Reminder saved!",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Button(
                onClick = {
                    viewModel.saveNotificationPrefs(localPrefs)
                    saved = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}

@Composable
fun DayPicker(selectedDay: Int, onDaySelected: (Int) -> Unit) {
    val days = listOf(
        Calendar.SUNDAY    to "Sun",
        Calendar.MONDAY    to "Mon",
        Calendar.TUESDAY   to "Tue",
        Calendar.WEDNESDAY to "Wed",
        Calendar.THURSDAY  to "Thu",
        Calendar.FRIDAY    to "Fri",
        Calendar.SATURDAY  to "Sat"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { (calDay, label) ->
            FilterChip(
                selected = selectedDay == calDay,
                onClick  = { onDaySelected(calDay) },
                label    = { Text(label) }
            )
        }
    }
}

@Composable
fun TimePicker(hour: Int, minute: Int, onTimeChanged: (Int, Int) -> Unit) {
    val isPm        = hour >= 12
    val displayHour = when {
        hour == 0  -> 12
        hour > 12  -> hour - 12
        else       -> hour
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Hour column
        SpinnerColumn(
            value    = String.format("%02d", displayHour),
            onUp     = { onTimeChanged(if (hour == 23) 0 else hour + 1, minute) },
            onDown   = { onTimeChanged(if (hour == 0) 23 else hour - 1, minute) }
        )

        Text(":", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Minute column (steps of 5)
        SpinnerColumn(
            value    = String.format("%02d", minute),
            onUp     = { onTimeChanged(hour, if (minute >= 55) 0 else minute + 5) },
            onDown   = { onTimeChanged(hour, if (minute == 0) 55 else minute - 5) }
        )

        Spacer(Modifier.width(16.dp))

        // AM/PM toggle
        FilledTonalButton(onClick = {
            val newHour = if (isPm) hour - 12 else hour + 12
            onTimeChanged(newHour.coerceIn(0, 23), minute)
        }) {
            Text(if (isPm) "PM" else "AM", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SpinnerColumn(value: String, onUp: () -> Unit, onDown: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onUp) {
            Text("▲", style = MaterialTheme.typography.bodyLarge)
        }
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = onDown) {
            Text("▼", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
