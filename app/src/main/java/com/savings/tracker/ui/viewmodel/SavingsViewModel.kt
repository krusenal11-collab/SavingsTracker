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

    val accounts: StateFlow<List<BankAccount>>
    val allEntries: StateFlow<List<SavingsEntry>>
    val depositsAsc: StateFlow<List<SavingsEntry>>   // Chart: deposits only, oldest first
    val allGoals: StateFlow<List<Goal>>
    val allTitheEntries: StateFlow<List<TitheEntry>>
    val notificationPrefs: StateFlow<NotificationPrefs>
    val tithePrefs: StateFlow<TithePrefs>
    val displayCurrency: StateFlow<String>

    private val _exchangeRate = MutableStateFlow(83.5)
    private val _rateIsLive   = MutableStateFlow(false)
    val exchangeRate: StateFlow<Double>  = _exchangeRate.asStateFlow()
    val rateIsLive: StateFlow<Boolean>   = _rateIsLive.asStateFlow()

    // Withdrawal result feedback
    private val _withdrawalError = MutableStateFlow<String?>(null)
    val withdrawalError: StateFlow<String?> = _withdrawalError.asStateFlow()

    val totalSavingsInr: StateFlow<Double>
    val totalSavingsUsd: StateFlow<Double>

    init {
        val dao      = SavingsDatabase.getDatabase(application).savingsDao()
        repository   = SavingsRepository(dao)
        prefsManager = PreferencesManager(application)

        accounts = repository.allAccounts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        allEntries = repository.allEntries
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        depositsAsc = repository.depositsAsc
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        allGoals = repository.allGoals
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        allTitheEntries = repository.allTitheEntries
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        notificationPrefs = prefsManager.notificationPrefs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationPrefs())

        tithePrefs = prefsManager.tithePrefs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TithePrefs())

        displayCurrency = prefsManager.displayCurrency
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "INR")

        totalSavingsInr = combine(accounts, _exchangeRate) { accs, rate ->
            accs.sumOf { ExchangeRateService.convert(it.balance, it.currency, "INR", rate) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

        totalSavingsUsd = combine(accounts, _exchangeRate) { accs, rate ->
            accs.sumOf { ExchangeRateService.convert(it.balance, it.currency, "USD", rate) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

        viewModelScope.launch {
            val (cachedRate, cacheTime) = prefsManager.exchangeRateCache.first()
            val (rate, isLive) = ExchangeRateService.getRate(cachedRate, cacheTime)
            _exchangeRate.value = rate
            _rateIsLive.value   = isLive
            if (isLive) prefsManager.saveExchangeRate(rate)
        }

        viewModelScope.launch {
            val prefs = prefsManager.notificationPrefs.first()
            if (prefs.enabled) AlarmScheduler.schedule(application, prefs.dayOfWeek, prefs.hour, prefs.minute)
        }
    }

    // ── Currency toggle ────────────────────────────────────────────────────

    fun toggleDisplayCurrency() {
        viewModelScope.launch {
            val current = prefsManager.displayCurrency.first()
            prefsManager.saveDisplayCurrency(if (current == "INR") "USD" else "INR")
        }
    }

    val displaySymbol: String get() = if (displayCurrency.value == "INR") "₹" else "$"

    // ── Account actions ────────────────────────────────────────────────────

    fun addAccount(name: String, currency: String, onDuplicate: () -> Unit = {}) {
        viewModelScope.launch {
            val exists = accounts.value.any { it.name.equals(name.trim(), ignoreCase = true) }
            if (exists) { onDuplicate(); return@launch }
            repository.addAccount(name, currency)
        }
    }

    fun deleteAccount(account: BankAccount) {
        viewModelScope.launch { repository.deleteAccount(account) }
    }

    // ── Deposit / Withdrawal ───────────────────────────────────────────────

    fun addDeposit(accountId: Int, amount: Double, note: String, goalId: Int?, depositDate: Long) {
        viewModelScope.launch {
            val account = accounts.value.find { it.id == accountId } ?: return@launch
            repository.addDeposit(accountId, amount, account.currency, note, goalId, depositDate)
        }
    }

    /** Withdraw from a specific account. Calls onResult(true) on success, onResult(false) on insufficient funds. */
    fun withdrawFromAccount(
        accountId: Int, amount: Double, note: String,
        goalId: Int? = null, withdrawalDate: Long,
        onResult: (success: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val account = accounts.value.find { it.id == accountId }
            if (account == null) { onResult(false); return@launch }
            val success = repository.withdrawFromAccount(
                accountId, amount, account.currency, note, goalId, withdrawalDate
            )
            // Switch to Main dispatcher to update UI state safely after coroutine
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    /** Withdraw from a goal — reduces account balance + goal progress */
    fun withdrawFromGoal(
        goalId: Int, accountId: Int, amount: Double,
        note: String, withdrawalDate: Long,
        onResult: (success: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val account = accounts.value.find { it.id == accountId }
            if (account == null) { onResult(false); return@launch }
            val success = repository.withdrawFromGoal(
                goalId, accountId, amount, account.currency, note, withdrawalDate
            )
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    fun getEntriesForAccount(accountId: Int): Flow<List<SavingsEntry>> =
        repository.getEntriesForAccount(accountId)

    fun getEntriesForGoal(goalId: Int): Flow<List<SavingsEntry>> =
        repository.getEntriesForGoal(goalId)

    // ── Goal actions ───────────────────────────────────────────────────────

    fun addGoal(name: String, emoji: String, targetAmount: Double, targetCurrency: String, targetDate: Long) {
        viewModelScope.launch {
            repository.addGoal(Goal(name = name.trim(), emoji = emoji,
                targetAmount = targetAmount, targetCurrency = targetCurrency, targetDate = targetDate))
        }
    }

    /**
     * Delete a goal.
     * withdrawFunds=true  → subtract saved amounts from accounts (fulfilled goal)
     * withdrawFunds=false → keep money in accounts, just unlink deposits
     */
    fun deleteGoal(goal: Goal, withdrawFunds: Boolean = false) {
        viewModelScope.launch { repository.deleteGoal(goal, withdrawFunds) }
    }

    /** Saved amount toward a goal in the current display currency */
    fun savedAmountForGoal(goalId: Int, entries: List<SavingsEntry>): Double {
        val rate    = _exchangeRate.value
        val display = displayCurrency.value
        // Deposits add, withdrawals subtract — goal progress reflects net savings
        return entries
            .filter { it.goalId == goalId }
            .sumOf { entry ->
                val amount = ExchangeRateService.convert(entry.amount, entry.currency, display, rate)
                if (entry.transactionType == TransactionType.DEPOSIT) amount else -amount
            }
            .coerceAtLeast(0.0)
    }

    // ── Tithe actions ──────────────────────────────────────────────────────

    fun addTitheEntry(
        paycheckAmount: Double, paycheckCurrency: String,
        donationPercent: Double, donationAmount: Double,
        accountId: Int, paycheckDate: Long
    ) {
        viewModelScope.launch {
            repository.addTitheEntry(TitheEntry(
                paycheckAmount   = paycheckAmount,
                paycheckCurrency = paycheckCurrency,
                donationPercent  = donationPercent,
                donationAmount   = donationAmount,
                accountId        = accountId,
                paycheckDate     = paycheckDate,
                createdAt        = System.currentTimeMillis()
            ))
            prefsManager.saveTithePrefs(TithePrefs(donationPercent, paycheckAmount, paycheckCurrency))
        }
    }

    /** Delete a tithe entry completely */
    fun deleteTitheEntry(entry: TitheEntry) {
        viewModelScope.launch { repository.deleteTitheEntry(entry) }
    }

    /** Reduce a tithe entry to a new (lower) amount */
    fun reduceTitheEntry(entry: TitheEntry, newAmount: Double, onError: () -> Unit = {}) {
        viewModelScope.launch {
            try { repository.reduceTitheEntry(entry, newAmount) }
            catch (e: Exception) { onError() }
        }
    }

    // ── Notification ───────────────────────────────────────────────────────

    fun saveNotificationPrefs(prefs: NotificationPrefs) {
        viewModelScope.launch {
            prefsManager.saveNotificationPrefs(prefs)
            val ctx = getApplication<Application>()
            if (prefs.enabled) AlarmScheduler.schedule(ctx, prefs.dayOfWeek, prefs.hour, prefs.minute)
            else AlarmScheduler.cancel(ctx)
        }
    }
}
