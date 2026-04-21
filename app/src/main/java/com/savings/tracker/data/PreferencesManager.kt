package com.savings.tracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

// Extension property — creates a single DataStore instance for the app
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_settings")

/**
 * Holds the user's notification preferences.
 * Defaults to every Friday at 9:00 AM.
 */
data class NotificationPrefs(
    val enabled: Boolean = true,
    val dayOfWeek: Int = Calendar.FRIDAY,  // Calendar constants: SUN=1 ... SAT=7
    val hour: Int = 9,
    val minute: Int = 0
)

class PreferencesManager(private val context: Context) {

    companion object {
        private val NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
        private val NOTIF_DAY     = intPreferencesKey("notif_day")
        private val NOTIF_HOUR    = intPreferencesKey("notif_hour")
        private val NOTIF_MINUTE  = intPreferencesKey("notif_minute")
    }

    val notificationPrefs: Flow<NotificationPrefs> = context.dataStore.data.map { prefs ->
        NotificationPrefs(
            enabled   = prefs[NOTIF_ENABLED] ?: true,
            dayOfWeek = prefs[NOTIF_DAY]     ?: Calendar.FRIDAY,
            hour      = prefs[NOTIF_HOUR]    ?: 9,
            minute    = prefs[NOTIF_MINUTE]  ?: 0
        )
    }

    suspend fun save(prefs: NotificationPrefs) {
        context.dataStore.edit { settings ->
            settings[NOTIF_ENABLED] = prefs.enabled
            settings[NOTIF_DAY]     = prefs.dayOfWeek
            settings[NOTIF_HOUR]    = prefs.hour
            settings[NOTIF_MINUTE]  = prefs.minute
        }
    }
}
