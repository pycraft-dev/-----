package com.enterprise.manufacturing.core.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.enterprise.manufacturing.core.datastore.EnterpriseSettingsKeys
import com.enterprise.manufacturing.core.datastore.enterpriseSettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManifestUrlSettingsImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : UpdateManifestUrlSettings {

    private val store = context.enterpriseSettingsDataStore

    override fun observeOverride(): Flow<String> =
        store.data
            .map { prefs ->
                prefs[EnterpriseSettingsKeys.UPDATE_MANIFEST_URL_OVERRIDE]?.trim().orEmpty()
            }
            .distinctUntilChanged()

    override suspend fun resolveEffectiveUrl(buildTimeDefault: String): String {
        val def = buildTimeDefault.trim()
        val o =
            store.data
                .map { prefs ->
                    prefs[EnterpriseSettingsKeys.UPDATE_MANIFEST_URL_OVERRIDE]?.trim().orEmpty()
                }
                .first()
        return o.ifEmpty { def }
    }

    override suspend fun setManifestUrlOverride(raw: String) {
        val t = raw.trim()
        if (t.isEmpty()) {
            clearManifestUrlOverride()
            return
        }
        require(t.startsWith("https://")) { "manifest_https_only" }
        store.edit { prefs ->
            prefs[EnterpriseSettingsKeys.UPDATE_MANIFEST_URL_OVERRIDE] = t
        }
    }

    override suspend fun clearManifestUrlOverride() {
        store.edit { prefs ->
            prefs -= EnterpriseSettingsKeys.UPDATE_MANIFEST_URL_OVERRIDE
        }
    }
}
