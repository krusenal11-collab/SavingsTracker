package com.savings.tracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

data class NotificationPrefs(
    val enabled: Boolean = true,
    val dayOfWeek: Int = Calendar.FRIDAY,
    val hour: Int = 9,
    val minute: Int = 0
)

/** Fix #37: Tithe preferences persisted across sessions. */
data class TithePrefs(
    val lastPercent: Double = 10.0,
    val lastPaycheckAmount: Double = 0.0,
    val lastPaycheckCurrency: String = "INR"
)

class PreferencesManager(private val context: Context) {

    companion object {
        // Notification
        private val NOTIF_ENABLED   = booleanPreferencesKey("notif_enabled")
        private val NOTIF_DAY       = intPreferencesKey("notif_day")
        private val NOTIF_HOUR      = intPreferencesKey("notif_hour")
        private val NOTIF_MINUTE    = intPreferencesKey("notif_minute")
        // Fix #3: Currency display toggle
        private val DISPLAY_CURRENCY = stringPreferencesKey("display_currency")
        // Fix #18/#20: Exchange rate cache
        private val CACHED_RATE      = floatPreferencesKey("cached_usd_inr_rate")
        private val RATE_FETCH_TIME  = longPreferencesKey("rate_fetch_time")
        // Fix #37: Tithe preferences
        private val TITHE_PERCENT    = floatPreferencesKey("tithe_percent")
        private val TITHE_AMOUNT     = floatPreferencesKey("tithe_amount")
        private val TITHE_CURRENCY   = stringPreferencesKey("tithe_currency")
    }

    val notificationPrefs: Flow<NotificationPrefs> = context.dataStore.data.map { p ->
        NotificationPrefs(
            enabled   = p[NOTIF_ENABLED] ?: true,
            dayOfWeek = p[NOTIF_DAY]     ?: Calendar.FRIDAY,
            hour      = p[NOTIF_HOUR]    ?: 9,
            minute    = p[NOTIF_MINUTE]  ?: 0
        )
    }

    val displayCurrency: Flow<String> = context.dataStore.data.map { p ->
        p[DISPLAY_CURRENCY] ?: "INR"
    }

    val exchangeRateCache: Flow<Pair<Double, Long>> = context.dataStore.data.map { p ->
        val rate = (p[CACHED_RATE] ?: 83.5f).toDouble()
        val time = p[RATE_FETCH_TIME] ?: 0L
        Pair(rate, time)
    }

    val tithePrefs: Flow<TithePrefs> = context.dataStore.data.map { p ->
        TithePrefs(
            lastPercent          = (p[TITHE_PERCENT] ?: 10f).toDouble(),
            lastPaycheckAmount   = (p[TITHE_AMOUNT]  ?: 0f).toDouble(),
            lastPaycheckCurrency = p[TITHE_CURRENCY] ?: "INR"
        )
    }

    suspend fun saveNotificationPrefs(prefs: NotificationPrefs) {
        context.dataStore.edit {
            it[NOTIF_ENABLED] = prefs.enabled
            it[NOTIF_DAY]     = prefs.dayOfWeek
            it[NOTIF_HOUR]    = prefs.hour
            it[NOTIF_MINUTE]  = prefs.minute
        }
    }

    suspend fun saveDisplayCurrency(currency: String) {
        context.dataStore.edit { it[DISPLAY_CURRENCY] = currency }
    }

    suspend fun saveExchangeRate(rate: Double) {
        context.dataStore.edit {
            it[CACHED_RATE]     = rate.toFloat()
            it[RATE_FETCH_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun saveTithePrefs(prefs: TithePrefs) {
        context.dataStore.edit {
            it[TITHE_PERCENT]   = prefs.lastPercent.toFloat()
            it[TITHE_AMOUNT]    = prefs.lastPaycheckAmount.toFloat()
            it[TITHE_CURRENCY]  = prefs.lastPaycheckCurrency
        }
    }
}
