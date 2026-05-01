package com.enterprise.manufacturing.auth.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal val Context.authSessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auth_session",
)
