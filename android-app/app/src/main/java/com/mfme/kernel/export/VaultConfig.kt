package com.mfme.kernel.export

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vaultDataStore by preferencesDataStore(name = "vault_config")

/** Persists the SAF tree URI for the user's Obsidian vault. */
class VaultConfig(private val context: Context) {
    private val store = context.vaultDataStore
    private val KEY_TREE = stringPreferencesKey("tree_uri")

    val treeUri: Flow<Uri?> = store.data.map { prefs ->
        prefs[KEY_TREE]?.let { Uri.parse(it) }
    }

    suspend fun setTreeUri(uri: Uri?) {
        store.edit { prefs ->
            if (uri == null) prefs.remove(KEY_TREE) else prefs[KEY_TREE] = uri.toString()
        }
    }
}

