package com.mfme.kernel.cloud

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.cloudPrefsDataStore by preferencesDataStore(name = "cloud_prefs")

class CloudPreferences(private val context: Context) {
    private val store = context.cloudPrefsDataStore

    companion object Keys {
        val KEY_WIFI_ONLY = booleanPreferencesKey("drive_wifi_only")
    }

    data class Model(
        val wifiOnly: Boolean,
    )

    val flow: Flow<Model> = store.data.map { prefs ->
        Model(
            wifiOnly = prefs[KEY_WIFI_ONLY] ?: true,
        )
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        store.edit { it[KEY_WIFI_ONLY] = enabled }
    }

    fun currentWifiOnly(): Boolean = runBlocking { flow.first().wifiOnly }
}

