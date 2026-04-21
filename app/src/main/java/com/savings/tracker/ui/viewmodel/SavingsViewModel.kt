package com.savings.tracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.savings.tracker.data.*
import com.savings.tracker.notification.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SavingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SavingsRepository
    private val prefsManager: PreferencesManager

    // ── Exposed state ──────────────────────────────────────────────────────

    val accounts: StateFlow<List<BankAccount>>
    val totalSavings: StateFlow<Double>
    val notificationPrefs: StateFlow<NotificationPrefs>

    init {
        val dao = SavingsDatabase.getDatabase(application).savingsDao()
        repository    = SavingsRepository(dao)
        prefsManager  = PreferencesManager(application)

        accounts = repository.allAccounts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        totalSavings = repository.totalSavings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

        notificationPrefs = prefsManager.notificationPrefs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationPrefs())
    }

    // ── Account actions ────────────────────────────────────────────────────

    fun addAccount(name: String) {
        viewModelScope.launch { repository.addAccount(name) }
    }

    fun deleteAccount(account: BankAccount) {
        viewModelScope.launch { repository.deleteAccount(account) }
    }

    // ── Deposit actions ────────────────────────────────────────────────────

    fun addDeposit(accountId: Int, amount: Double, note: String) {
        viewModelScope.launch { repository.addDeposit(accountId, amount, note) }
    }

    fun getEntriesForAccount(accountId: Int): Flow<List<SavingsEntry>> =
        repository.getEntriesForAccount(accountId)

    // ── Notification settings ──────────────────────────────────────────────

    fun saveNotificationPrefs(prefs: NotificationPrefs) {
        viewModelScope.launch {
            prefsManager.save(prefs)
            val ctx = getApplication<Application>()
            if (prefs.enabled) {
                AlarmScheduler.schedule(ctx, prefs.dayOfWeek, prefs.hour, prefs.minute)
            } else {
                AlarmScheduler.cancel(ctx)
            }
        }
    }
}
