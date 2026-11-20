package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hassan_agro_prefs")

data class ServerSyncConfig(
    val serverBaseUrl: String = "",
    val apiToken: String = "",
    val wsUrl: String = "",
    val deviceId: String = "",
    val autoSyncEnabled: Boolean = false,
    val lastSyncTimestamp: Long = 0L
) {
    val isConfigured: Boolean
        get() = serverBaseUrl.trim().startsWith("http://") || serverBaseUrl.trim().startsWith("https://")
}

class PreferencesStore(private val context: Context) {
    private object Keys {
        val ACTIVE_BUSINESS_ID = stringPreferencesKey("active_business_id")
        val SERVER_BASE_URL = stringPreferencesKey("server_base_url")
        val API_TOKEN = stringPreferencesKey("api_token")
        val WS_URL = stringPreferencesKey("ws_url")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync_enabled")
        val LAST_SYNC_TS = longPreferencesKey("last_sync_timestamp")
    }

    val activeBusinessIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_BUSINESS_ID] ?: AppDatabase.DEFAULT_BUSINESS_ID
    }

    val serverConfigFlow: Flow<ServerSyncConfig> = context.dataStore.data.map { prefs ->
        ServerSyncConfig(
            serverBaseUrl = prefs[Keys.SERVER_BASE_URL] ?: "",
            apiToken = prefs[Keys.API_TOKEN] ?: "",
            wsUrl = prefs[Keys.WS_URL] ?: "",
            deviceId = prefs[Keys.DEVICE_ID] ?: "",
            autoSyncEnabled = prefs[Keys.AUTO_SYNC] ?: false,
            lastSyncTimestamp = prefs[Keys.LAST_SYNC_TS] ?: 0L
        )
    }

    suspend fun ensureDeviceId(): String {
        val existing = context.dataStore.data.first()[Keys.DEVICE_ID]
        if (!existing.isNullOrBlank()) return existing
        val generated = "HA-${UUID.randomUUID().toString().take(8).uppercase()}"
        context.dataStore.edit { it[Keys.DEVICE_ID] = generated }
        return generated
    }

    suspend fun setActiveBusinessId(businessId: String) {
        context.dataStore.edit { it[Keys.ACTIVE_BUSINESS_ID] = businessId }
    }

    suspend fun saveServerConfig(
        baseUrl: String,
        apiToken: String,
        wsUrl: String,
        autoSyncEnabled: Boolean
    ) {
        val normalizedUrl = baseUrl.trim().let { url ->
            if (url.isNotEmpty() && !url.endsWith("/")) "$url/" else url
        }
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_BASE_URL] = normalizedUrl
            prefs[Keys.API_TOKEN] = apiToken.trim()
            prefs[Keys.WS_URL] = wsUrl.trim()
            prefs[Keys.AUTO_SYNC] = autoSyncEnabled
        }
    }

    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC_TS] = timestamp }
    }
}
