package com.enterprise.manufacturing.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

internal val Context.enterpriseSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "enterprise_settings",
)

object EnterpriseSettingsKeys {
    /** HTTPS на `update.json`; если пусто в DataStore — используется значение из сборки. */
    val UPDATE_MANIFEST_URL_OVERRIDE = stringPreferencesKey("update_manifest_url_override")
}
