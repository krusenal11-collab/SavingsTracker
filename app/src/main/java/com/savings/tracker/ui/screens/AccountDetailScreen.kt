package com.savings.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.savings.tracker.data.SavingsEntry
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
    // remember(accountId) ensures the same Flow instance is reused across recompositions.
    // Without this, getEntriesForAccount() returns a new Flow object every time the
    // composable recomposes, causing collectAsState to restart the DB subscription needlessly.
    val entriesFlow = remember(accountId) { viewModel.getEntriesForAccount(accountId) }
    val entries  by entriesFlow.collectAsState(emptyList())

    var showDepositDialog by remember { mutableStateOf(false) }

    val moneyFmt = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val dateFmt  = remember { SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.name ?: "Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDepositDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Log Deposit")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Balance card
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Current Balance", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            moneyFmt.format(account?.balance ?: 0.0),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (entries.isNotEmpty()) {
                    Text(
                        "Deposit History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Savings,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("No deposits yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "Tap + to log your first deposit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    DepositCard(entry = entry, moneyFmt = moneyFmt, dateFmt = dateFmt)
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showDepositDialog) {
        AddDepositDialog(
            onDismiss = { showDepositDialog = false },
            onConfirm = { amount, note ->
                viewModel.addDeposit(accountId, amount, note)
                showDepositDialog = false
            }
        )
    }
}

@Composable
fun DepositCard(entry: SavingsEntry, moneyFmt: NumberFormat, dateFmt: SimpleDateFormat) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    moneyFmt.format(entry.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (entry.note.isNotBlank()) {
                    Text(entry.note, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    dateFmt.format(Date(entry.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AddDepositDialog(onDismiss: () -> Unit, onConfirm: (Double, String) -> Unit) {
    var amountText by remember { mutableStateOf("1000") }
    var note       by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull()
    val valid  = amount != null && amount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Deposit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = !valid && amountText.isNotEmpty(),
                    prefix = { Text("$") }
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("Weekly transfer") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (valid) onConfirm(amount!!, note.trim()) },
                enabled = valid
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
