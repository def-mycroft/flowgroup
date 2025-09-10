package com.mfme.kernel.ping.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.locationPingDataStore by preferencesDataStore(name = "location_ping_prefs")

/**
 * DataStore-backed preferences for the Location Ping feature.
 * - Supports multiple recipients as a comma-separated E.164 list.
 * - Stores a simple send-history (epoch seconds, comma-separated) for rate limiting.
 */
class LocationPingPreferences(private val context: Context) {
    private val store = context.locationPingDataStore

    companion object Keys {
        val KEY_ENABLED = booleanPreferencesKey("ping_enabled")
        val KEY_TO_E164_LIST = stringPreferencesKey("ping_to_e164_list")
        val KEY_INTERVAL_MIN = intPreferencesKey("ping_interval_min")
        val KEY_SELF_TEST_E164 = stringPreferencesKey("ping_self_test_e164")
        val KEY_SENT_HISTORY = stringPreferencesKey("ping_sent_history")

        private const val DEFAULT_INTERVAL_MIN = 15
    }

    data class Model(
        val enabled: Boolean,
        val recipients: List<String>,
        val intervalMin: Int,
        val selfTestNumber: String?,
        val sentHistoryEpochSec: List<Long>,
    )

    val flow: Flow<Model> = store.data.map { prefs ->
        val listCsv = prefs[KEY_TO_E164_LIST] ?: ""
        val recipients = listCsv.split(',').mapNotNull { it.trim().ifBlank { null } }
        val histCsv = prefs[KEY_SENT_HISTORY] ?: ""
        val history = histCsv.split(',').mapNotNull { it.trim().toLongOrNull() }
        Model(
            enabled = prefs[KEY_ENABLED] ?: false,
            recipients = recipients,
            intervalMin = (prefs[KEY_INTERVAL_MIN] ?: DEFAULT_INTERVAL_MIN).coerceAtLeast(DEFAULT_INTERVAL_MIN),
            selfTestNumber = prefs[KEY_SELF_TEST_E164],
            sentHistoryEpochSec = history,
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        store.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setRecipients(list: List<String>) {
        val csv = list.joinToString(",")
        store.edit { it[KEY_TO_E164_LIST] = csv }
    }

    suspend fun setIntervalMin(min: Int) {
        store.edit { it[KEY_INTERVAL_MIN] = min }
    }

    suspend fun setSelfTestNumber(number: String?) {
        store.edit { prefs ->
            if (number.isNullOrBlank()) prefs.remove(KEY_SELF_TEST_E164) else prefs[KEY_SELF_TEST_E164] = number
        }
    }

    suspend fun updateSentHistory(historyEpochSec: List<Long>) {
        val csv = historyEpochSec.joinToString(",")
        store.edit { it[KEY_SENT_HISTORY] = csv }
    }
}

