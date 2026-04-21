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

    // ── Raw DB streams ─────────────────────────────────────────────────────
    val accounts: StateFlow<List<BankAccount>>
    val allEntries: StateFlow<List<SavingsEntry>>
    val allEntriesAsc: StateFlow<List<SavingsEntry>>
    val allGoals: StateFlow<List<Goal>>
    val allTitheEntries: StateFlow<List<TitheEntry>>
    val notificationPrefs: StateFlow<NotificationPrefs>
    val tithePrefs: StateFlow<TithePrefs>

    // Fix #3: Global currency toggle persisted in DataStore
    val displayCurrency: StateFlow<String>

    // Fix #20: Exchange rate with offline fallback
    private val _exchangeRate    = MutableStateFlow(83.5)
    private val _rateIsLive      = MutableStateFlow(false)
    val exchangeRate: StateFlow<Double>  = _exchangeRate.asStateFlow()
    val rateIsLive: StateFlow<Boolean>   = _rateIsLive.asStateFlow()

    // Fix #6/#15: Total savings computed in-memory after currency conversion
    val totalSavingsInr: StateFlow<Double>
    val totalSavingsUsd: StateFlow<Double>

    init {
        val dao    = SavingsDatabase.getDatabase(application).savingsDao()
        repository = SavingsRepository(dao)
        prefsManager = PreferencesManager(application)

        accounts = repository.allAccounts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        allEntries = repository.allEntries
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        allEntriesAsc = repository.allEntriesAsc
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

        // Fix #6: Compute totals in-memory using the live exchange rate
        totalSavingsInr = combine(accounts, _exchangeRate) { accs, rate ->
            accs.sumOf { acc ->
                ExchangeRateService.convert(acc.balance, acc.currency, "INR", rate)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

        totalSavingsUsd = combine(accounts, _exchangeRate) { accs, rate ->
            accs.sumOf { acc ->
                ExchangeRateService.convert(acc.balance, acc.currency, "USD", rate)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

        // Fetch exchange rate on launch
        viewModelScope.launch {
            val (cachedRate, cacheTime) = prefsManager.exchangeRateCache.first()
            val (rate, isLive) = ExchangeRateService.getRate(cachedRate, cacheTime)
            _exchangeRate.value = rate
            _rateIsLive.value   = isLive
            if (isLive) prefsManager.saveExchangeRate(rate)
        }

        // Auto-schedule notification alarm on startup
        viewModelScope.launch {
            val prefs = prefsManager.notificationPrefs.first()
            if (prefs.enabled) {
                AlarmScheduler.schedule(application, prefs.dayOfWeek, prefs.hour, prefs.minute)
            }
        }
    }

    // ── Currency toggle ────────────────────────────────────────────────────

    /** Fix #3: Single toggle updates DataStore → propagates to all screens via StateFlow */
    fun toggleDisplayCurrency() {
        viewModelScope.launch {
            val current = prefsManager.displayCurrency.first()
            prefsManager.saveDisplayCurrency(if (current == "INR") "USD" else "INR")
        }
    }

    fun convertToDisplay(amount: Double, fromCurrency: String): Double {
        val target = displayCurrency.value
        return ExchangeRateService.convert(amount, fromCurrency, target, _exchangeRate.value)
    }

    val displaySymbol: String get() = if (displayCurrency.value == "INR") "₹" else "$"

    // ── Account actions ────────────────────────────────────────────────────

    /** Fix #7/#25: Currency param + duplicate name check */
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

    // ── Deposit actions ────────────────────────────────────────────────────

    fun addDeposit(
        accountId: Int,
        amount: Double,
        note: String,
        goalId: Int?,
        depositDate: Long
    ) {
        viewModelScope.launch {
            val account = accounts.value.find { it.id == accountId } ?: return@launch
            repository.addDeposit(accountId, amount, account.currency, note, goalId, depositDate)
        }
    }

    fun getEntriesForAccount(accountId: Int): Flow<List<SavingsEntry>> =
        repository.getEntriesForAccount(accountId)

    fun getEntriesForGoal(goalId: Int): Flow<List<SavingsEntry>> =
        repository.getEntriesForGoal(goalId)

    // ── Goal actions ───────────────────────────────────────────────────────

    fun addGoal(name: String, emoji: String, targetAmount: Double, targetCurrency: String, targetDate: Long) {
        viewModelScope.launch {
            repository.addGoal(Goal(
                name           = name.trim(),
                emoji          = emoji,
                targetAmount   = targetAmount,
                targetCurrency = targetCurrency,
                targetDate     = targetDate
            ))
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch { repository.deleteGoal(goal) }
    }

    fun savedAmountForGoal(goalId: Int, entries: List<SavingsEntry>): Double {
        val rate = _exchangeRate.value
        val display = displayCurrency.value
        return entries
            .filter { it.goalId == goalId }
            .sumOf { ExchangeRateService.convert(it.amount, it.currency, display, rate) }
    }

    // ── Tithe actions ──────────────────────────────────────────────────────

    fun addTitheEntry(
        paycheckAmount: Double,
        paycheckCurrency: String,
        donationPercent: Double,
        donationAmount: Double,
        accountId: Int,
        paycheckDate: Long
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
            // Fix #37: Persist tithe preferences for next session
            prefsManager.saveTithePrefs(TithePrefs(donationPercent, paycheckAmount, paycheckCurrency))
        }
    }

    // ── Notification settings ──────────────────────────────────────────────

    fun saveNotificationPrefs(prefs: NotificationPrefs) {
        viewModelScope.launch {
            prefsManager.saveNotificationPrefs(prefs)
            val ctx = getApplication<Application>()
            if (prefs.enabled) AlarmScheduler.schedule(ctx, prefs.dayOfWeek, prefs.hour, prefs.minute)
            else AlarmScheduler.cancel(ctx)
        }
    }
}
